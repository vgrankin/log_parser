package com.ef;

/**
 * Contains possible values for "duration" to calculate range of dates to check log records against
 */
public enum Duration
{
    HOURLY(60 * 1),
    DAILY(60 * 24);

    public final int minutes;
    
    /**
     * Construct Duration with given time in minutes
     * 
     * @param minutes How long this duration is
     */
    private Duration(int minutes)
    {
        this.minutes = minutes;
    }
}