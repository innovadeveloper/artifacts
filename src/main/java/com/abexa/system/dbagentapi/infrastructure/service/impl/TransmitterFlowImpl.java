package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.domain.dto.ManifestDTO;
import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.abexa.system.dbagentapi.infrastructure.service.TransmitterFlow;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
        String command = "sqlcmd -S " + transmitterConfigDTO.getHostnameDb() + "," + transmitterConfigDTO.getPortDb() + " -U " + transmitterConfigDTO.getUsernameDb() + " -P " + transmitterConfigDTO.getPasswordDb() + " -Q \"BACKUP DATABASE " + dbName + " TO DISK = '" + dbName + Constants.BAK_EXTENSION + "' WITH INIT, COMPRESSION\" -C";
        shellService.executeCommand(command);
    }

    @Override
    public void split(String dbName) {
        String splitCommand = "split -b " + transmitterConfigDTO.getFileSize() + " \""+ transmitterConfigDTO.getSharedDirectory() + "/" + (dbName + Constants.BAK_EXTENSION) + "\" " + ( transmitterConfigDTO.getSharedDirectory() + "/" + dbName  + Constants.PART_EXTENSION);
        shellService.executeCommand(splitCommand);
    }

    @SneakyThrows
    @Override
    public void createManifest(String dbName) {
        File file = new File(transmitterConfigDTO.getSharedDirectory() + "/" + dbName + Constants.MANIFEST_EXTENSION);
        FileUtils.writeStringToFile(file,
                new Gson().toJson(ManifestDTO.builder()
                                .fileNameList( this.buildFileNamesList(dbName, true) )
                                .timestamp(System.currentTimeMillis())
                                .dbName(dbName)
                                .build())
                , "UTF-8");
    }

    @Override
    public void transmit(String dbName) {
        String manifestPath = transmitterConfigDTO.getSharedDirectory() + "/" + dbName + Constants.MANIFEST_EXTENSION;
        ftpFactory.saveFilesConcurrently(this.buildFileNamesList(dbName, false), manifestPath)
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

    /**
     * construye
     * @param dbName String
     * @return {@link List<String>}
     */
    private List<String> buildFileNamesList(String dbName, boolean isOnlyName){
        File[] sharedPublicFiles = new File(transmitterConfigDTO.getSharedDirectory())
                .listFiles(pathname ->
                        pathname.getName().startsWith(dbName)
                        && !(pathname.getName().endsWith(Constants.BAK_EXTENSION) || pathname.getName().endsWith(Constants.MANIFEST_EXTENSION))
                );
        return Arrays.stream(Objects.requireNonNull(sharedPublicFiles)).map((isOnlyName) ? File::getName : File::getPath).toList();
    }
}
