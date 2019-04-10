package com.kordata.odbcbridge;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeExtensions {
    public static Timestamp timestampValue(JsonNode node) {
        try {
            Instant instant = Instant.parse(node.asText());
            return Timestamp.from(instant);
        } catch (DateTimeParseException e) {
            return new Timestamp(0);
        }
    }

    public static Date dateValue(JsonNode node) {
        try {
            LocalDate date = LocalDate.parse(node.asText());
            return Date.valueOf(date);
        } catch (DateTimeParseException e) {
            return new Date(0);
        }
    }

    public static Time timeValue(JsonNode node) {
        try {
            LocalTime time = LocalTime.parse(node.asText());
            return Time.valueOf(time);
        } catch (DateTimeParseException e) {
            return new Time(0);
        }
    }

    public static Object toObject(JsonNode node, String type) {
        if (node.isNull())
            return null;

        switch (type) {
            case "string":
                return node.textValue();
            case "int":
                return node.intValue();
            case "short":
                return node.shortValue();
            case "byte":
                return (byte) node.intValue();
            case "boolean":
                return node.booleanValue();
            case "decimal":
                return node.decimalValue();
            case "dateTime":
                return timestampValue(node);
            case "date":
                return dateValue(node);
            case "time":
                return timeValue(node);
            default:
                return node.asText();
        }
    }
}
