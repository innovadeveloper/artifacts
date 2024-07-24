package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShellServiceImpl implements ShellService {
    @Override
    public Pair<Integer, String> executeCommand(String command) {
        Pair<Integer, String> result = Pair.of(Constants.FATAL_ERROR, "");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        log.warn(command);

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.warn(line);
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();
//            log.info("Output: " + output + "\nExit code: " + exitCode);
            result = Pair.of(exitCode, output.toString());
            log.warn("exitCode: " + exitCode + " output: " + output);
            return result;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            result = Pair.of(Constants.FATAL_ERROR, e.getMessage());
            return result;
        }
    }
}
