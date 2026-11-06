
package com.bevans.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.bevans.dto.PassageiroRotaDTO;
import com.bevans.model.Endereco;
import com.bevans.model.Presenca;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.repository.AlunoTurmaRepository;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.PresencaRepository;
import com.bevans.repository.TurmaRepository;
import com.bevans.repository.UsuarioRepository;
import com.bevans.service.RotaService;
import com.bevans.service.ViagemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "2. Gestão de Rotas",
        description = "Presenças e otimização logística"
)
@RestController
@RequestMapping("/rota")
@CrossOrigin(origins = "*")
public class RotaController {

    private final PresencaRepository presencaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final TurmaRepository turmaRepository;
    private final RotaService rotaService;
    private final ViagemService viagemService;

    private static final Map<Long, Map<String, Double>>
            localizacoesVans = new ConcurrentHashMap<>();

    private static final Set<Long> rotasAtivas =
            ConcurrentHashMap.newKeySet();

    public RotaController(
            PresencaRepository presencaRepository,
            UsuarioRepository usuarioRepository,
            EnderecoRepository enderecoRepository,
            AlunoTurmaRepository alunoTurmaRepository,
            TurmaRepository turmaRepository,
            RotaService rotaService,
            ViagemService viagemService
    ) {
        this.presencaRepository = presencaRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.turmaRepository = turmaRepository;
        this.rotaService = rotaService;
        this.viagemService = viagemService;
    }

    private LocalDate hoje() {
        return LocalDate.now(
                ZoneId.of("America/Recife")
        );
    }

    private boolean passageiroAprovado(
            Long usuarioId,
            Long turmaId
    ) {
        return alunoTurmaRepository
                .existsByTurmaIdAndAlunoIdAndStatus(
                        turmaId,
                        usuarioId,
                        "APROVADO"
                );
    }

    private boolean coordenadasValidas(Endereco endereco) {
        if (endereco == null) {
            return false;
        }

        Double latitude = endereco.getLatitude();
        Double longitude = endereco.getLongitude();

        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180
                && !(latitude == 0.0 && longitude == 0.0);
    }

    private Endereco buscarEnderecoDoUsuario(
            Long enderecoId,
            Long usuarioId
    ) {
        Endereco endereco = enderecoRepository
                .findById(enderecoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Endereço não encontrado."
                        )
                );

        if (endereco.getUsuario() == null
                || !usuarioId.equals(
                        endereco.getUsuario().getId()
                )) {
            throw new IllegalArgumentException(
                    "Endereço não pertence ao passageiro."
            );
        }

        if (!coordenadasValidas(endereco)) {
            throw new IllegalArgumentException(
                    "Endereço sem coordenadas válidas."
            );
        }

