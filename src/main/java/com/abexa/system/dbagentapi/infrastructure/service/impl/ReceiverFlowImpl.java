package com.abexa.system.dbagentapi.infrastructure.service.impl;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.dto.ReceiverConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ReceiverFlow;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiverFlowImpl implements ReceiverFlow {

    private final static long TOLERANCE = 30 * 1000;
    private final ReceiverConfigDTO receiverConfigDTO;
    private final ShellService shellService;

    @Override
    public void scan() {
        // filtrar los dbname_manifest en el directorio compartido
        // /mnt/virtual_machines/SQL/*.ldf
        if(this.buildManifestFilesList().isEmpty()) {
            log.error("No se encontraron ficheros manifests");
            System.exit(Constants.FATAL_ERROR);
        }
    }

    @SneakyThrows
    @Override
    public void verify(String dbName) {
        List<File> manifestFilesList = this.buildManifestFilesList();
        Optional<File> manifestOptional = manifestFilesList.stream().filter(file -> {
            // modifiedAt : 2000 ms
            // checked : 2500 ms , 3500
            // operation : checked - tolerance => 2500 - 1000
            // operation : checked - tolerance => 3500 - 1000
            // operation : 1500 < modifiedAt (yes)
            // operation : 2500 < modifiedAt (no)
//            return (file.lastModified() >= System.currentTimeMillis() - TOLERANCE);
            return (file.getName().equals(dbName));
        }).findFirst();
        if(manifestOptional.isEmpty()){
            log.error("No se encontró el fichero " + dbName);
            System.exit(Constants.FATAL_ERROR);
        }
    }

    @Override
    public void merge(String dbName) {
        // cat AGPS_Pruebas_part* > AGPS_Pruebas_full.bak
        String command = "cat " + this.buildPartDbNameString(dbName) + "* > " + (dbName + Constants.BAK_EXTENSION);
        shellService.executeCommand(command);
    }

    @Override
    public void restore(String dbName) {
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "RESTORE DATABASE db_products FROM DISK = '/var/opt/mssql/data/db_products_full.bak' WITH NORECOVERY" -C
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "RESTORE DATABASE AGPS_SantaCruz FROM DISK = '/var/opt/mssql/data/AGPS_SantaCruz_full.bak' WITH NORECOVERY" -C
        // sqlcmd -S 192.168.1.2,1434 -U yorklin -P 123 -k -Q "RESTORE DATABASE db_products FROM DISK = '/var/opt/mssql/data/db_products_full.bak' WITH NORECOVERY" -C
        // AGPS_SantaCruz_full.bak
        String command = "sqlcmd -S " + receiverConfigDTO.getHostnameDb() + "," + receiverConfigDTO.getPortDb() + " -U " + receiverConfigDTO.getUsernameDb() + " -P " + receiverConfigDTO.getPasswordDb()
                + " -k -Q \"RESTORE DATABASE " + dbName + " FROM DISK = '" + dbName + Constants.BAK_EXTENSION + "' WITH NORECOVERY\" -C";
        shellService.executeCommand(command);
    }

    @Override
    public void clean(String dbName) {
        String command = "sudo rm " + receiverConfigDTO.getSharedDirectory() + "/" + dbName + "*";
        shellService.executeCommand(command);
    }

    /**
     * construye una lista de manifest
     * @return {@link List <String>}
     */
    private List<File> buildManifestFilesList(){
        File[] manifestFiles = new File(receiverConfigDTO.getSharedDirectory())
                .listFiles(pathname -> pathname.getName().endsWith(Constants.MANIFEST_EXTENSION));
        return Arrays.stream(Objects.requireNonNull(manifestFiles)).toList();
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
