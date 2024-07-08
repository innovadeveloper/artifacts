package com.abexa.system.dbagentapi;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.domain.dto.DatabaseFileDTO;
import com.abexa.system.dbagentapi.infrastructure.dto.ReceiverConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ReceiverFlow;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.abexa.system.dbagentapi.infrastructure.service.TransmitterFlow;
import com.abexa.system.dbagentapi.infrastructure.service.impl.FTPFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class DbagentApiApplication implements ApplicationRunner {

    private final TransmitterFlow transmitterFlow;
    private final ReceiverFlow receiverFlow;
    private final TransmitterConfigDTO transmitterConfigDTO;
    private final ReceiverConfigDTO receiverConfigDTO;
    @Value("${spring.flow-mode}")
    private String flowMode;

	public static void main(String[] args) {
		SpringApplication.run(DbagentApiApplication.class, args);
	}

    /**
     * tx =>
     * java -jar /home/jenkins/dbagent-api-6.0.0.jar --spring.flow-mode=transmitter --spring.transmitter.config.database-name=AGPS_SantaCruz --spring.transmitter.config.shared-directory=/home/jenkins/DockerProjects/samba-dockerproject/shared
     * rx =>
     * java -jar dbagent-api-0.0.1-SNAPSHOT.jar --spring.flow-mode=receiver --spring.receiver.config.database-name=AGPS_SantaCruz --spring.receiver.config.mssql-internal-directory=/var/opt/mssql/data --spring.receiver.config.mssql-external-directory=/mnt/virtual_machines/SQL --spring.receiver.config.shared-directory=/home/serverdev/Documents/DockerProjects/ftp-docker-project/data
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
//        String dbName = "AGPS_SantaCruz";
        log.info("flowMode => " + flowMode);
        if(flowMode.toLowerCase().equals(Constants.TRANSMITTER.toLowerCase())){
            String dbName = transmitterConfigDTO.getDatabaseName();
            transmitterFlow.clean(dbName);
            transmitterFlow.dump(dbName);
            transmitterFlow.split(dbName);
            transmitterFlow.createManifest(dbName);
            transmitterFlow.transmit(dbName);
        }else if(flowMode.toLowerCase().equals(Constants.RECEIVER.toLowerCase())){
            String dbName = receiverConfigDTO.getDatabaseName();
            receiverFlow.merge(dbName);
            receiverFlow.move(dbName);
            List<DatabaseFileDTO> databaseFileList = receiverFlow.verify(dbName);
            int status = receiverFlow.dropAndRestore(dbName, databaseFileList);
            if(status == Constants.RESULT_OK)
                receiverFlow.clean(dbName);
            System.exit(status);
        }
    }
}
