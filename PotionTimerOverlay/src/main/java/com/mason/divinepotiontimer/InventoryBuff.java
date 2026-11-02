package com.mason.divinepotiontimer;

import java.util.Locale;
import java.util.regex.Pattern;

/** Consumable effects whose matching inventory items can display a timer. */
enum InventoryBuff
{
    ANTIPOISON, ANTIVENOM, IMBUED_HEART, SMELLING_SALTS, PRAYER_ENHANCE, OVERLOAD,
    PRAYER_REGENERATION, STAMINA, ANTIFIRE, SUPER_ANTIFIRE, GOADING_POTION,
    MENAPHITE_REMEDY, LIQUID_ADRENALINE, SURGE_POTION, MOONLIGHT_POTION;

    private static final Pattern DOSE = Pattern.compile("\\s*\\(\\d+\\)$");

    static InventoryBuff fromItemName(String name)
    {
        if (name == null) return null;
        String item = DOSE.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceFirst("").trim();
        if (item.startsWith("prayer enhance")) return PRAYER_ENHANCE;
        if (item.equals("smelling salts")) return SMELLING_SALTS;
        if (item.startsWith("overload") || item.startsWith("blighted overload")) return OVERLOAD;
        if (item.startsWith("prayer regeneration potion")) return PRAYER_REGENERATION;
        if (item.startsWith("extended stamina potion") || item.startsWith("stamina potion")) return STAMINA;
        if (item.startsWith("extended super antifire") || item.startsWith("super antifire")) return SUPER_ANTIFIRE;
        if (item.startsWith("extended antifire") || item.startsWith("antifire potion")) return ANTIFIRE;
        if (item.startsWith("goading potion")) return GOADING_POTION;
        if (item.startsWith("menaphite remedy")) return MENAPHITE_REMEDY;
        if (item.startsWith("liquid adrenaline")) return LIQUID_ADRENALINE;
        if (item.startsWith("surge potion")) return SURGE_POTION;
        if (item.startsWith("moonlight potion")) return MOONLIGHT_POTION;

        switch (item)
        {
            case "antipoison": case "superantipoison": case "antidote+": case "antidote++": case "sanfew serum":
                return ANTIPOISON;
            case "anti-venom": case "anti-venom+": case "extended anti-venom+":
            case "antivenom": case "antivenom+": case "extended antivenom+":
                return ANTIVENOM;
            case "imbued heart": case "saturated heart":
                return IMBUED_HEART;
            default:
                return null;
        }
    }
}
