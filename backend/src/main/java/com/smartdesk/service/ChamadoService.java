package com.smartdesk.service;

import com.smartdesk.client.GeminiClient;
import com.smartdesk.dto.ChamadoResponse;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {
    private final GeminiClient gemini;
    public ChamadoService(GeminiClient gemini) { this.gemini = gemini; }
    public ChamadoResponse analisar(String descricao) { return gemini.analisar(descricao.strip()); }
}
