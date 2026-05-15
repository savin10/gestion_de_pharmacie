package com.pharmacie.benin.controller;

import com.pharmacie.benin.dto.PharmacieDistanceDTO;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import com.pharmacie.benin.service.PharmacieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PharmacieController {

    @Autowired
    private PharmacieService pharmacieService;

    @GetMapping("/medicaments/recherche")
    public List<Medicament> rechercher(@RequestParam String query) {
        return pharmacieService.rechercherMedicaments(query);
    }

    @GetMapping("/medicaments/{id}/disponibilite")
    public List<PharmacieDistanceDTO> verifierDisponibilite(
            @PathVariable Long id,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return pharmacieService.trouverPharmaciesProches(id, lat, lon);
    }

    @GetMapping("/pharmacies")
    public List<Pharmacie> obtenirToutesPharmacie() {
        return pharmacieService.obtenirToutesPharmacie();
    }

    @GetMapping("/pharmacies/garde")
    public List<Pharmacie> obtenirPharmaciesGarde() {
        return pharmacieService.obtenirPharmaciesGarde();
    }

    @GetMapping("/pharmacies/proches")
    public List<PharmacieDistanceDTO> obtenirPharmaciesProches(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return pharmacieService.obtenirToutesPharmaciesProches(lat, lon);
    }
}

