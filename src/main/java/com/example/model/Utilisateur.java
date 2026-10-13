package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idU;

    @Column(nullable = false, unique = true)
    private String emailU;

    @Column(nullable = false)
    private String nomU;

    @OneToMany(mappedBy = "utilisateurR", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservationsU = new HashSet<>();

    public Utilisateur() {
    }

    public Utilisateur(String emailU, String nomU) {
        this.emailU = emailU;
        this.nomU = nomU;
    }

    public Utilisateur(Long idU, String emailU, String nomU, Set<Reservation> reservationsU) {
        this.idU = idU;
        this.emailU = emailU;
        this.nomU = nomU;
        this.reservationsU = reservationsU;
    }

    public Long getIdU() {
        return idU;
    }

    public void setIdU(Long idU) {
        this.idU = idU;
    }

    public String getEmailU() {
        return emailU;
    }

    public void setEmailU(String emailU) {
        this.emailU = emailU;
    }

    public String getNomU() {
        return nomU;
    }

    public void setNomU(String nomU) {
        this.nomU = nomU;
    }

    public Set<Reservation> getReservationsU() {
        return reservationsU;
    }

    public void setReservationsU(Set<Reservation> reservationsU) {
        this.reservationsU = reservationsU;
    }

    public void addReservation(Reservation reservation) {
        reservationsU.add(reservation);
        reservation.setUtilisateurR(this);
    }

    public void removeReservation(Reservation reservation) {
        if (reservationsU.remove(reservation) && reservation.getUtilisateurR() == this) {
            reservation.setUtilisateurR(null);
        }
    }
}
