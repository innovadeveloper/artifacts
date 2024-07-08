package com.abexa.system.dbagentapi.infrastructure.service;

import org.apache.commons.lang3.tuple.Pair;

public interface ShellService {
    Pair<Integer, String> executeCommand(String command);
}
