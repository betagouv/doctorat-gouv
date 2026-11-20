package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltreRechercheRequest {

    @NotBlank
    private String nom;

    @NotNull
    private Object filtres;
}
