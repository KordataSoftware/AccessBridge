package com.kordata.odbcbridge;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OdbcResultSetMetaData implements ResultSetMetaData {
    private final ArrayNode schemaArray;

    public OdbcResultSetMetaData(ArrayNode schemaArray) {
        this.schemaArray = schemaArray;
    }

    private ObjectNode getColumnSchema(int column) throws SQLException {
        if (column < 1 || column >= schemaArray.size()) {
            throw new SQLException("Column index out of bounds");
        }

        return (ObjectNode) schemaArray.get(column - 1);
    }

    public String getOdbcType(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("dataType").textValue();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return schemaArray.size();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("isAutoIncrement").booleanValue();
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        if (columnNode.get("allowDBNull").booleanValue()) {
            return columnNullable;
        }

        return columnNoNulls;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        String columnType = columnNode.get("type").textValue();

        return columnType == "int" || columnType == "decimal" || columnType == "short"
                || columnType == "byte";
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("columnSize").intValue();
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("columnName").textValue();
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("columnName").textValue();
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("baseSchemaName").textValue();
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("numericPrecision").intValue();
    }

    @Override
    public int getScale(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("numericScale").intValue();
    }

    @Override
    public String getTableName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("baseTableName").textValue();
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("baseCatalogName").textValue();
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        String columnType = columnNode.get("dataType").textValue();
        boolean isLong = columnNode.get("isLong").booleanValue();

        switch (columnType) {
            case "string":
                return isLong ? Types.LONGVARCHAR : Types.VARCHAR;
            case "int":
                return Types.BIGINT;
            case "short":
                return Types.INTEGER;
            case "byte":
                return Types.TINYINT;
            case "boolean":
                return Types.BOOLEAN;
            case "decimal":
                return Types.DECIMAL;
            case "dateTime":
                return Types.TIMESTAMP;
            default:
                return Types.OTHER;
        }
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("dataType").textValue();
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return columnNode.get("isReadOnly").booleanValue();
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return !columnNode.get("isReadOnly").booleanValue();
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        return !columnNode.get("isReadOnly").booleanValue();
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        ObjectNode columnNode = getColumnSchema(column);

        String columnType = columnNode.get("dataType").textValue();

        switch (columnType) {
            case "string":
                return String.class.getName();
            case "int":
                return Integer.class.getName();
            case "short":
                return Short.class.getName();
            case "byte":
                return Byte.class.getName();
            case "boolean":
                return Boolean.class.getName();
            case "decimal":
                return BigDecimal.class.getName();
            case "dateTime":
                return Timestamp.class.getName();
            default:
                return Object.class.getName();
        }
    }
}
