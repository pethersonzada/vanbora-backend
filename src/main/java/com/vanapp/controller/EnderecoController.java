package com.vanapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanapp.model.Endereco;
import com.vanapp.service.EnderecoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Endereços", description = "Gestão de endereços dos usuários e motoristas")
@RestController
@RequestMapping("/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @Operation(summary = "Cadastrar Endereço", description = "Salva um novo endereço para o usuário e atualiza as coordenadas principais se for o motorista.")
    @PostMapping("/usuario/{userId}")
    public ResponseEntity<?> salvarEndereco(@PathVariable Long userId, @RequestBody Endereco novoEndereco) {
        try {
            Endereco salvo = enderecoService.cadastrarEndereco(userId, novoEndereco);
            return ResponseEntity.ok(salvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao salvar endereço: " + e.getMessage());
        }
    }

    @Operation(summary = "Listar Endereços do Usuário", description = "Retorna todos os endereços cadastrados de um usuário específico.")
    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<Endereco>> listarPorUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(enderecoService.listarPorUsuario(userId));
    }

    @Operation(summary = "Atualizar Endereço", description = "Atualiza os dados de um endereço existente (ex: apelido).")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarEndereco(@PathVariable Long id, @RequestBody Endereco dadosAtualizados) {
        try {
            Endereco atualizado = enderecoService.atualizarEndereco(id, dadosAtualizados);
            return ResponseEntity.ok(atualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar endereço: " + e.getMessage());
        }
    }

    @Operation(summary = "Excluir Endereço", description = "Remove um endereço do usuário.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirEndereco(@PathVariable Long id) {
        try {
            enderecoService.excluirEndereco(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir endereço: " + e.getMessage());
        }
    }
}