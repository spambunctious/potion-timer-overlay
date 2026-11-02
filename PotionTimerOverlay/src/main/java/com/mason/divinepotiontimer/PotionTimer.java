package com.mason.divinepotiontimer;

import net.runelite.api.Constants;

/** Tracks one buff's active countdown and post-expiration icon state. */
final class PotionTimer
{
    private static final long TICK_MILLIS = Constants.GAME_TICK_LENGTH;
    private long expiresAtMillis, inactiveSinceMillis, durationMillis;
    private int lastRemainingTicks;

    void updateFromRemainingTicks(int ticks, long now)
    {
        if (ticks > 0)
        {
            if (expiresAtMillis <= now || ticks > lastRemainingTicks || durationMillis <= 0)
                durationMillis = ticks * TICK_MILLIS;
            expiresAtMillis = inactiveSinceMillis = now + ticks * TICK_MILLIS;
            lastRemainingTicks = ticks;
        }
        else if (expiresAtMillis > 0)
        {
            expire(now);
        }
    }

    void updateFromDurationMillis(long remaining, long now)
    {
        if (remaining > 0)
        {
            durationMillis = remaining;
            expiresAtMillis = inactiveSinceMillis = now + remaining;
            lastRemainingTicks = 0;
        }
        else if (expiresAtMillis > 0)
        {
            expire(now);
        }
    }

    private void expire(long now)
    {
        expiresAtMillis = durationMillis = 0;
        lastRemainingTicks = 0;
        inactiveSinceMillis = now;
    }

    boolean isActive(long now) { return expiresAtMillis > now; }

    int getSecondsRemaining(long now)
    {
        return isActive(now) ? (int) Math.max(1, (expiresAtMillis - now + 999) / 1000) : 0;
    }

    double getRemainingFraction(long now)
    {
        return isActive(now) && durationMillis > 0
            ? Math.max(0, Math.min(1, (double) (expiresAtMillis - now) / durationMillis)) : 0;
    }

    boolean isWithinResetDelay(long now, int delaySeconds)
    {
        return !isActive(now) && inactiveSinceMillis > 0 && delaySeconds > 0
            && now < inactiveSinceMillis + delaySeconds * 1000L;
    }
}
