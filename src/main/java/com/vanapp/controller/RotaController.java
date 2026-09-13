package com.vanapp.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vanapp.model.Endereco;
import com.vanapp.model.Presenca;
import com.vanapp.model.Usuario;
import com.vanapp.repository.EnderecoRepository;
import com.vanapp.repository.PresencaRepository;
import com.vanapp.repository.UsuarioRepository;
import com.vanapp.service.RotaService;
import com.vanapp.service.ViagemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "2. Gestão de Rotas", description = "Operações de presença e otimização logística")
@RestController
@RequestMapping("/rota")
@CrossOrigin(origins = "*")
public class RotaController {

    private final PresencaRepository presencaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final RotaService rotaService;
    private final ViagemService viagemService;

    public RotaController(
            PresencaRepository presencaRepository, 
            UsuarioRepository usuarioRepository, 
            EnderecoRepository enderecoRepository,
            RotaService rotaService, 
            ViagemService viagemService
    ) {
        this.presencaRepository = presencaRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.rotaService = rotaService;
        this.viagemService = viagemService;
    }

    private static final Map<String, Double> localizacaoAtualVan = new ConcurrentHashMap<>();
    private static boolean rotaAtiva = false;

    @Operation(summary = "Iniciar Rota", description = "O motorista inicia a viagem oficialmente.")
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarRota(@RequestBody Map<String, Object> payload) {
        try {
            Object turmaIdObj = payload.get("turmaId");
            Object sentidoObj = payload.get("sentido");

            if (turmaIdObj == null || sentidoObj == null) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Dados inválidos: turmaId e sentido são obrigatórios."));
            }

            Long turmaId = Long.valueOf(turmaIdObj.toString());
            String sentido = sentidoObj.toString();
            
            viagemService.iniciarRota(turmaId, sentido);
            rotaAtiva = true;
            localizacaoAtualVan.clear();
            
            return ResponseEntity.ok(Map.of("mensagem", "Rota iniciada com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Dados inválidos: " + e.getMessage()));
        }
    }

    @Operation(summary = "Encerrar Rota", description = "O motorista finaliza a viagem oficialmente.")
    @PostMapping("/encerrar")
    public ResponseEntity<?> encerrarRota() {
        viagemService.encerrarRota();
        rotaAtiva = false;
        localizacaoAtualVan.clear();
        return ResponseEntity.ok("Rota encerrada com sucesso.");
    }

    @Operation(summary = "Status Atual", description = "Verifica se há viagem em andamento.")
    @GetMapping("/status-atual")
    public ResponseEntity<Map<String, Object>> getStatusAtual() {
        return ResponseEntity.ok(viagemService.verificarStatusAtual());
    }

    @Operation(summary = "Confirmar/Atualizar Presença", description = "Marca a presença e o endereço específico do passageiro para o dia atual ou limpa o registro.")
    @PostMapping("/confirmar")
    @Transactional
    public ResponseEntity<String> confirmarPresenca(
            @RequestParam Long usuarioId, 
            @RequestParam String status,
            @RequestParam(required = false) Long enderecoId
    ) {
        if (usuarioId == null) return ResponseEntity.badRequest().body("usuarioId não pode ser nulo");

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        Presenca p = presencaRepository.findByUsuarioIdAndData(usuarioId, hoje);

        if ("LIMPAR".equals(status)) {
            if (p != null) presencaRepository.delete(p);
        } else {
            if (p == null) {
                p = new Presenca();
                Usuario user = usuarioRepository.findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
                p.setUsuario(user);
                p.setData(hoje);
            }
            p.setStatus(status);

            if (enderecoId != null) {
                Endereco endereco = enderecoRepository.findById(enderecoId)
                        .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
                p.setEndereco(endereco);
            } else {
                p.setEndereco(null);
            }

            presencaRepository.save(p);
        }
        return ResponseEntity.ok("SUCESSO");
    }

    @Operation(summary = "Otimizar Rota", description = "Calcula a melhor sequência de paradas com os endereços do dia.")
    @GetMapping("/otimizar")
    public ResponseEntity<?> otimizarRota(@RequestParam String sentido) {
        try {
            return ResponseEntity.ok(rotaService.otimizarRota(sentido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", "Erro interno: " + e.getMessage()));
        }
    }

    @Operation(summary = "Atualizar Localização", description = "Motorista transmite coordenadas reais.")
    @PostMapping("/localizacao-van")
    public ResponseEntity<String> atualizarLocalizacaoVan(@RequestParam Double latitude, @RequestParam Double longitude) {
        if (!rotaAtiva) return ResponseEntity.badRequest().body("Rota inativa.");
        
        localizacaoAtualVan.put("latitude", latitude);
        localizacaoAtualVan.put("longitude", longitude);
        return ResponseEntity.ok("Localização atualizada.");
    }

    @Operation(summary = "Buscar Localização", description = "Passageiro consulta radar. Retorna 404 se a rota estiver inativa.")
    @GetMapping("/localizacao-van")
    public ResponseEntity<Map<String, Double>> obterLocalizacaoVan() {
        if (!rotaAtiva || localizacaoAtualVan.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(localizacaoAtualVan);
    }
}