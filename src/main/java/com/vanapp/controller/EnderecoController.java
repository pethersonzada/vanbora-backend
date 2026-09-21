package com.vanapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanapp.model.Endereco;
import com.vanapp.model.Usuario;
import com.vanapp.repository.EnderecoRepository;
import com.vanapp.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Endereços", description = "Gestão de endereços dos usuários e motoristas")
@RestController
@RequestMapping("/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public EnderecoController(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Cadastrar Endereço", description = "Salva um novo endereço para o usuário e atualiza as coordenadas principais se for o motorista.")
    @PostMapping("/usuario/{userId}")
    @Transactional
    public ResponseEntity<?> salvarEndereco(@PathVariable Long userId, @RequestBody Endereco novoEndereco) {
        try {
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            novoEndereco.setUsuario(usuario);
            enderecoRepository.save(novoEndereco);

            if (novoEndereco.getLatitude() != null && novoEndereco.getLongitude() != null) {
                usuario.setLatitude(novoEndereco.getLatitude());
                usuario.setLongitude(novoEndereco.getLongitude());
                usuarioRepository.save(usuario);
            }

            return ResponseEntity.ok("Endereço cadastrado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao salvar endereço: " + e.getMessage());
        }
    }
}