package com.mywork.CPI_Assistant.Service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class ValueMappingService {

    public ByteArrayResource generateValueMapping(
            MultipartFile file,
            String source,
            String target,
            String sourceAgency,
            String targetAgency,
            String sourceIdentifier,
            String targetIdentifier
    ) {

        try {

            validateFile(file);

            log.info("Processing file: {}", file.getOriginalFilename());

            InputStream csvInputStream =
                    getCsvInputStream(file, source, target);

            Map<String, String> mappings =
                    extractMappings(
                            csvInputStream,
                            source,
                            target
                    );

            log.info(
                    "Successfully extracted {} unique mappings",
                    mappings.size()
            );

            byte[] result =
                    generateOutputCsv(
                            mappings,
                            sourceAgency,
                            targetAgency,
                            sourceIdentifier,
                            targetIdentifier
                    );

            return new ByteArrayResource(result);

        } catch (Exception ex) {

            log.error(
                    "Error while generating value mapping",
                    ex
            );

            throw new RuntimeException(
                    "Failed to process file: "
                            + ex.getMessage()
            );
        }
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Uploaded file is empty"
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                !(fileName.endsWith(".csv")
                        || fileName.endsWith(".xls")
                        || fileName.endsWith(".xlsx"))) {

            throw new RuntimeException(
                    "Only CSV, XLS and XLSX files are supported"
            );
        }
    }

    private InputStream getCsvInputStream(
            MultipartFile file,
            String source,
            String target
    ) throws Exception {

        String fileName =
                file.getOriginalFilename()
                        .toLowerCase();

        if (fileName.endsWith(".csv")) {

            log.info("CSV file detected");

            return new ByteArrayInputStream(
                    file.getBytes()
            );
        }

        log.info("Excel file detected");

        return convertExcelToCsv(file);
    }

    private ByteArrayInputStream convertExcelToCsv(
            MultipartFile file
    ) throws Exception {

        Workbook workbook =
                WorkbookFactory.create(
                        file.getInputStream()
                );

        Sheet sheet =
                workbook.getSheetAt(0);

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        BufferedWriter writer =
                new BufferedWriter(
                        new OutputStreamWriter(
                                outputStream,
                                StandardCharsets.UTF_8
                        )
                );

        for (Row row : sheet) {

            StringBuilder rowData =
                    new StringBuilder();

            int lastCell =
                    row.getLastCellNum();

            for (int i = 0; i < lastCell; i++) {

                Cell cell =
                        row.getCell(
                                i,
                                Row.MissingCellPolicy
                                        .CREATE_NULL_AS_BLANK
                        );

                rowData.append(
                        getCellValue(cell)
                );

                if (i < lastCell - 1) {
                    rowData.append(",");
                }
            }

            writer.write(
                    rowData.toString()
            );

            writer.newLine();
        }

        writer.flush();
        workbook.close();

        return new ByteArrayInputStream(
                outputStream.toByteArray()
        );
    }

    private String getCellValue(
            Cell cell
    ) {

        return switch (cell.getCellType()) {

            case STRING ->
                    cell.getStringCellValue();

            case NUMERIC -> {

                double value =
                        cell.getNumericCellValue();

                if (value == (long) value) {
                    yield String.valueOf(
                            (long) value
                    );
                }

                yield String.valueOf(value);
            }

            case BOOLEAN ->
                    String.valueOf(
                            cell.getBooleanCellValue()
                    );

            case FORMULA ->
                    cell.getCellFormula();

            default -> "";
        };
    }

    private Map<String, String> extractMappings(
            InputStream csvStream,
            String source,
            String target
    ) throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                csvStream,
                                StandardCharsets.UTF_8
                        )
                );

        List<String[]> rows =
                new ArrayList<>();

        String line;

        while ((line = reader.readLine()) != null) {
            rows.add(line.split(","));
        }

        if (rows.isEmpty()) {
            throw new RuntimeException(
                    "CSV file is empty"
            );
        }

        String[] header =
                rows.get(0);

        int sourceIndex = -1;
        int targetIndex = -1;

        for (int i = 0; i < header.length; i++) {

            String column =
                    header[i]
                            .trim()
                            .replace("\"", "");

            if (column.equalsIgnoreCase(source)) {
                sourceIndex = i;
            }

            if (column.equalsIgnoreCase(target)) {
                targetIndex = i;
            }
        }

        if (sourceIndex == -1
                || targetIndex == -1) {

            throw new RuntimeException(
                    "Source or Target column not found"
            );
        }

        Map<String, String> mappings =
                new LinkedHashMap<>();

        for (int i = 1; i < rows.size(); i++) {

            String[] row =
                    rows.get(i);

            String src =
                    sourceIndex < row.length
                            ? row[sourceIndex].trim()
                            : "";

            String tgt =
                    targetIndex < row.length
                            ? row[targetIndex].trim()
                            : "";

            if (src.isBlank()
                    || tgt.isBlank()) {
                continue;
            }

            mappings.putIfAbsent(
                    src,
                    tgt
            );
        }

        return mappings;
    }

    private byte[] generateOutputCsv(
            Map<String, String> mappings,
            String sourceAgency,
            String targetAgency,
            String sourceIdentifier,
            String targetIdentifier
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

        writer.write(
                "," +
                        sourceAgency +
                        "|" +
                        sourceIdentifier +
                        "," +
                        targetAgency +
                        "|" +
                        targetIdentifier
        );

        writer.newLine();

        for (Map.Entry<String, String> entry :
                mappings.entrySet()) {

            String source =
                    removeQuotes(
                            entry.getKey()
                    );

            String target =
                    removeQuotes(
                            entry.getValue()
                    );

            writer.write(
                    "Receiver," +
                            source +
                            "|," +
                            target
            );

            writer.newLine();
        }

        writer.flush();

        return outputStream.toByteArray();
    }

    private String removeQuotes(
            String value
    ) {

        if (value == null) {
            return "";
        }

        if (value.startsWith("\"")
                && value.endsWith("\"")) {

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        return value;
    }
}