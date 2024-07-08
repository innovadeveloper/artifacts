package com.abexa.system.dbagentapi.infrastructure.service;

import com.abexa.system.dbagentapi.domain.dto.DatabaseFileDTO;

import java.util.List;

public interface ReceiverFlow {

    void scan(String dbName);
    void merge(String dbName);
    void move(String dbName);
    List<DatabaseFileDTO> verify(String dbName);
//    void restore(String dbName);
    int dropAndRestore(String dbName, List<DatabaseFileDTO> databaseFileList);
    int restore(String dbName, List<DatabaseFileDTO> databaseFileList);
    void clean(String dbName);
}
