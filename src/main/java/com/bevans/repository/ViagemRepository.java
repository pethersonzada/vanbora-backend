package com.bevans.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.bevans.model.Viagem;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    
    Optional<Viagem> findFirstByTurmaIdAndStatus(Long turmaId, String status);
    
    boolean existsByTurmaIdAndStatus(Long turmaId, String status);
    boolean existsByTurmaMotoristaIdAndStatus(Long motoristaId, String status);
    
    @Modifying
    @Query("DELETE FROM Viagem v WHERE v.turma.id = :turmaId")
    void deleteAllByTurmaId(@Param("turmaId") Long turmaId);

    @Modifying
    @Query("DELETE FROM Viagem v WHERE v.turma.motorista.id = :motoristaId")
    void deleteAllByTurmaMotoristaId(@Param("motoristaId") Long motoristaId);
}