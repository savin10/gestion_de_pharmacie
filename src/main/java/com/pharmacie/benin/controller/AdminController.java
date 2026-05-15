package com.pharmacie.benin.controller;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Disponibilite;
import com.pharmacie.benin.model.Medicament;
import com.pharmacie.benin.model.Pharmacie;
import com.pharmacie.benin.repository.AdminUserRepository;
import com.pharmacie.benin.repository.DisponibiliteRepository;
import com.pharmacie.benin.repository.MedicamentRepository;
import com.pharmacie.benin.repository.PharmacieRepository;
import com.pharmacie.benin.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private PharmacieRepository pharmacieRepository;

    @Autowired
    private DisponibiliteRepository disponibiliteRepository;

    /**
     * Affiche la page de login
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Identifiants invalides");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Vous avez été déconnecté");
        }
        return "admin/login";
    }

    /**
     * Affiche la page de déconnexion avec formulaire POST
     */
    @GetMapping("/logout")
    public String logout() {
        return "admin/logout";
    }

    /**
     * Affiche le dashboard administrateur
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        // Récupérer les statistiques
        long totalMedicaments = medicamentRepository.count();
        long totalPharmacies = pharmacieRepository.count();

        model.addAttribute("user", user);
        model.addAttribute("totalMedicaments", totalMedicaments);
        model.addAttribute("totalPharmacies", totalPharmacies);

        return "admin/dashboard";
    }

    /**
     * Affiche le dashboard pharmacien (nouveau design)
     */
    @GetMapping("/pharmacien/dashboard")
    public String pharmacienDashboard(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        return "admin/pharmacien-dashboard";
    }

    /**
     * Affiche la page des stocks (placeholder)
     */
    @GetMapping("/pharmacien/stocks")
    public String pharmacienStocks(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        return "redirect:/admin/pharmacien/dashboard";
    }

    /**
     * Affiche la page des ordonnances (placeholder)
     */
    @GetMapping("/pharmacien/ordonnances")
    public String pharmacienOrdonnances(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        return "redirect:/admin/pharmacien/dashboard";
    }

    /**
     * Affiche la page des ventes (placeholder)
     */
    @GetMapping("/pharmacien/ventes")
    public String pharmacienVentes(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        return "redirect:/admin/pharmacien/dashboard";
    }

    /**
     * Affiche la page des paramètres (placeholder)
     */
    @GetMapping("/pharmacien/parametres")
    public String pharmacienParametres(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        return "redirect:/admin/pharmacien/dashboard";
    }

    /**
     * Affiche la liste des médicaments
     */
    @GetMapping("/medicaments")
    public String medicaments(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        List<Medicament> medicaments = medicamentRepository.findAll();
        model.addAttribute("user", user);
        model.addAttribute("medicaments", medicaments);

        return "admin/medicaments";
    }

    /**
     * Affiche le formulaire d'ajout de médicament
     */
    @GetMapping("/medicaments/add")
    public String addMedicamentForm(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("medicament", new Medicament());

        return "admin/add-medicament";
    }

    /**
     * Sauvegarde un nouveau médicament
     */
    @PostMapping("/medicaments/save")
    public String saveMedicament(
            @ModelAttribute("medicament") Medicament medicament,
            Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        medicamentRepository.save(medicament);
        return "redirect:/admin/medicaments";
    }

    /**
     * Affiche le formulaire d'édition d'un médicament
     */
    @GetMapping("/medicaments/edit/{id}")
    public String editMedicamentForm(@PathVariable Long id, Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Medicament medicament = medicamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médicament non trouvé"));

        model.addAttribute("user", user);
        model.addAttribute("medicament", medicament);

        return "admin/add-medicament";
    }

    /**
     * Supprime un médicament
     */
    @GetMapping("/medicaments/delete/{id}")
    public String deleteMedicament(@PathVariable Long id, Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        medicamentRepository.deleteById(id);
        return "redirect:/admin/medicaments";
    }

    /**
     * Affiche les disponibilités d'un médicament
     */
    @GetMapping("/medicaments/{id}/disponibilites")
    public String medicamentDisponibilites(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Medicament medicament = medicamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médicament non trouvé"));

        List<Disponibilite> disponibilites = disponibiliteRepository.findByMedicament(medicament);
        List<Pharmacie> pharmacies = pharmacieRepository.findAll();

        model.addAttribute("user", user);
        model.addAttribute("medicament", medicament);
        model.addAttribute("disponibilites", disponibilites);
        model.addAttribute("pharmacies", pharmacies);

        return "admin/medicament-disponibilites";
    }

    /**
     * Ajoute une disponibilité pour un médicament
     */
    @PostMapping("/medicaments/{medId}/disponibilites/add")
    public String addDisponibilite(
            @PathVariable Long medId,
            @RequestParam Long pharmacieId,
            @RequestParam Integer quantite,
            @RequestParam Boolean disponible,
            Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Medicament medicament = medicamentRepository.findById(medId).orElse(null);
        Pharmacie pharmacie = pharmacieRepository.findById(pharmacieId).orElse(null);

        if (medicament != null && pharmacie != null) {
            // Vérifier si cette disponibilité existe déjà
            if (disponibiliteRepository.findByPharmacieAndMedicament(pharmacie, medicament).isEmpty()) {
                // Si quantité = 0, forcer le statut à rupture de stock
                boolean isDisponible = quantite > 0 && disponible;
                int finalQuantite = Math.max(quantite, 0);

                Disponibilite dispo = new Disponibilite(null, pharmacie, medicament, isDisponible, finalQuantite);
                disponibiliteRepository.save(dispo);
            }
        }

        return "redirect:/admin/medicaments/" + medId + "/disponibilites";
    }

    /**
     * Affiche le formulaire de modification d'une disponibilité
     */
    @GetMapping("/medicaments/{medId}/disponibilites/{dispoId}/edit-form")
    public String editDisponibiliteForm(
            @PathVariable Long medId,
            @PathVariable Long dispoId,
            Authentication authentication,
            Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Medicament medicament = medicamentRepository.findById(medId).orElse(null);
        Disponibilite disponibilite = disponibiliteRepository.findById(dispoId).orElse(null);

        if (medicament == null || disponibilite == null) {
            return "redirect:/admin/medicaments/" + medId + "/disponibilites";
        }

        model.addAttribute("user", user);
        model.addAttribute("medicament", medicament);
        model.addAttribute("disponibilite", disponibilite);

        return "admin/edit-disponibilite";
    }

    /**
     * Modifie une disponibilité
     */
    @GetMapping("/medicaments/{medId}/disponibilites/{dispoId}/edit")
    public String editDisponibilite(
            @PathVariable Long medId,
            @PathVariable Long dispoId,
            @RequestParam Boolean disponible,
            @RequestParam Integer quantite,
            Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Disponibilite dispo = disponibiliteRepository.findById(dispoId).orElse(null);
        if (dispo != null) {
            // Si quantité = 0, marquer automatiquement comme rupture de stock
            if (quantite <= 0) {
                dispo.setDisponible(false);
                dispo.setQuantiteStock(0);
            } else {
                dispo.setDisponible(disponible);
                dispo.setQuantiteStock(quantite);
            }
            disponibiliteRepository.save(dispo);
        }

        return "redirect:/admin/medicaments/" + medId + "/disponibilites";
    }

    /**
     * Affiche la liste des pharmacies
     */
    @GetMapping("/pharmacies")
    public String pharmacies(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        List<Pharmacie> pharmacies = pharmacieRepository.findAll();
        model.addAttribute("user", user);
        model.addAttribute("pharmacies", pharmacies);

        return "admin/pharmacies";
    }

    /**
     * Affiche le formulaire d'ajout de pharmacie
     */
    @GetMapping("/pharmacies/add")
    public String addPharmacieForm(Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("pharmacie", new Pharmacie());

        return "admin/add-pharmacie";
    }

    /**
     * Sauvegarde une nouvelle pharmacie
     */
    @PostMapping("/pharmacies/save")
    public String savePharmacy(
            @ModelAttribute("pharmacie") Pharmacie pharmacie,
            Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        pharmacieRepository.save(pharmacie);
        return "redirect:/admin/pharmacies";
    }

    /**
     * Affiche le formulaire d'édition d'une pharmacie
     */
    @GetMapping("/pharmacies/edit/{id}")
    public String editPharmacieForm(@PathVariable Long id, Authentication authentication, Model model) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        Pharmacie pharmacie = pharmacieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacie non trouvée"));

        model.addAttribute("user", user);
        model.addAttribute("pharmacie", pharmacie);

        return "admin/add-pharmacie";
    }

    /**
     * Supprime une pharmacie
     */
    @GetMapping("/pharmacies/delete/{id}")
    public String deletePharmacy(@PathVariable Long id, Authentication authentication) {
        AdminUser user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/admin/login";
        }

        pharmacieRepository.deleteById(id);
        return "redirect:/admin/pharmacies";
    }

    /**
     * Récupère l'utilisateur authentifié
     */
    private AdminUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return adminUserRepository.findByUsername(username).orElse(null);
    }
}

