
package com.bevans.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bevans.model.AlunoTurma;

public interface AlunoTurmaRepository
        extends JpaRepository<AlunoTurma, Long> {

    List<AlunoTurma> findByTurmaIdAndStatus(
            Long turmaId,
            String status
    );

    Optional<AlunoTurma> findByTurmaIdAndAlunoId(
            Long turmaId,
            Long alunoId
    );

    List<AlunoTurma> findByAlunoIdAndStatus(
            Long alunoId,
            String status
    );

    List<AlunoTurma> findByAlunoId(
            Long alunoId
    );

    boolean existsByTurmaIdAndAlunoIdAndStatus(
            Long turmaId,
            Long alunoId,
            String status
    );
}
