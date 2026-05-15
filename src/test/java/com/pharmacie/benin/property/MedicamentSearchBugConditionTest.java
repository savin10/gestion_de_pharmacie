package com.pharmacie.benin.property;

import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.repository.MedicamentRepository;
import com.pharmacie.benin.service.PharmacieService;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug Condition Exploration Property Test for Medication Search
 * 
 * **CRITIQUE**: Ce test DOIT ÉCHOUER sur le code non corrigé - l'échec confirme que le bug existe
 * **NE PAS tenter de corriger le test ou le code quand il échoue**
 * 
 * **OBJECTIF**: Générer des contre-exemples qui démontrent l'existence du bug
 * 
 * Feature: medication-search-fix
 * Property 1: Bug Condition - Recherche de Médicaments Retourne des Résultats
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
 * 
 * Ce test encode le comportement attendu - il validera la correction quand il passera après l'implémentation.
 * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide au lieu de retourner
 * les médicaments correspondants.
 */
@SpringBootTest
@ActiveProfiles("test")
public class MedicamentSearchBugConditionTest {
    
    @Autowired
    private PharmacieService pharmacieService;
    
    @Autowired
    private MedicamentRepository medicamentRepository;
    
    /**
     * Initialise la base de données avec des médicaments de test avant chaque test.
     * Ces médicaments sont utilisés pour vérifier que la recherche fonctionne correctement.
     */
    @BeforeEach
    void setUp() {
        // Nettoyer la base de données
        medicamentRepository.deleteAll();
        
        // Ajouter des médicaments de test
        Medicament paracetamol500 = new Medicament();
        paracetamol500.setNom("Paracétamol 500mg");
        paracetamol500.setPrincipeActif("Paracétamol");
        paracetamol500.setForme("Comprimé");
        paracetamol500.setDosage("500mg");
        paracetamol500.setDescription("Antalgique et antipyrétique");
        medicamentRepository.save(paracetamol500);
        
        Medicament paracetamol1000 = new Medicament();
        paracetamol1000.setNom("Paracétamol 1000mg");
        paracetamol1000.setPrincipeActif("Paracétamol");
        paracetamol1000.setForme("Comprimé");
        paracetamol1000.setDosage("1000mg");
        paracetamol1000.setDescription("Antalgique et antipyrétique forte dose");
        medicamentRepository.save(paracetamol1000);
        
        Medicament ibuprofene = new Medicament();
        ibuprofene.setNom("Advil 400mg");
        ibuprofene.setPrincipeActif("Ibuprofène");
        ibuprofene.setForme("Comprimé");
        ibuprofene.setDosage("400mg");
        ibuprofene.setDescription("Anti-inflammatoire non stéroïdien");
        medicamentRepository.save(ibuprofene);
        
        Medicament amoxicilline = new Medicament();
        amoxicilline.setNom("Amoxicilline 500mg");
        amoxicilline.setPrincipeActif("Amoxicilline");
        amoxicilline.setForme("Gélule");
        amoxicilline.setDosage("500mg");
        amoxicilline.setDescription("Antibiotique de la famille des pénicillines");
        medicamentRepository.save(amoxicilline);
    }
    
    /**
     * Test Case 1: Recherche par nom complet
     * 
     * QUAND un utilisateur saisit un terme de recherche valide correspondant au nom d'un médicament existant
     * ALORS le système DOIT retourner tous les médicaments dont le nom contient ce terme (insensible à la casse)
     * 
     * **Validates: Requirements 2.1**
     * 
     * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide.
     */
    @Property(tries = 10)
    @Tag("property-1")
    void rechercheParNomCompletRetourneResultats() {
        // Given: Un médicament "Paracétamol 500mg" existe dans la base de données
        // (configuré dans setUp())
        
        // When: L'utilisateur recherche "Paracétamol"
        List<Medicament> resultats = pharmacieService.rechercherMedicaments("Paracétamol");
        
        // Then: Le système DOIT retourner au moins un médicament
        assertThat(resultats)
            .as("La recherche 'Paracétamol' devrait retourner au moins un médicament")
            .isNotEmpty();
        
        // Then: Tous les résultats doivent contenir "Paracétamol" dans le nom ou le principe actif
        assertThat(resultats)
            .as("Tous les résultats doivent contenir 'Paracétamol' dans le nom ou le principe actif")
            .allMatch(m -> 
                m.getNom().toLowerCase().contains("paracétamol".toLowerCase()) ||
                m.getPrincipeActif().toLowerCase().contains("paracétamol".toLowerCase())
            );
        
        // Then: Le résultat devrait contenir au moins "Paracétamol 500mg"
        assertThat(resultats)
            .as("Le résultat devrait contenir 'Paracétamol 500mg'")
            .anyMatch(m -> m.getNom().equals("Paracétamol 500mg"));
    }
    
    /**
     * Test Case 2: Recherche partielle
     * 
     * QUAND un utilisateur saisit un terme de recherche partiel correspondant au début ou à une partie d'un nom de médicament
     * ALORS le système DOIT retourner tous les médicaments dont le nom contient cette partie (insensible à la casse)
     * 
     * **Validates: Requirements 2.2**
     * 
     * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide.
     */
    @Property(tries = 10)
    @Tag("property-1")
    void recherchePartielleRetourneResultats() {
        // Given: Des médicaments commençant par "Para" existent dans la base de données
        // (configuré dans setUp())
        
        // When: L'utilisateur recherche "Para"
        List<Medicament> resultats = pharmacieService.rechercherMedicaments("Para");
        
        // Then: Le système DOIT retourner au moins un médicament
        assertThat(resultats)
            .as("La recherche 'Para' devrait retourner au moins un médicament")
            .isNotEmpty();
        
        // Then: Tous les résultats doivent contenir "Para" dans le nom ou le principe actif
        assertThat(resultats)
            .as("Tous les résultats doivent contenir 'Para' dans le nom ou le principe actif")
            .allMatch(m -> 
                m.getNom().toLowerCase().contains("para") ||
                m.getPrincipeActif().toLowerCase().contains("para")
            );
        
        // Then: Le résultat devrait contenir au moins 2 médicaments (Paracétamol 500mg et 1000mg)
        assertThat(resultats)
            .as("Le résultat devrait contenir au moins 2 médicaments")
            .hasSizeGreaterThanOrEqualTo(2);
    }
    
