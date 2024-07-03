package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.abexa.system.dbagentapi.infrastructure.service.TransmitterFlow;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransmitterFlowImpl implements TransmitterFlow {

    private final TransmitterConfigDTO transmitterConfigDTO;
    private final ShellService shellService;
    private final FTPFactory ftpFactory;
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
        String splitCommand = "split -b " + transmitterConfigDTO.getFileSize() + " \""+ transmitterConfigDTO.getSharedDirectory() + "/" + dbName + ".bak\" " + ( transmitterConfigDTO.getSharedDirectory() + "/" + dbName ) + "_part";
        shellService.executeCommand(splitCommand);
        // remove the original file
//        String removeCommand = "sudo rm " + transmitterConfigDTO.getSharedDirectory() + "/" + dbName + ".bak";
//        shellService.executeCommand(removeCommand);
    }

    @Override
    public void transmit(String dbName) {
        File[] sharedPublicFiles = new File(transmitterConfigDTO.getSharedDirectory())
                .listFiles(pathname -> pathname.getName().startsWith(dbName) && !pathname.getName().equals(dbName + ".bak"));
        List<String> fileNames = Arrays.stream(Objects.requireNonNull(sharedPublicFiles)).map(File::getPath).toList();
        ftpFactory.saveFilesConcurrently(fileNames)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        allSuccess -> {
                            if (allSuccess) {
                                log.info("Todos los archivos fueron subidos exitosamente.");
                                System.exit(Constants.RESULT_OK);
                            } else {
                                log.info("Algunos archivos no se subieron correctamente.");
                                System.exit(Constants.FATAL_ERROR);
                            }
                        },
                        error -> {
                            log.error("Error al subir archivos: " + error.getMessage());
                            System.exit(Constants.FATAL_ERROR);
                        }
                );
    }
}
