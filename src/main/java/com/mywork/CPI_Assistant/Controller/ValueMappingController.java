package com.mywork.CPI_Assistant.Controller;
import com.mywork.CPI_Assistant.Service.ValueMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ValueMappingController {

    private final ValueMappingService valueMappingService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ByteArrayResource> process(
            @RequestParam("file") MultipartFile file,
            @RequestParam("keyField") String source,
            @RequestParam("valueField") String target,
            @RequestParam("sourceAgency") String sourceAgency,
            @RequestParam("targetAgency") String targetAgency,
            @RequestParam("sourceIdentifier") String sourceIdentifier,
            @RequestParam("targetIdentifier") String targetIdentifier
    ) {

        log.info("Value Mapping generation started for file: {}",
                file.getOriginalFilename());

        ByteArrayResource resource =
                valueMappingService.generateValueMapping(
                        file,
                        source,
                        target,
                        sourceAgency,
                        targetAgency,
                        sourceIdentifier,
                        targetIdentifier
                );

        String fileName =
                source.replaceAll("[^a-zA-Z0-9_-]", "") +
                        "-" +
                        target.replaceAll("[^a-zA-Z0-9_-]", "") +
                        "_VM.csv";

        log.info("Value Mapping generation completed successfully. Output File: {}", fileName);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}