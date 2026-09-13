package com.vanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vanapp.model.Turma;
import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findByMotoristaId(Long motoristaId);
    Optional<Turma> findByCodigoConvite(String codigoConvite);
}