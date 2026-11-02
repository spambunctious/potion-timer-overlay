package com.mason.divinepotiontimer;

/** Immutable timer state used by Debug previews. */
final class TestTimerPreview
{
    private final int secondsRemaining;
    private final double remainingFraction;
    private final boolean venomPhase;

    TestTimerPreview(int seconds, double fraction, boolean venomPhase)
    {
        secondsRemaining = Math.max(0, seconds);
        remainingFraction = Math.max(0, Math.min(1, fraction));
        this.venomPhase = venomPhase;
    }

    int getSecondsRemaining() { return secondsRemaining; }
    double getRemainingFraction() { return remainingFraction; }
    boolean isVenomPhase() { return venomPhase; }
}
