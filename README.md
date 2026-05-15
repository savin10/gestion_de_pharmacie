# PharmaBenin - Interface de recherche de médicaments

Application web moderne pour localiser rapidement les médicaments disponibles dans les pharmacies du Bénin.

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)

##  Table des matières

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Technologies](#technologies)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Architecture](#architecture)
- [API](#api)
- [Documentation](#documentation)
- [Contribution](#contribution)

##  Aperçu

PharmaBenin est une plateforme qui permet aux utilisateurs de :
- Rechercher des médicaments par nom ou principe actif
- Localiser les pharmacies disposant du médicament recherché
- Voir la disponibilité en temps réel et les niveaux de stock
- Calculer la distance depuis leur position
- Obtenir des itinéraires vers les pharmacies

### Captures d'écran

**Page d'accueil**
- Design moderne et épuré
- Barre de recherche centrée
- Catégories de santé
- Section "Comment ça marche"

**Page de résultats**
- Liste des pharmacies avec filtres
- Carte Google Maps interactive
- Informations détaillées (stock, horaires, distance)
- Badges de statut (En Stock / Stock Faible / 24h/24)

##  Fonctionnalités

### Pour les utilisateurs

- ✅ **Recherche intelligente** : Recherche par nom ou principe actif avec suggestions
- ✅ **Géolocalisation** : Détection automatique de la position utilisateur
- ✅ **Calcul de distance** : Distance en temps réel vers chaque pharmacie
- ✅ **Carte interactive** : Visualisation sur Google Maps avec marqueurs
- ✅ **Informations détaillées** : Stock, horaires, téléphone, adresse
- ✅ **Filtres** : En Stock, Ouvert 24/7, Distance
- ✅ **Responsive** : Compatible mobile, tablette, desktop

### Pour les pharmacies

- ✅ Gestion du stock en temps réel
- ✅ Mise à jour des disponibilités
- ✅ Gestion des horaires
- ✅ Dashboard administrateur

##  Technologies

### Backend
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Security**
- **H2 Database** (dev) / **PostgreSQL** (prod)
- **Lombok**

### Frontend
- **HTML5**
- **Tailwind CSS**
- **JavaScript (Vanilla)**
- **Google Maps JavaScript API**
- **Font Awesome**

### Outils
- **Maven** - Gestion des dépendances
- **Git** - Contrôle de version

##  Installation

### Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- Git
- Clé API Google Maps

### Étapes

1. **Cloner le repository**
```bash
git clone https://github.com/savin10/gestion_de_pharmacie.git

```

2. **Configurer la base de données**

Éditer `src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

3. **Configurer Google Maps API**

Remplacer la clé API dans :
- `src/main/resources/templates/index.html`
- `src/main/resources/templates/search-results.html`

```html
<script async defer src="https://maps.googleapis.com/maps/api/js?key=VOTRE_CLE_API&libraries=places,geometry"></script>
```

4. **Compiler et lancer**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

5. **Accéder à l'application**
```
http://localhost:8080
```

##  Utilisation

### Recherche de médicaments

1. Ouvrir `http://localhost:8080`
2. Autoriser la géolocalisation (optionnel)
3. Taper le nom d'un médicament (ex: "Paracétamol")
4. Appuyer sur Entrée ou cliquer sur "Rechercher"
5. Consulter les résultats avec carte et liste

### Navigation

- **Page d'accueil** : `/`
- **Résultats de recherche** : `/search-results?query=...&lat=...&lon=...`
- **Liste des pharmacies** : `/pharmacies`
- **Admin** : `/admin/login`

### Données de test

L'application charge automatiquement des données de test au démarrage :

**Médicaments** :
- Paracétamol
- Amoxicilline
- Ibuprofène
- Aspirine
- Doliprane

**Pharmacies** :
- Pharmacie Camp Guezo (Avenue Jean-Paul II, Cotonou)
- Pharmacie Les Cocotiers (Quartier Les Cocotiers, Cotonou)
- Pharmacie Saint Luc (Cadjehoun, Cotonou)
- Pharmacie de l'Etoile (Zone Résidentielle, Cotonou)
- Pharmacie Fidjrossè (Fidjrossè, Cotonou)

**Compte admin** :
- Username: `admin`
- Password: `admin123`

##  Architecture

### Structure du projet

```
src/
├── main/
│   ├── java/com/pharmacie/benin/
│   │   ├── config/          # Configuration (Security, DataLoader)
│   │   ├── controller/      # Contrôleurs REST et Web
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # Entités JPA
│   │   ├── repository/      # Repositories Spring Data
│   │   └── service/         # Logique métier
│   └── resources/
│       ├── static/
│       │   ├── css/         # Styles CSS
│       │   └── js/          # Scripts JavaScript
│       ├── templates/       # Templates HTML (Thymeleaf)
│       └── application.properties
└── test/                    # Tests unitaires et d'intégration
```

### Modèle de données

```
Medicament
├── id (Long)
├── nom (String)
├── principeActif (String)
├── forme (String)
├── dosage (String)
└── description (String)

Pharmacie
├── id (Long)
├── nom (String)
├── adresse (String)
├── latitude (Double)
├── longitude (Double)
├── telephone (String)
└── horaires (String)

Disponibilite
├── id (Long)
├── pharmacie (Pharmacie)
├── medicament (Medicament)
├── disponible (Boolean)
└── quantiteStock (Integer)
```

##  API

### Endpoints publics

#### Recherche de médicaments
```http
GET /api/medicaments/recherche?query={query}
```

**Paramètres** :
- `query` (string) : Nom ou principe actif du médicament

**Réponse** :
```json
[
  {
    "id": 1,
    "nom": "Paracétamol",
    "principeActif": "Paracétamol",
    "forme": "Comprimé",
    "dosage": "500mg",
    "description": "Antalgique et antipyrétique"
  }
]
```

#### Disponibilité dans les pharmacies
```http
GET /api/medicaments/{id}/disponibilite?lat={lat}&lon={lon}
```

**Paramètres** :
- `id` (long) : ID du médicament
- `lat` (double) : Latitude de l'utilisateur
- `lon` (double) : Longitude de l'utilisateur

**Réponse** :
```json
[
  {
    "pharmacie": {
      "id": 1,
      "nom": "Pharmacie Camp Guezo",
      "adresse": "Avenue Jean-Paul II, Cotonou",
      "latitude": 6.3619,
      "longitude": 2.4244,
      "telephone": "+229 21 31 22 22",
      "horaires": "Fermeture à 22:00"
    },
    "distance": 0.52,
    "disponibilite": {
      "id": 1,
      "disponible": true,
      "quantiteStock": 100
    }
  }
]
```

#### Liste de toutes les pharmacies
```http
GET /api/pharmacies?lat={lat}&lon={lon}
```

### Endpoints admin (authentification requise)

- `GET /admin/dashboard` - Dashboard administrateur
- `GET /admin/medicaments` - Liste des médicaments
- `POST /admin/medicaments` - Ajouter un médicament
- `GET /admin/pharmacies` - Liste des pharmacies
- `POST /admin/pharmacies` - Ajouter une pharmacie

##  Documentation

- [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) - Notes d'implémentation détaillées
- [GUIDE_TEST.md](GUIDE_TEST.md) - Guide de test complet
- [ROADMAP.md](ROADMAP.md) - Feuille de route et fonctionnalités futures

##  Tests

### Lancer les tests
```bash
./mvnw test
```

### Tests disponibles
- Tests unitaires des services
- Tests d'intégration des repositories
- Tests des contrôleurs REST
- Tests de sécurité

##  Contribution

Les contributions sont les bienvenues ! Voici comment contribuer :

1. Fork le projet
2. Créer une branche (`git checkout -b feature/AmazingFeature`)
3. Commit les changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

### Guidelines

- Suivre les conventions de code Java
- Ajouter des tests pour les nouvelles fonctionnalités
- Mettre à jour la documentation
- Respecter le style de code existant

##  Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

##  Auteurs

- **Équipe PharmaBenin** - *Développement initial*

##  Remerciements

- Google Maps API pour la cartographie
- Tailwind CSS pour le design
- Font Awesome pour les icônes
- Spring Boot pour le framework backend

##  Contact

- **Email** : contact@pharmabenin.com
- **Site web** : https://pharmabenin.com
- **Support** : support@pharmabenin.com

##  Signaler un bug

Ouvrir une issue sur GitHub avec :
- Description du bug
- Étapes pour reproduire
- Comportement attendu vs observé
- Captures d'écran si applicable
- Environnement (OS, navigateur, version)

##  Demander une fonctionnalité

Ouvrir une issue avec le label "enhancement" et décrire :
- La fonctionnalité souhaitée
- Le cas d'usage
- Les bénéfices attendus

---


