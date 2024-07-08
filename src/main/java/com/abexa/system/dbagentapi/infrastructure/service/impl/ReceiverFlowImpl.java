package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.domain.dto.DatabaseFileDTO;
import com.abexa.system.dbagentapi.domain.utils.ParseUtils;
import com.abexa.system.dbagentapi.infrastructure.dto.ReceiverConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ReceiverFlow;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    public void merge(String dbName) {
        String command = "cat " + Paths.get(receiverConfigDTO.getSharedDirectory(), this.buildPartDbNameString(dbName) + "*") + " > " + Paths.get(receiverConfigDTO.getSharedDirectory(), (dbName + Constants.BAK_EXTENSION));
        shellService.executeCommand(command);
    }

    @Override
    public void move(String dbName) {
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
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "RESTORE FILELISTONLY FROM DISK = '/var/opt/mssql/data/AGPS_SantaCruz_united_3.bak';"
        String command = "sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"RESTORE FILELISTONLY FROM DISK = '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), dbName + Constants.BAK_EXTENSION) + "';\" -C";
        Pair<Integer, String> resultPair = shellService.executeCommand(command);
        List<DatabaseFileDTO> databaseFileList = new ArrayList<>();
        if(resultPair.getLeft() == Constants.RESULT_OK){
            databaseFileList = ParseUtils.parseSQLCmdOutput(resultPair.getRight());
            if(databaseFileList.isEmpty())
                System.exit(Constants.FATAL_ERROR);
        }
        return databaseFileList;
    }

    @Override
    public int dropAndRestore(String dbName, List<DatabaseFileDTO> databaseFileList) {
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "DROP DATABASE AGPS_SantaCruz" -C
        String command = "sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"DROP DATABASE " + dbName + "\" -C";
        shellService.executeCommand(command);
        return this.restore(dbName, databaseFileList);
    }

    @Override
    public int restore(String dbName, List<DatabaseFileDTO> databaseFileList) {
        final String backgroundMode = "&";
        String moveArgsCommand = databaseFileList.stream().map(databasefile
                -> "MOVE '" + databasefile.getLogicalName() + "' TO '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), databasefile.getName()) + "'")
                .collect(Collectors.joining(","));
        String command = "nohup sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"RESTORE DATABASE " + dbName + " FROM DISK = '" + Paths.get(receiverConfigDTO.getMssqlInternalDirectory(), dbName + Constants.BAK_EXTENSION)
                + "' WITH " + moveArgsCommand + ", RECOVERY\" -C " + backgroundMode;
        Pair<Integer, String> result = shellService.executeCommand(command);
        return result.getLeft();
//        System.exit((result.getLeft() != Constants.RESULT_OK) ? Constants.FATAL_ERROR : Constants.RESULT_OK);
//        log.info("comando: " + command);
//        log.info("salida de restore: " + result.getRight());
    }

    @Override
    public void clean(String dbName) {
        String command = "sudo rm " + Paths.get(receiverConfigDTO.getSharedDirectory(), this.buildPartDbNameString(dbName) + "*");
        log.info("clean command: " + command);
        shellService.executeCommand(command);
//        String command = "sudo rm " + receiverConfigDTO.getSharedDirectory() + "/" + dbName + "*";
//        shellService.executeCommand(command);
    }

    /**
     * retorna [dbname]_part
     * @param dbName {@link String}
     * @return String
     */
    private String buildPartDbNameString(String dbName){
        return (dbName + Constants.PART_EXTENSION);
    }

}
