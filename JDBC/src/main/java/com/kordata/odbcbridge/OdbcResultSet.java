package com.kordata.odbcbridge;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OdbcResultSet implements ResultSet {
    private int cursorPosition = -1;
    private final ArrayNode resultArray;
    private final Statement statement;

    private ObjectNode currentRow;
    private List<String> fieldNames;

    private boolean closed = false;

    private boolean lastReadWasNull = false;

    public OdbcResultSet(Statement statement, ArrayNode resultArray) {
        this.statement = statement;
        this.resultArray = resultArray;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
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

    @Override
    public boolean wasNull() throws SQLException {
        return lastReadWasNull;
    }

    private JsonNode getColumn(int columnIndex) {
        return getColumn(fieldNames.get(columnIndex - 1));
    }

    private JsonNode getColumn(String columnLabel) {
        JsonNode column = currentRow.get(columnLabel);

        if (column.isNull()) {
            lastReadWasNull = true;
        }

        return column;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return getColumn(columnIndex).textValue();
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return getColumn(columnIndex).booleanValue();
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return (byte) getColumn(columnIndex).intValue();
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return (short) getColumn(columnIndex).shortValue();
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return getColumn(columnIndex).intValue();
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return getColumn(columnIndex).longValue();
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return (float) getColumn(columnIndex).floatValue();
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return getColumn(columnIndex).doubleValue();
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return BigDecimal.valueOf(getColumn(columnIndex).doubleValue());
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        String text = getColumn(columnIndex).textValue();

        if (text == null)
            return null;

        return text.getBytes();
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String text = getColumn(columnIndex).textValue();

        if (text == null)
            return null;

        LocalDate date = LocalDate.parse(text);

        return Date.valueOf(date);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String text = getColumn(columnIndex).textValue();

        if (text == null)
            return null;

        LocalTime time = LocalTime.parse(text);

        return Time.valueOf(time);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String text = getColumn(columnIndex).textValue();

        if (text == null)
            return null;

        Instant instant = Instant.parse(text);

        return Timestamp.from(instant);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getColumn(columnLabel).textValue();
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getColumn(columnLabel).booleanValue();
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return (byte) getColumn(columnLabel).intValue();
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return (short) getColumn(columnLabel).intValue();
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getColumn(columnLabel).intValue();
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getColumn(columnLabel).longValue();
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return (float) getColumn(columnLabel).doubleValue();
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return (double) getColumn(columnLabel).doubleValue();
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return BigDecimal.valueOf(getColumn(columnLabel).doubleValue());
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        String text = getColumn(columnLabel).textValue();

        if (text == null)
            return null;

        return text.getBytes();
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        String text = getColumn(columnLabel).textValue();

        if (text == null)
            return null;

        LocalDate date = LocalDate.parse(text);

        return Date.valueOf(date);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        String text = getColumn(columnLabel).textValue();

        if (text == null)
            return null;

        LocalTime time = LocalTime.parse(text);

        return Time.valueOf(time);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        String text = getColumn(columnLabel).textValue();

        if (text == null)
            return null;

        Instant instant = Instant.parse(text);

        return Timestamp.from(instant);
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
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public String getCursorName() throws SQLException {
        return null;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    private Object getObject(JsonNode node) {
        switch (node.getNodeType()) {
            case NUMBER:
                return node.asDouble();
            case BOOLEAN:
                return node.asBoolean();
            case NULL:
                return null;
            case STRING:
            case OBJECT:
            case ARRAY:
            default:
                return node.asText();
        }
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        JsonNode node = currentRow.get(fieldNames.get(columnIndex));

        return getObject(node);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        JsonNode node = getColumn(columnLabel);

        return getObject(node);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return fieldNames.indexOf(columnLabel) + 1;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return BigDecimal.valueOf(currentRow.get(fieldNames.get(columnIndex)).asDouble());
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return BigDecimal.valueOf(getColumn(columnLabel).asDouble());
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
    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
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
    public Statement getStatement() throws SQLException {
        return statement;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
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
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
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
    public RowId getRowId(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
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
    public int getHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
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
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw new SQLException("Result set is read only");
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
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
    public String getNString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
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

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
