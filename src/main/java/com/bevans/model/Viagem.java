package com.bevans.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "viagem")
@SQLDelete(sql = "UPDATE viagem SET excluido = true WHERE id=?")
@SQLRestriction("excluido = false")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @Column(nullable = false)
    private String sentido;

    @Column(nullable = false)
    private String status;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private boolean excluido = false;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }
    public String getSentido() { return sentido; }
    public void setSentido(String sentido) { this.sentido = sentido; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    
    public boolean isExcluido() { return excluido; }
    public void setExcluido(boolean excluido) { this.excluido = excluido; }
}