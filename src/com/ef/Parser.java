package com.ef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser
{
    private static final String DELIMITER = Pattern.quote("|");
    private static final int LOG_FILE_FIELDS_CNT = 5;
    private enum Duration { HOURLY, DAILY };
    
    /**
     * Parses given log file into a list of LogEntry items
     * 
     * @param filePath Full path to log file to parse
     * @return A list of LogEntry items. Each item contains details about corresponding line of given log file.
     */
    public List<LogEntry> parse(String filePath)
    {
        List<LogEntry> list = new ArrayList<>();

        // reading log file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {            
            list = stream.map((String line) -> {
                String[] p = line.split(DELIMITER);
                
                if (p.length != LOG_FILE_FIELDS_CNT) {
                    throw new IllegalArgumentException(
                        "Parser expects that each line of log file contains exactly " 
                            + LOG_FILE_FIELDS_CNT + " fields. " + p.length + " field found."
                    );
                }
                
                LogEntry item = new LogEntry();
                                
                try {
                    item.setDate(p[0]);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                        "Unable to parse given date (" + p[0] + "). Please see LogEntry.formatter pattern for details"
                    );
                }       
                
                item.ip = p[1];
                item.request = p[2].replace("\"", "");
                item.status = Integer.valueOf(p[3]);
                item.userAgent = p[4].replace("\"", "");
                
                return item;
            }).collect(Collectors.toList());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        return list;
    }
    
    public void saveLogEntries(List<LogEntry> list)
    {
        
    }
}