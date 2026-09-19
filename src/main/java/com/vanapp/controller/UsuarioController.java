package com.vanapp.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanapp.dto.EnderecoRequest;
import com.vanapp.model.Presenca;
import com.vanapp.model.Usuario;
import com.vanapp.repository.PresencaRepository;
import com.vanapp.repository.UsuarioRepository;
import com.vanapp.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Gestão de Usuários", description = "Operações de Motoristas e Passageiros")
@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PresencaRepository presencaRepository;
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioRepository usuarioRepository, PresencaRepository presencaRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.presencaRepository = presencaRepository;
        this.usuarioService = usuarioService;
    }

    public static class PassageiroDTO {
        public Long id;
        public String nome;
        public String status;
        public Double latitude;
        public Double longitude;
        public String tipo;

        public PassageiroDTO(Usuario u, String status) {
            this.id = u.getId();
            this.nome = u.getNome();
            this.status = status;
            this.latitude = u.getLatitude();
            this.longitude = u.getLongitude();
            this.tipo = u.getTipo();
        }
    }

    @Operation(summary = "Listar Passageiros", description = "Retorna todos os passageiros ativos e o status de presença do dia.")
    @Transactional(readOnly = true)
    @GetMapping("/passageiros")
    public ResponseEntity<List<PassageiroDTO>> listarPassageiros() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        List<Usuario> passageiros = usuarioRepository.findByTipo("PASSAGEIRO");
        return ResponseEntity.ok(passageiros.stream().map(u -> {
            Long uid = u.getId();
            if (uid == null) return new PassageiroDTO(u, null);
            Presenca p = presencaRepository.findByUsuarioIdAndData(uid, hoje);
            return new PassageiroDTO(u, (p != null) ? p.getStatus() : null);
        }).collect(Collectors.toList()));
    }

    @Operation(summary = "Buscar Motorista", description = "Busca o perfil do motorista vinculado ao sistema.")
    @GetMapping("/motorista")
    public ResponseEntity<Usuario> getMotorista() {
        return usuarioRepository.findByTipo("MOTORISTA")
                .stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @Operation(summary = "Buscar por Firebase UID", description = "Retorna os dados do usuário com base no UID do Firebase Authentication.")
    @GetMapping("/por-uid/{firebaseUid}")
    public ResponseEntity<Usuario> buscarPorFirebaseUid(@PathVariable String firebaseUid) {
        return usuarioRepository.findByFirebaseUid(firebaseUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @Operation(summary = "Cadastrar Usuário", description = "Cria um novo registro.")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario novoUsuario) {
        try {
            Usuario usuarioSalvo = usuarioService.cadastrarUsuario(novoUsuario);
            return ResponseEntity.ok(usuarioSalvo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Atualizar Endereço", description = "Atualiza latitude, longitude e endereço completo do passageiro.")
    @PutMapping("/salvar-endereco")
    public ResponseEntity<?> atualizarEndereco(@RequestBody EnderecoRequest request) {
        Long idUsuario = request.getIdUsuario();
        if (idUsuario == null) {
            return ResponseEntity.badRequest().body("idUsuario não pode ser nulo");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + idUsuario));
        usuario.setLatitude(request.getLatitude());
        usuario.setLongitude(request.getLongitude());
        usuario.setEnderecoCompleto(request.getEnderecoCompleto());
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Excluir Usuário", description = "Apaga permanentemente a conta do usuário do sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirConta(@PathVariable Long id) {
        try {
            usuarioService.excluirUsuario(id);
            return ResponseEntity.ok(Map.of("mensagem", "Conta excluída com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}