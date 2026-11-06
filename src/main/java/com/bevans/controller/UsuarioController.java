package com.bevans.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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

import com.bevans.dto.EnderecoRequest;
import com.bevans.model.Usuario;
import com.bevans.repository.PresencaRepository;
import com.bevans.repository.UsuarioRepository;
import com.bevans.service.UsuarioService;
import com.bevans.security.AccessPolicy;

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
    private final AccessPolicy access;

    @Value("${MOTORISTA_CADASTRO_CODIGO:}")
    private String codigoMotoristaServidor;

    public UsuarioController(UsuarioRepository usuarioRepository, PresencaRepository presencaRepository, UsuarioService usuarioService, AccessPolicy access) {
        this.usuarioRepository = usuarioRepository;
        this.presencaRepository = presencaRepository;
        this.usuarioService = usuarioService;
        this.access = access;
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

@Operation(
    summary = "Listar Passageiros",
    description = "Retorna todos os passageiros cadastrados."
)
@Transactional(readOnly = true)
@GetMapping("/passageiros")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<PassageiroDTO>>
listarPassageiros() {

    List<Usuario> passageiros =
            access.passageirosVisiveis();

    return ResponseEntity.ok(
            passageiros.stream()
                    .map(usuario ->
                            new PassageiroDTO(
                                    usuario,
                                    null
                            )
                    )
                    .collect(Collectors.toList())
    );
}


    @Operation(summary = "Buscar Motorista", description = "Busca o perfil do motorista vinculado ao sistema.")
    @GetMapping("/motorista")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Usuario> getMotorista() {
        Usuario motorista = access.motoristaVisivel();
        return motorista == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(motorista);
    }

    @Operation(summary = "Buscar por Firebase UID", description = "Retorna os dados do usuário com base no UID do Firebase Authentication.")
    @GetMapping("/por-uid/{firebaseUid}")
    @PreAuthorize("@access.ownsUid(#p0)")
    public ResponseEntity<Usuario> buscarPorFirebaseUid(@PathVariable String firebaseUid) {
        return usuarioRepository.findByFirebaseUid(firebaseUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @Operation(summary = "Cadastrar Usuário autenticado no Firebase")
    @PostMapping("/cadastrar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cadastrar(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        String uid = access.uid();
        String firebaseEmail = (String) request.getAttribute("firebaseEmail");
        String uidInformado = valor(payload, "firebaseUid");
        String emailInformado = valor(payload, "email");
        String tipo = valor(payload, "tipo").toUpperCase(java.util.Locale.ROOT);

        if (uid == null || firebaseEmail == null
                || !Objects.equals(uid, uidInformado)
                || !firebaseEmail.equalsIgnoreCase(emailInformado)) {
            return ResponseEntity.status(403).body(
                    Map.of("erro", "Dados não correspondem ao token Firebase."));
        }

        if (!"PASSAGEIRO".equals(tipo) && !"MOTORISTA".equals(tipo)) {
            return ResponseEntity.badRequest().body(
                    Map.of("erro", "Tipo de usuário inválido."));
        }

        // A autorização de motorista deve vir do servidor.
        // Um código presente no aplicativo não é uma credencial segura.
        if ("MOTORISTA".equals(tipo)) {
            String codigoServidor = codigoMotoristaServidor;
            String codigoRecebido = valor(payload, "codigoAcesso");
            if (codigoServidor == null || codigoServidor.isBlank()
                    || !codigoServidor.equals(codigoRecebido)) {
                return ResponseEntity.status(403).body(
                        Map.of("erro", "Cadastro de motorista não autorizado."));
            }
        }

        if (usuarioRepository.findByFirebaseUid(uid).isPresent()) {
            return ResponseEntity.status(409).body(
                    Map.of("erro", "Identidade Firebase já cadastrada."));
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(valor(payload, "nome"));
        novoUsuario.setEmail(firebaseEmail);
        novoUsuario.setFirebaseUid(uid);
        novoUsuario.setTelefone(valor(payload, "telefone"));
        novoUsuario.setTipo(tipo);
        novoUsuario.setEnderecoCompleto("Endereço Pendente");
        novoUsuario.setLatitude(0.0);
        novoUsuario.setLongitude(0.0);

        if (novoUsuario.getNome().isBlank() || novoUsuario.getTelefone().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("erro", "Nome e telefone são obrigatórios."));
        }
        return ResponseEntity.ok(usuarioService.cadastrarUsuario(novoUsuario));
    }

    private String valor(Map<String, Object> dados, String chave) {
        if (dados == null || dados.get(chave) == null) return "";
        return dados.get(chave).toString().trim();
    }

    @Operation(summary = "Atualizar Endereço", description = "Atualiza latitude, longitude e endereço completo do passageiro.")
    @PutMapping("/salvar-endereco")
    @PreAuthorize("@access.own(#p0.idUsuario)")
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

    @Operation(summary = "Atualizar E-mail", description = "Atualiza o e-mail do usuário no banco local.")
    @PutMapping("/{id}/email")
    @PreAuthorize("@access.own(#p0)")
    public ResponseEntity<?> atualizarEmail(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String novoEmail = body.get("email");
            Usuario usuarioAtualizado = usuarioService.atualizarEmail(id, novoEmail);
            return ResponseEntity.ok(usuarioAtualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", "Erro interno: " + e.getMessage()));
        }
    }

    @Operation(summary = "Excluir Usuário", description = "Apaga permanentemente a conta do usuário do sistema.")
    @DeleteMapping("/{id}")
    @PreAuthorize("@access.own(#p0)")
    public ResponseEntity<?> excluirConta(@PathVariable Long id) {
        try {
            usuarioService.excluirUsuario(id);
            return ResponseEntity.ok(Map.of("mensagem", "Conta excluída com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}