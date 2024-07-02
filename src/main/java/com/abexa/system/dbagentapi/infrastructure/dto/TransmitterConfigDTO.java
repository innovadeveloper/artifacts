package com.abexa.system.dbagentapi.infrastructure.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "spring.transmitter.config")
@Data
public class TransmitterConfigDTO {
    private String sharedDirectory;
    private String usernameDb;
    private String passwordDb;
    private String hostnameDb;
    private String portDb;
    private String fileSize;
}
