package com.ef;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogEntryTest
{

    private static TestUtils util;

    /**
     * Test of setDate method, of class LogEntry.
     */
    @Test
    public void testSetDate()
    {
        System.out.println("setDate");        
        LogEntry instance = new LogEntry();
        instance.setDate("2017-01-01 00:00:11.763");

        Assert.assertEquals("2017-01-01 00:00:11.763", util.formatter.format(instance.date));
    }

    /**
     * Test of toString method, of class LogEntry.
     */
    @Test
    public void testToString()
    {
        System.out.println("toString");
        LogEntry instance = new LogEntry();        

        instance.setDate("2017-01-01 00:00:11.763");
        instance.ip = "192.168.234.82";
        instance.request = "GET / HTTP/1.1";
        instance.status = 200;
        instance.userAgent = "swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0";
        
        String result = instance.toString();
        System.out.println(result);

        String expResult = "LogEntry{date=2017-01-01 00:00:11.763, ip=192.168.234.82, request=GET / HTTP/1.1, status=200, userAgent=swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0}";        
        assertEquals(expResult, result);
    }

}
