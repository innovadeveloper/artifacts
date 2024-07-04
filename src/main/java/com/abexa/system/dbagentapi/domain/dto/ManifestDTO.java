package com.abexa.system.dbagentapi.domain.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class ManifestDTO {

    private long timestamp;
    private String dbName;
    private List<String> fileNameList;
}
