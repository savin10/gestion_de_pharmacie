-- Script de vérification de l'intégrité de la base de données PharmaBenin
-- Ce script ne modifie rien, il vérifie seulement

-- ============================================
-- 1. STATISTIQUES GÉNÉRALES
-- ============================================

SELECT '========================================' as separator;
SELECT '   STATISTIQUES GÉNÉRALES' as title;
SELECT '========================================' as separator;

SELECT 
    'Médicaments' as table_name, 
    COUNT(*) as total_records,
    COUNT(DISTINCT nom) as unique_names
FROM medicament
UNION ALL
SELECT 
    'Pharmacies', 
    COUNT(*), 
    COUNT(DISTINCT nom)
FROM pharmacie
UNION ALL
SELECT 
    'Disponibilités', 
    COUNT(*), 
    COUNT(DISTINCT pharmacie_id || '-' || medicament_id)
FROM disponibilite
UNION ALL
SELECT 
    'Admins', 
    COUNT(*), 
    COUNT(DISTINCT username)
FROM admin_users;

-- ============================================
-- 2. VÉRIFICATION DES DOUBLONS
-- ============================================

SELECT '========================================' as separator;
SELECT '   VÉRIFICATION DES DOUBLONS' as title;
SELECT '========================================' as separator;

-- Doublons de médicaments
SELECT 'MÉDICAMENTS - Doublons détectés:' as check_type;
SELECT nom, dosage, forme, COUNT(*) as occurrences
FROM medicament
GROUP BY nom, dosage, forme
HAVING COUNT(*) > 1
ORDER BY occurrences DESC;

-- Si aucun doublon
SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Aucun doublon de médicament'
    ELSE '❌ ' || COUNT(*) || ' groupe(s) de doublons trouvé(s)'
END as result
FROM (
    SELECT nom, dosage, forme
    FROM medicament
    GROUP BY nom, dosage, forme
    HAVING COUNT(*) > 1
) as duplicates;

-- Doublons de pharmacies
SELECT 'PHARMACIES - Doublons détectés:' as check_type;
SELECT nom, adresse, COUNT(*) as occurrences
FROM pharmacie
GROUP BY nom, adresse
HAVING COUNT(*) > 1
ORDER BY occurrences DESC;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Aucun doublon de pharmacie'
    ELSE '❌ ' || COUNT(*) || ' groupe(s) de doublons trouvé(s)'
END as result
FROM (
    SELECT nom, adresse
    FROM pharmacie
    GROUP BY nom, adresse
    HAVING COUNT(*) > 1
) as duplicates;

-- Doublons de disponibilités
SELECT 'DISPONIBILITÉS - Doublons détectés:' as check_type;
SELECT 
    p.nom as pharmacie,
    m.nom as medicament,
    COUNT(*) as occurrences
FROM disponibilite d
JOIN pharmacie p ON d.pharmacie_id = p.id
JOIN medicament m ON d.medicament_id = m.id
GROUP BY p.nom, m.nom, d.pharmacie_id, d.medicament_id
HAVING COUNT(*) > 1
ORDER BY occurrences DESC;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Aucun doublon de disponibilité'
    ELSE '❌ ' || COUNT(*) || ' groupe(s) de doublons trouvé(s)'
END as result
FROM (
    SELECT pharmacie_id, medicament_id
    FROM disponibilite
    GROUP BY pharmacie_id, medicament_id
    HAVING COUNT(*) > 1
) as duplicates;

-- Doublons d'admins (username)
SELECT 'ADMINS (USERNAME) - Doublons détectés:' as check_type;
SELECT username, COUNT(*) as occurrences
FROM admin_users
GROUP BY username
HAVING COUNT(*) > 1
ORDER BY occurrences DESC;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Aucun doublon de username'
    ELSE '❌ ' || COUNT(*) || ' username(s) en doublon'
END as result
FROM (
    SELECT username
    FROM admin_users
    GROUP BY username
    HAVING COUNT(*) > 1
) as duplicates;

-- Doublons d'admins (email)
SELECT 'ADMINS (EMAIL) - Doublons détectés:' as check_type;
SELECT email, COUNT(*) as occurrences
FROM admin_users
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY occurrences DESC;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Aucun doublon d''email'
    ELSE '❌ ' || COUNT(*) || ' email(s) en doublon'
END as result
FROM (
    SELECT email
    FROM admin_users
    GROUP BY email
    HAVING COUNT(*) > 1
) as duplicates;

-- ============================================
-- 3. VÉRIFICATION DES CONTRAINTES
-- ============================================

SELECT '========================================' as separator;
SELECT '   CONTRAINTES D''UNICITÉ' as title;
SELECT '========================================' as separator;

SELECT 
    tc.table_name as "Table", 
    tc.constraint_name as "Contrainte",
    STRING_AGG(kcu.column_name, ', ' ORDER BY kcu.ordinal_position) as "Colonnes"
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_schema = 'public'
    AND tc.constraint_type = 'UNIQUE'
    AND tc.table_name IN ('medicament', 'pharmacie', 'disponibilite', 'admin_users')
