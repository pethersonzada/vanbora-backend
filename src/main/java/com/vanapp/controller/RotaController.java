package com.vanapp.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    private static final Map<Long, Map<String, Double>> localizacoesVans = new ConcurrentHashMap<>();
    private static final Set<Long> rotasAtivas = ConcurrentHashMap.newKeySet();

    @Operation(summary = "Iniciar Rota", description = "O motorista inicia a viagem oficialmente.")
    @PostMapping("/iniciar")
    public ResponseEntity> iniciarRota(@RequestBody Map<String, Object> payload) {
        Object turmaIdObj = payload.get("turmaId");
        Object sentidoObj = payload.get("sentido");

        if (turmaIdObj == null || sentidoObj == null) {
            throw new IllegalArgumentException("Dados inválidos: turmaId e sentido são obrigatórios.");
        }

        Long turmaId = Long.valueOf(turmaIdObj.toString());
        String sentido = sentidoObj.toString();
        
        viagemService.iniciarRota(turmaId, sentido);
        rotasAtivas.add(turmaId);
        localizacoesVans.remove(turmaId);
        
        return ResponseEntity.ok(Map.of("mensagem", "Rota iniciada com sucesso."));
    }

    @Operation(summary = "Encerrar Rota", description = "O motorista finaliza a viagem oficialmente.")
    @PostMapping("/encerrar")
    public ResponseEntity<String> encerrarRota(@RequestParam Long turmaId) {
        viagemService.encerrarRota(turmaId);
        rotasAtivas.remove(turmaId);
        localizacoesVans.remove(turmaId);
        return ResponseEntity.ok("Rota encerrada com sucesso.");
    }

    @Operation(summary = "Status Atual", description = "Verifica se há viagem em andamento.")
    @GetMapping("/status-atual")
    public ResponseEntity> getStatusAtual(@RequestParam Long turmaId) {
        return ResponseEntity.ok(viagemService.verificarStatusAtual(turmaId));
    }

    @Operation(summary = "Confirmar/Atualizar Presença", description = "Marca a presença e o endereço específico do passageiro para o dia atual ou limpa o registro.")
    @CacheEvict(value = "rotasOtimizadas", allEntries = true)
    @PostMapping("/confirmar")
    @Transactional
    public ResponseEntity<String> confirmarPresenca(
            @RequestParam Long usuarioId, 
            @RequestParam String status,
            @RequestParam(required = false) Long enderecoId
    ) {
        if (usuarioId == null) throw new IllegalArgumentException("usuarioId não pode ser nulo");

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
    public CompletableFuture<ResponseEntity<Object>> otimizarRota(@RequestParam String sentido, @RequestParam Long turmaId) {
        return rotaService.otimizarRota(sentido, turmaId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    throw new RuntimeException(cause.getMessage(), cause);
                });
    }

    @Operation(summary = "Atualizar Localização", description = "Motorista transmite coordenadas reais.")
    @PostMapping("/localizacao-van")
    public ResponseEntity<String> atualizarLocalizacaoVan(@RequestParam Long turmaId, @RequestParam Double latitude, @RequestParam Double longitude) {
        if (!rotasAtivas.contains(turmaId)) throw new RuntimeException("Rota inativa para esta turma.");
        
        Map<String, Double> coords = new ConcurrentHashMap<>();
        coords.put("latitude", latitude);
        coords.put("longitude", longitude);
        
        localizacoesVans.put(turmaId, coords);
        return ResponseEntity.ok("Localização atualizada.");
    }

    @Operation(summary = "Buscar Localização", description = "Passageiro consulta radar. Retorna 404 se a rota estiver inativa.")
    @GetMapping("/localizacao-van")
    public ResponseEntity> obterLocalizacaoVan(@RequestParam Long turmaId) {
        if (!rotasAtivas.contains(turmaId) || !localizacoesVans.containsKey(turmaId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(localizacoesVans.get(turmaId));
    }
}