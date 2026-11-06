
package com.bevans.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.bevans.model.Endereco;
import com.bevans.model.Presenca;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.repository.PresencaRepository;
import com.bevans.service.TurmaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "4. Gestão de Turmas",
    description = "Turmas, passageiros e convites"
)
@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*")
public class TurmaController {

    private final TurmaService turmaService;
    private final PresencaRepository presencaRepository;

    public TurmaController(
            TurmaService turmaService,
            PresencaRepository presencaRepository
    ) {
        this.turmaService = turmaService;
        this.presencaRepository = presencaRepository;
    }

    private Map<String, Object> resumirEndereco(
            Endereco endereco
    ) {
        Map<String, Object> resultado = new HashMap<>();

        if (endereco == null) {
            return resultado;
        }

        resultado.put("id", endereco.getId());
        resultado.put("apelido", endereco.getApelido());
        resultado.put("rua", endereco.getRua());
        resultado.put("numero", endereco.getNumero());
        resultado.put("bairro", endereco.getBairro());
        resultado.put("latitude", endereco.getLatitude());
        resultado.put("longitude", endereco.getLongitude());

        return resultado;
    }

    private String enderecoCompleto(
            Endereco endereco
    ) {
        if (endereco == null) return null;

        return (
                (endereco.getRua() == null
                        ? ""
                        : endereco.getRua())
                + ", "
                + (endereco.getNumero() == null
                        ? "S/N"
                        : endereco.getNumero())
                + " - "
                + (endereco.getBairro() == null
                        ? ""
                        : endereco.getBairro())
        );
    }

    @Operation(summary = "Criar Turma")
    @PostMapping
    @PreAuthorize("@access.canCreateTurma(#p0)")
    public ResponseEntity<Turma> criarTurma(
            @RequestBody Turma turma
    ) {
        return ResponseEntity.ok(
                turmaService.criarTurma(turma)
        );
    }

    @Operation(summary = "Listar Turmas do Motorista")
    @GetMapping("/motorista/{motoristaId}")
    @PreAuthorize("@access.isMotorista() and @access.own(#p0)")
    public ResponseEntity<List<Turma>> listarPorMotorista(
            @PathVariable Long motoristaId
    ) {
        return ResponseEntity.ok(
                turmaService.listarTurmasPorMotorista(
                        motoristaId
                )
        );
    }

    @Operation(summary = "Listar Turmas do Passageiro")
    @GetMapping("/usuario/{usuarioId}/todas")
    @PreAuthorize("@access.own(#p0)")
    public ResponseEntity<List<Turma>> listarTurmasDoAluno(
            @PathVariable Long usuarioId
    ) {
        return ResponseEntity.ok(
                turmaService.listarTurmasDoAluno(
                        usuarioId
                )
        );
    }

    // Mantido para compatibilidade.
    @Operation(summary = "Buscar Turma do Aluno")
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("@access.own(#p0)")
    public ResponseEntity<Object> buscarTurmaPorUsuario(
            @PathVariable Long usuarioId
    ) {
        Turma turma =
                turmaService.buscarTurmaPorAlunoId(
                        usuarioId
                );

        if (turma == null) {
            return ResponseEntity.status(404).body(
                    Map.of(
                        "erro",
                        "Aluno não vinculado a nenhuma turma."
                    )
            );
        }

        return ResponseEntity.ok(turma);
    }

    @Operation(summary = "Listar Passageiros da Turma")
    @GetMapping("/{turmaId}/passageiros")
    @Transactional(readOnly = true)
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<Object> listarPassageirosDaTurma(
            @PathVariable Long turmaId
    ) {
        List<Usuario> passageiros =
                turmaService.listarPassageirosPorTurma(
                        turmaId
                );

        if (passageiros.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        LocalDate hoje = LocalDate.now(
                ZoneId.of("America/Recife")
        );

        List<Presenca> presencas =
                presencaRepository.findByDataAndTurmaId(
                        hoje, turmaId
                );

        Map<Long, Presenca> mapa = presencas
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUsuario().getId(),
                        p -> p
                ));

