# Guide de test - Interface de recherche PharmaBenin

## Prérequis

1. Base de données configurée (H2 ou PostgreSQL)
2. Application Spring Boot démarrée
3. Navigateur web moderne
4. Connexion Internet (pour Google Maps)

## Démarrage de l'application

```bash
# Avec Maven
./mvnw spring-boot:run

# Ou avec Maven wrapper sur Windows
mvnw.cmd spring-boot:run

# L'application démarre sur http://localhost:8080
```

## Scénarios de test

### 1. Page d'accueil

**URL** : `http://localhost:8080/`

**Éléments à vérifier** :
- [ ] Header avec logo "PharmaBenin" et navigation
- [ ] Titre principal : "Trouvez vos médicaments instantanément partout au Bénin"
- [ ] Barre de recherche centrée avec bouton "Rechercher"
- [ ] Bouton "Utiliser ma position actuelle"
- [ ] Section "Catégories de Santé" avec 3 cartes colorées
- [ ] Section "Comment ça marche ?" avec 3 étapes
- [ ] Section "Professionnel de santé ?"
- [ ] Footer avec mentions légales

**Actions à tester** :
1. Cliquer sur "Utiliser ma position actuelle" → Demande d'autorisation de géolocalisation
2. Taper "Para" dans la barre de recherche → Suggestions apparaissent
3. Appuyer sur Entrée ou cliquer sur "Rechercher" → Redirection vers page de résultats

### 2. Recherche de médicaments

**Médicaments de test disponibles** :
- Paracétamol (5 pharmacies)
- Amoxicilline (3 pharmacies)
- Ibuprofène (3 pharmacies)
- Aspirine (3 pharmacies)
- Doliprane (4 pharmacies)

**Test 1 : Recherche "Paracétamol"**

1. Sur la page d'accueil, taper "Paracétamol"
2. Appuyer sur Entrée
3. **Résultat attendu** :
   - Redirection vers `/search-results?query=Paracétamol&lat=...&lon=...`
   - Affichage de "Résultats pour 'Paracétamol'"
   - Compteur : "5 Pharmacies" (ou selon données)
   - Liste de 5 pharmacies triées par distance
   - Carte avec 5 marqueurs

**Test 2 : Vérification des informations de pharmacie**

Pour chaque pharmacie affichée, vérifier :
- [ ] Nom de la pharmacie
- [ ] Adresse complète
- [ ] Distance en km
- [ ] Prix (500 FCFA)
- [ ] Badge de stock :
  - "En Stock" (vert) si quantité > 5
  - "Stock Faible" (rouge) si quantité ≤ 5
- [ ] Badge horaires :
  - "Ouvert 24h/24" (orange) si horaires contient "24h"
  - Horaires normaux sinon
- [ ] Bouton "Commander"
- [ ] Bouton "Détails"

**Pharmacies attendues pour Paracétamol** :
1. **Pharmacie Camp Guezo**
   - Adresse : Avenue Jean-Paul II, Cotonou
   - Distance : ~0.5 km (selon position)
   - Stock : 100 unités → Badge "En Stock"
   - Horaires : "Fermeture à 22:00"

2. **Pharmacie Les Cocotiers**
   - Adresse : Quartier Les Cocotiers, Cotonou
   - Distance : ~1.2 km
   - Stock : 50 unités → Badge "En Stock"
   - Horaires : "24h/24" → Badge orange

3. **Pharmacie Saint Luc**
   - Adresse : Cadjehoun, Cotonou
   - Distance : ~1.8 km
   - Stock : 3 unités → Badge "Stock Faible" (rouge)
   - Horaires : "Fermeture à 20:00"

4. **Pharmacie de l'Etoile**
   - Adresse : Zone Résidentielle, Cotonou
   - Distance : ~2.5 km
   - Stock : 75 unités → Badge "En Stock"
   - Horaires : "24h/24" → Badge orange

5. **Pharmacie Fidjrossè**
   - Adresse : Fidjrossè, Cotonou
   - Distance : Variable
   - Stock : 200 unités → Badge "En Stock"
   - Horaires : "24h/24" → Badge orange

### 3. Interaction avec la carte

