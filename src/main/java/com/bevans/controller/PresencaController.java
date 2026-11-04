package com.bevans.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vanapp.model.Presenca;
import com.vanapp.model.Usuario;
import com.vanapp.repository.PresencaRepository;
import com.vanapp.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "3. Gestão de Presenças", description = "Operações para controle de embarque diário")
@RestController
@RequestMapping("/presenca")
@CrossOrigin(origins = "*")
public class PresencaController {

    private final PresencaRepository presencaRepository;
    private final UsuarioRepository usuarioRepository;

    public PresencaController(PresencaRepository presencaRepository, UsuarioRepository usuarioRepository) {
        this.presencaRepository = presencaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Marcar Presença", description = "Registra o status do passageiro para a data atual.")
    @CacheEvict(value = "rotasOtimizadas", allEntries = true)
    @PostMapping("/marcar")
    public ResponseEntity<String> marcarPresenca(@RequestBody Map<String, Object> payload) {
        Object rawId = payload.get("usuarioId");
        if (rawId == null) {
            rawId = payload.get("passageiroId");
        }

        if (rawId == null) {
            throw new IllegalArgumentException("O usuarioId é obrigatório para registrar a presença.");
        }

        Long usuarioId = Long.valueOf(rawId.toString());
        String status = (String) payload.get("status");

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no sistema."));

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        Presenca existente = presencaRepository.findByUsuarioIdAndData(usuarioId, hoje);

        if (existente != null) {
            existente.setStatus(status);
            existente.setViagem(null);
            presencaRepository.save(existente);
        } else {
            Presenca novaPresenca = new Presenca();
            novaPresenca.setUsuario(usuario);
            novaPresenca.setData(hoje);
            novaPresenca.setStatus(status);
            novaPresenca.setViagem(null);
            presencaRepository.save(novaPresenca);
        }

        return ResponseEntity.ok("Presença registrada com sucesso");
    }
}