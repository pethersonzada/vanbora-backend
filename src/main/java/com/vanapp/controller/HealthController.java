package com.vanapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "0. Sistema", description = "Verificação de status e saúde da API")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "Verificar Status do Servidor", description = "Retorna uma resposta simples indicando que a API está online e operando corretamente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servidor ativo e operacional"),
    })
    @GetMapping
    public ResponseEntity<String> check() {
        return ResponseEntity.ok("OK");
    }
}