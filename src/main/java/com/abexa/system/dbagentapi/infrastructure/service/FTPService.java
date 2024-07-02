package com.abexa.system.dbagentapi.infrastructure.service;

import io.reactivex.Single;

public interface FTPService {
    void connect();
    void listAllFiles();
    Single<Boolean> saveFile(String path);
}
