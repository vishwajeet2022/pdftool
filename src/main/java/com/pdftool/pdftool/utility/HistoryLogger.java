package com.pdftool.pdftool.utility;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class HistoryLogger {

    private static final String LOG_FILE_PATH = "user-history-log.txt";

    public static void log(String username, String action, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().toString();
            writer.write(String.format("[%s] User: %s | Action: %s | File: %s%n",
                    timestamp, username, action, filename));
        } catch (IOException e) {
            System.err.println("Failed to write to history log: " + e.getMessage());
        }
    }
}
