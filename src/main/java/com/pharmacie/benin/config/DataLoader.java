package com.pharmacie.benin.config;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Disponibilite;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import com.pharmacie.benin.repository.AdminUserRepository;
import com.pharmacie.benin.repository.DisponibiliteRepository;
import com.pharmacie.benin.repository.MedicamentRepository;
import com.pharmacie.benin.repository.PharmacieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    @Transactional
    CommandLineRunner initDatabase(PharmacieRepository pharmacieRepo, 
                                   MedicamentRepository medicamentRepo, 
                                   DisponibiliteRepository dispoRepo,
                                   AdminUserRepository adminUserRepo,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            logger.info("🚀 Initialisation de la base de données...");
            
            // Création de l'utilisateur admin par défaut
            createAdminUser(adminUserRepo, passwordEncoder);
            
            // Création des médicaments
            Map<String, Medicament> medicaments = createMedicaments(medicamentRepo);
            
            // Création des pharmacies
            Map<String, Pharmacie> pharmacies = createPharmacies(pharmacieRepo);
            
            // Création des disponibilités
            createDisponibilites(dispoRepo, pharmacies, medicaments);
            
            logger.info("✅ Base de données initialisée avec succès!");
            logger.info("📊 Statistiques:");
            logger.info("   - Médicaments: {}", medicamentRepo.count());
            logger.info("   - Pharmacies: {}", pharmacieRepo.count());
            logger.info("   - Disponibilités: {}", dispoRepo.count());
            logger.info("   - Admins: {}", adminUserRepo.count());
        };
    }
    
    private void createAdminUser(AdminUserRepository adminUserRepo, PasswordEncoder passwordEncoder) {
        if (adminUserRepo.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@pharma-benin.com");
            admin.setActive(true);
            adminUserRepo.save(admin);
            logger.info("✅ Utilisateur admin créé (username: admin, password: admin123)");
        } else {
            logger.info("ℹ️  Utilisateur admin existe déjà");
        }
    }
    
    private Map<String, Medicament> createMedicaments(MedicamentRepository medicamentRepo) {
        Map<String, Medicament> medicaments = new HashMap<>();
        
        // Définition des médicaments
        String[][] medsData = {
            {"Paracétamol", "Paracétamol", "Comprimé", "500mg", "Antalgique et antipyrétique"},
            {"Amoxicilline", "Amoxicilline", "Gélule", "500mg", "Antibiotique"},
            {"Ibuprofène", "Ibuprofène", "Comprimé", "400mg", "Anti-inflammatoire non stéroïdien"},
            {"Aspirine", "Acide acétylsalicylique", "Comprimé", "500mg", "Antalgique et antipyrétique"},
            {"Doliprane", "Paracétamol", "Comprimé", "1000mg", "Antalgique et antipyrétique"}
        };
        
        for (String[] data : medsData) {
            String key = data[0];
            // Vérifier si le médicament existe déjà (par nom, dosage et forme)
            if (medicamentRepo.findByNomAndDosageAndForme(data[0], data[3], data[2]).isEmpty()) {
                Medicament med = new Medicament(null, data[0], data[1], data[2], data[3], data[4]);
                med = medicamentRepo.save(med);
                medicaments.put(key, med);
                logger.info("✅ Médicament créé: {}", data[0]);
            } else {
                Medicament med = medicamentRepo.findByNomAndDosageAndForme(data[0], data[3], data[2]).get();
                medicaments.put(key, med);
                logger.info("ℹ️  Médicament existe déjà: {}", data[0]);
            }
        }
        
        return medicaments;
    }
    
    private Map<String, Pharmacie> createPharmacies(PharmacieRepository pharmacieRepo) {
        Map<String, Pharmacie> pharmacies = new HashMap<>();
        
        // Définition des pharmacies
        Object[][] pharmsData = {
            {"Camp Guezo", "Pharmacie Camp Guezo", "Avenue Jean-Paul II, Cotonou", 6.3619, 2.4244, "+229 21 31 22 22", "Fermeture à 22:00"},
            {"Les Cocotiers", "Pharmacie Les Cocotiers", "Quartier Les Cocotiers, Cotonou", 6.3704, 2.4233, "+229 21 30 15 15", "24h/24"},
            {"Saint Luc", "Pharmacie Saint Luc", "Cadjehoun, Cotonou", 6.3604, 2.4133, "+229 21 30 20 20", "Fermeture à 20:00"},
            {"Etoile", "Pharmacie de l'Etoile", "Zone Résidentielle, Cotonou", 6.3754, 2.4283, "+229 21 30 10 10", "24h/24"},
            {"Fidjrossè", "Pharmacie Fidjrossè", "Fidjrossè, Cotonou", 6.3483, 2.3611, "+229 21 30 00 00", "24h/24"}
        };
        
        for (Object[] data : pharmsData) {
            String key = (String) data[0];
            String nom = (String) data[1];
            String adresse = (String) data[2];
            
            // Vérifier si la pharmacie existe déjà (par nom et adresse)
            if (pharmacieRepo.findByNomAndAdresse(nom, adresse).isEmpty()) {
                Pharmacie pharm = new Pharmacie(null, nom, adresse, 
                    (Double) data[3], (Double) data[4], (String) data[5], (String) data[6]);
                pharm = pharmacieRepo.save(pharm);
                pharmacies.put(key, pharm);
                logger.info("✅ Pharmacie créée: {}", nom);
            } else {
                Pharmacie pharm = pharmacieRepo.findByNomAndAdresse(nom, adresse).get();
                pharmacies.put(key, pharm);
                logger.info("ℹ️  Pharmacie existe déjà: {}", nom);
            }
        }
        
        return pharmacies;
    }
    
    private void createDisponibilites(DisponibiliteRepository dispoRepo, 
                                     Map<String, Pharmacie> pharmacies, 
                                     Map<String, Medicament> medicaments) {
        
        // Définition des disponibilités [pharmacie, medicament, quantité]
        Object[][] dispoData = {
            // Paracétamol
            {"Camp Guezo", "Paracétamol", 100},
            {"Les Cocotiers", "Paracétamol", 50},
            {"Saint Luc", "Paracétamol", 3},
            {"Etoile", "Paracétamol", 75},
            {"Fidjrossè", "Paracétamol", 200},
            
            // Amoxicilline
            {"Camp Guezo", "Amoxicilline", 20},
            {"Les Cocotiers", "Amoxicilline", 45},
            {"Fidjrossè", "Amoxicilline", 10},
            
            // Ibuprofène
            {"Camp Guezo", "Ibuprofène", 75},
            {"Les Cocotiers", "Ibuprofène", 60},
            {"Etoile", "Ibuprofène", 90},
            
            // Aspirine
            {"Les Cocotiers", "Aspirine", 40},
            {"Saint Luc", "Aspirine", 4},
            {"Fidjrossè", "Aspirine", 30},
            
            // Doliprane
            {"Camp Guezo", "Doliprane", 120},
            {"Les Cocotiers", "Doliprane", 85},
            {"Etoile", "Doliprane", 110},
            {"Fidjrossè", "Doliprane", 90}
        };
        
        int created = 0;
        int existing = 0;
        
        for (Object[] data : dispoData) {
            Pharmacie pharmacie = pharmacies.get(data[0]);
            Medicament medicament = medicaments.get(data[1]);
            Integer quantite = (Integer) data[2];
            
            if (pharmacie != null && medicament != null) {
                // Vérifier si la disponibilité existe déjà
                if (dispoRepo.findByPharmacieAndMedicament(pharmacie, medicament).isEmpty()) {
                    Disponibilite dispo = new Disponibilite(null, pharmacie, medicament, true, quantite);
                    dispoRepo.save(dispo);
                    created++;
                } else {
                    existing++;
                }
            }
        }
        
        logger.info("✅ Disponibilités créées: {}", created);
        if (existing > 0) {
            logger.info("ℹ️  Disponibilités existantes: {}", existing);
        }
    }
}