**Actions à tester** :
1. [ ] Cliquer sur un marqueur → Info-bulle s'affiche avec détails
2. [ ] Cliquer sur une carte de pharmacie → Carte se centre sur le marqueur
3. [ ] Utiliser les boutons de zoom (+/-) → Carte zoom/dézoom
4. [ ] Cliquer sur le bouton de recentrage → Carte revient à la position utilisateur
5. [ ] Vérifier la légende :
   - Point bleu : Votre position
   - Points verts : Pharmacies avec stock disponible
   - Points rouges : Pharmacies avec stock critique

### 4. Fonctionnalités des boutons

**Bouton "Commander"** :
- Cliquer → Alert "Fonctionnalité en cours de développement"

**Bouton "Détails"** :
- Cliquer → Modal s'affiche avec :
  - Nom de la pharmacie
  - Adresse complète
  - Téléphone (cliquable)
  - Horaires
  - Distance
  - Bouton "Itinéraire" (ouvre Google Maps)
  - Bouton "Fermer"

### 5. Filtres (à implémenter)

**Filtres disponibles** :
- [ ] "En Stock" (actif par défaut)
- [ ] "Ouvert 24/7"
- [ ] "< 2km"
- [ ] "Plus de filtres"

**Note** : La logique de filtrage n'est pas encore implémentée

### 6. Tests de cas limites

**Test 1 : Médicament inexistant**
- Rechercher "XYZ123"
- **Résultat attendu** : Message "Aucun médicament trouvé"

**Test 2 : Médicament sans disponibilité**
- Créer un médicament sans disponibilité
- Rechercher ce médicament
- **Résultat attendu** : Message "Aucune pharmacie disponible"

**Test 3 : Sans géolocalisation**
- Refuser l'autorisation de géolocalisation
- **Résultat attendu** : Position par défaut (Cotonou : 6.3667, 2.4333)

### 7. Tests responsive

**Desktop (> 768px)** :
- [ ] Layout en deux colonnes (liste + carte)
- [ ] Navigation horizontale visible
- [ ] Tous les éléments visibles

**Tablette (768px - 1024px)** :
- [ ] Layout adapté
- [ ] Carte réduite mais visible

**Mobile (< 768px)** :
- [ ] Liste en pleine largeur
- [ ] Carte en dessous ou masquée
- [ ] Menu hamburger
- [ ] Boutons empilés verticalement

## Vérification des API

### API 1 : Recherche de médicaments
```bash
curl "http://localhost:8080/api/medicaments/recherche?query=Para"
```

**Réponse attendue** :
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

### API 2 : Disponibilité dans les pharmacies
```bash
curl "http://localhost:8080/api/medicaments/1/disponibilite?lat=6.3667&lon=2.4333"
```

**Réponse attendue** :
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

## Problèmes courants

### Problème 1 : Carte ne s'affiche pas
**Cause** : Clé API Google Maps invalide ou manquante
**Solution** : Vérifier la clé dans `index.html` et `search-results.html`

### Problème 2 : Aucune pharmacie affichée
**Cause** : Base de données vide
**Solution** : Vérifier que `DataLoader` s'est exécuté au démarrage

### Problème 3 : Erreur 404 sur `/search-results`
**Cause** : Route non configurée
**Solution** : Vérifier `WebController.java`

### Problème 4 : Distances incorrectes
**Cause** : Géolocalisation non autorisée
**Solution** : Autoriser la géolocalisation ou utiliser position par défaut

## Logs à surveiller

```
# Démarrage de l'application
INFO  o.s.b.w.e.tomcat.TomcatWebServer : Tomcat started on port(s): 8080

# Chargement des données
INFO  c.p.b.config.DataLoader : Données de test chargées

# Requêtes API
INFO  c.p.b.c.PharmacieController : Recherche de médicament: Paracétamol
INFO  c.p.b.c.PharmacieController : Disponibilité pour médicament ID: 1
```

## Checklist finale

- [ ] Page d'accueil s'affiche correctement
- [ ] Recherche redirige vers page de résultats
- [ ] Résultats affichent les bonnes pharmacies
- [ ] Carte affiche les marqueurs
- [ ] Distances sont calculées correctement
- [ ] Badges de stock sont corrects
- [ ] Badges 24h/24 sont corrects
- [ ] Bouton "Détails" ouvre le modal
- [ ] Info-bulles sur la carte fonctionnent
- [ ] Design correspond aux maquettes
- [ ] Responsive fonctionne sur mobile
