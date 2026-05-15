package com.pharmacie.benin.repository;

import com.pharmacie.benin.model.Disponibilite;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DisponibiliteRepository extends JpaRepository<Disponibilite, Long> {
    List<Disponibilite> findByMedicamentAndDisponibleTrue(Medicament medicament);
    Optional<Disponibilite> findByPharmacieAndMedicament(Pharmacie pharmacie, Medicament medicament);
    List<Disponibilite> findByMedicament(Medicament medicament);
}

