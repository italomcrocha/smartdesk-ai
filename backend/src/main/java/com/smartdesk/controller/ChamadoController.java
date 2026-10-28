package com.smartdesk.controller;

import com.smartdesk.dto.ChamadoRequest;
import com.smartdesk.dto.ChamadoResponse;
import com.smartdesk.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {
    private final ChamadoService service;
    public ChamadoController(ChamadoService service) { this.service = service; }
    @PostMapping("/analisar") public ChamadoResponse analisar(@Valid @RequestBody ChamadoRequest request) {
        return service.analisar(request.descricao());
    }
}
