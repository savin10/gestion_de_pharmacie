# Scripts SQL - PharmaBenin

Ce dossier contient des scripts SQL utiles pour la maintenance de la base de données.

## 📋 Liste des scripts

### 1. check_database_integrity.sql
**Description** : Vérifie l'intégrité de la base de données sans rien modifier.

**Utilisation** :
```bash
psql -U postgres -d postgres -f scripts/check_database_integrity.sql
```

**Ce qu'il vérifie** :
- ✅ Statistiques générales (nombre d'enregistrements)
- ✅ Doublons dans toutes les tables
- ✅ Présence des contraintes d'unicité
- ✅ Valeurs NULL dans les colonnes obligatoires
- ✅ Intégrité référentielle (clés étrangères)
- ✅ Statistiques par pharmacie et médicament
- ✅ Alertes de stock (stock critique, ruptures)

**Quand l'utiliser** :
- Après chaque déploiement
- Avant de nettoyer les doublons
- En cas de problème suspect
- Régulièrement pour monitoring

### 2. clean_duplicates.sql
**Description** : Nettoie les doublons et ajoute les contraintes d'unicité.

**⚠️ ATTENTION** : Ce script **SUPPRIME** des données. Faire une sauvegarde avant !

**Utilisation** :
```bash
# 1. Faire une sauvegarde
pg_dump -U postgres -d postgres > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Exécuter le script
psql -U postgres -d postgres -f scripts/clean_duplicates.sql
```

**Ce qu'il fait** :
1. Analyse les doublons
2. Supprime les doublons (garde le premier ID)
3. Ajoute les contraintes d'unicité
4. Vérifie le résultat

**Quand l'utiliser** :
- Après migration depuis l'ancienne version
- Si des doublons sont détectés
- Une seule fois normalement (les contraintes empêchent les futurs doublons)

## 🔧 Commandes utiles

### Connexion à la base de données
```bash
# PostgreSQL
psql -U postgres -d postgres

# Avec mot de passe
psql -U postgres -d postgres -W
```

### Sauvegarde
```bash
# Sauvegarde complète
pg_dump -U postgres -d postgres > backup.sql

# Sauvegarde avec timestamp
pg_dump -U postgres -d postgres > backup_$(date +%Y%m%d_%H%M%S).sql

# Sauvegarde compressée
pg_dump -U postgres -d postgres | gzip > backup.sql.gz
```

### Restauration
```bash
# Restaurer depuis une sauvegarde
psql -U postgres -d postgres < backup.sql

# Restaurer depuis une sauvegarde compressée
gunzip -c backup.sql.gz | psql -U postgres -d postgres
```

### Exécution de scripts
```bash
# Exécuter un script SQL
psql -U postgres -d postgres -f script.sql

# Exécuter avec sortie dans un fichier
psql -U postgres -d postgres -f script.sql > output.txt 2>&1

# Exécuter en mode silencieux
psql -U postgres -d postgres -f script.sql -q
```

## 📊 Requêtes utiles

### Compter les enregistrements
```sql
SELECT 'Médicaments' as table_name, COUNT(*) as total FROM medicament
UNION ALL
SELECT 'Pharmacies', COUNT(*) FROM pharmacie
UNION ALL
SELECT 'Disponibilités', COUNT(*) FROM disponibilite
UNION ALL
SELECT 'Admins', COUNT(*) FROM admin_users;
```

### Lister les contraintes
```sql
SELECT 
    tc.table_name, 
    tc.constraint_name, 
    tc.constraint_type
FROM information_schema.table_constraints tc
WHERE tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_type;
```

### Trouver les doublons
```sql
-- Médicaments
SELECT nom, dosage, forme, COUNT(*) 
FROM medicament 
GROUP BY nom, dosage, forme 
HAVING COUNT(*) > 1;

-- Pharmacies
SELECT nom, adresse, COUNT(*) 
FROM pharmacie 
GROUP BY nom, adresse 
HAVING COUNT(*) > 1;
```

### Vérifier les stocks
```sql
-- Stock total par médicament
SELECT 
    m.nom,
    COUNT(d.id) as nb_pharmacies,
    SUM(d.quantite_stock) as stock_total
FROM medicament m
LEFT JOIN disponibilite d ON m.id = d.medicament_id
WHERE d.disponible = true
GROUP BY m.id, m.nom
ORDER BY stock_total DESC;

-- Médicaments en rupture
SELECT 
    p.nom as pharmacie,
    m.nom as medicament,
    d.quantite_stock
FROM disponibilite d
JOIN pharmacie p ON d.pharmacie_id = p.id
JOIN medicament m ON d.medicament_id = m.id
WHERE d.quantite_stock <= 5
ORDER BY d.quantite_stock;
```

## 🚨 Dépannage

### Problème : "psql: command not found"
**Solution** : Installer PostgreSQL client
```bash
# Ubuntu/Debian
sudo apt-get install postgresql-client

# macOS
brew install postgresql

# Windows
# Télécharger depuis https://www.postgresql.org/download/windows/
```

### Problème : "FATAL: password authentication failed"
**Solution** : Vérifier les identifiants dans application.properties
```properties
spring.datasource.username=postgres
spring.datasource.password=votre_mot_de_passe
```

### Problème : "ERROR: duplicate key value violates unique constraint"
**Solution** : Des doublons existent déjà
1. Exécuter `check_database_integrity.sql` pour identifier les doublons
2. Faire une sauvegarde
3. Exécuter `clean_duplicates.sql` pour nettoyer

### Problème : "ERROR: relation does not exist"
**Solution** : La table n'existe pas
1. Vérifier que l'application a démarré au moins une fois
2. Vérifier la configuration de la base de données
3. Vérifier que Hibernate a créé les tables

## 📝 Bonnes pratiques

### Avant toute modification
1. ✅ Faire une sauvegarde
2. ✅ Tester sur une base de développement
3. ✅ Vérifier l'intégrité avec `check_database_integrity.sql`
4. ✅ Documenter les changements

### Maintenance régulière
- **Quotidien** : Vérifier les logs de l'application
- **Hebdomadaire** : Exécuter `check_database_integrity.sql`
- **Mensuel** : Faire une sauvegarde complète
- **Avant déploiement** : Vérifier l'intégrité et faire une sauvegarde

### Sécurité
- ⚠️ Ne jamais commiter les mots de passe
- ⚠️ Utiliser des variables d'environnement
- ⚠️ Limiter les accès à la base de production
- ⚠️ Chiffrer les sauvegardes sensibles

## 🔗 Ressources

- [Documentation PostgreSQL](https://www.postgresql.org/docs/)
- [Documentation Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Documentation Hibernate](https://hibernate.org/orm/documentation/)

## 📞 Support

En cas de problème :
1. Consulter les logs : `tail -f logs/application.log`
2. Vérifier la documentation : `GESTION_DOUBLONS.md`
3. Exécuter le script de vérification
4. Contacter l'équipe technique

---

**Dernière mise à jour** : 2024-01-15
