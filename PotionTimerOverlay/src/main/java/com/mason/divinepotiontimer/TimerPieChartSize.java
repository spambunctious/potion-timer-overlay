package com.mason.divinepotiontimer;

/** Size and placement choices for the inventory pie timer. */
public enum TimerPieChartSize
{
    NONE("None"), SMALL("Small"), LARGE("Large");
    private final String label;
    TimerPieChartSize(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
