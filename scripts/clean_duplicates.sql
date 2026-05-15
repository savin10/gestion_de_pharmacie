-- Script de nettoyage des doublons dans la base de données PharmaBenin
-- ATTENTION : Ce script supprime les doublons. Faire une sauvegarde avant !

-- ============================================
-- 1. SAUVEGARDE (Recommandé)
-- ============================================
-- pg_dump -U postgres -d postgres > backup_before_cleanup.sql

-- ============================================
-- 2. ANALYSE DES DOUBLONS
-- ============================================

-- Vérifier les doublons de médicaments
SELECT nom, dosage, forme, COUNT(*) as count
FROM medicament
GROUP BY nom, dosage, forme
HAVING COUNT(*) > 1
ORDER BY count DESC;

-- Vérifier les doublons de pharmacies
SELECT nom, adresse, COUNT(*) as count
FROM pharmacie
GROUP BY nom, adresse
HAVING COUNT(*) > 1
ORDER BY count DESC;

-- Vérifier les doublons de disponibilités
SELECT pharmacie_id, medicament_id, COUNT(*) as count
FROM disponibilite
GROUP BY pharmacie_id, medicament_id
HAVING COUNT(*) > 1
ORDER BY count DESC;

-- Vérifier les doublons d'admins
SELECT username, COUNT(*) as count
FROM admin_users
GROUP BY username
HAVING COUNT(*) > 1
ORDER BY count DESC;

SELECT email, COUNT(*) as count
FROM admin_users
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY count DESC;

-- ============================================
-- 3. SUPPRESSION DES DOUBLONS
-- ============================================

-- 3.1. Supprimer les doublons de disponibilités (dépendances)
-- Garder seulement la première occurrence (ID le plus petit)
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY pharmacie_id, medicament_id ORDER BY id) as rn
    FROM disponibilite
)
DELETE FROM disponibilite 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- Vérifier le résultat
SELECT 'Disponibilités après nettoyage' as info, COUNT(*) as total FROM disponibilite;

-- 3.2. Supprimer les doublons de médicaments
-- Garder seulement la première occurrence (ID le plus petit)
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY nom, dosage, forme ORDER BY id) as rn
    FROM medicament
)
DELETE FROM medicament 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- Vérifier le résultat
SELECT 'Médicaments après nettoyage' as info, COUNT(*) as total FROM medicament;

-- 3.3. Supprimer les doublons de pharmacies
-- Garder seulement la première occurrence (ID le plus petit)
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY nom, adresse ORDER BY id) as rn
    FROM pharmacie
)
DELETE FROM pharmacie 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- Vérifier le résultat
SELECT 'Pharmacies après nettoyage' as info, COUNT(*) as total FROM pharmacie;

-- 3.4. Supprimer les doublons d'admins par username
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY username ORDER BY id) as rn
    FROM admin_users
)
DELETE FROM admin_users 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- 3.5. Supprimer les doublons d'admins par email
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) as rn
    FROM admin_users
)
DELETE FROM admin_users 
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- Vérifier le résultat
SELECT 'Admins après nettoyage' as info, COUNT(*) as total FROM admin_users;

-- ============================================
-- 4. AJOUT DES CONTRAINTES D'UNICITÉ
-- ============================================

-- 4.1. Contraintes pour medicament
DO $$ 
BEGIN
    -- Supprimer la contrainte si elle existe déjà
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_medicament_nom_dosage_forme') THEN
        ALTER TABLE medicament DROP CONSTRAINT uk_medicament_nom_dosage_forme;
    END IF;
    
    -- Ajouter la contrainte
    ALTER TABLE medicament 
    ADD CONSTRAINT uk_medicament_nom_dosage_forme 
    UNIQUE (nom, dosage, forme);
    
    RAISE NOTICE 'Contrainte uk_medicament_nom_dosage_forme ajoutée';
END $$;

-- 4.2. Contraintes pour pharmacie
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_pharmacie_nom_adresse') THEN
        ALTER TABLE pharmacie DROP CONSTRAINT uk_pharmacie_nom_adresse;
    END IF;
    
    ALTER TABLE pharmacie 
    ADD CONSTRAINT uk_pharmacie_nom_adresse 
    UNIQUE (nom, adresse);
    
    RAISE NOTICE 'Contrainte uk_pharmacie_nom_adresse ajoutée';
