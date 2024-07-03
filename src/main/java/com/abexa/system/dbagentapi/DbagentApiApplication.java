package com.abexa.system.dbagentapi;

import com.abexa.system.dbagentapi.domain.constants.Constants;
import com.abexa.system.dbagentapi.infrastructure.dto.TransmitterConfigDTO;
import com.abexa.system.dbagentapi.infrastructure.service.ShellService;
import com.abexa.system.dbagentapi.infrastructure.service.TransmitterFlow;
import com.abexa.system.dbagentapi.infrastructure.service.impl.FTPFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class DbagentApiApplication implements ApplicationRunner {

    private final TransmitterFlow transmitterFlow;
    private final TransmitterConfigDTO transmitterConfigDTO;

	public static void main(String[] args) {
		SpringApplication.run(DbagentApiApplication.class, args);
	}

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        String dbName = "AGPS_Pruebas";
        String dbName = transmitterConfigDTO.getDatabaseName();
//        log.info("\n\n\n\n\ndbName => " + dbName + "\n\n\n\n");
        transmitterFlow.clean(dbName);
        transmitterFlow.dump(dbName);
        transmitterFlow.split(dbName);
        transmitterFlow.transmit(dbName);

//        System.exit(Constants.RESULT_OK);
//        File[] sharedPublicFiles = new File("/Users/kenny/Desktop/").listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File pathname) {
//                String name  = pathname.getName();
//                return name.startsWith("Captura");
//            }
//        });
//        List<String> fileNames = Arrays.stream(sharedPublicFiles).map(file -> file.getPath()).toList();
        log.info("__");

//        ftpFactory.saveFilesConcurrently(Arrays.asList("/Users/kenny/Desktop/PC_04.zip", "/Users/kenny/Desktop/RAP_EJEMPLO_1.pdf", "/Users/kenny/Desktop/firma.jpeg"))
//                .subscribe(
//                allSuccess -> {
//                    if (allSuccess) {
//                        log.info("Todos los archivos fueron subidos exitosamente.");
//                        System.exit(Constants.RESULT_OK);
//                    } else {
//                        log.info("Algunos archivos no se subieron correctamente.");
//                        System.exit(Constants.FATAL_ERROR);
//                    }
//                },
//                error -> {
//                    log.error("Error al subir archivos: " + error.getMessage());
//
//                    System.exit(Constants.FATAL_ERROR);
//                }
//        );
    }

}
