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
            assertFalse(service.estSalleDisponible(salle.getId(), debut, debut.plusHours(1)));
            assertFalse(service.estSalleDisponible(salle.getId(), debut.plusMinutes(15), debut.plusMinutes(45)));
            assertFalse(service.estSalleDisponible(salle.getId(), debut.minusHours(1), debut.plusHours(2)));
            assertTrue(service.estSalleDisponible(salle.getId(), debut.minusHours(1), debut));
            assertTrue(service.estSalleDisponible(salle.getId(), debut.plusHours(1), debut.plusHours(2)));
            assertTrue(service.estSalleDisponible(autreSalle.getId(), debut, debut.plusHours(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.estSalleDisponible(salle.getId(), debut, debut));
            assertThrows(IllegalArgumentException.class,
                    () -> service.estSalleDisponible(-1L, debut, debut.plusHours(1)));
            assertEquals(reservation.getId(), service.reservationsUtilisateur(utilisateur.getId()).get(0).getId());
            assertTrue(service.reservationsUtilisateur(autreUtilisateur.getId()).isEmpty());
            assertEquals(1, service.sallesAvecEquipement("Projecteur").size());
            assertEquals(salle.getId(), service.sallesAvecEquipement("Projecteur").get(0).getId());
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
            session.get(Reservation.class, reservation.getId()).setDateFin(debut);
            assertThrows(IllegalArgumentException.class, () -> session.flush());
            session.getTransaction().rollback();
        }
        try (Session session = sessionFactory.openSession()) {
            assertEquals(debut.plusHours(1), session.get(Reservation.class, reservation.getId()).getDateFin());
        }
    }

    @Test
    public void persisterEtSupprimerReservationOrpheline() {
        Utilisateur utilisateur = new Utilisateur("amina@example.com", "Amina");
        Salle salle = new Salle("Salle A", 20);
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 10, 0);
        Reservation reservation = new Reservation(debut, debut.plusHours(1), utilisateur, salle);
        utilisateur.getReservations().add(reservation);

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(salle);
            session.persist(utilisateur);
            salle.getReservations().add(reservation);
            session.getTransaction().commit();
        }

        assertNotNull(reservation.getId());
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Reservation chargee = session.get(Reservation.class, reservation.getId());
            assertEquals(debut, chargee.getDateDebut());
            assertEquals(debut.plusHours(1), chargee.getDateFin());
            assertEquals("Amina", chargee.getUtilisateur().getNom());
            assertEquals(20, chargee.getSalle().getCapacite());
            assertTrue(chargee.getSalle().getReservations().contains(chargee));
            chargee.getSalle().getReservations().remove(chargee);
            assertTrue(chargee.getUtilisateur().getReservations().remove(chargee));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNull(session.get(Reservation.class, reservation.getId()));
            assertNotNull(session.get(Utilisateur.class, utilisateur.getId()));
            assertNotNull(session.get(Salle.class, salle.getId()));
        }
    }

    @Test
    public void partagerEquipementSansLeSupprimerAvecSalle() {
        Salle salleA = new Salle("Salle A", 20);
        Salle salleB = new Salle("Salle B", 10);
        Equipement equipement = new Equipement("Projecteur");
        salleA.ajouterEquipement(equipement);
        salleB.ajouterEquipement(equipement);
        assertEquals(2, equipement.getSalles().size());

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(salleA);
            session.persist(salleB);
            session.getTransaction().commit();
        }

        assertNotNull(equipement.getId());
        equipement.setNom("Projecteur HD");
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(salleB);
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Equipement charge = session.get(Equipement.class, equipement.getId());
            assertEquals("Projecteur HD", charge.getNom());
            assertEquals(2, charge.getSalles().size());
            assertEquals(Long.valueOf(1), session.createQuery(
                    "select count(e) from Equipement e", Long.class).getSingleResult());
            session.remove(session.get(Salle.class, salleA.getId()));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Equipement charge = session.get(Equipement.class, equipement.getId());
            assertNotNull(charge);
            assertEquals(1, charge.getSalles().size());
            Salle restante = session.get(Salle.class, salleB.getId());
            assertTrue(restante.getEquipements().contains(charge));
            restante.retirerEquipement(charge);
            assertTrue(charge.getSalles().isEmpty());
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session.get(Equipement.class, equipement.getId()));
            assertTrue(session.get(Salle.class, salleB.getId()).getEquipements().isEmpty());
        }
    }

    @Test
    public void supprimerSalleSupprimeSesReservations() {
        Utilisateur utilisateur = new Utilisateur("youssef@example.com", "Youssef");
        Salle salle = new Salle("Salle B", 10);
        LocalDateTime debut = LocalDateTime.of(2026, 10, 13, 14, 0);
        Reservation reservation = new Reservation(debut, debut.plusHours(2), utilisateur, salle);
        salle.getReservations().add(reservation);

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(utilisateur);
            session.persist(salle);
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.remove(session.get(Salle.class, salle.getId()));
            session.getTransaction().commit();
        }

        try (Session session = sessionFactory.openSession()) {
            assertNull(session.get(Salle.class, salle.getId()));
            assertNull(session.get(Reservation.class, reservation.getId()));
            assertNotNull(session.get(Utilisateur.class, utilisateur.getId()));
        }
    }
}
