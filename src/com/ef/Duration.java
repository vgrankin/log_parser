package com.ef;

/**
 * Contains possible values for "duration" to calculate range of dates to check log records against
 */
public enum Duration
{
    HOURLY(60 * 1),
    DAILY(60 * 24);

    public final int minutes;

    private Duration(int minutes)
    {
        this.minutes = minutes;
    }
}