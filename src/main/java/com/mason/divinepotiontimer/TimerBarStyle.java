package com.mason.divinepotiontimer;

/** Layout choices for the inventory time bar. */
public enum TimerBarStyle
{
    NONE("None"), VERTICAL("Vertical"), HORIZONTAL("Horizontal");
    private final String label;
    TimerBarStyle(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
