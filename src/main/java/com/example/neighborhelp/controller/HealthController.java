package com.example.neighborhelp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // Responde a AMBAS rutas
    @GetMapping(value = {"/actuator/health", "/health"})
    public String health() {
        return "{\"status\":\"UP\"}";  // Formato JSON que esperan muchos healthchecks
    }

    @GetMapping("/")
    public String home() {
        return "NeighborHelp Backend is running";
    }
}