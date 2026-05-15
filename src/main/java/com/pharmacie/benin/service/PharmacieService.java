package com.pharmacie.benin.service;

import com.pharmacie.benin.dto.PharmacieDistanceDTO;
import com.pharmacie.benin.model.Disponibilite;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import com.pharmacie.benin.repository.DisponibiliteRepository;
import com.pharmacie.benin.repository.MedicamentRepository;
import com.pharmacie.benin.repository.PharmacieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PharmacieService {

    @Autowired
    private PharmacieRepository pharmacieRepository;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private DisponibiliteRepository disponibiliteRepository;

    public List<Medicament> rechercherMedicaments(String query) {
        return medicamentRepository.findByNomContainingIgnoreCaseOrPrincipeActifContainingIgnoreCase(query, query);
    }

    public List<PharmacieDistanceDTO> trouverPharmaciesProches(Long medicamentId, Double userLat, Double userLon) {
        Medicament medicament = medicamentRepository.findById(medicamentId)
                .orElseThrow(() -> new RuntimeException("Médicament non trouvé"));

        List<Disponibilite> disponibilites = disponibiliteRepository.findByMedicamentAndDisponibleTrue(medicament);

        return disponibilites.stream()
                .map(d -> {
                    Pharmacie p = d.getPharmacie();
                    double distance = calculerDistance(userLat, userLon, p.getLatitude(), p.getLongitude());
                    return new PharmacieDistanceDTO(p, distance, d);
                })
                .sorted(Comparator.comparing(PharmacieDistanceDTO::getDistance))
                .collect(Collectors.toList());
    }

    public List<Pharmacie> obtenirToutesPharmacie() {
        return pharmacieRepository.findAll();
    }

    public List<Pharmacie> obtenirPharmaciesGarde() {
        return pharmacieRepository.findAll()
                .stream()
                .filter(p -> p.getHoraires().contains("24h") || p.getHoraires().equals("24h/24"))
                .collect(Collectors.toList());
    }

    public List<PharmacieDistanceDTO> obtenirToutesPharmaciesProches(Double userLat, Double userLon) {
        return pharmacieRepository.findAll()
                .stream()
                .map(p -> {
                    double distance = calculerDistance(userLat, userLon, p.getLatitude(), p.getLongitude());
                    return new PharmacieDistanceDTO(p, distance, null);
                })
                .sorted(Comparator.comparing(PharmacieDistanceDTO::getDistance))
                .collect(Collectors.toList());
    }

    public Medicament obtenirMedicament(Long id) {
        return medicamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médicament non trouvé"));
    }

    public Pharmacie obtenirPharmacie(Long id) {
        return pharmacieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacie non trouvée"));
    }

    public List<Medicament> obtenirAlternatives(Long medicamentId) {
        Medicament medicament = obtenirMedicament(medicamentId);
        // Trouver des médicaments avec le même principe actif ou la même forme
        return medicamentRepository.findAll().stream()
                .filter(m -> !m.getId().equals(medicamentId))
                .filter(m -> m.getPrincipeActif().equalsIgnoreCase(medicament.getPrincipeActif()) 
                          || m.getForme().equalsIgnoreCase(medicament.getForme()))
                .limit(4)
                .collect(Collectors.toList());
    }

    public List<PharmacieDistanceDTO> obtenirAutresPharmaciesAvecMedicament(Long medicamentId, Long pharmacieExclueId, Double userLat, Double userLon) {
        Medicament medicament = obtenirMedicament(medicamentId);
        List<Disponibilite> disponibilites = disponibiliteRepository.findByMedicamentAndDisponibleTrue(medicament);

        return disponibilites.stream()
                .filter(d -> !d.getPharmacie().getId().equals(pharmacieExclueId))
                .map(d -> {
                    Pharmacie p = d.getPharmacie();
                    double distance = calculerDistance(userLat, userLon, p.getLatitude(), p.getLongitude());
                    return new PharmacieDistanceDTO(p, distance, d);
                })
                .sorted(Comparator.comparing(PharmacieDistanceDTO::getDistance))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Calcul de la distance entre deux points (Formule de Haversine)
     */
    private double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
