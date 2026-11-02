package com.mason.divinepotiontimer;

import java.util.Locale;
import java.util.regex.Pattern;
import net.runelite.api.gameval.VarbitID;

/** The divine potion families and their game effect varbits. */
enum DivinePotion
{
    BASTION("divine bastion potion", VarbitID.DIVINEBASTION_POTION_TIME),
    BATTLEMAGE("divine battlemage potion", VarbitID.DIVINEBATTLEMAGE_POTION_TIME),
    MAGIC("divine magic potion", VarbitID.DIVINEMAGIC_POTION_TIME),
    RANGING("divine ranging potion", VarbitID.DIVINERANGE_POTION_TIME),
    SUPER_ATTACK("divine super attack potion", VarbitID.DIVINEATTACK_POTION_TIME),
    SUPER_COMBAT("divine super combat potion", VarbitID.DIVINECOMBAT_POTION_TIME),
    SUPER_DEFENCE("divine super defence potion", VarbitID.DIVINEDEFENCE_POTION_TIME),
    SUPER_STRENGTH("divine super strength potion", VarbitID.DIVINESTRENGTH_POTION_TIME);

    private static final Pattern DOSE = Pattern.compile("\\s*\\(\\d+\\)$");
    private final String itemName;
    private final int varbitId;

    DivinePotion(String itemName, int varbitId) { this.itemName = itemName; this.varbitId = varbitId; }

    static DivinePotion fromItemName(String name)
    {
        if (name == null) return null;
        String normalized = DOSE.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceFirst("").trim();
        for (DivinePotion potion : values()) if (potion.itemName.equals(normalized)) return potion;
        return null;
    }

    static DivinePotion fromVarbit(int id)
    {
        for (DivinePotion potion : values()) if (potion.varbitId == id) return potion;
        return null;
    }
}
