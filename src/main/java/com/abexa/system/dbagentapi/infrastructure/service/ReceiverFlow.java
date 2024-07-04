package com.abexa.system.dbagentapi.infrastructure.service;

public interface ReceiverFlow {

    void scan();
    void verify(String dbName);
    void merge(String dbName);
    void restore(String dbName);
    void clean(String dbName);
}
