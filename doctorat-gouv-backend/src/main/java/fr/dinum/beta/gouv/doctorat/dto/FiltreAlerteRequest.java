package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltreAlerteRequest {

    private boolean active;

    private String frequence;
}
