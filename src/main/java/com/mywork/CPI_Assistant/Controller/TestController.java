package com.mywork.CPI_Assistant.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "SUCCESS");
        response.put("message", "Spring Boot Backend is Working");
        response.put("code", 200);

        return response;
    }
}
