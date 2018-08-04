package com.ef;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest
{

    private static TestUtils util;

    /**
     * Test of parse method, of class Parser.
     */
    @Test
    public void testParse_whenCorrectTwoLogLineFileIsParsed_listOfTwoLogEntryItemWithDataIsReturned()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11.763|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\"\n"
            + "2017-01-01 00:00:21.164|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        Parser parser = new Parser();
        List<LogEntry> list = parser.parse(filePath);

        Assert.assertEquals(2, list.size());

        LogEntry item = list.get(0);

        Assert.assertEquals("2017-01-01 00:00:11.763", util.formatter.format(item.date));
        Assert.assertEquals("192.168.234.82", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.status);
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", item.userAgent);

        item = list.get(1);

        Assert.assertEquals("2017-01-01 00:00:21.164", util.formatter.format(item.date));
        Assert.assertEquals("192.168.234.82", item.ip);
        Assert.assertEquals("GET / HTTP/1.1", item.request);
        Assert.assertEquals(200, item.status);
        Assert.assertEquals("swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0", item.userAgent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParse_whenLogFileFieldIsMissing_IllegalArgumentExceptionIsThrown()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11.763|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        Parser parser = new Parser();
        parser.parse(filePath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParse_whenLogFileDateFormatIsIncorrect_IllegalArgumentExceptionIsThrown()
    {
        File tmpFile = util.prepareTestFile(
            "2017-01-01 00:00:11|192.168.234.82|\"GET / HTTP/1.1\"|200|\"swcd (unknown version) CFNetwork/808.2.16 Darwin/15.6.0\""
        );
        String filePath = tmpFile.getAbsolutePath();

        Parser parser = new Parser();
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

        Parser parser = new Parser();
        List<LogEntry> list = parser.parse(filePath);
        
        // clear all table records
        util.executeQuery("TRUNCATE TABLE ip_activity_logs");
        
        parser.saveLogEntries(list);
    }
}
