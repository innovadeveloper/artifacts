package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.dto.CredentialsConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.FTPService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FTPFactory {
    @Autowired
    private CredentialsConfigDTO credentialsConfigDTO;
    @Autowired
    private TransmitterConfigDTO transmitterConfigDTO;

    FTPService create(){
        return new FTPServiceImpl(credentialsConfigDTO);
    }

    /**
     * guarda ficheros concurrentemente
     * @param filePaths List<String>
     * @param manifestFilePath String
     * @return Observable
     */
    public Observable<Boolean> saveFilesConcurrently(List<String> filePaths, String manifestFilePath) {
        if(filePaths.size() > transmitterConfigDTO.getFileLimit())
        {
            log.error("The number of files to save is greater than the limit allowed: " + transmitterConfigDTO.getFileLimit());
            System.exit(Constants.FATAL_ERROR);
            throw new RuntimeException("The number of files to save is greater than the limit allowed: " + transmitterConfigDTO.getFileLimit());
        }

        List<Observable<Boolean>> observables = filePaths.stream()
                .map(filePath -> {
                    FTPService ftpService = create();
                    ftpService.connect();
                    return ftpService.saveFile(filePath).toObservable();
                }).collect(Collectors.toList());
        return Observable.zip(observables, results -> {
                    for (Object result : results) {
                        if (!(Boolean) result)
                            return false;
                    }
                    return true;
                }).flatMap(allSuccess -> {
                    if (allSuccess) {
                        FTPService ftpService = create();
                        ftpService.connect();
                        return ftpService.saveFile(manifestFilePath).toObservable();
                    } else {
                        return Observable.just(false);
                    }
                });
    }

}
