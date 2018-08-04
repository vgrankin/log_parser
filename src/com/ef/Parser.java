package com.ef;

import com.sun.prism.impl.Disposer.Record;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser
{

    private static final String DELIMITER = Pattern.quote("|");
    private static final int LOG_FILE_FIELDS_CNT = 5;

    private enum Duration
    {
        HOURLY, DAILY
    };

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

    /**
     * Save given list of LongEntry items to database
     *
     * @param list List of LogEntry items
     */
    public void saveLogEntries(List<LogEntry> list)
    {
        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);

            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ip_activity_logs (date, ip, request, status, user_agent) VALUES (?, ?, ?, ?, ?)"
            );
            for (LogEntry item : list) {
                
                ps.setObject(1, item.date);
                ps.setString(2, item.ip);
                ps.setString(3, item.request);
                ps.setInt(4, item.status);
                ps.setString(5, item.userAgent);
                
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();

        } catch (SQLException ex) {
            System.out.println("Unable to insert list of LogEntry items to database.");
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
