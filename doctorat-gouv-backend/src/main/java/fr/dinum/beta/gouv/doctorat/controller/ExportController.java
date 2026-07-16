package fr.dinum.beta.gouv.doctorat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.ExportResponseDTO;
import fr.dinum.beta.gouv.doctorat.service.ExportService;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Endpoint pour exporter les propositions de thèse actives.
     * Exemple d'appel : GET /api/export/propositions-these?page=0&size=100
     */
    @GetMapping("/propositions-these")
    public ResponseEntity<ExportResponseDTO> exportPropositionsThese(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {

        ExportResponseDTO response = exportService.exportPropositionsActives(page, size);
        return ResponseEntity.ok(response);
    }
}
