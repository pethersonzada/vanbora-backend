package com.vanapp.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vanapp.model.Presenca;
import com.vanapp.model.Turma;
import com.vanapp.model.Usuario;
import com.vanapp.repository.PresencaRepository;
import com.vanapp.service.TurmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "4. Gestão de Turmas", description = "Endpoints para gerenciamento de rotas e turnos")
@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*")
public class TurmaController {

    private final TurmaService turmaService;
    private final PresencaRepository presencaRepository;

    public TurmaController(TurmaService turmaService, PresencaRepository presencaRepository) {
        this.turmaService = turmaService;
        this.presencaRepository = presencaRepository;
    }

    @Operation(summary = "Criar Turma", description = "Cadastra uma nova rota/turno para o motorista.")
    @PostMapping
    public ResponseEntity<Turma> criarTurma(@RequestBody Turma turma) {
        return ResponseEntity.ok(turmaService.criarTurma(turma));
    }

    @Operation(summary = "Listar Turmas do Motorista")
    @GetMapping("/motorista/{motoristaId}")
    public ResponseEntity<List<Turma>> listarPorMotorista(@PathVariable Long motoristaId) {
        return ResponseEntity.ok(turmaService.listarTurmasPorMotorista(motoristaId));
    }

    @Operation(summary = "Listar Passageiros da Turma")
    @GetMapping("/{turmaId}/passageiros")
    public ResponseEntity<List>> listarPassageirosDaTurma(@PathVariable Long turmaId) {
        List<Usuario> passageiros = turmaService.listarPassageirosPorTurma(turmaId);
        if (passageiros.isEmpty()) return ResponseEntity.ok(List.of());

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        List<Long> ids = passageiros.stream().map(Usuario::getId).toList();
        List<Presenca> presencas = presencaRepository.findByUsuarioIdInAndData(ids, hoje);
        
        Map<Long, Presenca> mapPresencas = presencas.stream()
                .collect(Collectors.toMap(p -> p.getUsuario().getId(), p -> p));

        List> resultado = passageiros.stream().map(aluno -> {
            Presenca p = mapPresencas.get(aluno.getId());
            String endereco = aluno.getEnderecoCompleto();
            if (p != null && p.getEndereco() != null) {
                endereco = p.getEndereco().getRua() + ", " + p.getEndereco().getNumero() + " - " + p.getEndereco().getBairro();
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", aluno.getId());
            map.put("nome", aluno.getNome());
            map.put("telefone", aluno.getTelefone());
            map.put("enderecoCompleto", endereco);
            map.put("status", p != null ? p.getStatus() : null);
            return map;
        }).toList();

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Adicionar Aluno à Turma")
    @PostMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<String> adicionarAlunoTurma(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        turmaService.adicionarAluno(turmaId, alunoId);
        return ResponseEntity.ok("Aluno vinculado com sucesso.");
    }

    @Operation(summary = "Remover Aluno da Turma")
    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<String> removerAlunoTurma(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        turmaService.removerAluno(turmaId, alunoId);
        return ResponseEntity.ok("Aluno removido com sucesso.");
    }

    @Operation(summary = "Buscar Turma do Aluno")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Object> buscarTurmaPorUsuario(@PathVariable Long usuarioId) {
        Turma turma = turmaService.buscarTurmaPorAlunoId(usuarioId);
        if (turma == null) return ResponseEntity.status(404).body(Map.of("erro", "Aluno não vinculado a nenhuma turma."));
        return ResponseEntity.ok(turma);
    }

    @Operation(summary = "Atualizar Turma")
    @PutMapping("/{turmaId}")
    public ResponseEntity<Turma> atualizarTurma(@PathVariable Long turmaId, @RequestBody Turma turmaAtualizada) {
        return ResponseEntity.ok(turmaService.atualizarTurma(turmaId, turmaAtualizada));
    }

    @Operation(summary = "Solicitar Entrada na Turma")
    @PostMapping("/entrar/{alunoId}")
    public ResponseEntity<String> solicitarEntradaTurma(@PathVariable Long alunoId, @RequestParam String codigo) {
        turmaService.solicitarEntradaPorCodigo(alunoId, codigo);
        return ResponseEntity.ok("Solicitação enviada com sucesso! Aguarde a aprovação do motorista.");
    }

    @Operation(summary = "Listar Alunos Pendentes")
    @GetMapping("/{turmaId}/pendentes")
    public ResponseEntity<Object> listarAlunosPendentes(@PathVariable Long turmaId) {
        return ResponseEntity.ok(turmaService.listarAlunosPorStatus(turmaId, "PENDENTE"));
    }

    @Operation(summary = "Aprovar ou Rejeitar Aluno")
    @PutMapping("/analisar/{vinculoId}")
    public ResponseEntity<String> analisarSolicitacaoAluno(@PathVariable Long vinculoId, @RequestParam String status) {
        turmaService.analisarSolicitacao(vinculoId, status.toUpperCase());
        return ResponseEntity.ok("Status atualizado com sucesso.");
    }

    @Operation(summary = "Regenerar Código de Convite")
    @PutMapping("/{turmaId}/regenerar-codigo")
    public ResponseEntity<Turma> regenerarCodigoConvite(@PathVariable Long turmaId) {
        return ResponseEntity.ok(turmaService.regenerarCodigoConvite(turmaId));
    }

    @Operation(summary = "Excluir Turma")
    @DeleteMapping("/{turmaId}")
    public ResponseEntity<String> excluirTurma(@PathVariable Long turmaId) {
        turmaService.deletarTurma(turmaId);
        return ResponseEntity.ok("Turma excluída com sucesso.");
    }
}