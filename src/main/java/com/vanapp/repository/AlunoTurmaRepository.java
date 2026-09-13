package com.vanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vanapp.model.AlunoTurma;
import java.util.List;
import java.util.Optional;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {
    List<AlunoTurma> findByTurmaIdAndStatus(Long turmaId, String status);
    Optional<AlunoTurma> findByTurmaIdAndAlunoId(Long turmaId, Long alunoId);
}