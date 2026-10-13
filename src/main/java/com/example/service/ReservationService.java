package com.example.service;

import com.example.model.Reservation;
import com.example.model.Salle;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {

    private final EntityManager em;

    public ReservationService(EntityManager em) {
        this.em = em;
    }

    public boolean estSalleDisponible(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !debut.isBefore(fin)) {
            throw new IllegalArgumentException("Choisissez une periode avec un debut avant la fin.");
        }
        if (salleId == null || em.find(Salle.class, salleId) == null) {
            throw new IllegalArgumentException("Cette salle n'existe pas.");
        }

        // Deux reservations peuvent se suivre, mais elles ne doivent pas se chevaucher.
        Long conflits = em.createQuery(
                "select count(r) from Reservation r where r.salleR.idS = :salleId "
                        + "and r.dateDebutR < :fin and r.dateFinR > :debut", Long.class)
                .setParameter("salleId", salleId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return conflits == 0;
    }

    public List<Reservation> reservationsUtilisateur(Long utilisateurId) {
        return em.createQuery(
                "select r from Reservation r where r.utilisateurR.idU = :utilisateurId "
                        + "order by r.dateDebutR", Reservation.class)
                .setParameter("utilisateurId", utilisateurId)
                .getResultList();
    }

    public List<Salle> sallesAvecEquipement(String nomEquipement) {
        return em.createQuery(
                "select distinct s from Salle s join s.equipementsS e where e.nomE = :nom", Salle.class)
                .setParameter("nom", nomEquipement)
                .getResultList();
    }
}
