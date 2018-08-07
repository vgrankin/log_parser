package com.ef;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest
{

    private static TestUtils util;

    /**
     * Test of parse method, of class ParserModel.
     */
    @Test
    public void testParse_whenCorrectTwoLogLineFileIsParsed_listOfTwoLogEntryItemWithDataIsReturned()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11.763|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\"\n"
            + "2017-01-01 00:00:21.164|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        ParserModel parser = new ParserModel();
        List<LogEntry> list = parser.parse(filePath);

        Assert.assertEquals(2, list.size());

        LogEntry item = list.get(0);

        Assert.assertEquals("2017-01-01 00:00:11.763", util.formatter.format(item.date));
        Assert.assertEquals("192.168.234.82", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.getStatus());
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", item.userAgent);

        item = list.get(1);

        Assert.assertEquals("2017-01-01 00:00:21.164", util.formatter.format(item.date));
        Assert.assertEquals("192.168.234.82", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.getStatus());
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", item.userAgent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParse_whenLogFileFieldIsMissing_IllegalArgumentExceptionIsThrown()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11.763|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        ParserModel parser = new ParserModel();
        parser.parse(filePath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParse_whenLogFileDateFormatIsIncorrect_IllegalArgumentExceptionIsThrown()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        ParserModel parser = new ParserModel();
        parser.parse(filePath);
    }

    @Test
    public void testSaveLogEntries_whenCalledWithListOfEntryLogEntityItems_DataIsSavedToMySQLDatabase() throws SQLException
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11.763|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\"\n"
            + "2017-01-01 00:00:21.164|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        ParserModel parser = new ParserModel();
        List<LogEntry> list = parser.parse(filePath);

        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");

        parser.saveLogEntries(list);

        String query
            = "SELECT date"
            + ", INET6_NTOA(`ip`) AS ip"
            + ", request"
            + ", status"
            + ", user_agent"
            + " FROM ip_activity_logs "
            + " ORDER BY id";
        ResultSet rs = util.readDbRows(query);

        rs.next();

        LocalDateTime date = rs.getObject("date", LocalDateTime.class);
        String ip = rs.getString("ip");
        String request = rs.getString("request");
        int status = rs.getInt("status");
        String userAgent = rs.getString("user_agent");

        Assert.assertEquals("2017-01-01 00:00:11.763", util.formatter.format(date));
        Assert.assertEquals("192.168.234.82", ip);
        Assert.assertEquals("GET / HTTP/1.1", request);
        Assert.assertEquals(200, status);
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", userAgent);

        rs.next();

        date = rs.getObject("date", LocalDateTime.class);
        ip = rs.getString("ip");
        request = rs.getString("request");
        status = rs.getInt("status");
        userAgent = rs.getString("user_agent");

        Assert.assertEquals("2017-01-01 00:00:21.164", util.formatter.format(date));
        Assert.assertEquals("192.168.234.82", ip);
        Assert.assertEquals("GET / HTTP/1.1", request);
        Assert.assertEquals(200, status);
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", userAgent);
    }

    @Test
    public void testFindIPs_whenThereAreAboveGivenHourlyThresholdIPs_CorrectIPsListAndCountIsReturned() throws SQLException
    {
        String ip = "192.168.70.134";
        LocalDateTime date = LocalDateTime.parse("2017-01-01 13:00:00.000", LogEntry.formatter);

        List<LogEntry> list = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            LogEntry item = new LogEntry();
            item.setDate(util.formatter.format(date));
            item.ip = ip;
            item.request = "GET / HTTP/1.1";
            item.setStatus(200);
            item.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";

            list.add(item);

            date = date.plusSeconds(5);
        }

        // explicitly add item/entry out of given range
        LogEntry item = new LogEntry();
        item.setDate("2017-01-01 14:00:00.000");
        item.ip = "192.168.70.134";
        item.request = "GET / HTTP/1.1";
        item.setStatus(200);
        item.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
        list.add(item);

        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");

        ParserModel parser = new ParserModel();
        parser.saveLogEntries(list);

        LocalDateTime startDate = parser.prepareDateArgument("2017-01-01.13:00:00");
                
        Map<String, Integer> result = parser.findAboveThresholdIPs(startDate, Duration.HOURLY, 100);

        Assert.assertEquals(1, result.size());

        
        Assert.assertTrue(result.containsKey(ip));        
                
        int foundCnt = result.get(ip);
        Assert.assertEquals(200, foundCnt); // make sure out-of-range item is NOT counted since range is HOURLY
        
        result = parser.findAboveThresholdIPs(startDate, Duration.DAILY, 100);

        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.containsKey(ip));

        foundCnt = result.get(ip);
        Assert.assertEquals(201, foundCnt); // now 201 is correct since range is DAILY
    }

    @Test
    public void testFindIPs_whenThereAreAboveGivenDailyThresholdIPs_CorrectIPsListAndCountIsReturned() throws SQLException
    {
        String ipFirst = "1.1.1.1";
        String ipSecond = "2.2.2.2";

        List<LogEntry> list = new ArrayList<>();

        for (String ip : new String[]{ipFirst, ipSecond}) {
            LocalDateTime date = LocalDateTime.parse("2017-01-01 13:00:00.000", LogEntry.formatter);
            for (int i = 0; i < 200; i++) {
                LogEntry item = new LogEntry();
                item.setDate(util.formatter.format(date));
                item.ip = ip;
                item.request = "GET / HTTP/1.1";
                item.setStatus(200);
                item.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";

                list.add(item);

                date = date.plusMinutes(5);
            }
        }

        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");

        ParserModel parser = new ParserModel();
        parser.saveLogEntries(list);

        LocalDateTime startDate = parser.prepareDateArgument("2017-01-01.13:00:00");
        
        // "hourly" should NOT work because threshold is too high
        Map<String, Integer> result = parser.findAboveThresholdIPs(startDate, Duration.HOURLY, 100);
        Assert.assertEquals(0, result.size());
        
        // now should work ("daily") because threshold is OK, there are above threshold IPs
        result = parser.findAboveThresholdIPs(startDate, Duration.DAILY, 100);

        Assert.assertEquals(2, result.size());
        
        Assert.assertTrue(result.containsKey(ipFirst));        
                
        int foundCnt = result.get(ipFirst);        
        Assert.assertEquals(200, foundCnt);
        
        Assert.assertTrue(result.containsKey(ipSecond));

        foundCnt = result.get(ipSecond);
        Assert.assertEquals(200, foundCnt);        
    }

    @Test
    public void testFindIPs_whenCalledWithThresholdAboveExistingMaxThresholdInDB_EmptyListIsReturned() throws SQLException
    {
        String ip = "192.168.70.134";
        LocalDateTime date = LocalDateTime.parse("2017-01-01 13:00:00.000", LogEntry.formatter);

        List<LogEntry> list = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            LogEntry item = new LogEntry();
            item.setDate(util.formatter.format(date));
            item.ip = ip;
            item.request = "GET / HTTP/1.1";
            item.setStatus(200);
            item.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";

            list.add(item);

            date = date.plusSeconds(5);
        }

        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");

        ParserModel parser = new ParserModel();
        parser.saveLogEntries(list);

        LocalDateTime startDate = parser.prepareDateArgument("2017-01-01.13:00:00");

        // there should be no records with above given threshold cnt for given dates range (1 h from startDate)
        int threshold = 1;
        Map<String, Integer> result = parser.findAboveThresholdIPs(startDate, Duration.HOURLY, threshold);

        Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void testGetRequestsByIP_whenExistingIPIsGiven_CorrectLogEntryListIsReturned() throws SQLException
    {
        String ipFirst = "1.1.1.1";
        String ipSecond = "2.2.2.2";

        List<LogEntry> list = new ArrayList<>();

        for (String ip : new String[]{ipFirst, ipSecond}) {
            LocalDateTime date = LocalDateTime.parse("2017-01-01 13:00:00.164", LogEntry.formatter);
            for (int i = 0; i < 10; i++) {
                LogEntry item = new LogEntry();
                item.setDate(util.formatter.format(date));
                item.ip = ip;
                item.request = "GET / HTTP/1.1";
                item.setStatus(200);
                item.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";

                list.add(item);

                date = date.plusMinutes(5);
            }
        }

        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");

        ParserModel parser = new ParserModel();
        parser.saveLogEntries(list);
        
        List<LogEntry> result = parser.getRequestsByIP(ipFirst);        
        Assert.assertEquals(10, result.size());
        
        LogEntry item = result.get(9);
        Assert.assertEquals("2017-01-01 13:45:00.164", item.getFormattedDate());
        Assert.assertEquals("1.1.1.1", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.getStatus());
        Assert.assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36", item.userAgent);        
        
        result = parser.getRequestsByIP(ipSecond);
        Assert.assertEquals(10, result.size());
        
        item = result.get(9);
        Assert.assertEquals("2017-01-01 13:45:00.164", item.getFormattedDate());
        Assert.assertEquals("2.2.2.2", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.getStatus());
        Assert.assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36", item.userAgent);        
        
        result = parser.getRequestsByIP(ipSecond);
        Assert.assertEquals(10, result.size());
    }
    
    @Test
    public void testLogBlockedIPs_givenListOfBlockedIPs_IPsWithCorrectBlockReasonAreSavedToDb() throws SQLException
    {
        String ipFirst = "1.1.1.1";
        int ipFirstCnt = 211;        
        
        String ipSecond = "2.2.2.2";
        int ipSecondCnt = 800;
        
        Map<String, Integer> blockedIps = new HashMap<>();
        blockedIps.put(ipFirst, ipFirstCnt);
        blockedIps.put(ipSecond, ipSecondCnt);
        
        LocalDateTime startDate = LocalDateTime.parse("2017-01-01 13:00:00.164", LogEntry.formatter);                        
        
        // check hourly duration
        
        // clear all table records
        util.executeQuery("TRUNCATE TABLE blocked_ips");                
        
        Duration duration = Duration.HOURLY;
        int threshold = 200;
        
        ParserModel parser = new ParserModel();
        parser.logBlockedIPs(blockedIps, startDate, duration, threshold);
        
        String query
            = "SELECT created_at"
            + ", INET6_NTOA(`ip`) AS ip"
            + ", reason"
            + " FROM blocked_ips"
            + " ORDER BY id";

        ResultSet rs = util.readDbRows(query);                        
        
        rs.next();

        LocalDateTime endDate = parser.getEndDate(startDate, duration);
        String reason = duration.toString().toLowerCase() + " threshold (" + threshold + ") crossed "
            + "(" + ipFirstCnt + ") in the following dates range: " 
            + LogEntry.formatter.format(startDate) + " - " + LogEntry.formatter.format(endDate);        
        
        Assert.assertEquals(ipFirst, rs.getString("ip"));
        Assert.assertEquals(reason, rs.getString("reason"));
        
        // check daily duration
        
        // clear all table records
        util.executeQuery("TRUNCATE TABLE blocked_ips");             
        
        duration = Duration.DAILY;
        threshold = 200;
        
        parser = new ParserModel();
        parser.logBlockedIPs(blockedIps, startDate, duration, threshold);
        
        query
            = "SELECT created_at"
            + ", INET6_NTOA(`ip`) AS ip"
            + ", reason"
            + " FROM blocked_ips"
            + " ORDER BY id";

        rs = util.readDbRows(query);                        
        
        rs.next();

        endDate = parser.getEndDate(startDate, duration);
        reason = duration.toString().toLowerCase() + " threshold (" + threshold + ") crossed "
            + "(" + ipFirstCnt + ") in the following dates range: " 
            + LogEntry.formatter.format(startDate) + " - " + LogEntry.formatter.format(endDate);        
        
        Assert.assertEquals(ipFirst, rs.getString("ip"));
        Assert.assertEquals(reason, rs.getString("reason"));
    }
}