END $$;

-- 4.3. Contraintes pour admin_users (email)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_admin_email') THEN
        ALTER TABLE admin_users DROP CONSTRAINT uk_admin_email;
    END IF;
    
    ALTER TABLE admin_users 
    ADD CONSTRAINT uk_admin_email 
    UNIQUE (email);
    
    RAISE NOTICE 'Contrainte uk_admin_email ajoutée';
END $$;

-- 4.4. Contrainte pour disponibilite (déjà existante normalement)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_disponibilite_pharmacie_medicament') THEN
        ALTER TABLE disponibilite 
        ADD CONSTRAINT uk_disponibilite_pharmacie_medicament 
        UNIQUE (pharmacie_id, medicament_id);
        
        RAISE NOTICE 'Contrainte uk_disponibilite_pharmacie_medicament ajoutée';
    ELSE
        RAISE NOTICE 'Contrainte uk_disponibilite_pharmacie_medicament existe déjà';
    END IF;
END $$;

-- ============================================
-- 5. VÉRIFICATION FINALE
-- ============================================

-- Vérifier qu'il n'y a plus de doublons
SELECT 'Vérification finale des doublons' as info;

-- Médicaments
SELECT 'Doublons médicaments' as type, COUNT(*) as count
FROM (
    SELECT nom, dosage, forme, COUNT(*) as cnt
    FROM medicament
    GROUP BY nom, dosage, forme
    HAVING COUNT(*) > 1
) as duplicates;

-- Pharmacies
SELECT 'Doublons pharmacies' as type, COUNT(*) as count
FROM (
    SELECT nom, adresse, COUNT(*) as cnt
    FROM pharmacie
    GROUP BY nom, adresse
    HAVING COUNT(*) > 1
) as duplicates;

-- Disponibilités
SELECT 'Doublons disponibilités' as type, COUNT(*) as count
FROM (
    SELECT pharmacie_id, medicament_id, COUNT(*) as cnt
    FROM disponibilite
    GROUP BY pharmacie_id, medicament_id
    HAVING COUNT(*) > 1
) as duplicates;

-- Admins (username)
SELECT 'Doublons admins (username)' as type, COUNT(*) as count
FROM (
    SELECT username, COUNT(*) as cnt
    FROM admin_users
    GROUP BY username
    HAVING COUNT(*) > 1
) as duplicates;

-- Admins (email)
SELECT 'Doublons admins (email)' as type, COUNT(*) as count
FROM (
    SELECT email, COUNT(*) as cnt
    FROM admin_users
    GROUP BY email
    HAVING COUNT(*) > 1
) as duplicates;

-- Statistiques finales
SELECT 'STATISTIQUES FINALES' as info;
SELECT 'Médicaments' as table_name, COUNT(*) as total FROM medicament
UNION ALL
SELECT 'Pharmacies', COUNT(*) FROM pharmacie
UNION ALL
SELECT 'Disponibilités', COUNT(*) FROM disponibilite
UNION ALL
SELECT 'Admins', COUNT(*) FROM admin_users;

-- ============================================
-- 6. LISTE DES CONTRAINTES ACTIVES
-- ============================================

SELECT 
    tc.table_name, 
    tc.constraint_name, 
    tc.constraint_type,
    STRING_AGG(kcu.column_name, ', ' ORDER BY kcu.ordinal_position) as columns
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_schema = 'public'
    AND tc.constraint_type = 'UNIQUE'
    AND tc.table_name IN ('medicament', 'pharmacie', 'disponibilite', 'admin_users')
GROUP BY tc.table_name, tc.constraint_name, tc.constraint_type
ORDER BY tc.table_name, tc.constraint_name;

-- ============================================
-- FIN DU SCRIPT
-- ============================================

-- Message de succès
DO $$ 
BEGIN
    RAISE NOTICE '✅ Nettoyage terminé avec succès!';
    RAISE NOTICE '📊 Vérifiez les statistiques ci-dessus';
    RAISE NOTICE '🔒 Les contraintes d''unicité sont maintenant actives';
END $$;
