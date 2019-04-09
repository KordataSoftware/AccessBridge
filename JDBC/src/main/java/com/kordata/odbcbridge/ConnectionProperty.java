package com.kordata.odbcbridge;

public class ConnectionProperty {
    private final String key;
    private final String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public ConnectionProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static ConnectionProperty Create(String fragment) {
        if (fragment == null)
            return null;

        if (!fragment.contains("="))
            return null;

        String[] components = fragment.split("=");
        return new ConnectionProperty(components[0], components[1]);
    }
}
