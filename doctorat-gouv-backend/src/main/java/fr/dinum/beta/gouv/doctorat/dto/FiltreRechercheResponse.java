package fr.dinum.beta.gouv.doctorat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltreRechercheResponse {

    private String id;
    private String nom;
    private Object filtres;
    private boolean alerteActive;
    private String frequenceAlerte;
    private LocalDateTime derniereAlerteEnvoyeeLe;
    private LocalDateTime dateCreation;
}
