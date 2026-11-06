
package com.bevans.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bevans.model.Presenca;

public interface PresencaRepository
        extends JpaRepository<Presenca, Long> {

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.data = :data
    """)
    List<Presenca> findByData(
            @Param("data") LocalDate data
    );

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.usuario.id IN :usuarioIds
          AND p.data = :data
    """)
    List<Presenca> findByUsuarioIdInAndData(
            @Param("usuarioIds") List<Long> usuarioIds,
            @Param("data") LocalDate data
    );

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.usuario.id = :usuarioId
          AND p.turma.id = :turmaId
          AND p.data = :data
    """)
    Optional<Presenca> findByUsuarioIdAndTurmaIdAndData(
            @Param("usuarioId") Long usuarioId,
            @Param("turmaId") Long turmaId,
            @Param("data") LocalDate data
    );

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.usuario.id = :usuarioId
          AND p.data = :data
    """)
    List<Presenca> findAllByUsuarioIdAndData(
            @Param("usuarioId") Long usuarioId,
            @Param("data") LocalDate data
    );

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.usuario.id = :usuarioId
          AND p.viagem.id = :viagemId
          AND p.data = :data
    """)
    Optional<Presenca> findByUsuarioIdAndViagemIdAndData(
            @Param("usuarioId") Long usuarioId,
            @Param("viagemId") Long viagemId,
            @Param("data") LocalDate data
    );

    @Query("""
        SELECT p FROM Presenca p
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.endereco
        LEFT JOIN FETCH p.enderecoIda
        LEFT JOIN FETCH p.enderecoVolta
        WHERE p.data = :data
          AND p.turma.id = :turmaId
          AND EXISTS (
              SELECT at.id FROM AlunoTurma at
              WHERE at.aluno.id = p.usuario.id
                AND at.turma.id = :turmaId
                AND UPPER(at.status) = 'APROVADO'
          )
    """)
    List<Presenca> findByDataAndTurmaId(
            @Param("data") LocalDate data,
            @Param("turmaId") Long turmaId
    );

    @Modifying
    @Query("""
        DELETE FROM Presenca p
        WHERE p.usuario.id = :usuarioId
    """)
    void deleteAllByUsuarioId(
            @Param("usuarioId") Long usuarioId
    );
}
