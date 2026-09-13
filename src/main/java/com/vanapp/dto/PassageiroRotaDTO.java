package com.vanapp.dto;

public class PassageiroRotaDTO {
    private Long id;
    private String nome;
    private Double latitude;
    private Double longitude;

    public PassageiroRotaDTO(Long id, String nome, Double latitude, Double longitude) {
        this.id = id;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}