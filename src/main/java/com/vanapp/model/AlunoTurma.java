package com.vanapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "aluno_turma")
public class AlunoTurma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    @JsonIgnore
    private Turma turma;

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    private String status; // PENDENTE, APROVADO, REJEITADO

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public void setAluno(Usuario aluno) {
        this.aluno = aluno;
    }

    public String getStatus() {
        return status;
    }

    public void status(String status) {
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}