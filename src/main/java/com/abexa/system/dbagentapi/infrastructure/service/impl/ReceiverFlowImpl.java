package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.domain.dto.DatabaseFileDTO;
import com.abexa.system.dbagentapi.domain.utils.ParseUtils;
import com.abexa.system.dbagentapi.infrastructure.dto.ReceiverConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ReceiverFlow;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.google.gson.Gson;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiverFlowImpl implements ReceiverFlow {

    private final ReceiverConfigDTO receiverConfigDTO;
    private final ShellService shellService;

    @Override
    public void scan(String dbName) {
        log.info("###scan dbName: " + dbName + "###");
        List<String> fileNamesList = this.buildFileNamesList(dbName, false);
        if(fileNamesList.isEmpty()){
           log.error("No se encontraron partes del archivo " + dbName + Constants.PART_EXTENSION + " en el directorio " + receiverConfigDTO.getSharedDirectory());
           System.exit(Constants.FATAL_ERROR);
        }
    }

    @Override
    public void merge(String dbName) {
        log.info("###merge dbName: " + dbName + "###");
        // cat AGPS_SantaCruz_part* > AGPS_SantaCruz_united.bak
        String command = "cat " + Paths.get(receiverConfigDTO.getSharedDirectory(), this.buildPartDbNameString(dbName) + "*") + " > " + Paths.get(receiverConfigDTO.getSharedDirectory(), (dbName + Constants.BAK_EXTENSION));
        shellService.executeCommand(command);
    }

    @Override
    public void move(String dbName) {
        log.info("###move dbName: " + dbName + "###");
        String command = "mv " + Paths.get(receiverConfigDTO.getSharedDirectory(), (dbName + Constants.BAK_EXTENSION)) + " " + Paths.get(receiverConfigDTO.getMssqlExternalDirectory(), (dbName + Constants.BAK_EXTENSION));
        shellService.executeCommand(command);
    }

    /**
     * verifica las partes y retorna una lista de los objetos lógicos con sus ubicaciones físicas
     * @param dbName {@link String}
     * @return List
     */
    @SneakyThrows
    @Override
    public List<DatabaseFileDTO> verify(String dbName) {
        log.info("###verify dbName: " + dbName + "###");
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "RESTORE FILELISTONLY FROM DISK = '/var/opt/mssql/data/AGPS_SantaCruz_united_3.bak';"
        String command = "sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"RESTORE FILELISTONLY FROM DISK = '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), dbName + Constants.BAK_EXTENSION) + "';\" -C";
        Pair<Integer, String> resultPair = shellService.executeCommand(command);
        List<DatabaseFileDTO> databaseFileList = new ArrayList<>();
        if(resultPair.getLeft() == Constants.RESULT_OK){
            databaseFileList = ParseUtils.parseSQLCmdOutput(resultPair.getRight());
            log.info("verify #2 databaseFileList:\n" + new Gson().toJson(databaseFileList));
            if(databaseFileList.isEmpty())
            {
                log.error("No se encontraron archivos en la base de datos " + dbName + " en el directorio " + receiverConfigDTO.getMssqlInternalDirectory());
                System.exit(Constants.FATAL_ERROR);
            }
        }
        return databaseFileList;
    }

    @Override
    public int dropAndRestore(String dbName, List<DatabaseFileDTO> databaseFileList) {
        log.info("### dropAndRestore dbName: " + dbName + "###, databaseFileList:\n" + new Gson().toJson(databaseFileList));
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "DROP DATABASE AGPS_SantaCruz" -C
        String command = "sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"DROP DATABASE " + dbName + "\" -C";
        Pair<Integer, String> resultPair = shellService.executeCommand(command);
        log.info("### drop dbName: " + dbName + "###, result shell : " + resultPair.getKey());
        return this.restore(dbName, databaseFileList);
    }

    @Override
    public int restore(String dbName, List<DatabaseFileDTO> databaseFileList) {
        log.info("### restore dbName: " + dbName + "###, databaseFileList:\n" + new Gson().toJson(databaseFileList));
        final String backgroundMode = "&";
        String moveArgsCommand = databaseFileList.stream().map(databasefile
                -> "MOVE '" + databasefile.getLogicalName() + "' TO '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), databasefile.getName()) + "'")
                .collect(Collectors.joining(","));
        String command = "nohup sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"RESTORE DATABASE " + dbName + " FROM DISK = '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), dbName + Constants.BAK_EXTENSION)
                + "' WITH " + moveArgsCommand + ", RECOVERY\" -C " + backgroundMode;
        Pair<Integer, String> result = shellService.executeCommand(command);
        log.info("### restore : " + dbName + "###, result shell : " + result.getKey());
        return result.getLeft();
//        System.exit((result.getLeft() != Constants.RESULT_OK) ? Constants.FATAL_ERROR : Constants.RESULT_OK);
//        log.info("comando: " + command);
//        log.info("salida de restore: " + result.getRight());
    }

    @Override
    public void clean(String dbName) {
        String command = "sudo rm " + Paths.get(receiverConfigDTO.getSharedDirectory(), this.buildPartDbNameString(dbName) + "*");
        log.info("### clean dbName: " + dbName);
        shellService.executeCommand(command);
    }

    /**
     * retorna [dbname]_part
     * @param dbName {@link String}
     * @return String
     */
    private String buildPartDbNameString(String dbName){
        return (dbName + Constants.PART_EXTENSION);
    }

    /**
     * construye
     * @param dbName String
     * @return {@link List<String>}
     */
    private List<String> buildFileNamesList(String dbName, boolean isOnlyName){
        File[] sharedPublicFiles = new File(receiverConfigDTO.getSharedDirectory())
                .listFiles(pathname -> pathname.getName().startsWith(dbName + Constants.PART_EXTENSION));
        return Arrays.stream(Objects.requireNonNull(sharedPublicFiles)).map((isOnlyName) ? File::getName : File::getPath).toList();
    }
}
