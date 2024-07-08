package com.abexa.system.dbagentapi.domain.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class DatabaseFileDTO {
    private String logicalName;
    private String physicalName;
    private String name;
}
