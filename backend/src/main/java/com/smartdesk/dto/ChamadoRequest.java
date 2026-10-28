package com.smartdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
    @NotBlank(message = "Descreva o chamado antes de analisar.")
    @Size(max = 10000, message = "O chamado deve ter no máximo 10.000 caracteres.") String descricao
) {}
