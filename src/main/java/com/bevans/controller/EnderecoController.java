package com.bevans.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bevans.model.Endereco;
import com.bevans.service.EnderecoService;

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

    @Operation(summary = "Cadastrar Endereço")
    @PostMapping("/usuario/{userId}")
    public ResponseEntity<Endereco> salvarEndereco(@PathVariable Long userId, @RequestBody Endereco novoEndereco) {
        return ResponseEntity.ok(enderecoService.cadastrarEndereco(userId, novoEndereco));
    }

    @Operation(summary = "Listar Endereços do Usuário")
    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<Endereco>> listarPorUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(enderecoService.listarPorUsuario(userId));
    }

    @Operation(summary = "Atualizar Endereço")
    @PutMapping("/{id}")
    public ResponseEntity<Endereco> atualizarEndereco(@PathVariable Long id, @RequestBody Endereco dadosAtualizados) {
        return ResponseEntity.ok(enderecoService.atualizarEndereco(id, dadosAtualizados));
    }

    @Operation(summary = "Excluir Endereço")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEndereco(@PathVariable Long id) {
        enderecoService.excluirEndereco(id);
        return ResponseEntity.ok().build();
    }
}