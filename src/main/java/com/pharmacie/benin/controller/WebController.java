package com.pharmacie.benin.controller;

import com.pharmacie.benin.dto.PharmacieDistanceDTO;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import com.pharmacie.benin.service.PharmacieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private PharmacieService pharmacieService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/pharmacies")
    public String pharmacies() {
        return "pharmacies";
    }

    @GetMapping("/search-results")
    public String searchResults() {
        return "search-results";
    }

    @GetMapping("/medicament/{medicamentId}/pharmacie/{pharmacieId}")
    public String medicamentDetail(
            @PathVariable Long medicamentId,
            @PathVariable Long pharmacieId,
            @RequestParam(required = false, defaultValue = "6.3667") Double lat,
            @RequestParam(required = false, defaultValue = "2.4333") Double lon,
            Model model) {
        
        // Récupérer le médicament
        Medicament medicament = pharmacieService.obtenirMedicament(medicamentId);
        model.addAttribute("medicament", medicament);
        
        // Récupérer la pharmacie
        Pharmacie pharmacie = pharmacieService.obtenirPharmacie(pharmacieId);
        model.addAttribute("pharmacie", pharmacie);
        
        // Récupérer les alternatives
        List<Medicament> alternatives = pharmacieService.obtenirAlternatives(medicamentId);
        model.addAttribute("alternatives", alternatives);
        
        // Récupérer les autres pharmacies proches avec ce médicament
        List<PharmacieDistanceDTO> autresPharmacies = pharmacieService.obtenirAutresPharmaciesAvecMedicament(
                medicamentId, pharmacieId, lat, lon);
        model.addAttribute("autresPharmacies", autresPharmacies);
        
        return "medicament-detail";
    }
}
