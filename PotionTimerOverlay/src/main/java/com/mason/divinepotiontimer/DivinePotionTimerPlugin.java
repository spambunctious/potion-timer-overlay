package com.mason.divinepotiontimer;

import com.google.inject.Provides;
import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Potion Timer Overlay",
    description = "Displays Potion Timer overlays for potions, consumable buffs, poison protection, anti-venom, and heart effects.",
    tags = {"divine", "potion", "antipoison", "antivenom", "overload", "prayer", "salts", "heart", "timer", "inventory", "buff", "overlay"}
)
public class DivinePotionTimerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DivinePotionTimerOverlay overlay;

    private static final int POISON_TICK_LENGTH = 30;
    // Antivenom provides a venom-immunity phase followed by 38 poison ticks.
    private static final int ANTIVENOM_POISON_PHASE_TICKS = 38 * POISON_TICK_LENGTH;
    private static final int OVERLOAD_TICK_LENGTH = 25;
    private static final int ANTIFIRE_TICK_LENGTH = 30;
    private static final int SUPER_ANTIFIRE_TICK_LENGTH = 20;
    private static final long PRAYER_ENHANCE_DURATION_MILLIS = 290_000L;
    private static final long LIQUID_ADRENALINE_DURATION_MILLIS = 150_000L;

    private final Map<DivinePotion, PotionTimer> timers = new EnumMap<>(DivinePotion.class);
    private final Map<InventoryBuff, PotionTimer> inventoryBuffTimers = new EnumMap<>(InventoryBuff.class);
    private final PotionTimer combinedAntivenomTimer = new PotionTimer();
    private static final long TEST_ANTIVENOM_VENOM_PHASE_MILLIS = 60L * 1000L;
    private static final long TEST_ANTIVENOM_POISON_PHASE_MILLIS = 120L * 1000L;
    private static final long TEST_ANTIVENOM_CYCLE_MILLIS =
        TEST_ANTIVENOM_VENOM_PHASE_MILLIS + TEST_ANTIVENOM_POISON_PHASE_MILLIS;
    private static final long TEST_POTION_DURATION_MILLIS = 5 * 60_000L;

    private long testTimerExpiresAtMillis;
    private long testAntivenomCycleStartedAtMillis;
    private int nextPoisonTick = -1;
    private int nextOverloadRefreshTick = -1;
    private int activeOverloadVarbitId = -1;
    private int nextAntifireTick = -1;
    private int nextSuperAntifireTick = -1;

    @Provides
    DivinePotionTimerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DivinePotionTimerConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            synchronizeTimersWithGame();
        }
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        overlay.clearImageCache();
        resetSession();
    }

    private void resetSession()
    {
        timers.clear();
        inventoryBuffTimers.clear();
        combinedAntivenomTimer.updateFromRemainingTicks(0, System.currentTimeMillis());
        testTimerExpiresAtMillis = testAntivenomCycleStartedAtMillis = 0L;
        nextPoisonTick = nextOverloadRefreshTick = activeOverloadVarbitId = -1;
        nextAntifireTick = nextSuperAntifireTick = -1;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            resetSession();
        }
        else if (event.getGameState() == GameState.LOGGED_IN)
        {
            synchronizeTimersWithGame();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        int changedVarbitId = event.getVarbitId();
        int changedValue = event.getValue();
        long now = System.currentTimeMillis();

        if (DivinePotion.fromVarbit(changedVarbitId) != null)
        {
            // Read all eight values together. Some potions (for example,
            // super combat) also set the component stat varbits. The priority
            // checks below mirror RuneLite's own handling so one sip does not
            // create several duplicate timers for the same combined effect.
            synchronizeDivineTimers(changedVarbitId, changedValue);
        }

        if (changedVarbitId == VarbitID.IMBUED_HEART_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.IMBUED_HEART,
                Math.max(0, changedValue) * 10, now);
        }

        if (event.getVarpId() == VarPlayerID.POISON)
        {
            updateAntipoisonTimer(changedValue, now);
        }

        if (changedVarbitId == VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.SMELLING_SALTS,
                Math.max(0, changedValue) * 25, now);
        }

        if (changedVarbitId == VarbitID.STATRENEWAL_POTION_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.MENAPHITE_REMEDY,
                Math.max(0, changedValue) * 25, now);
        }

        if (changedVarbitId == VarbitID.PRAYER_REGENERATION_POTION_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.PRAYER_REGENERATION,
                Math.max(0, changedValue) * 12, now);
        }

        if (changedVarbitId == VarbitID.GOADING_POTION_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.GOADING_POTION,
                Math.max(0, changedValue) * 6, now);
        }

        if (changedVarbitId == VarbitID.NZONE_OVERLOAD_POTION_EFFECTS
            || changedVarbitId == VarbitID.RAIDS_OVERLOAD_TIMER
            || changedVarbitId == VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS
            || changedVarbitId == VarbitID.RAIDS_CLIENT_INDUNGEON)
        {
            updateOverloadTimer(changedVarbitId, changedValue, now);
        }

        if (changedVarbitId == VarbitID.STAMINA_ACTIVE || changedVarbitId == VarbitID.STAMINA_DURATION)
        {
            updateStaminaTimer(changedVarbitId, changedValue, now);
        }

        if (changedVarbitId == VarbitID.ANTIFIRE_POTION)
        {
            updateAntifireTimer(InventoryBuff.ANTIFIRE, changedValue,
                ANTIFIRE_TICK_LENGTH, false, now);
        }

        if (changedVarbitId == VarbitID.SUPER_ANTIFIRE_POTION)
        {
            updateAntifireTimer(InventoryBuff.SUPER_ANTIFIRE, changedValue,
                SUPER_ANTIFIRE_TICK_LENGTH, true, now);
        }

        if (changedVarbitId == VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE)
        {
            updateInventoryBuffTimerDuration(InventoryBuff.LIQUID_ADRENALINE,
                changedValue > 0 ? LIQUID_ADRENALINE_DURATION_MILLIS : 0L, now);
        }

        if (changedVarbitId == VarbitID.SURGE_POTION_TIMER)
        {
            updateInventoryBuffTimer(InventoryBuff.SURGE_POTION,
                Math.max(0, changedValue) * 10, now);
        }

        if (changedVarbitId == VarbitID.MOONLIGHT_POTION_TIME)
        {
            int moonlightValue = changedValue;
            // RuneLite accounts for a one-tick desync when moonlight potion
            // and divine defence are active together.
            if (client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME) == moonlightValue + 1)
            {
                moonlightValue++;
            }
            updateInventoryBuffTimer(InventoryBuff.MOONLIGHT_POTION, Math.max(0, moonlightValue), now);
        }

        if (changedVarbitId == VarbitID.RAIDS_CLIENT_INDUNGEON && changedValue == 0)
        {
            // Prayer Enhance only exists in the raid. Leaving the raid ends its effect.
            updateInventoryBuffTimerDuration(InventoryBuff.PRAYER_ENHANCE, 0L, now);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        String message = event.getMessage();
        if (message == null
            || (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE))
        {
            return;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        if (message.startsWith("You drink some of your") && normalized.contains("prayer enhance"))
        {
            // RuneLite's Timers & Buffs plugin uses the same game message and
            // a fixed 290-second duration for Prayer Enhance.
            updateInventoryBuffTimerDuration(InventoryBuff.PRAYER_ENHANCE,
                PRAYER_ENHANCE_DURATION_MILLIS, now);
        }
        else if (normalized.contains("your prayer enhance effect has worn off"))
        {
            updateInventoryBuffTimerDuration(InventoryBuff.PRAYER_ENHANCE, 0L, now);
        }
        else if (normalized.contains("reducing the energy cost of your special attacks"))
        {
            // Liquid adrenaline has a 150-second duration. Its active varbit
            // clears the timer at expiration; this chat trigger also catches
            // the sip in case the corresponding varbit is already non-zero.
            updateInventoryBuffTimerDuration(InventoryBuff.LIQUID_ADRENALINE,
                LIQUID_ADRENALINE_DURATION_MILLIS, now);
        }
    }

    private void synchronizeTimersWithGame()
    {
        synchronizeDivineTimers(-1, 0);
        synchronizeInventoryBuffTimers();
    }

    private void synchronizeInventoryBuffTimers()
    {
        long now = System.currentTimeMillis();
        int heartRemaining = Math.max(0, client.getVarbitValue(VarbitID.IMBUED_HEART_TIMER));
        updateInventoryBuffTimer(InventoryBuff.IMBUED_HEART, heartRemaining * 10, now);
        updateAntipoisonTimer(client.getVarpValue(VarPlayerID.POISON), now);

        updateInventoryBuffTimer(InventoryBuff.SMELLING_SALTS,
            Math.max(0, client.getVarbitValue(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER)) * 25, now);
        updateInventoryBuffTimer(InventoryBuff.MENAPHITE_REMEDY,
            Math.max(0, client.getVarbitValue(VarbitID.STATRENEWAL_POTION_TIMER)) * 25, now);
        updateInventoryBuffTimer(InventoryBuff.PRAYER_REGENERATION,
            Math.max(0, client.getVarbitValue(VarbitID.PRAYER_REGENERATION_POTION_TIMER)) * 12, now);
        updateInventoryBuffTimer(InventoryBuff.GOADING_POTION,
            Math.max(0, client.getVarbitValue(VarbitID.GOADING_POTION_TIMER)) * 6, now);
        updateOverloadTimer(-1, 0, now);
        updateStaminaTimer(-1, 0, now);
        updateAntifireTimer(InventoryBuff.ANTIFIRE,
            client.getVarbitValue(VarbitID.ANTIFIRE_POTION), ANTIFIRE_TICK_LENGTH, false, now);
        updateAntifireTimer(InventoryBuff.SUPER_ANTIFIRE,
            client.getVarbitValue(VarbitID.SUPER_ANTIFIRE_POTION), SUPER_ANTIFIRE_TICK_LENGTH, true, now);
        updateInventoryBuffTimer(InventoryBuff.SURGE_POTION,
            Math.max(0, client.getVarbitValue(VarbitID.SURGE_POTION_TIMER)) * 10, now);

        int moonlightValue = client.getVarbitValue(VarbitID.MOONLIGHT_POTION_TIME);
        if (client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME) == moonlightValue + 1)
        {
            moonlightValue++;
        }
        updateInventoryBuffTimer(InventoryBuff.MOONLIGHT_POTION, Math.max(0, moonlightValue), now);
        // Prayer Enhance is chat-message timed, so it is intentionally not
        // reconstructed from a full 290 seconds when the plugin starts mid-effect.
        // Liquid adrenaline is a boolean varbit; only a new activation can give
        // us an accurate 150-second start time.
    }

    private void updateInventoryBuffTimerDuration(InventoryBuff buff, long remainingMillis, long now)
    {
        PotionTimer timer = inventoryBuffTimers.computeIfAbsent(buff, ignored -> new PotionTimer());
        timer.updateFromDurationMillis(remainingMillis, now);
    }

    private void updateStaminaTimer(int changedVarbitId, int changedValue, long now)
    {
        int staminaActive = readVarbit(VarbitID.STAMINA_ACTIVE, changedVarbitId, changedValue);
        int staminaDuration = readVarbit(VarbitID.STAMINA_DURATION, changedVarbitId, changedValue);
        int remainingTicks = staminaActive == 1 ? Math.max(0, staminaDuration) * 10 : 0;
        updateInventoryBuffTimer(InventoryBuff.STAMINA, remainingTicks, now);
    }

    private void updateAntifireTimer(InventoryBuff buff, int remainingUnits,
        int tickInterval, boolean superAntifire, long now)
    {
        int tickCount = client.getTickCount();
        int nextTick = superAntifire ? nextSuperAntifireTick : nextAntifireTick;
        if (remainingUnits <= 0)
        {
            nextTick = -1;
            updateInventoryBuffTimer(buff, 0, now);
        }
        else
        {
            if (nextTick - tickCount <= 0)
            {
                nextTick = tickCount + tickInterval;
            }
            int ticksRemaining = nextTick - tickCount + (remainingUnits - 1) * tickInterval;
            updateInventoryBuffTimer(buff, Math.max(0, ticksRemaining), now);
        }

        if (superAntifire)
        {
            nextSuperAntifireTick = nextTick;
        }
        else
        {
            nextAntifireTick = nextTick;
        }
    }

    private void updateOverloadTimer(int changedVarbitId, int changedValue, long now)
    {
        int deadman = readVarbit(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS, changedVarbitId, changedValue);
        int raids = readVarbit(VarbitID.RAIDS_OVERLOAD_TIMER, changedVarbitId, changedValue);
        int nmz = readVarbit(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, changedVarbitId, changedValue);
        boolean inRaid = changedVarbitId == VarbitID.RAIDS_CLIENT_INDUNGEON
            ? changedValue == 1
            : client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
        int selectedVarbit = -1;
        int remainingUnits = 0;

        if (deadman > 0)
        {
            selectedVarbit = VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS;
            remainingUnits = deadman;
        }
        else if ((inRaid || changedVarbitId == VarbitID.RAIDS_OVERLOAD_TIMER) && raids > 0)
        {
            // Accept the raid overload varbit's own activation event even if
            // the in-dungeon flag is delivered a moment later.
            selectedVarbit = VarbitID.RAIDS_OVERLOAD_TIMER;
            remainingUnits = raids;
        }
        else if (nmz > 0)
        {
            selectedVarbit = VarbitID.NZONE_OVERLOAD_POTION_EFFECTS;
            remainingUnits = nmz;
        }

        if (remainingUnits <= 0)
        {
            nextOverloadRefreshTick = -1;
            activeOverloadVarbitId = -1;
            updateInventoryBuffTimer(InventoryBuff.OVERLOAD, 0, now);
            return;
        }

        int tickCount = client.getTickCount();
        if (selectedVarbit != activeOverloadVarbitId || nextOverloadRefreshTick - tickCount <= 0)
        {
            nextOverloadRefreshTick = tickCount + OVERLOAD_TICK_LENGTH;
            activeOverloadVarbitId = selectedVarbit;
        }

        int ticksRemaining = nextOverloadRefreshTick - tickCount
            + (remainingUnits - 1) * OVERLOAD_TICK_LENGTH;
        updateInventoryBuffTimer(InventoryBuff.OVERLOAD, Math.max(0, ticksRemaining), now);
    }

    private void synchronizeDivineTimers(int changedVarbitId, int changedValue)
    {
        final long now = System.currentTimeMillis();

        // Prefer the value carried by the event for the variable that just
        // changed. Read the other values from the client to resolve combined
        // divine effects against their current state.
        final int attack = readVarbit(VarbitID.DIVINEATTACK_POTION_TIME, changedVarbitId, changedValue);
        final int strength = readVarbit(VarbitID.DIVINESTRENGTH_POTION_TIME, changedVarbitId, changedValue);
        final int defence = readVarbit(VarbitID.DIVINEDEFENCE_POTION_TIME, changedVarbitId, changedValue);
        final int ranging = readVarbit(VarbitID.DIVINERANGE_POTION_TIME, changedVarbitId, changedValue);
        final int magic = readVarbit(VarbitID.DIVINEMAGIC_POTION_TIME, changedVarbitId, changedValue);
        final int combat = readVarbit(VarbitID.DIVINECOMBAT_POTION_TIME, changedVarbitId, changedValue);
        final int bastion = readVarbit(VarbitID.DIVINEBASTION_POTION_TIME, changedVarbitId, changedValue);
        final int battlemage = readVarbit(VarbitID.DIVINEBATTLEMAGE_POTION_TIME, changedVarbitId, changedValue);

        updateTimer(DivinePotion.SUPER_ATTACK, attack > 0 && attack > combat ? attack : 0, now);
        updateTimer(DivinePotion.SUPER_STRENGTH, strength > 0 && strength > combat ? strength : 0, now);
        updateTimer(DivinePotion.SUPER_DEFENCE,
            defence > 0 && defence > combat && defence > bastion && defence > battlemage ? defence : 0, now);
        updateTimer(DivinePotion.RANGING, ranging > 0 && ranging > bastion ? ranging : 0, now);
        updateTimer(DivinePotion.MAGIC, magic > 0 && magic > battlemage ? magic : 0, now);
        updateTimer(DivinePotion.SUPER_COMBAT, combat, now);
        updateTimer(DivinePotion.BASTION, bastion, now);
        updateTimer(DivinePotion.BATTLEMAGE, battlemage, now);
    }

    private int readVarbit(int varbitId, int changedVarbitId, int changedValue)
    {
        return varbitId == changedVarbitId ? changedValue : client.getVarbitValue(varbitId);
    }

    private void updateTimer(DivinePotion potion, int ticks, long now)
    {
        updateTimer(timers, potion, ticks, now);
    }

    PotionTimer getTimer(DivinePotion potion) { return timers.get(potion); }
    PotionTimer getTimer(InventoryBuff buff) { return inventoryBuffTimers.get(buff); }
    PotionTimer getCombinedAntivenomTimer() { return combinedAntivenomTimer; }

    private void updateInventoryBuffTimer(InventoryBuff buff, int ticks, long now)
    {
        updateTimer(inventoryBuffTimers, buff, ticks, now);
    }

    private static <K> void updateTimer(Map<K, PotionTimer> map, K key, int ticks, long now)
    {
        map.computeIfAbsent(key, ignored -> new PotionTimer()).updateFromRemainingTicks(ticks, now);
    }

    private void updateAntipoisonTimer(int poisonVarpValue, long now)
    {
        int tickCount = client.getTickCount();
        if (poisonVarpValue < 0)
        {
            if (nextPoisonTick - tickCount <= 0)
            {
                nextPoisonTick = tickCount + POISON_TICK_LENGTH;
            }

            int ticksUntilNextPoisonUpdate = nextPoisonTick - tickCount;
            int poisonTicksRemaining = 0;
            int venomTicksRemaining = 0;
            if (poisonVarpValue >= -38)
            {
                // The regular poison-immunity phase runs from -38 through -1.
                poisonTicksRemaining = ticksUntilNextPoisonUpdate
                    + Math.abs((poisonVarpValue + 1) * POISON_TICK_LENGTH);
                updateInventoryBuffTimer(InventoryBuff.ANTIPOISON,
                    Math.max(0, poisonTicksRemaining), now);
            }
            else
            {
                updateInventoryBuffTimer(InventoryBuff.ANTIPOISON, 0, now);
            }

            if (poisonVarpValue < -38)
            {
                // Values below -38 represent the separate venom-immunity phase.
                venomTicksRemaining = ticksUntilNextPoisonUpdate
                    + Math.abs((poisonVarpValue + 1 + 38) * POISON_TICK_LENGTH);
                updateInventoryBuffTimer(InventoryBuff.ANTIVENOM,
                    Math.max(0, venomTicksRemaining), now);
                // Include the full following poison-protection phase so this
                // timer counts down continuously across both phases.
                combinedAntivenomTimer.updateFromRemainingTicks(
                    Math.max(0, venomTicksRemaining + ANTIVENOM_POISON_PHASE_TICKS), now);
            }
            else
            {
                updateInventoryBuffTimer(InventoryBuff.ANTIVENOM, 0, now);
                combinedAntivenomTimer.updateFromRemainingTicks(Math.max(0, poisonTicksRemaining), now);
            }
        }
        else
        {
            nextPoisonTick = -1;
            updateInventoryBuffTimer(InventoryBuff.ANTIPOISON, 0, now);
            updateInventoryBuffTimer(InventoryBuff.ANTIVENOM, 0, now);
            combinedAntivenomTimer.updateFromRemainingTicks(0, now);
        }
    }

    /** A shared five-minute Potion Timer preview for every supported inventory item. */
    TestTimerPreview getTestPotionTimerPreview(boolean enabled, long nowMillis)
    {
        if (!enabled)
        {
            testTimerExpiresAtMillis = 0L;
            return null;
        }

        if (testTimerExpiresAtMillis <= nowMillis)
        {
            testTimerExpiresAtMillis = nowMillis + TEST_POTION_DURATION_MILLIS;
        }

        long remainingMillis = testTimerExpiresAtMillis - nowMillis;
        int secondsRemaining = (int) Math.max(1L, (remainingMillis + 999L) / 1000L);
        double remainingFraction = (double) remainingMillis / TEST_POTION_DURATION_MILLIS;
        return new TestTimerPreview(secondsRemaining, remainingFraction, false);
    }


    /**
     * Returns a repeating anti-venom preview: one minute of venom immunity
     * followed by two minutes of poison immunity. This makes the phase change
     * quick to test without consuming an in-game potion.
     */
    TestTimerPreview getTestAntivenomPreview(boolean enabled, long nowMillis)
    {
        if (!enabled)
        {
            testAntivenomCycleStartedAtMillis = 0L;
            return null;
        }

        if (testAntivenomCycleStartedAtMillis == 0L)
        {
            testAntivenomCycleStartedAtMillis = nowMillis;
        }

        long elapsed = Math.floorMod(nowMillis - testAntivenomCycleStartedAtMillis,
            TEST_ANTIVENOM_CYCLE_MILLIS);
        if (elapsed < TEST_ANTIVENOM_VENOM_PHASE_MILLIS)
        {
            long remaining = TEST_ANTIVENOM_VENOM_PHASE_MILLIS - elapsed;
            int seconds = (int) Math.max(1L, (remaining + 999L) / 1000L);
            return new TestTimerPreview(seconds,
                (double) remaining / TEST_ANTIVENOM_VENOM_PHASE_MILLIS, true);
        }

        long poisonElapsed = elapsed - TEST_ANTIVENOM_VENOM_PHASE_MILLIS;
        long remaining = TEST_ANTIVENOM_POISON_PHASE_MILLIS - poisonElapsed;
        int seconds = (int) Math.max(1L, (remaining + 999L) / 1000L);
        return new TestTimerPreview(seconds,
            (double) remaining / TEST_ANTIVENOM_POISON_PHASE_MILLIS, false);
    }
}
