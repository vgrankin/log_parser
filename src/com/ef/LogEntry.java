package com.ef;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry
{
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public LocalDateTime date;
    public String ip;
    public String request;
    public int status;
    public String userAgent;

    public void setDate(String dateStr)
    {        
        this.date = LocalDateTime.parse(dateStr, formatter);
    }

    @Override
    public String toString()
    {
        return "LogEntry{" + "date=" + formatter.format(date) + ", ip=" + ip + ", request=" + request + ", status=" + status + ", userAgent=" + userAgent + '}';
    }
}