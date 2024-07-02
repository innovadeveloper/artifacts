package com.abexa.system.dbagentapi.infrastructure.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "spring.credentials.config")
@Data
public class CredentialsConfigDTO {
    private String hostname;
    private String username;
    private String password;
    private int port;
    private int connectionTimeout;
}