        List<Map<String, Object>> resultado =
                passageiros.stream().map(aluno -> {

                    Presenca presenca =
                            mapa.get(aluno.getId());

                    Endereco ida = null;
                    Endereco volta = null;

                    if (presenca != null) {
                        ida = presenca.getEnderecoIda();
                        volta = presenca.getEnderecoVolta();

                        String status =
                                presenca.getStatus();

                        // Compatibilidade com dados antigos.
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
                    }

                    String enderecoAntigo =
                            aluno.getEnderecoCompleto();

                    if (ida != null) {
                        enderecoAntigo =
                                enderecoCompleto(ida);
                    } else if (volta != null) {
                        enderecoAntigo =
                                enderecoCompleto(volta);
                    }

                    Map<String, Object> item =
                            new HashMap<>();

                    item.put("id", aluno.getId());
                    item.put("nome", aluno.getNome());
                    item.put(
                        "telefone",
                        aluno.getTelefone()
                    );

                    item.put(
                        "status",
                        presenca != null
                            ? presenca.getStatus()
                            : null
                    );

                    // Campo usado pela interface antiga.
                    item.put(
                        "enderecoCompleto",
                        enderecoAntigo
                    );

                    // Novos campos da confirmação.
                    item.put(
                        "enderecoIda",
                        ida != null
                            ? resumirEndereco(ida)
                            : null
                    );

                    item.put(
                        "enderecoVolta",
                        volta != null
                            ? resumirEndereco(volta)
                            : null
                    );

                    return item;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Adicionar Aluno à Turma")
    @PostMapping("/{turmaId}/alunos/{alunoId}")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<String> adicionarAlunoTurma(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId
    ) {
        turmaService.adicionarAluno(
                turmaId, alunoId
        );

        return ResponseEntity.ok(
                "Aluno vinculado com sucesso."
        );
    }

    @Operation(summary = "Remover Aluno da Turma")
    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<String> removerAlunoTurma(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId
    ) {
        turmaService.removerAluno(
                turmaId, alunoId
        );

        return ResponseEntity.ok(
                "Aluno removido com sucesso."
        );
    }

    @Operation(summary = "Atualizar Turma")
    @PutMapping("/{turmaId}")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<Turma> atualizarTurma(
            @PathVariable Long turmaId,
            @RequestBody Turma turmaAtualizada
    ) {
        return ResponseEntity.ok(
                turmaService.atualizarTurma(
                        turmaId,
                        turmaAtualizada
                )
        );
    }

    @Operation(summary = "Solicitar Entrada na Turma")
    @PostMapping("/entrar/{alunoId}")
    @PreAuthorize("@access.own(#p0)")
    public ResponseEntity<String> solicitarEntradaTurma(
            @PathVariable Long alunoId,
            @RequestParam String codigo
    ) {
        turmaService.solicitarEntradaPorCodigo(
                alunoId, codigo
        );

        return ResponseEntity.ok(
                "Solicitação enviada. Aguarde aprovação."
        );
    }

    @Operation(summary = "Listar Alunos Pendentes")
    @GetMapping("/{turmaId}/pendentes")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<Object> listarAlunosPendentes(
            @PathVariable Long turmaId
    ) {
        return ResponseEntity.ok(
                turmaService.listarAlunosPorStatus(
                        turmaId,
                        "PENDENTE"
                )
        );
    }

    @Operation(summary = "Analisar Solicitação")
    @PutMapping("/analisar/{vinculoId}")
    @PreAuthorize("@access.driverOfVinculo(#p0)")
    public ResponseEntity<String> analisarSolicitacaoAluno(
            @PathVariable Long vinculoId,
            @RequestParam String status
    ) {
        turmaService.analisarSolicitacao(
                vinculoId,
                status.toUpperCase()
        );

        return ResponseEntity.ok(
                "Status atualizado com sucesso."
        );
    }

    @Operation(summary = "Regenerar Código de Convite")
    @PutMapping("/{turmaId}/regenerar-codigo")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<Turma> regenerarCodigoConvite(
            @PathVariable Long turmaId
    ) {
        return ResponseEntity.ok(
                turmaService.regenerarCodigoConvite(
                        turmaId
                )
        );
    }

    @Operation(summary = "Excluir Turma")
    @DeleteMapping("/{turmaId}")
    @PreAuthorize("@access.driverOfTurma(#p0)")
    public ResponseEntity<String> excluirTurma(
            @PathVariable Long turmaId
    ) {
        turmaService.deletarTurma(turmaId);

        return ResponseEntity.ok(
                "Turma excluída com sucesso."
        );
    }
}
