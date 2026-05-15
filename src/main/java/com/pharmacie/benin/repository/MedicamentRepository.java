package com.pharmacie.benin.repository;

import com.pharmacie.benin.model.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
    List<Medicament> findByNomContainingIgnoreCaseOrPrincipeActifContainingIgnoreCase(String nom, String principeActif);
    Optional<Medicament> findByNom(String nom);
    Optional<Medicament> findByNomAndDosageAndForme(String nom, String dosage, String forme);
}

