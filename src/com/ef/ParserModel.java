package com.ef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParserModel
{

    private static final String DELIMITER = Pattern.quote("|");
    private static final int LOG_FILE_FIELDS_CNT = 5;
    public static final String DATE_PATTERN = "yyyy-MM-dd.HH:mm:ss";

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
                        "Unable to parse given date (" + p[0] + "). Please see LogEntry.formatter pattern for details",
                        e
                    );
                }

                item.ip = p[1];
                item.request = p[2].replace("\"", "");
                item.setStatus(Integer.valueOf(p[3]));
                item.userAgent = p[4].replace("\"", "");

                return item;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Unable to read file for given path (" + filePath + "). Please make sure it exists and is readable.", e
            );
        }

        return list;
    }

    /**
     * Find requests made by a given IP
     *
     * @param ip IP address to get requests of
     */
    public List<LogEntry> getRequestsByIP(String ip)
        throws SQLException
    {
        String query
            = "SELECT date"
            + ", INET6_NTOA(`ip`) AS ip"
            + ", request"
            + ", status"
            + ", user_agent"
            + " FROM ip_activity_logs"
            + " WHERE ip = INET6_ATON(?)"
            + " ORDER BY date ASC";

        List<LogEntry> list = new ArrayList<>();

        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);
            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, ip);

            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LogEntry item = new LogEntry();
                item.date = rs.getObject("date", LocalDateTime.class);
                item.ip = rs.getString("ip");
                item.request = rs.getString("request");
                item.setStatus(rs.getInt("status"));
                item.userAgent = rs.getString("user_agent");

                list.add(item);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new SQLException("Unable to select IPs at findIPs().", e);
        }

        return list;
    }

    /**
     * Save given list of LongEntry items to database
     *
     * @param list List of LogEntry items
     */
    public void saveLogEntries(List<LogEntry> list)
        throws SQLException
    {
        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);

            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ip_activity_logs (date, ip, request, status, user_agent) VALUES (?, INET6_ATON(?), ?, ?, ?)"
            );
            for (LogEntry item : list) {
                ps.setObject(1, item.getFormattedDate());
                ps.setString(2, item.ip);
                ps.setString(3, item.request);
                ps.setInt(4, item.getStatus());
                ps.setString(5, item.userAgent);

                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
            ps.close();
        } catch (SQLException e) {
            throw new SQLException("Unable to insert list of LogEntry items to database.", e);
        }
    }

    /**
     * Find IPs that made more than a certain number of requests for a given time period
     *
     * @param startDate Date range start
     * @param duration Is used to calculate date range end value
     * @param threshold Look for IPs having more requests than given threshold in calculated date range/interval
     * @return List of IPs having more than given threshold requests for calculated (based on StartDate and duration)
     * date interval
     */
    public Map<String, Integer> findAboveThresholdIPs(LocalDateTime startDate, Duration duration, int threshold)
        throws SQLException
    {
        LocalDateTime endDate = getEndDate(startDate, duration);

        String query
            = "SELECT INET6_NTOA(`ip`) AS ip"
            + ", COUNT(*) AS cnt"
            + " FROM ip_activity_logs"
            + " WHERE date >= ? AND date < ?"
            + " GROUP BY ip"
            + " HAVING cnt > ?";

        Map<String, Integer> map = new HashMap<>();

        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);
            PreparedStatement ps = conn.prepareStatement(query);

            ps.setObject(1, LogEntry.formatter.format(startDate));
            ps.setObject(2, LogEntry.formatter.format(endDate));
            ps.setInt(3, threshold);

            // process the results
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String ip = rs.getString("ip");
                int cnt = rs.getInt("cnt");
                map.put(ip, cnt);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new SQLException("Unable to select IPs at findIPs().", e);
        }

        return map;
    }

    /**
     * Log/save to DB blocked IPs from a given ipList with comments on why they are blocked
     *
     * @param ipList List of blocked IPs
     * @param startDate Starting date IPs were checked against
     * @param duration Duration type used
     * @param threshold Threshold used to identify if IP is to-be-blocked
     */
    public void logBlockedIPs(Map<String, Integer> ipList, LocalDateTime startDate, Duration duration, int threshold)
        throws SQLException
    {
        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);

            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO blocked_ips (ip, reason) VALUES (INET6_ATON(?), ?)"
            );

            LocalDateTime endDate = getEndDate(startDate, duration);
            for (Map.Entry<String, Integer> entry : ipList.entrySet()) {
                String ip = entry.getKey();
                int cnt = entry.getValue();
                String reason = duration.toString().toLowerCase() + " threshold (" + threshold + ") crossed "
                    + "(" + cnt + ") in the following dates range: "
                    + LogEntry.formatter.format(startDate) + " - " + LogEntry.formatter.format(endDate);

                ps.setString(1, ip);
                ps.setString(2, reason);

                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
            ps.close();
        } catch (SQLException e) {
            throw new SQLException("Unable to insert list of blocked IPs to database.", e);
        }
    }

    /**
     * Convert string date argument to LocalDateTime object
     *
     * @param dateStr String date having "yyyy-MM-dd.HH:mm:ss" format
     * @return LocalDateTime object
     */
    public LocalDateTime prepareDateArgument(String dateStr)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return LocalDateTime.parse(dateStr, formatter);
    }

    /**
     * Calculate end-date by given start-date and duration
     *
     * @param startDate
     * @param duration
     * @return
     */
    protected LocalDateTime getEndDate(LocalDateTime startDate, Duration duration)
    {
        return startDate.plusMinutes(duration.minutes);
    }
}
