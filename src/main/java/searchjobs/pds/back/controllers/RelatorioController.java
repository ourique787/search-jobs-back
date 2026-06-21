package searchjobs.pds.back.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.dto.RelatorioDTO;
import searchjobs.pds.back.dto.RelatorioFiltroDTO;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.RelatorioService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorio")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /**
     * GET /api/relatorio
     *
     * Parâmetros opcionais:
     *   dataInicio    (yyyy-MM-dd) — início do período
     *   dataFim       (yyyy-MM-dd) — fim do período
     *   stackIds      (1,2,3)      — IDs das stacks para filtrar
     *   senioridades  (JUNIOR,PLENO) — senioridades para filtrar
     *
     * Exemplos:
     *   /api/relatorio
     *   /api/relatorio?dataInicio=2024-01-01&dataFim=2024-12-31
     *   /api/relatorio?senioridades=JUNIOR,PLENO&stackIds=1,3
     */
    @GetMapping
    public ResponseEntity<RelatorioDTO> gerarRelatorio(
            @AuthenticationPrincipal User usuario,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFim,

            @RequestParam(required = false)
            List<Long> stackIds,

            @RequestParam(required = false)
            List<Senioridade> senioridades
    ) {
        RelatorioFiltroDTO filtro = new RelatorioFiltroDTO(
                dataInicio, dataFim, stackIds, senioridades);

        return ResponseEntity.ok(relatorioService.gerar(usuario, filtro));
    }
}
