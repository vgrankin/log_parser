package com.ef;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser
{

    private static final String[] validOptions = new String[]{"accesslog", "startDate", "duration", "threshold"};
    private static final String[] requiredOptions = new String[]{"startDate", "duration", "threshold"};

    /**
     * Retrieve command-line args, check them and output above-threshold IPs (if any)
     *
     * @param args
     */
    static public void main(String[] args)
    {
        try {
            ParserModel parser = new ParserModel();

            Map<String, String> options = getOptions(args);

            // process received options
            
            if (hasRequiredOptionsMissing(options)) {
                throw new IllegalArgumentException(
                    "Please provide all required options: " + String.join(",", requiredOptions)
                );
            }

            String duration = options.get("duration");
            if (!isValidDuration(duration.toUpperCase())) {
                throw new IllegalArgumentException("Unknown duration: " + duration);
            }

            String startDateStr = options.get("startDate");
            LocalDateTime startDate = null;
            try {
                startDate = parser.prepareDateArgument(startDateStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Expected date pattern: " + ParserModel.DATE_PATTERN
                );
            }

            String thresholdStr = options.get("threshold");
            int threshold;
            try {
                threshold = Integer.parseInt(thresholdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Expected int value for threshold, actual: " + thresholdStr
                );
            }

            // take action based on processed options
            // if "accesslog" option is provided, also process log file
            if (options.containsKey("accesslog")) {
                List<LogEntry> list = parser.parse(options.get("accesslog"));
                parser.saveLogEntries(list);
            }
            
            duration = duration.toUpperCase();
            Map<String, Integer> result = parser.findAboveThresholdIPs(startDate, Duration.valueOf(duration), threshold);

            if (result.isEmpty()) {
                System.out.println("No above-threshold IPs for given arguments");
            } else {
                result.keySet().forEach((ip) -> {
                    System.out.println(ip);
                });

                parser.logBlockedIPs(result, startDate, Duration.valueOf(duration), threshold);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Extract required options from args to run parser methods
     *
     * @param args
     * @return
     */
    private static Map<String, String> getOptions(String[] args)
    {
        Map<String, String> options = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].charAt(1) == '-') {
                    if (args[i].length() < 3) {
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    }

                    String arg = args[i].substring(2, args[i].length());
                    String[] keyVal = arg.split("=");
                    if (keyVal.length != 2) {
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    }

                    String optionKey = keyVal[0];
                    String optionVal = keyVal[1];

                    if (!Arrays.asList(validOptions).contains(optionKey)) {
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    }

                    options.put(optionKey, optionVal);
                } else {
                    throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                }
            }
        }

        return options;
    }

    /**
     * Check if given string is a valid Duration
     * 
     * @param duration
     * @return true if is valid duration, false otherwise
     */
    private static boolean isValidDuration(String duration)
    {
        for (Duration d : Duration.values()) {
            if (d.name().equals(duration)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check given options map is missing any of the required options
     * 
     * @param options Options to check
     * @return true if has missing required-options, false otherwise
     */
    private static boolean hasRequiredOptionsMissing(Map<String, String> options)
    {        
        for (String option : requiredOptions) {
            if (!options.containsKey(option)) {
                return true;
            }
        }
        
        return false;
    }
}
