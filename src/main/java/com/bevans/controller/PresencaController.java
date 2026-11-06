
package com.bevans.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "3. Gestão de Presenças",
    description = "Controle de embarque diário"
)
@RestController
@RequestMapping("/presenca")
@CrossOrigin(origins = "*")
public class PresencaController {

    @Operation(
        summary = "Endpoint legado de presença",
        description = "Use /rota/confirmar com turmaId."
    )
    @PostMapping("/marcar")
    public ResponseEntity<String> marcarPresenca(
            @RequestBody Map<String, Object> payload
    ) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(
                    "Este endpoint foi substituído. "
                    + "Utilize POST /rota/confirmar "
                    + "informando usuarioId, turmaId, "
                    + "status, enderecoIda e enderecoVolta."
                );
    }
}
