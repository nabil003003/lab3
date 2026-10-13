![Création des tables](docs/images/creation-tables.jpg)

Hibernate crée les tables des entités et la table de jointure entre les salles et les équipements.

![Suppression d’une réservation](docs/images/suppression-reservation.jpg)

Hibernate supprime la réservation retirée de la collection grâce à `orphanRemoval`, puis vérifie qu’elle n’existe plus.

![Résultat après suppression de la salle](docs/images/resultats-suppression-salle.jpg)

Dans cette capture, la salle et ses réservations sont supprimées, tandis que l’utilisateur et l’équipement sont conservés.