GROUP BY tc.table_name, tc.constraint_name
ORDER BY tc.table_name, tc.constraint_name;

-- Vérifier les contraintes attendues
SELECT 'Vérification des contraintes attendues:' as check_type;

SELECT CASE 
    WHEN EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'medicament' 
        AND constraint_type = 'UNIQUE'
        AND constraint_name LIKE '%nom%dosage%forme%'
    ) THEN '✅ Contrainte medicament (nom, dosage, forme) présente'
    ELSE '❌ Contrainte medicament (nom, dosage, forme) MANQUANTE'
END as result
UNION ALL
SELECT CASE 
    WHEN EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'pharmacie' 
        AND constraint_type = 'UNIQUE'
        AND constraint_name LIKE '%nom%adresse%'
    ) THEN '✅ Contrainte pharmacie (nom, adresse) présente'
    ELSE '❌ Contrainte pharmacie (nom, adresse) MANQUANTE'
END
UNION ALL
SELECT CASE 
    WHEN EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'disponibilite' 
        AND constraint_type = 'UNIQUE'
    ) THEN '✅ Contrainte disponibilite présente'
    ELSE '❌ Contrainte disponibilite MANQUANTE'
END
UNION ALL
SELECT CASE 
    WHEN EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'admin_users' 
        AND constraint_type = 'UNIQUE'
        AND constraint_name LIKE '%email%'
    ) THEN '✅ Contrainte admin_users (email) présente'
    ELSE '❌ Contrainte admin_users (email) MANQUANTE'
END;

-- ============================================
-- 4. VÉRIFICATION DES DONNÉES NULL
-- ============================================

SELECT '========================================' as separator;
SELECT '   VÉRIFICATION DES VALEURS NULL' as title;
SELECT '========================================' as separator;

-- Médicaments avec valeurs NULL
SELECT 'Médicaments avec valeurs NULL:' as check_type;
SELECT 
    COUNT(*) FILTER (WHERE nom IS NULL) as nom_null,
    COUNT(*) FILTER (WHERE principe_actif IS NULL) as principe_actif_null,
    COUNT(*) FILTER (WHERE forme IS NULL) as forme_null,
    COUNT(*) FILTER (WHERE dosage IS NULL) as dosage_null
FROM medicament;

-- Pharmacies avec valeurs NULL
SELECT 'Pharmacies avec valeurs NULL:' as check_type;
SELECT 
    COUNT(*) FILTER (WHERE nom IS NULL) as nom_null,
    COUNT(*) FILTER (WHERE adresse IS NULL) as adresse_null,
    COUNT(*) FILTER (WHERE latitude IS NULL) as latitude_null,
    COUNT(*) FILTER (WHERE longitude IS NULL) as longitude_null,
    COUNT(*) FILTER (WHERE telephone IS NULL) as telephone_null
FROM pharmacie;

-- Disponibilités avec valeurs NULL
SELECT 'Disponibilités avec valeurs NULL:' as check_type;
SELECT 
    COUNT(*) FILTER (WHERE pharmacie_id IS NULL) as pharmacie_null,
    COUNT(*) FILTER (WHERE medicament_id IS NULL) as medicament_null,
    COUNT(*) FILTER (WHERE disponible IS NULL) as disponible_null
FROM disponibilite;

-- ============================================
-- 5. VÉRIFICATION DE L'INTÉGRITÉ RÉFÉRENTIELLE
-- ============================================

SELECT '========================================' as separator;
SELECT '   INTÉGRITÉ RÉFÉRENTIELLE' as title;
SELECT '========================================' as separator;

-- Disponibilités avec pharmacie inexistante
SELECT 'Disponibilités orphelines (pharmacie):' as check_type;
SELECT COUNT(*) as count
FROM disponibilite d
LEFT JOIN pharmacie p ON d.pharmacie_id = p.id
WHERE p.id IS NULL;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Toutes les disponibilités ont une pharmacie valide'
    ELSE '❌ ' || COUNT(*) || ' disponibilité(s) avec pharmacie invalide'
END as result
FROM disponibilite d
LEFT JOIN pharmacie p ON d.pharmacie_id = p.id
WHERE p.id IS NULL;

-- Disponibilités avec médicament inexistant
SELECT 'Disponibilités orphelines (médicament):' as check_type;
SELECT COUNT(*) as count
FROM disponibilite d
LEFT JOIN medicament m ON d.medicament_id = m.id
WHERE m.id IS NULL;

SELECT CASE 
    WHEN COUNT(*) = 0 THEN '✅ Toutes les disponibilités ont un médicament valide'
    ELSE '❌ ' || COUNT(*) || ' disponibilité(s) avec médicament invalide'
END as result
FROM disponibilite d
LEFT JOIN medicament m ON d.medicament_id = m.id
WHERE m.id IS NULL;

-- ============================================
-- 6. STATISTIQUES PAR PHARMACIE
-- ============================================

SELECT '========================================' as separator;
SELECT '   STATISTIQUES PAR PHARMACIE' as title;
SELECT '========================================' as separator;

