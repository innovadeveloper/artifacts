package com.abexa.system.dbagentapi.infrastructure.service;

public interface TransmitterFlow{

    void clean(String dbName);
    void dump(String dbName);
    void split(String dbName);
    void createManifest(String dbName);
    void transmit(String dbName);

}
