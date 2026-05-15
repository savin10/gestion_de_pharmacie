package com.pharmacie.benin.repository;

import com.pharmacie.benin.model.Pharmacie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PharmacieRepository extends JpaRepository<Pharmacie, Long> {
    Optional<Pharmacie> findByNom(String nom);
    Optional<Pharmacie> findByNomAndAdresse(String nom, String adresse);
}

