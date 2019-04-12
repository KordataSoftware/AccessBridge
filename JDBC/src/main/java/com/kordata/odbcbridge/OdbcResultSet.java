package com.kordata.odbcbridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OdbcResultSet implements ResultSet {
    private int cursorPosition = -1;
    private final ArrayNode resultArray;
    private final ArrayNode schemaArray;

    private ObjectNode currentRow;
    private List<String> fieldNames;

    private final OdbcResultSetMetaData metaData;

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return metaData;
    }

    private final OdbcStatement statement;

    @Override
    public Statement getStatement() throws SQLException {
        return statement;
    }

    private boolean closed = false;

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public String getCursorName() throws SQLException {
        return null;
    }

    @Override
    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    private boolean lastReadWasNull = false;

    @Override
    public boolean wasNull() throws SQLException {
        checkClosed();
        return lastReadWasNull;
    }

    public OdbcResultSet(OdbcStatement statement, ObjectNode responseObject) {
        this.statement = statement;

        this.resultArray = (ArrayNode) responseObject.get("results");
        this.schemaArray = (ArrayNode) responseObject.get("schema");

        metaData = new OdbcResultSetMetaData(schemaArray);
    }

    public static OdbcResultSet Empty() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode results = mapper.createArrayNode();
        ArrayNode schema = mapper.createArrayNode();
        ObjectNode response = mapper.createObjectNode();
        response.set("results", results);
        response.set("schema", schema);

        return new OdbcResultSet(null, response);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    void checkClosed() throws SQLException {

        if (closed) {
            throw new SQLException("ResultSet is closed");
        }

        statement.checkClosed();
    }

    private void updateCurrentRow() {
        if (cursorPosition >= 0 && cursorPosition < resultArray.size()) {
            currentRow = (ObjectNode) resultArray.get(cursorPosition);
            fieldNames = new ArrayList<>();
            currentRow.fieldNames().forEachRemaining(fieldNames::add);
        } else {
            currentRow = null;
        }
    }

    @Override
    public boolean next() throws SQLException {
        return relative(1);
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    private String getColumnLabel(int columnIndex) throws SQLException {
        if (columnIndex < 1 || columnIndex > fieldNames.size())
            throw new SQLException(columnIndex + " is out of range for the ResultSet.");

        return fieldNames.get(columnIndex - 1);
    }

    private JsonNode getColumn(String columnLabel) throws SQLException {
        if (!currentRow.has(columnLabel))
            throw new SQLException(columnLabel + " is not a valid column in the ResultSet.");

        JsonNode column = currentRow.get(columnLabel);

        if (column.isNull()) {
            lastReadWasNull = true;
        }

        return column;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        return column.asText();
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return false;

        return column.asBoolean();
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return (byte) column.asInt();
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return (short) column.asInt();
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return column.asInt();
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return column.asLong();
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return (float) column.asDouble();
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return 0;

        return column.asDouble();
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getColumn(columnLabel).decimalValue();
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            return new BigDecimal(column.asText());
        } catch (NumberFormatException e) {
            return column.decimalValue();
        }
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        if (column.isBinary()) {
            try {
                return column.binaryValue();
            } catch (IOException e) {
                throw new SQLException(e.getMessage());
            }
        }

        if (column.isTextual()) {
            return column.textValue().getBytes();
        }

        if (column.isDouble()) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putDouble(column.doubleValue());
            return buffer.array();
        }

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(column.asInt());
        return buffer.array();
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            LocalDate date = LocalDate.parse(column.asText());

            return Date.valueOf(date);
        } catch (DateTimeParseException e) {
            return new Date(0);
        }
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            LocalTime time = LocalTime.parse(column.asText());

            return Time.valueOf(time);
        } catch (DateTimeParseException e) {
            return new Time(0);
        }
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            Instant instant = Instant.parse(column.asText());
            return Timestamp.from(instant);
        } catch (DateTimeParseException e) {
            return new Timestamp(0);
        }
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        checkClosed();
        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            Instant instant = Instant.parse(column.asText());

            TimeZone tz = cal.getTimeZone();
            ZonedDateTime zdt = instant.atZone(tz.toZoneId());

            return Timestamp.valueOf(zdt.toLocalDateTime());
        } catch (DateTimeParseException e) {
            return new Timestamp(0);
        }
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        checkClosed();

        JsonNode node = getColumn(columnLabel);
        String odbcType = metaData.getOdbcType(fieldNames.indexOf(columnLabel));

        return JsonNodeExtensions.toObject(node, odbcType);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        JsonNode node = getColumn(columnLabel);

        Object returnValue;
        if (type == String.class) {
            returnValue = getString(columnLabel);
        } else if (type == Byte.class) {
            byte byteValue = getByte(columnLabel);
            returnValue = wasNull() ? null : byteValue;
        } else if (type == Short.class) {
            short shortValue = getShort(columnLabel);
            returnValue = wasNull() ? null : shortValue;
        } else if (type == Integer.class) {
            int intValue = getInt(columnLabel);
            returnValue = wasNull() ? null : intValue;
        } else if (type == Long.class) {
            long longValue = getLong(columnLabel);
            returnValue = wasNull() ? null : longValue;
        } else if (type == BigDecimal.class) {
            returnValue = getBigDecimal(columnLabel);
        } else if (type == Boolean.class) {
            boolean booleanValue = getBoolean(columnLabel);
            returnValue = wasNull() ? null : booleanValue;
        } else if (type == java.sql.Date.class) {
            returnValue = getDate(columnLabel);
        } else if (type == java.sql.Time.class) {
            returnValue = getTime(columnLabel);
        } else if (type == java.sql.Timestamp.class) {
            returnValue = getTimestamp(columnLabel);
        } else if (type == java.time.LocalDateTime.class || type == java.time.LocalDate.class
                || type == java.time.LocalTime.class) {
            java.sql.Timestamp ts = getTimestamp(columnLabel,
                    Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")));
            if (ts == null) {
                returnValue = null;
            } else {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(ts.toInstant(),
                        java.time.ZoneId.of("UTC"));
                if (type == java.time.LocalDateTime.class) {
                    returnValue = ldt;
                } else if (type == java.time.LocalDate.class) {
                    returnValue = ldt.toLocalDate();
                } else {
                    returnValue = ldt.toLocalTime();
                }
            }
        } else if (type == byte[].class) {
            returnValue = getBytes(columnLabel);
        } else if (type == Float.class) {
            float floatValue = getFloat(columnLabel);
            returnValue = wasNull() ? null : floatValue;
        } else if (type == Double.class) {
            double doubleValue = getDouble(columnLabel);
            returnValue = wasNull() ? null : doubleValue;
        } else {
            // if the type is not supported the specification says the should
            // a SQLException instead of SQLFeatureNotSupportedException
            throw new SQLException("Conversion not supported");
        }

        return type.cast(returnValue);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        checkClosed();

        JsonNode column = getColumn(columnLabel);

        if (wasNull())
            return null;

        try {
            return new URL(column.asText());
        } catch (MalformedURLException e) {
            throw new SQLException("Malformed URL: " + column.asText());
        }
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return getString(getColumnLabel(columnIndex));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return getBoolean(getColumnLabel(columnIndex));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return getByte(getColumnLabel(columnIndex));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return getShort(getColumnLabel(columnIndex));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return getInt(getColumnLabel(columnIndex));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return getLong(getColumnLabel(columnIndex));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return getFloat(getColumnLabel(columnIndex));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return getDouble(getColumnLabel(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return currentRow.get(fieldNames.get(columnIndex)).decimalValue();
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal(getColumnLabel(columnIndex));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return getBytes(getColumnLabel(columnIndex));
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        return getDate(getColumnLabel(columnIndex));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(getColumnLabel(columnIndex), cal);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        return getTime(getColumnLabel(columnIndex));
    }


    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(getColumnLabel(columnIndex), cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return getTimestamp(getColumnLabel(columnIndex));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(getColumnLabel(columnIndex), cal);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return getAsciiStream(getColumnLabel(columnIndex));
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return getUnicodeStream(getColumnLabel(columnIndex));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return getBinaryStream(getColumnLabel(columnIndex));
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return getCharacterStream(getColumnLabel(columnIndex));
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getObject(getColumnLabel(columnIndex));
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return getObject(getColumnLabel(columnIndex), type);
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(getColumnLabel(columnIndex), map);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return getRef(getColumnLabel(columnIndex));
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        return getBlob(getColumnLabel(columnIndex));
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        return getClob(getColumnLabel(columnIndex));
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return getArray(getColumnLabel(columnIndex));
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return getURL(getColumnLabel(columnIndex));
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return getRowId(getColumnLabel(columnIndex));
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return getNClob(getColumnLabel(columnIndex));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return getSQLXML(getColumnLabel(columnIndex));
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getNString(getColumnLabel(columnIndex));
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return getNCharacterStream(getColumnLabel(columnIndex));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (!currentRow.has(columnLabel))
            throw new SQLException(columnLabel + " is not in ResultSet");

        return fieldNames.indexOf(columnLabel) + 1;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return cursorPosition < 0;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return cursorPosition >= resultArray.size();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return cursorPosition == 0;
    }

    @Override
    public boolean isLast() throws SQLException {
        return cursorPosition == resultArray.size() - 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        cursorPosition = -1;
        updateCurrentRow();
    }

    @Override
    public void afterLast() throws SQLException {
        cursorPosition = resultArray.size();
        updateCurrentRow();
    }

    @Override
    public boolean first() throws SQLException {
        cursorPosition = 0;
        updateCurrentRow();

        return resultArray.size() > 0;
    }

    @Override
    public boolean last() throws SQLException {
        cursorPosition = resultArray.size() - 1;
        updateCurrentRow();

        return resultArray.size() > 0;
    }

    @Override
    public int getRow() throws SQLException {
        if (cursorPosition > 0 && cursorPosition < resultArray.size()) {
            return cursorPosition + 1;
        }

        return 0;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (row == 0) {
            beforeFirst();
            return false;
        }

        if (row > 0) {
            cursorPosition = row - 1;
        } else {
            cursorPosition = resultArray.size() - 1;
        }

        updateCurrentRow();

        return cursorPosition >= 0 && cursorPosition < resultArray.size();
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        cursorPosition += rows;
        updateCurrentRow();

        return cursorPosition >= 0 && cursorPosition < resultArray.size();
    }

    @Override
    public boolean previous() throws SQLException {
        return relative(-1);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {

    }

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {

    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length)
            throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLException("Result set is read only");
    }
}
