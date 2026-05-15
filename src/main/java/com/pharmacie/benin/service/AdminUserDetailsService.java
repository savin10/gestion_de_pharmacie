package com.pharmacie.benin.service;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service pour charger les détails des utilisateurs administrateurs depuis la base de données.
 *
 * Cette classe implémente UserDetailsService de Spring Security, qui est utilisée lors du processus
 * d'authentification pour charger les informations d'un utilisateur à partir du nom d'utilisateur.
 *
 * Elle convertit les entités AdminUser en objets UserDetails reconnus par Spring Security.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Chercher l'utilisateur dans la base de données
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

        // Vérifier que l'utilisateur est actif
        if (!adminUser.getActive()) {
            throw new UsernameNotFoundException("Compte inactif: " + username);
        }

        // Convertir l'entité AdminUser en UserDetails pour Spring Security
        return User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();
    }
}

