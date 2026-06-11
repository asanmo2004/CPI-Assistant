package com.mywork.CPI_Assistant.Controller;
import com.mywork.CPI_Assistant.Service.ValueMappingService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/VM")
public class ValueMappingController {

    @Autowired
    private ValueMappingService valueMappingService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> process(
            @RequestParam("file") MultipartFile file,
            @RequestParam("keyField") String source,
            @RequestParam("valueField") String target,
            @RequestParam("sourceAgency") String source_agency,
            @RequestParam("targetAgency") String target_agency,
            @RequestParam("sourceIdentifier") String sourceId,
            @RequestParam("sourceIdentifier") String targetId
    ) {

        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("File is required");
            }

            String filename =
                    file.getOriginalFilename().toLowerCase();

            ByteArrayInputStream csvInputStream;

            // CSV File
            if (filename.endsWith(".csv")) {

                source = "\"" + source + "\"";
                target = "\"" + target + "\"";

                csvInputStream =
                        new ByteArrayInputStream(file.getBytes());
            }

            // Excel File
            else if (
                    filename.endsWith(".xlsx")
                            || filename.endsWith(".xls")
            ) {

                csvInputStream = convertExcelToCsv(file);
            }

            else {

                return ResponseEntity.badRequest()
                        .body(
                                "Only CSV, XLSX and XLS files are supported"
                        );
            }

            byte[] resultCsv =
                    valueMappingService.processCsv(
                            csvInputStream,
                            source,
                            target,
                            sourceId,
                            targetId,
                            source_agency,
                            target_agency
                    );

            ByteArrayResource resource =
                    new ByteArrayResource(resultCsv);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=output.csv"
                    )
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(resultCsv.length)
                    .body(resource);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }

    private ByteArrayInputStream convertExcelToCsv(
            MultipartFile file
    ) throws Exception {

        Workbook workbook =
                WorkbookFactory.create(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);

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

            StringBuilder rowData = new StringBuilder();

            int lastCell = row.getLastCellNum();

            for (int i = 0; i < lastCell; i++) {

                Cell cell = row.getCell(
                        i,
                        Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
                );

                rowData.append(getCellValue(cell));

                if (i < lastCell - 1) {
                    rowData.append(",");
                }
            }

            writer.write(rowData.toString());
            writer.newLine();
        }

        writer.flush();

        workbook.close();

        return new ByteArrayInputStream(
                outputStream.toByteArray()
        );
    }

    private String getCellValue(Cell cell) {

        return switch (cell.getCellType()) {

            case STRING ->
                    cell.getStringCellValue();

            case NUMERIC -> {

                double value = cell.getNumericCellValue();

                if (value == (long) value) {
                    yield String.valueOf((long) value);
                }

                yield String.valueOf(value);
            }

            case BOOLEAN ->
                    String.valueOf(cell.getBooleanCellValue());

            case FORMULA ->
                    cell.getCellFormula();

            default -> "";
        };
    }
}