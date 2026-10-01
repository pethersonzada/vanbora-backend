package com.vanapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "turmas")
public class Turma {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String turno;
    private String codigoConvite;

    private String destinoNome;
    private Double destinoLatitude;
    private Double destinoLongitude;

    private String origemNome;
    private Double origemLatitude;
    private Double origemLongitude;

    @ManyToOne
    @JoinColumn(name = "motorista_id")
    private Usuario motorista;

    @OneToMany(mappedBy = "turma", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AlunoTurma> alunoTurmas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getCodigoConvite() { return codigoConvite; }
    public void setCodigoConvite(String codigoConvite) { this.codigoConvite = codigoConvite; }

    public String getDestinoNome() { return destinoNome; }
    public void setDestinoNome(String destinoNome) { this.destinoNome = destinoNome; }

    public Double getDestinoLatitude() { return destinoLatitude; }
    public void setDestinoLatitude(Double destinoLatitude) { this.destinoLatitude = destinoLatitude; }

    public Double getDestinoLongitude() { return destinoLongitude; }
    public void setDestinoLongitude(Double destinoLongitude) { this.destinoLongitude = destinoLongitude; }

    public String getOrigemNome() { return origemNome; }
    public void setOrigemNome(String origemNome) { this.origemNome = origemNome; }

    public Double getOrigemLatitude() { return origemLatitude; }
    public void setOrigemLatitude(Double origemLatitude) { this.origemLatitude = origemLatitude; }

    public Double getOrigemLongitude() { return origemLongitude; }
    public void setOrigemLongitude(Double origemLongitude) { this.origemLongitude = origemLongitude; }

    public Usuario getMotorista() { return motorista; }
    public void setMotorista(Usuario motorista) { this.motorista = motorista; }

    public List<AlunoTurma> getAlunoTurmas() { return alunoTurmas; }
    public void setAlunoTurmas(List<AlunoTurma> alunoTurmas) { this.alunoTurmas = alunoTurmas; }
}