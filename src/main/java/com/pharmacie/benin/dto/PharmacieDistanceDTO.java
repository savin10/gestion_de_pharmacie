package com.pharmacie.benin.dto;

import com.pharmacie.benin.model.Disponibilite;
import com.pharmacie.benin.model.Pharmacie;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PharmacieDistanceDTO {
    private Pharmacie pharmacie;
    private Double distance;
    private Disponibilite disponibilite;
}
