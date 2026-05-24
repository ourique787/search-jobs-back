package searchjobs.pds.back.dto;

import searchjobs.pds.back.entities.Senioridade;

import java.time.LocalDate;
import java.util.List;

public record RelatorioFiltroDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        List<Long> stackIds,
        List<Senioridade> senioridades
) {}
