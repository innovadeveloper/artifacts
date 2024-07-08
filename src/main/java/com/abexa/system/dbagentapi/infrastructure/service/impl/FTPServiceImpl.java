package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.dto.CredentialsConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.FTPService;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import java.io.File;
import java.io.FileInputStream;
import io.reactivex.Single;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FTPServiceImpl implements FTPService {

    FTPClient ftpClient = new FTPClient();
    private final CredentialsConfigDTO credentialsConfigDTO;

    public FTPServiceImpl(CredentialsConfigDTO credentialsConfigDTO) {
        this.credentialsConfigDTO = credentialsConfigDTO;
    }

    @SneakyThrows
    @Override
    public void connect() {
        ftpClient.setConnectTimeout(credentialsConfigDTO.getConnectionTimeout());
        ftpClient.connect(credentialsConfigDTO.getHostname(), credentialsConfigDTO.getPort());
        int replyCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            log.error("No se pudo conectar al servidor FTP");
            System.exit(Constants.FATAL_ERROR);
            return;
        }

        // Inicia sesión en el servidor FTP
        boolean loginSuccess = ftpClient.login(credentialsConfigDTO.getUsername(), credentialsConfigDTO.getPassword());
        if (!loginSuccess) {
            log.error("Error al iniciar sesión en el servidor FTP");
            System.exit(Constants.FATAL_ERROR);
            return;
        }
        ftpClient.enterLocalPassiveMode();
    }

    @SneakyThrows
    @Override
    public void listAllFiles() {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        FTPFile[] directoriesList = ftpClient.listDirectories();
        FTPFile[] filesList = ftpClient.listFiles("/");
        log.info("success");
    }

    @SneakyThrows
    @Override
    public Single<Boolean> saveFile(String path) {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        log.info("set file type: FTP.BINARY_FILE_TYPE");
        return Single.create(singleEmitter -> {
            try{
                File file = new File(path);
                FileInputStream inputStream = new FileInputStream(file);
                boolean isSuccess = ftpClient.storeFile(file.getName(), inputStream);
                inputStream.close();
                singleEmitter.onSuccess(isSuccess);
            }catch (Exception e){
                singleEmitter.onError(e);
                e.printStackTrace();
            }
        });
    }

}
