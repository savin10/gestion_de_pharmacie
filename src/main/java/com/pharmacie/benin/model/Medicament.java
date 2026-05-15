package com.pharmacie.benin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medicament", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nom", "dosage", "forme"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nom;
    
    @Column(nullable = false)
    private String principeActif;
    
    @Column(nullable = false)
    private String forme;
    
    @Column(nullable = false)
    private String dosage;
    
    private String description;
}
