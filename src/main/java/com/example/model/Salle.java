package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idS;

    @Column(nullable = false)
    private String nomS;

    @Column(nullable = false)
    private int capaciteS;

    @OneToMany(mappedBy = "salleR", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservationsS = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "salle_equipement",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id")
    )
    private Set<Equipement> equipementsS = new HashSet<>();

    public Salle() {
    }

    public Salle(String nomS, int capaciteS) {
        this.nomS = nomS;
        this.capaciteS = capaciteS;
    }

    public Long getIdS() {
        return idS;
    }

    public void setIdS(Long idS) {
        this.idS = idS;
    }

    public String getNomS() {
        return nomS;
    }

    public void setNomS(String nomS) {
        this.nomS = nomS;
    }

    public int getCapaciteS() {
        return capaciteS;
    }

    public void setCapaciteS(int capaciteS) {
        this.capaciteS = capaciteS;
    }

    public Set<Reservation> getReservationsS() {
        return reservationsS;
    }

    public void setReservationsS(Set<Reservation> reservationsS) {
        this.reservationsS = reservationsS;
    }

    public void addReservation(Reservation reservation) {
        reservationsS.add(reservation);
        reservation.setSalleR(this);
    }

    public void removeReservation(Reservation reservation) {
        if (reservationsS.remove(reservation) && reservation.getSalleR() == this) {
            reservation.setSalleR(null);
        }
    }

    public Set<Equipement> getEquipementsS() {
        return equipementsS;
    }

    public void setEquipementsS(Set<Equipement> equipementsS) {
        this.equipementsS = equipementsS;
    }

    public void ajouterEquipement(Equipement equipement) {
        equipementsS.add(equipement);
        equipement.getSallesE().add(this);
    }

    public void addEquipement(Equipement equipement) {
        ajouterEquipement(equipement);
    }

    public void retirerEquipement(Equipement equipement) {
        equipementsS.remove(equipement);
        equipement.getSallesE().remove(this);
    }
}
