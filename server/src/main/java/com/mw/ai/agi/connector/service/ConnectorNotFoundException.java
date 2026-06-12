package com.mw.ai.agi.connector.service;

public class ConnectorNotFoundException extends RuntimeException {
    public ConnectorNotFoundException(String id) {
        super("Connector not found: " + id);
    }
}
