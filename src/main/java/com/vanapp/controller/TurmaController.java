package com.vanapp.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<?> criarTurma(@RequestBody Turma turma) {
        try {
            Turma novaTurma = turmaService.criarTurma(turma);
            return ResponseEntity.ok(novaTurma);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Listar Turmas do Motorista", description = "Retorna todas as turmas vinculadas a um motorista específico.")
    @GetMapping("/motorista/{motoristaId}")
    public ResponseEntity<List<Turma>> listarPorMotorista(@PathVariable Long motoristaId) {
        List<Turma> turmas = turmaService.listarTurmasPorMotorista(motoristaId);
        return ResponseEntity.ok(turmas);
    }

    @Operation(summary = "Listar Passageiros da Turma", description = "Retorna os passageiros vinculados a uma turma junto com o status de presença e o endereço correto do dia.")
    @GetMapping("/{turmaId}/passageiros")
    public ResponseEntity<List<Map<String, Object>>> listarPassageirosDaTurma(@PathVariable Long turmaId) {
        List<Usuario> passageiros = turmaService.listarPassageirosPorTurma(turmaId);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));

        List<Map<String, Object>> resultado = passageiros.stream().map(aluno -> {
            Presenca presenca = presencaRepository.findByUsuarioIdAndData(aluno.getId(), hoje);
            
            String enderecoFinal = aluno.getEnderecoCompleto();
            if (presenca != null && presenca.getEndereco() != null) {
                var end = presenca.getEndereco();
                enderecoFinal = end.getRua() + ", " + end.getNumero() + " - " + end.getBairro();
            }
            
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", aluno.getId());
            map.put("nome", aluno.getNome());
            map.put("telefone", aluno.getTelefone());
            map.put("enderecoCompleto", enderecoFinal);
            map.put("status", presenca != null ? presenca.getStatus() : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Adicionar Aluno à Turma", description = "Vincula um usuário/passageiro a uma turma específica.")
    @PostMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> adicionarAlunoTurma(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        try {
            turmaService.adicionarAluno(turmaId, alunoId);
            return ResponseEntity.ok("Aluno vinculado com sucesso.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Remover Aluno da Turma", description = "Desvincula um usuário/passageiro de uma turma específica.")
    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> removerAlunoTurma(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        try {
            turmaService.removerAluno(turmaId, alunoId);
            return ResponseEntity.ok("Aluno removido com sucesso.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Buscar Turma do Aluno", description = "Retorna a turma vinculada a um usuário/aluno específico.")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> buscarTurmaPorUsuario(@PathVariable Long usuarioId) {
        try {
            Turma turma = turmaService.buscarTurmaPorAlunoId(usuarioId);
            if (turma == null) {
                return ResponseEntity.status(404).body("Aluno não vinculado a nenhuma turma.");
            }
            return ResponseEntity.ok(turma);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Atualizar Turma", description = "Altera o nome ou turno de uma turma existente.")
    @PutMapping("/{turmaId}")
    public ResponseEntity<?> atualizarTurma(@PathVariable Long turmaId, @RequestBody Turma turmaAtualizada) {
        try {
            Turma turmaModificada = turmaService.atualizarTurma(turmaId, turmaAtualizada);
            return ResponseEntity.ok(turmaModificada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Solicitar Entrada na Turma", description = "Permite ao aluno solicitar entrada em uma turma usando o código de convite.")
    @PostMapping("/entrar/{alunoId}")
    public ResponseEntity<?> solicitarEntradaTurma(@PathVariable Long alunoId, @RequestParam String codigo) {
        try {
            turmaService.solicitarEntradaPorCodigo(alunoId, codigo);
            return ResponseEntity.ok("Solicitação enviada com sucesso! Aguarde a aprovação do motorista.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Listar Alunos Pendentes", description = "Retorna os alunos que solicitaram entrada mas ainda não foram aprovados.")
    @GetMapping("/{turmaId}/pendentes")
    public ResponseEntity<?> listarAlunosPendentes(@PathVariable Long turmaId) {
        try {
            return ResponseEntity.ok(turmaService.listarAlunosPorStatus(turmaId, "PENDENTE"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Aprovar ou Rejeitar Aluno", description = "Permite ao motorista aceitar ou recusar a solicitação de entrada do aluno.")
    @PutMapping("/analisar/{vinculoId}")
    public ResponseEntity<?> analisarSolicitacaoAluno(@PathVariable Long vinculoId, @RequestParam String status) {
        try {
            turmaService.analisarSolicitacao(vinculoId, status.toUpperCase());
            return ResponseEntity.ok("Status atualizado com sucesso.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Regenerar Código de Convite", description = "Gera um novo código de convite para a turma sem perder os dados ou vínculos.")
    @PutMapping("/{turmaId}/regenerar-codigo")
    public ResponseEntity<?> regenerarCodigoConvite(@PathVariable Long turmaId) {
        try {
            Turma turmaAtualizada = turmaService.regenerarCodigoConvite(turmaId);
            return ResponseEntity.ok(turmaAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Excluir Turma", description = "Remove uma turma e seus vínculos do sistema.")
    @DeleteMapping("/{turmaId}")
    public ResponseEntity<?> excluirTurma(@PathVariable Long turmaId) {
        try {
            turmaService.deletarTurma(turmaId);
            return ResponseEntity.ok("Turma excluída com sucesso.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}