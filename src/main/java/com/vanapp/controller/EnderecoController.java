package com.vanapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vanapp.model.Endereco;
import com.vanapp.service.EnderecoService;

@RestController
@RequestMapping("/enderecos")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<Endereco> cadastrar(@PathVariable Long usuarioId, @RequestBody Endereco endereco) {
        Endereco novoEndereco = enderecoService.cadastrarEndereco(usuarioId, endereco);
        return ResponseEntity.ok(novoEndereco);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Endereco>> listarPorUsuario(@PathVariable Long usuarioId) {
        List<Endereco> enderecos = enderecoService.listarPorUsuario(usuarioId);
        return ResponseEntity.ok(enderecos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Endereco> atualizar(@PathVariable Long id, @RequestBody Endereco endereco) {
        Endereco atualizado = enderecoService.atualizarEndereco(id, endereco);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        enderecoService.excluirEndereco(id);
        return ResponseEntity.noContent().build();
    }
}