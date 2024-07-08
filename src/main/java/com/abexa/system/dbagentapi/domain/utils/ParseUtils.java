package com.abexa.system.dbagentapi.domain.utils;

import com.abexa.system.dbagentapi.domain.dto.DatabaseFileDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {
    /**
     * parsea la salida de consola del comando verify parts de sqlcmd
     * @param sqlCmdOutput String
     * @return List
     */
    public static List<DatabaseFileDTO> parseSQLCmdOutput(String sqlCmdOutput) {
        List<DatabaseFileDTO> databaseFiles = new ArrayList<>();

        // Dividir el resultado en líneas
        String[] lines = sqlCmdOutput.split("\n");

        // Saltar las líneas hasta encontrar la cabecera de LogicalName
        boolean foundHeader = false;
        for (String line : lines) {
            if (!foundHeader && line.trim().startsWith("LogicalName")) {
                foundHeader = true;
                continue;
            }
            if (foundHeader && !line.trim().isEmpty()) {
                // Procesar las líneas con los LogicalName y PhysicalName
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    String logicalName = parts[0];
                    String physicalName = parts[1];

                    String regex = "\\\\([^\\\\]+\\.[^\\\\]+)$";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(physicalName);
                    if (matcher.find())
                    {
                        String name = matcher.group(1);
                        databaseFiles.add(DatabaseFileDTO.builder().logicalName(logicalName).physicalName(physicalName).name(name).build());
                    }
                }
            }
        }

        return databaseFiles;
    }
}
