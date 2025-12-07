package com.lighthouse.dashboard.controller;

import com.lighthouse.dashboard.model.ResumenDiario;
import com.lighthouse.dashboard.model.TimeSeriesData;
import com.lighthouse.dashboard.model.ErrorStat;
import com.lighthouse.dashboard.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardRepository repository;

    // 1. Resumen
    @GetMapping("/resumen")
    public List<ResumenDiario> getResumen(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        System.out.println("📥 API Solicitada: Resumen para fecha " + date);
        return repository.getResumenDiario(date);
    }

    // 2. Tendencia
    @GetMapping("/trend")
    public List<TimeSeriesData> getTrend() {
        System.out.println("📥 API Solicitada: Tendencia 7 días");
        return repository.getTimeSeriesData();
    }

    // 3. Top Errores
    @GetMapping("/errors")
    public List<ErrorStat> getTopErrors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "Todos") String status,
            @RequestParam(defaultValue = "Todos") String entity,
            @RequestParam(defaultValue = "") String search) {
        if (date == null) date = LocalDate.now();
        System.out.println("📥 API Solicitada: Top Errores (" + date + ", " + status + ")");
        return repository.getTopErrors(date, status, entity, search);
    }

    // --- NUEVOS ENDPOINTS (Los que te están fallando) ---

    // 4. Última Actualización
    @GetMapping("/last-update")
    public String getLastUpdate() {
        System.out.println("📥 API Solicitada: Last Update");
        return repository.getLastUpdateDate();
    }

    // 5. Ambiente
    @GetMapping("/environment")
    public String getEnvironment() {
        System.out.println("📥 API Solicitada: Environment");
        return repository.getEnvironmentName();
    }

    // 6. Búsqueda Case Key
    @GetMapping("/search")
    public List<ResumenDiario> searchByCaseKey(
            @RequestParam String caseKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        System.out.println("📥 API Solicitada: Búsqueda CaseKey " + caseKey);
        return repository.findByCaseKey(caseKey, date);
    }

    // 7. Detalle Error (Excel)
    @GetMapping("/errors/detail")
    public List<Map<String, Object>> getErrorDetail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        System.out.println("📥 API Solicitada: Detalle Errores Export");
        return repository.getErrorDetail(date);
    }

    // 8. Detalle Fila (Excel Individual)
    @GetMapping("/row-detail")
    public List<Map<String, Object>> getRowDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String client,
            @RequestParam String entity,
            @RequestParam String status,
            @RequestParam(required = false) String message) {
        System.out.println("📥 API Solicitada: Detalle Fila Individual");
        return repository.getRowDetail(date, client, entity, status, message);
    }

}
