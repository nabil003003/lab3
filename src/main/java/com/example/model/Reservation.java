package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idR;

    @Column(nullable = false)
    private LocalDateTime dateDebutR;

    @Column(nullable = false)
    private LocalDateTime dateFinR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateurR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salleR;

    public Reservation() {
    }

    public Reservation(LocalDateTime dateDebutR, LocalDateTime dateFinR,
                       Utilisateur utilisateurR, Salle salleR) {
        this.dateDebutR = dateDebutR;
        this.dateFinR = dateFinR;
        this.utilisateurR = utilisateurR;
        this.salleR = salleR;
    }

    public Long getIdR() {
        return idR;
    }

    public void setIdR(Long idR) {
        this.idR = idR;
    }

    public LocalDateTime getDateDebutR() {
        return dateDebutR;
    }

    public void setDateDebutR(LocalDateTime dateDebutR) {
        this.dateDebutR = dateDebutR;
    }

    public LocalDateTime getDateFinR() {
        return dateFinR;
    }

    public void setDateFinR(LocalDateTime dateFinR) {
        this.dateFinR = dateFinR;
    }

    public Utilisateur getUtilisateurR() {
        return utilisateurR;
    }

    public void setUtilisateurR(Utilisateur utilisateurR) {
        this.utilisateurR = utilisateurR;
    }

    public Salle getSalleR() {
        return salleR;
    }

    public void setSalleR(Salle salleR) {
        this.salleR = salleR;
    }

    @PrePersist
    @PreUpdate
    public void verifierDates() {
        if (dateDebutR == null || dateFinR == null) {
            throw new IllegalArgumentException("Les dates de debut et de fin sont obligatoires.");
        }
        if (!dateDebutR.isBefore(dateFinR)) {
            throw new IllegalArgumentException("La date de debut doit etre avant la date de fin.");
        }
    }
}