        return endereco;
    }

    @Operation(summary = "Confirmar presença por turma")
    @CacheEvict(
            value = "rotasOtimizadas",
            allEntries = true
    )
    @PostMapping("/confirmar")
    @Transactional
    public ResponseEntity<String> confirmarPresenca(
            @RequestParam Long usuarioId,
            @RequestParam Long turmaId,
            @RequestParam String status,
            @RequestParam(required = false) Long enderecoIda,
            @RequestParam(required = false) Long enderecoVolta
    ) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Informe o status.");
        }

        String novoStatus = status.trim().toUpperCase();

        if (!Set.of(
                "IDA",
                "VOLTA",
                "AMBOS",
                "NAO_VOU",
                "LIMPAR"
        ).contains(novoStatus)) {
            return ResponseEntity.badRequest()
                    .body("Status inválido.");
        }

        if (!passageiroAprovado(usuarioId, turmaId)) {
            return ResponseEntity.status(403)
                    .body("Passageiro não aprovado nesta turma.");
        }

        Presenca presenca = presencaRepository
                .findByUsuarioIdAndTurmaIdAndData(
                        usuarioId,
                        turmaId,
                        hoje()
                )
                .orElse(null);

        if ("LIMPAR".equals(novoStatus)) {
            if (presenca != null) {
                presencaRepository.delete(presenca);
            }

            return ResponseEntity.ok("SUCESSO");
        }

        boolean temIda =
                "IDA".equals(novoStatus)
                || "AMBOS".equals(novoStatus);

        boolean temVolta =
                "VOLTA".equals(novoStatus)
                || "AMBOS".equals(novoStatus);

        if (temIda && enderecoIda == null) {
            return ResponseEntity.badRequest()
                    .body("Selecione o endereço da ida.");
        }

        if (temVolta && enderecoVolta == null) {
            return ResponseEntity.badRequest()
                    .body("Selecione o endereço da volta.");
        }

        Endereco ida = null;
        Endereco volta = null;

        try {
            if (temIda) {
                ida = buscarEnderecoDoUsuario(
                        enderecoIda,
                        usuarioId
                );
            }

            if (temVolta) {
                volta = buscarEnderecoDoUsuario(
                        enderecoVolta,
                        usuarioId
                );
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }

        if (presenca == null) {
            Usuario usuario = usuarioRepository
                    .findById(usuarioId)
                    .orElse(null);

            Turma turma = turmaRepository
                    .findById(turmaId)
                    .orElse(null);

            if (usuario == null || turma == null) {
                return ResponseEntity.badRequest()
                        .body("Usuário ou turma inexistente.");
            }

            presenca = new Presenca();
            presenca.setUsuario(usuario);
            presenca.setTurma(turma);
            presenca.setData(hoje());
        }

        presenca.setStatus(novoStatus);
        presenca.setEnderecoIda(ida);
        presenca.setEnderecoVolta(volta);

        // Mantém compatibilidade com consumidores antigos.
        presenca.setEndereco(
                ida != null ? ida : volta
        );

        presencaRepository.save(presenca);

        return ResponseEntity.ok("SUCESSO");
    }

    private Map<String, Object> resumirEndereco(
            Endereco endereco
    ) {
        if (endereco == null) {
            return null;
        }

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("id", endereco.getId());
        item.put("apelido", endereco.getApelido());
        item.put("rua", endereco.getRua());
        item.put("numero", endereco.getNumero());
        item.put("bairro", endereco.getBairro());

        return item;
    }

    @Operation(summary = "Consultar minha confirmação na turma")
    @GetMapping("/minha-confirmacao")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> minhaConfirmacao(
            @RequestParam Long usuarioId,
            @RequestParam Long turmaId
    ) {
        if (!passageiroAprovado(usuarioId, turmaId)) {
            return ResponseEntity.status(403).build();
        }

        Presenca presenca = presencaRepository
                .findByUsuarioIdAndTurmaIdAndData(
                        usuarioId,
                        turmaId,
                        hoje()
                )
                .orElse(null);

        Map<String, Object> resultado = new HashMap<>();

        if (presenca == null) {
            resultado.put("status", "");
            resultado.put("enderecoIda", null);
            resultado.put("enderecoVolta", null);

            return ResponseEntity.ok(resultado);
        }

        String status = presenca.getStatus();

        Endereco ida = presenca.getEnderecoIda();
        Endereco volta = presenca.getEnderecoVolta();

        // Compatibilidade com presenças antigas.
        if (ida == null
                && ("IDA".equals(status)
                || "AMBOS".equals(status))) {
            ida = presenca.getEndereco();
        }

        if (volta == null
                && ("VOLTA".equals(status)
                || "AMBOS".equals(status))) {
            volta = presenca.getEndereco();
        }

        resultado.put("status", status);
        resultado.put(
                "enderecoIda",
                resumirEndereco(ida)
        );
        resultado.put(
                "enderecoVolta",
                resumirEndereco(volta)
        );

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Iniciar Rota")
    @PostMapping("/iniciar")
    public ResponseEntity<Object> iniciarRota(
            @RequestBody Map<String, Object> payload
    ) {
        Object turmaIdObj = payload.get("turmaId");
        Object sentidoObj = payload.get("sentido");

        if (turmaIdObj == null || sentidoObj == null) {
            return ResponseEntity.badRequest().body(
                    "turmaId e sentido são obrigatórios."
            );
        }

        Long turmaId =
                Long.valueOf(turmaIdObj.toString());

        String sentido = sentidoObj.toString();

        viagemService.iniciarRota(turmaId, sentido);

        rotasAtivas.add(turmaId);
        localizacoesVans.remove(turmaId);

        return ResponseEntity.ok(
                Map.of(
                        "mensagem",
                        "Rota iniciada com sucesso."
                )
        );
    }

    @Operation(summary = "Encerrar Rota")
    @PostMapping("/encerrar")
    public ResponseEntity<String> encerrarRota(
            @RequestParam Long turmaId
    ) {
        viagemService.encerrarRota(turmaId);

        rotasAtivas.remove(turmaId);
        localizacoesVans.remove(turmaId);

        return ResponseEntity.ok(
                "Rota encerrada com sucesso."
        );
    }

    @Operation(summary = "Status Atual")
    @GetMapping("/status-atual")
    public ResponseEntity<Object> getStatusAtual(
            @RequestParam Long turmaId
    ) {
        return ResponseEntity.ok(
                viagemService.verificarStatusAtual(turmaId)
        );
    }

    @Operation(summary = "Otimizar Rota")
    @GetMapping("/otimizar")
    public CompletableFuture<ResponseEntity<List<PassageiroRotaDTO>>>
    otimizarRota(
            @RequestParam(required = false) String sentido,
            @RequestParam(required = false) Long turmaId
    ) {
        // Impede que parâmetros ausentes cheguem ao serviço.
        if (sentido == null || sentido.isBlank()
                || turmaId == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build()
            );
        }

        String sentidoNormalizado =
                sentido.trim().toUpperCase();

        if (!sentidoNormalizado.equals("IDA")
                && !sentidoNormalizado.equals("VOLTA")) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build()
            );
        }

        // Turma inexistente é 404, não 400.
        if (!turmaRepository.existsById(turmaId)) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.notFound().build()
            );
        }

        // Exceções inesperadas seguem para o tratamento global.
        return rotaService.otimizarRota(
                sentidoNormalizado,
                turmaId
        ).thenApply(ResponseEntity::ok);
    }

    @Operation(summary = "Atualizar Localização da Van")
    @PostMapping("/localizacao-van")
    public ResponseEntity<String> atualizarLocalizacaoVan(
            @RequestParam Long turmaId,
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        if (!rotasAtivas.contains(turmaId)) {
            return ResponseEntity.badRequest()
                    .body("Rota inativa para esta turma.");
        }

        Map<String, Double> coords =
                new ConcurrentHashMap<>();

        coords.put("latitude", latitude);
        coords.put("longitude", longitude);

        localizacoesVans.put(turmaId, coords);

        return ResponseEntity.ok(
                "Localização atualizada."
        );
    }

    @Operation(summary = "Buscar Localização da Van")
    @GetMapping("/localizacao-van")
    public ResponseEntity<Object> obterLocalizacaoVan(
            @RequestParam Long turmaId
    ) {
        if (!rotasAtivas.contains(turmaId)
                || !localizacoesVans.containsKey(turmaId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                localizacoesVans.get(turmaId)
        );
    }
}
