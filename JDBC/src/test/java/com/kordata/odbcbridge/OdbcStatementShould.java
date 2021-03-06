/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.kordata.odbcbridge;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OdbcStatementShould {
    private final ObjectMapper mapper;
    private final MockWebServer server;
    private final OdbcDriver driver;

    private Statement statement;
    private Connection connection;
    private String connectionString;

    private String buildConnectionString(HttpUrl baseUrl) {
        return "jdbc:odbcbridge://" + baseUrl.host() + ":" + baseUrl.port()
                + ";databaseName=a_database;user=aUser;password=aPassword;useSsl=false;";
    }

    public OdbcStatementShould() {
        mapper = new ObjectMapper();
        server = new MockWebServer();
        driver = new OdbcDriver();
    }

    private String buildQueryResponseString(int numResults) {
        ArrayNode node = mapper.createArrayNode();

        for (int i = 0; i < numResults; i++) {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("LastName", "LName" + i);
            obj.put("FirstName", "FName" + i);
            node.add(obj);
        }

        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            assumeNoException(e);
            return "";
        }
    }

    private String buildMutateResponseString(int rowsAffected) {
        ObjectNode node = mapper.createObjectNode();

        node.put("rowsAffected", rowsAffected);

        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            assumeNoException(e);
            return "";
        }
    }

    @Before
    public void setUp() throws IOException, SQLException {
        server.start();
        connectionString = buildConnectionString(server.url("/"));
    }

    @After
    public void tearDown() throws IOException, SQLException {
        if (statement != null) {
            statement.close();
        }

        if (connection != null) {
            connection.close();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void returnResultSetForQuery() {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setBody(buildQueryResponseString(5)));

        try {
            connection = driver.connect(connectionString, null);
            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM Customers");

            assertNotNull(rs);
        } catch (Exception e) {
            assumeNoException(e);
        }
    }

    @Test
    public void returnUpdateCountForMutate() {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setBody(buildMutateResponseString(5)));

        try {
            connection = driver.connect(connectionString, null);
            statement = connection.createStatement();

            int rowsAffected = statement.executeUpdate("UPDATE Customers SET LastName = 'test'");

            assertEquals(5, rowsAffected);
        } catch (Exception e) {
            assumeNoException(e);
        }
    }
}
