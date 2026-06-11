package com.mywork.CPI_Assistant.Service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ValueMappingService {

    public byte[] processCsv(
            InputStream csvStream,
            String source,
            String target,
            String sourceId,
            String targetId,
            String source_agency,
            String target_agency
    ) throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                csvStream,
                                StandardCharsets.UTF_8
                        )
                );

        List<String[]> rows = new ArrayList<>();

        String line;

        // Read CSV Rows
        while ((line = reader.readLine()) != null) {

            String[] values = line.split(",");

            rows.add(values);
        }

        if (rows.isEmpty()) {
            throw new Exception("Empty CSV File");
        }

        // Header Row
        String[] header = rows.get(0);

        int sourceIndex = -1;
        int targetIndex = -1;

        // Find Source & Target Index
        for (int i = 0; i < header.length; i++) {

            String column = header[i].trim();

            if (column.equals(source)) {
                sourceIndex = i;
            }

            if (column.equals(target)) {
                targetIndex = i;
            }
        }

        if (sourceIndex == -1 || targetIndex == -1) {

            throw new Exception(
                    "Source or Target column not found"
            );
        }

        /*
            HashMap to avoid duplicate source values

            Key   -> Source
            Value -> Target
        */
        Map<String, String> uniqueMappings =
                new LinkedHashMap<>();

        // Extract Source & Target Values
        for (int i = 1; i < rows.size(); i++) {

            String[] row = rows.get(i);

            String src = "";
            String tgt = "";

            if (sourceIndex < row.length) {
                src = row[sourceIndex].trim();
            }

            if (targetIndex < row.length) {
                tgt = row[targetIndex].trim();
            }

            // Skip Empty Values
            if (src.isBlank() || tgt.isBlank()) {
                continue;
            }

            /*
                Avoid Duplicate Source

                If source already exists,
                it will not overwrite existing value
            */
            uniqueMappings.putIfAbsent(src, tgt);
        }

        return generateCsv(
                source,
                target,
                uniqueMappings,
                sourceId,
                targetId,
                source_agency,
                target_agency
        );
    }

    private byte[] generateCsv(
            String source,
            String target,
            Map<String, String> data,
            String sourceId,
            String targetId,
            String source_agency,
            String target_agency
    ) throws Exception {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        BufferedWriter writer =
                new BufferedWriter(
                        new OutputStreamWriter(
                                outputStream,
                                StandardCharsets.UTF_8
                        )
                );
        if (source_agency.startsWith("\"")
                && source_agency.endsWith("\"")) {

            source_agency = source_agency.substring(1, source.length() - 1);
        }

        if (target_agency.startsWith("\"")
                && target_agency.endsWith("\"")) {

            target_agency = target_agency.substring(1, target.length() - 1);
        }
        // Header Row
        writer.write(
                "," +
                        source_agency + "|" + sourceId +
                        "," +
                        target_agency + "|" + targetId
        );

        writer.newLine();

        // Data Rows
        for (Map.Entry<String, String> entry : data.entrySet()) {

            String src = entry.getKey();
            String tgt = entry.getValue();
            if (src.startsWith("\"")
                    && src.endsWith("\"")) {

                src = src.substring(1, src.length() - 1);
            }

            if (tgt.startsWith("\"")
                    && tgt.endsWith("\"")) {

                tgt = tgt.substring(1, tgt.length() - 1);
            }
            writer.write(
                    "Receiver," +
                            src + "|," +
                            tgt
            );

            writer.newLine();
        }

        writer.flush();

        return outputStream.toByteArray();
    }
}