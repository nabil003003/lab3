package com.example.model;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.example.service.ReservationService;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class EntitesTest {

    private SessionFactory sessionFactory;

    @Before
    public void configurerBase() {
        sessionFactory = new Configuration()
                .addAnnotatedClass(Utilisateur.class)
                .addAnnotatedClass(Salle.class)
                .addAnnotatedClass(Reservation.class)
                .addAnnotatedClass(Equipement.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:entites;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
    }

    @After
    public void fermerBase() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void verifierDisponibiliteEtRequetes() {
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
        Salle salle = new Salle("Salle A", 20);
        Salle autreSalle = new Salle("Salle B", 10);
        Utilisateur utilisateur = new Utilisateur("alice@example.com", "Alice");
        Utilisateur autreUtilisateur = new Utilisateur("bob@example.com", "Bob");
        salle.addEquipement(new Equipement("Projecteur"));
        Reservation reservation = new Reservation(debut, debut.plusHours(1), utilisateur, salle);
        utilisateur.addReservation(reservation);

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(salle);
            session.persist(autreSalle);
            session.persist(utilisateur);
            session.persist(autreUtilisateur);
            session.getTransaction().commit();
            session.clear();

            ReservationService service = new ReservationService(session);
            assertFalse(service.estSalleDisponible(salle.getIdS(), debut, debut.plusHours(1)));
            assertFalse(service.estSalleDisponible(salle.getIdS(), debut.plusMinutes(15), debut.plusMinutes(45)));
            assertFalse(service.estSalleDisponible(salle.getIdS(), debut.minusHours(1), debut.plusHours(2)));
            assertTrue(service.estSalleDisponible(salle.getIdS(), debut.minusHours(1), debut));
            assertTrue(service.estSalleDisponible(salle.getIdS(), debut.plusHours(1), debut.plusHours(2)));
            assertTrue(service.estSalleDisponible(autreSalle.getIdS(), debut, debut.plusHours(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.estSalleDisponible(salle.getIdS(), debut, debut));
            assertThrows(IllegalArgumentException.class,
                    () -> service.estSalleDisponible(-1L, debut, debut.plusHours(1)));
            assertEquals(reservation.getIdR(), service.reservationsUtilisateur(utilisateur.getIdU()).get(0).getIdR());
            assertTrue(service.reservationsUtilisateur(autreUtilisateur.getIdU()).isEmpty());
            assertEquals(1, service.sallesAvecEquipement("Projecteur").size());
            assertEquals(salle.getIdS(), service.sallesAvecEquipement("Projecteur").get(0).getIdS());
            assertTrue(service.sallesAvecEquipement("Tableau").isEmpty());
        }
    }

    @Test
    public void refuserDatesInvalidesALInsertion() {
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
        LocalDateTime[][] periodes = {
                {null, debut}, {debut, null}, {debut, debut}, {debut, debut.minusHours(1)}
        };
        for (LocalDateTime[] periode : periodes) {
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();
                Salle salle = new Salle("Salle A", 20);
                Utilisateur utilisateur = new Utilisateur("alice@example.com", "Alice");
                session.persist(salle);
                session.persist(utilisateur);
                Reservation reservation = new Reservation(periode[0], periode[1], utilisateur, salle);
                assertThrows(IllegalArgumentException.class, () -> session.persist(reservation));
                session.getTransaction().rollback();
            }
        }
        try (Session session = sessionFactory.openSession()) {
            assertEquals(Long.valueOf(0), session.createQuery(
                    "select count(r) from Reservation r", Long.class).getSingleResult());
        }
    }

    @Test
    public void refuserDatesInvalidesALaMiseAJour() {
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
        Reservation reservation;
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Salle salle = new Salle("Salle A", 20);
            Utilisateur utilisateur = new Utilisateur("alice@example.com", "Alice");
            session.persist(salle);
            session.persist(utilisateur);
            reservation = new Reservation(debut, debut.plusHours(1), utilisateur, salle);
            session.persist(reservation);
            session.getTransaction().commit();
        }
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.get(Reservation.class, reservation.getIdR()).setDateFinR(debut);
            assertThrows(IllegalArgumentException.class, () -> session.flush());
            session.getTransaction().rollback();
        }
        try (Session session = sessionFactory.openSession()) {
            assertEquals(debut.plusHours(1), session.get(Reservation.class, reservation.getIdR()).getDateFinR());
        }
    }

    @Test
    public void persisterEtSupprimerReservationOrpheline() {
        Utilisateur utilisateur = new Utilisateur("amina@example.com", "Amina");
        Salle salle = new Salle("Salle A", 20);
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
        Reservation reservation = new Reservation(debut, debut.plusHours(1), utilisateur, salle);
        utilisateur.getReservationsU().add(reservation);

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(salle);
            session.persist(utilisateur);
            salle.getReservationsS().add(reservation);
            session.getTransaction().commit();
        }

        assertNotNull(reservation.getIdR());
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Reservation chargee = session.get(Reservation.class, reservation.getIdR());
            assertEquals(debut, chargee.getDateDebutR());
            assertEquals(debut.plusHours(1), chargee.getDateFinR());
            assertEquals("Amina", chargee.getUtilisateurR().getNomU());
            assertEquals(20, chargee.getSalleR().getCapaciteS());
            assertTrue(chargee.getSalleR().getReservationsS().contains(chargee));
            chargee.getSalleR().getReservationsS().remove(chargee);
            assertTrue(chargee.getUtilisateurR().getReservationsU().remove(chargee));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNull(session.get(Reservation.class, reservation.getIdR()));
            assertNotNull(session.get(Utilisateur.class, utilisateur.getIdU()));
            assertNotNull(session.get(Salle.class, salle.getIdS()));
        }
    }

    @Test
    public void partagerEquipementSansLeSupprimerAvecSalle() {
        Salle salleA = new Salle("Salle A", 20);
        Salle salleB = new Salle("Salle B", 10);
        Equipement equipement = new Equipement("Projecteur");
        salleA.ajouterEquipement(equipement);
        salleB.ajouterEquipement(equipement);
        assertEquals(2, equipement.getSallesE().size());

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(salleA);
            session.persist(salleB);
            session.getTransaction().commit();
        }

        assertNotNull(equipement.getIdE());
        equipement.setNomE("Projecteur HD");
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(salleB);
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Equipement charge = session.get(Equipement.class, equipement.getIdE());
            assertEquals("Projecteur HD", charge.getNomE());
            assertEquals(2, charge.getSallesE().size());
            assertEquals(Long.valueOf(1), session.createQuery(
                    "select count(e) from Equipement e", Long.class).getSingleResult());
            session.remove(session.get(Salle.class, salleA.getIdS()));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Equipement charge = session.get(Equipement.class, equipement.getIdE());
            assertNotNull(charge);
            assertEquals(1, charge.getSallesE().size());
            Salle restante = session.get(Salle.class, salleB.getIdS());
            assertTrue(restante.getEquipementsS().contains(charge));
            restante.retirerEquipement(charge);
            assertTrue(charge.getSallesE().isEmpty());
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session.get(Equipement.class, equipement.getIdE()));
            assertTrue(session.get(Salle.class, salleB.getIdS()).getEquipementsS().isEmpty());
        }
    }

    @Test
    public void supprimerSalleSupprimeSesReservations() {
        Utilisateur utilisateur = new Utilisateur("youssef@example.com", "Youssef");
        Salle salle = new Salle("Salle B", 10);
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 14, 0);
        Reservation reservation = new Reservation(debut, debut.plusHours(2), utilisateur, salle);
        salle.getReservationsS().add(reservation);

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(utilisateur);
            session.persist(salle);
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.remove(session.get(Salle.class, salle.getIdS()));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNull(session.get(Salle.class, salle.getIdS()));
            assertNull(session.get(Reservation.class, reservation.getIdR()));
            assertNotNull(session.get(Utilisateur.class, utilisateur.getIdU()));
        }
    }
}
