package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idE;

    @Column(nullable = false, unique = true)
    private String nomE;

    @ManyToMany(mappedBy = "equipementsS")
    private Set<Salle> sallesE = new HashSet<>();

    public Equipement() {
    }

    public Equipement(String nomE) {
        this.nomE = nomE;
    }

    public Long getIdE() {
        return idE;
    }

    public void setIdE(Long idE) {
        this.idE = idE;
    }

    public String getNomE() {
        return nomE;
    }

    public void setNomE(String nomE) {
        this.nomE = nomE;
    }

    public Set<Salle> getSallesE() {
        return sallesE;
    }

    public void setSallesE(Set<Salle> sallesE) {
        this.sallesE = sallesE;
    }
}
