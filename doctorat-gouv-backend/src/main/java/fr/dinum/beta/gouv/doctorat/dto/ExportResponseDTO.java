package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;

public record ExportResponseDTO(
        List<ExportPropositionTheseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
