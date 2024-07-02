package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.abexa.system.dbagentapi.infrastructure.service.TransmitterFlow;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransmitterFlowImpl implements TransmitterFlow {

    private final TransmitterConfigDTO transmitterConfigDTO;
    private final ShellService shellService;
//    private String sharedPublic = "/home/dbagent/Documents/DockerProjects/samba-dockerproject/shared";

    @Override
    public void clean(String dbName) {
        String command = "sudo rm " + transmitterConfigDTO.getSharedDirectory() + "/" + dbName + "*";
        shellService.executeCommand(command);
    }

    @Override
    public void dump(String dbName) {
        String command = "sqlcmd -S " + transmitterConfigDTO.getHostnameDb() + "," + transmitterConfigDTO.getPortDb() + " -U " + transmitterConfigDTO.getUsernameDb() + " -P " + transmitterConfigDTO.getPasswordDb() + " -Q \"BACKUP DATABASE " + dbName + " TO DISK = '" + dbName + ".bak' WITH COMPRESSION\" -C";
        shellService.executeCommand(command);
    }

    @Override
    public void split(String dbName) {
        String command = "split -b " + transmitterConfigDTO.getFileSize() + " \""+ transmitterConfigDTO.getSharedDirectory() + "/" + dbName + ".bak\" " + dbName + "_part";
        shellService.executeCommand(command);
    }

    @Override
    public void transmit(String dbName) {
        log.info("not implemented yet");
    }
}
