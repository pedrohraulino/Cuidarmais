package cuidar.mais.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class ApiController {

    @GetMapping
    public ResponseEntity<Map<String, String>> getApiInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Cuidar Mais API is running");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}
