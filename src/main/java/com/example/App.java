package com.example;

import com.example.model.*;
import com.example.service.ReservationService;
import javax.persistence.*;
import java.time.LocalDateTime;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
        EntityManager em = null;

        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            Utilisateur user = new Utilisateur("alice@example.com", "Alice Dupont");
            Salle salle = new Salle("Salle A", 20);
            Equipement projecteur = new Equipement("Projecteur");
            salle.addEquipement(projecteur);

            // Persister la salle avant ses reservations ; cascade PERSIST vers l'equipement.
            em.persist(salle);

            LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
            Reservation reservation = new Reservation();
            reservation.setDateDebutR(debut);
            reservation.setDateFinR(debut.plusHours(1));
            user.addReservation(reservation);
            salle.addReservation(reservation);

            Reservation suivante = new Reservation();
            suivante.setDateDebutR(debut.plusHours(2));
            suivante.setDateFinR(debut.plusHours(3));
            user.addReservation(suivante);
            salle.addReservation(suivante);

            // Cascade PERSIST de l'utilisateur vers les reservations.
            em.persist(user);
            em.getTransaction().commit();
            em.clear();

            System.out.println("Apres persistance (cascades PERSIST) :");
            afficherResultats(em);

            ReservationService service = new ReservationService(em);
            System.out.println("Reservations d'Alice : " + service.reservationsUtilisateur(user.getIdU()).size());
            for (Salle equipee : service.sallesAvecEquipement("Projecteur")) {
                System.out.println("Salle avec un projecteur : " + equipee.getNomS());
            }
            System.out.println("Salle disponible de 10h a 11h : "
                    + service.estSalleDisponible(salle.getIdS(), debut, debut.plusHours(1)));
            System.out.println("Salle disponible de 11h a 12h : "
                    + service.estSalleDisponible(salle.getIdS(), debut.plusHours(1), debut.plusHours(2)));
            em.clear();

            em.getTransaction().begin();
            Reservation aRetirer = em.find(Reservation.class, reservation.getIdR());
            aRetirer.getUtilisateurR().removeReservation(aRetirer);
            aRetirer.getSalleR().removeReservation(aRetirer);
            em.getTransaction().commit();
            em.clear();

            if (em.find(Reservation.class, reservation.getIdR()) != null) {
                throw new IllegalStateException("La reservation orpheline existe encore.");
            }
            System.out.println("Apres retrait d'une reservation (orphanRemoval) :");
            afficherResultats(em);
            em.clear();

            em.getTransaction().begin();
            Utilisateur aSupprimer = em.find(Utilisateur.class, user.getIdU());
            em.remove(aSupprimer);
            em.getTransaction().commit();
            em.clear();

            if (em.find(Utilisateur.class, user.getIdU()) != null) {
                throw new IllegalStateException("L'utilisateur existe encore.");
            }
            if (em.find(Reservation.class, suivante.getIdR()) != null) {
                throw new IllegalStateException("La cascade REMOVE n'a pas supprime la reservation.");
            }
            if (em.find(Salle.class, salle.getIdS()) == null) {
                throw new IllegalStateException("La salle doit etre conservee.");
            }
            System.out.println("Apres suppression de l'utilisateur (cascade REMOVE vers les reservations) :");
            afficherResultats(em);
            em.clear();

            em.getTransaction().begin();
            em.remove(em.find(Salle.class, salle.getIdS()));
            em.getTransaction().commit();
            em.clear();

            if (em.find(Equipement.class, projecteur.getIdE()) == null) {
                throw new IllegalStateException("L'equipement doit etre conserve.");
            }
            System.out.println("Apres suppression de la salle :");
            afficherResultats(em);
            System.out.println("Verification reussie : l'equipement est conserve.");
        } catch (RuntimeException exception) {
            try {
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (RuntimeException erreurAnnulation) {
                exception.addSuppressed(erreurAnnulation);
            }
            System.err.println("Impossible de terminer l'operation : " + exception.getMessage());
            throw exception;
        } finally {
            try {
                if (em != null) {
                    em.close();
                }
            } finally {
                emf.close();
            }
        }
    }

    private static void afficherResultats(EntityManager em) {
        System.out.println("Utilisateurs : " + compter(em, "Utilisateur")
                + ", salles : " + compter(em, "Salle")
                + ", reservations : " + compter(em, "Reservation")
                + ", equipements : " + compter(em, "Equipement"));
    }

    private static long compter(EntityManager em, String entite) {
        return em.createQuery("select count(e) from " + entite + " e", Long.class)
                .getSingleResult();
    }
}