SELECT 
    p.nom as pharmacie,
    COUNT(d.id) as nb_medicaments_disponibles,
    SUM(d.quantite_stock) as stock_total,
    AVG(d.quantite_stock) as stock_moyen
FROM pharmacie p
LEFT JOIN disponibilite d ON p.id = d.pharmacie_id AND d.disponible = true
GROUP BY p.id, p.nom
ORDER BY nb_medicaments_disponibles DESC;

-- ============================================
-- 7. STATISTIQUES PAR MÉDICAMENT
-- ============================================

SELECT '========================================' as separator;
SELECT '   STATISTIQUES PAR MÉDICAMENT' as title;
SELECT '========================================' as separator;

SELECT 
    m.nom as medicament,
    m.dosage,
    COUNT(d.id) as nb_pharmacies,
    SUM(d.quantite_stock) as stock_total,
    AVG(d.quantite_stock) as stock_moyen
FROM medicament m
LEFT JOIN disponibilite d ON m.id = d.medicament_id AND d.disponible = true
GROUP BY m.id, m.nom, m.dosage
ORDER BY nb_pharmacies DESC;

-- ============================================
-- 8. MÉDICAMENTS EN RUPTURE DE STOCK
-- ============================================

SELECT '========================================' as separator;
SELECT '   ALERTES STOCK' as title;
SELECT '========================================' as separator;

-- Médicaments avec stock critique (≤ 5)
SELECT 'Médicaments en stock critique (≤ 5 unités):' as alert_type;
SELECT 
    p.nom as pharmacie,
    m.nom as medicament,
    d.quantite_stock as stock
FROM disponibilite d
JOIN pharmacie p ON d.pharmacie_id = p.id
JOIN medicament m ON d.medicament_id = m.id
WHERE d.disponible = true 
    AND d.quantite_stock <= 5
ORDER BY d.quantite_stock ASC, p.nom;

-- Médicaments non disponibles
SELECT 'Médicaments marqués comme non disponibles:' as alert_type;
SELECT 
    p.nom as pharmacie,
    m.nom as medicament
FROM disponibilite d
JOIN pharmacie p ON d.pharmacie_id = p.id
JOIN medicament m ON d.medicament_id = m.id
WHERE d.disponible = false
ORDER BY p.nom, m.nom;

-- ============================================
-- 9. RÉSUMÉ FINAL
-- ============================================

SELECT '========================================' as separator;
SELECT '   RÉSUMÉ DE LA VÉRIFICATION' as title;
SELECT '========================================' as separator;

DO $$ 
DECLARE
    med_duplicates INTEGER;
    pharm_duplicates INTEGER;
    dispo_duplicates INTEGER;
    admin_duplicates INTEGER;
    orphan_dispo INTEGER;
    total_issues INTEGER;
BEGIN
    -- Compter les problèmes
    SELECT COUNT(*) INTO med_duplicates FROM (
        SELECT nom, dosage, forme FROM medicament 
        GROUP BY nom, dosage, forme HAVING COUNT(*) > 1
    ) as d;
    
    SELECT COUNT(*) INTO pharm_duplicates FROM (
        SELECT nom, adresse FROM pharmacie 
        GROUP BY nom, adresse HAVING COUNT(*) > 1
    ) as d;
    
    SELECT COUNT(*) INTO dispo_duplicates FROM (
        SELECT pharmacie_id, medicament_id FROM disponibilite 
        GROUP BY pharmacie_id, medicament_id HAVING COUNT(*) > 1
    ) as d;
    
    SELECT COUNT(*) INTO admin_duplicates FROM (
        SELECT username FROM admin_users 
        GROUP BY username HAVING COUNT(*) > 1
    ) as d;
    
    SELECT COUNT(*) INTO orphan_dispo FROM disponibilite d
    LEFT JOIN pharmacie p ON d.pharmacie_id = p.id
    LEFT JOIN medicament m ON d.medicament_id = m.id
    WHERE p.id IS NULL OR m.id IS NULL;
    
    total_issues := med_duplicates + pharm_duplicates + dispo_duplicates + admin_duplicates + orphan_dispo;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'RÉSUMÉ:';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Doublons médicaments: %', med_duplicates;
    RAISE NOTICE 'Doublons pharmacies: %', pharm_duplicates;
    RAISE NOTICE 'Doublons disponibilités: %', dispo_duplicates;
    RAISE NOTICE 'Doublons admins: %', admin_duplicates;
    RAISE NOTICE 'Disponibilités orphelines: %', orphan_dispo;
    RAISE NOTICE '========================================';
    RAISE NOTICE 'TOTAL PROBLÈMES: %', total_issues;
    RAISE NOTICE '========================================';
    
    IF total_issues = 0 THEN
        RAISE NOTICE '✅ BASE DE DONNÉES INTÈGRE - Aucun problème détecté';
    ELSE
        RAISE NOTICE '❌ PROBLÈMES DÉTECTÉS - Exécuter clean_duplicates.sql';
    END IF;
END $$;
