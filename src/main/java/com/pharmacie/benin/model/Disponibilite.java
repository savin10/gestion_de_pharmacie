package com.pharmacie.benin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "disponibilite", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pharmacie_id", "medicament_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disponibilite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pharmacie_id")
    private Pharmacie pharmacie;

    @ManyToOne
    @JoinColumn(name = "medicament_id")
    private Medicament medicament;

    private Boolean disponible;
    private Integer quantiteStock;
}
