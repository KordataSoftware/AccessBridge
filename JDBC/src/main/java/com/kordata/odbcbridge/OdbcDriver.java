package com.kordata.odbcbridge;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class OdbcDriver implements Driver {
    private final BridgeAPI api;

    public OdbcDriver() {
        api = new BridgeAPI();
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        // Per the ODBC spec.
        if (!acceptsURL(url))
            return null;

        api.setURL(url);


        // Make sure we can actually open the DB
        if (!api.remoteIsReachable()) {
            throw new SQLException("Can't connect to remote server");
        }

        return new OdbcConnection(api);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (url == null)
            return false;

        return url.contains("jdbc:odbcbridge://");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (info == null) {
            info = new Properties();
        }

        DriverPropertyInfo dpi[] = {

        };

        return dpi;
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
