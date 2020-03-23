package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2CacheDB {

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:";
    private static final String CACHE_FOLDER = "/cache/";

    private String openLHome = "";
    private String cacheName = "";

    public H2CacheDB(String cacheName) {
        this.cacheName = cacheName;
    }

    protected Connection getDBConnection() throws IOException {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION + openLHome + CACHE_FOLDER + cacheName, "", "");
            return dbConnection;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void setOpenLHome(String openLHome) {
        this.openLHome = openLHome;
    }
}