    /**
     * Test Case 3: Recherche par principe actif
     * 
     * QUAND un utilisateur saisit un terme correspondant au principe actif d'un médicament
     * ALORS le système DOIT retourner tous les médicaments contenant ce principe actif (insensible à la casse)
     * 
     * **Validates: Requirements 2.3**
     * 
     * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide.
     */
    @Property(tries = 10)
    @Tag("property-1")
    void rechercheParPrincipeActifRetourneResultats() {
        // Given: Un médicament avec le principe actif "Ibuprofène" existe dans la base de données
        // (configuré dans setUp())
        
        // When: L'utilisateur recherche "Ibuprofène"
        List<Medicament> resultats = pharmacieService.rechercherMedicaments("Ibuprofène");
        
        // Then: Le système DOIT retourner au moins un médicament
        assertThat(resultats)
            .as("La recherche 'Ibuprofène' devrait retourner au moins un médicament")
            .isNotEmpty();
        
        // Then: Tous les résultats doivent contenir "Ibuprofène" dans le nom ou le principe actif
        assertThat(resultats)
            .as("Tous les résultats doivent contenir 'Ibuprofène' dans le nom ou le principe actif")
            .allMatch(m -> 
                m.getNom().toLowerCase().contains("ibuprofène".toLowerCase()) ||
                m.getPrincipeActif().toLowerCase().contains("ibuprofène".toLowerCase())
            );
        
        // Then: Le résultat devrait contenir "Advil 400mg"
        assertThat(resultats)
            .as("Le résultat devrait contenir 'Advil 400mg'")
            .anyMatch(m -> m.getNom().equals("Advil 400mg"));
    }
    
    /**
     * Test Case 4: Recherche insensible à la casse
     * 
     * QUAND un utilisateur saisit un terme de recherche en minuscules alors que le médicament existe en majuscules
     * ALORS le système DOIT retourner les médicaments correspondants (insensible à la casse)
     * 
     * **Validates: Requirements 2.1, 2.4**
     * 
     * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide.
     */
    @Property(tries = 10)
    @Tag("property-1")
    void rechercheInsensibleCasseRetourneResultats() {
        // Given: Un médicament "Paracétamol 500mg" existe dans la base de données
        // (configuré dans setUp())
        
        // When: L'utilisateur recherche "paracetamol" (tout en minuscules)
        List<Medicament> resultats = pharmacieService.rechercherMedicaments("paracetamol");
        
        // Then: Le système DOIT retourner au moins un médicament
        assertThat(resultats)
            .as("La recherche 'paracetamol' (minuscules) devrait retourner au moins un médicament")
            .isNotEmpty();
        
        // Then: Tous les résultats doivent contenir "paracetamol" dans le nom ou le principe actif (insensible à la casse)
        assertThat(resultats)
            .as("Tous les résultats doivent contenir 'paracetamol' dans le nom ou le principe actif")
            .allMatch(m -> 
                m.getNom().toLowerCase().contains("paracetamol") ||
                m.getPrincipeActif().toLowerCase().contains("paracetamol")
            );
        
        // Then: Le résultat devrait contenir "Paracétamol 500mg"
        assertThat(resultats)
            .as("Le résultat devrait contenir 'Paracétamol 500mg'")
            .anyMatch(m -> m.getNom().equals("Paracétamol 500mg"));
    }
    
    /**
     * Test Case 5: Recherche avec plusieurs résultats
     * 
     * QUAND un utilisateur effectue une recherche qui correspond à plusieurs médicaments
     * ALORS le système DOIT retourner tous les médicaments correspondants
     * 
     * **Validates: Requirements 2.4, 2.5**
     * 
     * Sur le code non corrigé, ce test échouera car la recherche retourne une liste vide.
     */
    @Property(tries = 10)
    @Tag("property-1")
    void rechercheAvecPlusieursResultatsRetourneTousLesResultats() {
        // Given: Plusieurs médicaments contenant "Paracétamol" existent dans la base de données
        // (configuré dans setUp())
        
        // When: L'utilisateur recherche "Paracétamol"
        List<Medicament> resultats = pharmacieService.rechercherMedicaments("Paracétamol");
        
        // Then: Le système DOIT retourner au moins 2 médicaments
        assertThat(resultats)
            .as("La recherche 'Paracétamol' devrait retourner au moins 2 médicaments")
            .hasSizeGreaterThanOrEqualTo(2);
        
        // Then: Le résultat devrait contenir "Paracétamol 500mg" et "Paracétamol 1000mg"
        assertThat(resultats)
            .as("Le résultat devrait contenir 'Paracétamol 500mg'")
            .anyMatch(m -> m.getNom().equals("Paracétamol 500mg"));
        
        assertThat(resultats)
            .as("Le résultat devrait contenir 'Paracétamol 1000mg'")
            .anyMatch(m -> m.getNom().equals("Paracétamol 1000mg"));
    }
}
