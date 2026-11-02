package com.mason.divinepotiontimer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches RuneLite with this project loaded as a local development plugin. */
public final class PotionTimerOverlayLauncher
{
    private PotionTimerOverlayLauncher() { }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(DivinePotionTimerPlugin.class);
        RuneLite.main(args);
    }
}
