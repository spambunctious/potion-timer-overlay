package com.mason.divinepotiontimer;

import java.awt.Font;
import net.runelite.client.ui.FontManager;

/** RuneLite's built-in font choices for the inventory Potion Timer. */
public enum TimerFont
{
    RUNESCAPE_SMALL, RUNESCAPE_REGULAR, RUNESCAPE_BOLD, RUNELITE_DEFAULT, RUNELITE_DEFAULT_BOLD;

    Font getFont()
    {
        switch (this)
        {
            case RUNESCAPE_REGULAR: return FontManager.getRunescapeFont();
            case RUNESCAPE_BOLD: return FontManager.getRunescapeBoldFont();
            case RUNELITE_DEFAULT: return FontManager.getDefaultFont();
            case RUNELITE_DEFAULT_BOLD: return FontManager.getDefaultBoldFont();
            default: return FontManager.getRunescapeSmallFont();
        }
    }
}
