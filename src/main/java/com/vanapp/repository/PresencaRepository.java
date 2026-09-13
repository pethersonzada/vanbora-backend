package com.vanapp.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.vanapp.model.Presenca;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {

    @Query("SELECT p FROM Presenca p JOIN FETCH p.usuario LEFT JOIN FETCH p.endereco WHERE p.data = :data")
    List<Presenca> findByData(@Param("data") LocalDate data);

    @Query("SELECT p FROM Presenca p LEFT JOIN FETCH p.endereco WHERE p.usuario.id = :usuarioId AND p.data = :data")
    Presenca findByUsuarioIdAndData(@Param("usuarioId") Long usuarioId, @Param("data") LocalDate data);

    @Query("SELECT p FROM Presenca p LEFT JOIN FETCH p.endereco WHERE p.usuario.id = :usuarioId AND p.viagem.id = :viagemId AND p.data = :data")
    Presenca findByUsuarioIdAndViagemIdAndData(@Param("usuarioId") Long usuarioId, @Param("viagemId") Long viagemId, @Param("data") LocalDate data);

    @Modifying
    @Query("DELETE FROM Presenca p WHERE p.usuario.id = :usuarioId")
    void deleteAllByUsuarioId(@Param("usuarioId") Long usuarioId);
}