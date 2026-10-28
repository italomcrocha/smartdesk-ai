package com.smartdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChamadoResponse(
    @NotBlank @Pattern(regexp = "TI|RH|Financeiro|Infraestrutura|Atendimento|Outro") String categoria,
    @NotBlank @Pattern(regexp = "CRITICA|ALTA|MEDIA|BAIXA") String prioridade,
    @NotBlank @Size(max = 2000) String resumo,
    @NotBlank @Size(max = 3000) String acaoRecomendada
) {}
