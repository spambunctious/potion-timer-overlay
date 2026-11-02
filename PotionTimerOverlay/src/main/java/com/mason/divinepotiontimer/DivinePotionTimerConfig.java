package com.mason.divinepotiontimer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("divinepotiontimer")
public interface DivinePotionTimerConfig extends Config
{
    @ConfigSection(name = "Visual Timers", description = "Choose how Potion Timer visuals look on inventory items.", position = 100)
    String visualTimersSection = "visualTimersSection";

    @ConfigSection(name = "Antipoison & Anti-venom", description = "Show poison and venom protection overlays on matching items.", position = 110)
    String antipoisonSection = "antipoisonSection";

    @ConfigSection(name = "Potions & Buffs", description = "Choose which potion, heart, and consumable effects to show in your inventory.", position = 120)
    String potionsBuffsSection = "potionsBuffsSection";

    @ConfigSection(name = "Debug", description = "Preview the Potion Timer and inventory icon effects without using an item.", position = 130)
    String testingSection = "testingSection";

    @ConfigItem(keyName = "showTimer", name = "Show numeric timer", position = 0,
    description = "Show or hide the remaining potion time as a number (mm:ss) beneath active inventory items.")
    default boolean showTimer() { return true; }

    @Range(min = 8, max = 24)
    @ConfigItem(keyName = "timerFontSize", name = "Timer text size", position = 1,
    description = "Choose the size of the text for the potion timer.")
    default int timerFontSize() { return 15; }

    @ConfigItem(keyName = "timerFont", name = "Timer font", position = 2,
    description = "Choose the typeface used for the potion timer.")
    default TimerFont timerFont() { return TimerFont.RUNESCAPE_SMALL; }

    @ConfigItem(keyName = "timerTextColor", name = "Timer text color", position = 3,
    description = "Choose the color used for the potion timer text.")
    default Color timerTextColor() { return Color.WHITE; }

    @Range(min = 0, max = 300)
    @ConfigItem(keyName = "blinkStartSeconds", name = "Near expiry warning", position = 4,
    description = "Choose when active icons start blinking in seconds remaining. Set to 0 to disable.")
    default int blinkStartSeconds() { return 10; }

    @Range(min = 0, max = 300)
    @ConfigItem(keyName = "darkIconResetDelay", name = "Reset delay", position = 5,
    description = "Choose how long the white outline keeps blinking after an effect expires. Set to 0 to disable.")
    default int darkIconResetDelay() { return 30; }

    @ConfigItem(keyName = "pieChartSize", name = "Pie visual timer", position = 0, section = visualTimersSection,
    description = "Adds a pie timer visual on potion icon.")
    default TimerPieChartSize pieChartSize() { return TimerPieChartSize.SMALL; }

    @ConfigItem(keyName = "barTimerStyle", name = "Bar visual timer", position = 1, section = visualTimersSection,
    description = "Adds a bar timer visual on potion icon.")
    default TimerBarStyle barTimerStyle() { return TimerBarStyle.NONE; }

    @Range(min = 1, max = 12)
    @ConfigItem(keyName = "barWidth", name = "Bar thickness", position = 2, section = visualTimersSection,
    description = "Set the bar's thickness in pixels. This controls the vertical bar width and horizontal bar height.")
    default int barWidth() { return 4; }


    @Range(min = 0, max = 8)
    @ConfigItem(keyName = "borderWidth", name = "Border thickness", position = 3, section = visualTimersSection,
    description = "Set the outline thickness in pixels for both pie and bar visuals.")
    default int borderWidth() { return 1; }

    @ConfigItem(keyName = "timerBarColor", name = "Fill color", position = 4, section = visualTimersSection,
    description = "Choose the color of the filled portion of the pie and bar visuals.")
    default Color timerBarColor() { return Color.WHITE; }

    @ConfigItem(keyName = "timerBackgroundColor", name = "Background color", position = 5, section = visualTimersSection,
    description = "Choose the color of the unfilled portion of the pie and bar visuals.")
    default Color timerBackgroundColor() { return Color.BLACK; }

