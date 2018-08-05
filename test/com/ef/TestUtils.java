package com.ef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestUtils
{

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static File prepareTestFile(String fileContent)
    {
        // Create temp file.
        File temp = null;
        try {
            temp = File.createTempFile("pattern", ".suffix");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(fileContent);

            out.close();

        } catch (IOException ex) {
            Logger.getLogger(TestUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return temp;
    }

    public static void executeQuery(String query)
        throws SQLException
    {
        PreparedStatement pstmtU = null;
        try {
            Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);

            pstmtU = conn.prepareStatement(query);
            pstmtU.executeUpdate();
        } finally {
            if (pstmtU != null) {
                pstmtU.close();
            }
        }
    }

    public static ResultSet readDbRows(String query)
        throws SQLException
    {
        Statement stmt = null;

        Connection conn = DriverManager.getConnection(Config.JDBC_URL, Config.DB_USERNAME, Config.DB_PASSWORD);
        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        return rs;
    }
}
