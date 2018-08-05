package com.ef;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry
{
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public LocalDateTime date;
    public String ip;
    public String request;
    private int status;
    public String userAgent;

    public void setDate(String dateStr)
    {        
        this.date = LocalDateTime.parse(dateStr, formatter);
    }
    
    public String getFormattedDate()
    {
        return formatter.format(date);
    }
    
    public int getStatus()
    {
        return status;
    }
    
    public void setStatus(int status)
    {
        if (status < 1 || status > 1_000) {
            throw new IllegalArgumentException("HTTP Status is invalid.");
        }
        
        this.status = status;
    }

    @Override
    public String toString()
    {
        return "LogEntry{" + "date=" + getFormattedDate() + ", ip=" + ip + ", request=" + request + ", status=" + status + ", userAgent=" + userAgent + '}';
    }
}