    @ConfigItem(keyName = "borderColor", name = "Border color", position = 6, section = visualTimersSection,
    description = "Choose the outline color for the pie and bar visuals.")
    default Color borderColor() { return Color.WHITE; }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "timerVisualOpacity", name = "Visual timer opacity", position = 7, section = visualTimersSection,
    description = "Set the opacity of the pie and bar visuals from 0 (transparent) to 100 (fully opaque).")
    default int timerVisualOpacity() { return 100; }

    @ConfigItem(keyName = "showDivineTimer", name = "Show divine potion overlay", position = 0, section = potionsBuffsSection,
    description = "Show the remaining effect time on Divine Bastion, Battlemage, Magic, Ranging, Super Attack, Super Combat, Super Defence, and Super Strength potions.")
    default boolean showDivineTimer() { return true; }

    @ConfigItem(keyName = "showAntipoisonTimer", name = "Show antipoison overlay", position = 0, section = antipoisonSection,
    description = "Show poison-protection time on Antipoison, Superantipoison, Antidote+, Antidote++, and Sanfew serum.")
    default boolean showAntipoisonTimer() { return true; }

    @ConfigItem(keyName = "showAntivenomTimer", name = "Show anti-venom overlay", position = 1, section = antipoisonSection,
    description = "Show venom AND poison-protection time on Anti-venom, Anti-venom+, and Extended anti-venom+.")
    default boolean showAntivenomTimer() { return true; }

    @ConfigItem(keyName = "showAntivenomPhaseTimer", name = "Show venom phase first", position = 2, section = antipoisonSection,
    description = "Show the venom-protection time first, then switch to the remaining poison-protection time. Turn off to show both phases combined in one countdown.")
    default boolean showAntivenomPhaseTimer() { return true; }

    @ConfigItem(keyName = "useCustomColorForVenom", name = "Use custom venom text color", position = 3, section = antipoisonSection,
    description = "Use the selected venom text color instead of the general Potion Timer text color.")
    default boolean useCustomColorForVenom() { return true; }

    @ConfigItem(keyName = "venomTimerTextColor", name = "Venom text color", position = 4, section = antipoisonSection,
    description = "Choose the text color used during the venom-protection phase. Defaults to bright green.")
    default Color venomTimerTextColor() { return new Color(110, 255, 110); }

    @ConfigItem(keyName = "showSmellingSalts", name = "Show smelling salts overlay", position = 2, section = potionsBuffsSection,
    description = "Show the remaining Smelling Salts effect time on matching inventory items.")
    default boolean showSmellingSalts() { return true; }

    @ConfigItem(keyName = "showPrayerEnhance", name = "Show prayer enhance overlay", position = 3, section = potionsBuffsSection,
    description = "Show the remaining Prayer Enhance effect time on matching vials.")
    default boolean showPrayerEnhance() { return true; }

    @ConfigItem(keyName = "showOverloads", name = "Show overload overlay", position = 4, section = potionsBuffsSection,
    description = "Show the remaining effect time on regular, raid, and blighted overloads.")
    default boolean showOverloads() { return true; }

    @ConfigItem(keyName = "showPrayerRegeneration", name = "Show prayer regeneration overlay", position = 5, section = potionsBuffsSection,
    description = "Show the remaining Prayer regeneration effect time on matching potions.")
    default boolean showPrayerRegeneration() { return true; }

    @ConfigItem(keyName = "showStaminaPotion", name = "Show stamina overlay", position = 6, section = potionsBuffsSection,
    description = "Show the remaining Stamina effect time on matching potions.")
    default boolean showStaminaPotion() { return true; }

    @ConfigItem(keyName = "showAntifirePotions", name = "Show antifire overlay", position = 7, section = potionsBuffsSection,
    description = "Show the remaining effect time on Antifire, Extended Antifire, Super Antifire, and Extended Super Antifire potions.")
    default boolean showAntifirePotions() { return true; }

    @ConfigItem(keyName = "showGoadingPotion", name = "Show goading potion overlay", position = 8, section = potionsBuffsSection,
    description = "Show the remaining Goading potion effect time on matching potions.")
    default boolean showGoadingPotion() { return true; }

    @ConfigItem(keyName = "showMenaphiteRemedy", name = "Show menaphite remedy overlay", position = 9, section = potionsBuffsSection,
    description = "Show the remaining Menaphite Remedy effect time on matching potions.")
    default boolean showMenaphiteRemedy() { return false; }

    @ConfigItem(keyName = "showLiquidAdrenaline", name = "Show Liquid adrenaline overlay", position = 10, section = potionsBuffsSection,
    description = "Show the remaining Liquid Adrenaline effect time on matching vials.")
    default boolean showLiquidAdrenaline() { return true; }

    @ConfigItem(keyName = "showSurgePotion", name = "Show surge potion overlay", position = 11, section = potionsBuffsSection,
    description = "Show the remaining Surge potion effect time on matching potions.")
    default boolean showSurgePotion() { return true; }

    @ConfigItem(keyName = "showMoonlightPotion", name = "Show moonlight potion overlay", position = 12, section = potionsBuffsSection,
    description = "Show the remaining Moonlight potion effect time on matching potions.")
    default boolean showMoonlightPotion() { return false; }

    @ConfigItem(keyName = "showImbuedHeartTimer", name = "Show imbued/saturated heart overlay", position = 1, section = potionsBuffsSection,
    description = "Show the cooldown for the Imbued Heart and Saturated Heart.")
    default boolean showImbuedHeartTimer() { return false; }

    @ConfigItem(keyName = "testTimerDisplay", name = "Test potion timer", position = 0, section = testingSection,
    description = "Preview a repeating five-minute Potion Timer on every supported potion, heart, and consumable item without using one. The separate anti-venom test still previews both protection phases.")
    default boolean testTimerDisplay() { return false; }

    @ConfigItem(
        // Reuse the previous darkening-test key so existing users keep the
        // test enabled state. The expired appearance is the normal-brightness
        // inventory sprite with its always-on pulsing white silhouette outline.
        keyName = "testDarkening",
        name = "Test expired potion",
        description = "Preview a normal-brightness potion with a pulsing white outline on every supported potion, poison, heart, and consumable item.",
        position = 1,
        section = testingSection
    )
    default boolean testExpiredPotion() { return false; }

    @ConfigItem(keyName = "testBlinkingAnimation", name = "Test near-expiration blink", position = 2, section = testingSection,
    description = "Preview only the near-expiration icon blink on every supported item. This does not create a Potion Timer or pie/bar visual.")
    default boolean testBlinkingAnimation() { return false; }

    @ConfigItem(keyName = "testAntivenomTimer", name = "Test anti-venom timer", position = 3, section = testingSection,
    description = "Preview venom-protection time followed by poison-protection time on anti-venom items.")
    default boolean testAntivenomTimer() { return false; }

}
