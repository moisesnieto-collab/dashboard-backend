package com.lighthouse.dashboard.model;

public class ResumenDiario {
    private String cliente;
    private String entidad;
    private String fechaScrapper;
    private String horaInicio;
    private String horaFin;
    private String status;
    private String message;
    private int cantidad;

    // Constructor vacío (necesario para algunas librerías JSON)
    public ResumenDiario() {}

    // Constructor completo
    public ResumenDiario(String cliente, String entidad, String fechaScrapper,
                         String horaInicio, String horaFin, String status,
                         String message, int cantidad) {
        this.cliente = cliente;
        this.entidad = entidad;
        this.fechaScrapper = fechaScrapper;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.status = status;
        this.message = message;
        this.cantidad = cantidad;
    }

    // Getters y Setters (Necesarios para que Spring convierta a JSON automáticamente)
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getFechaScrapper() { return fechaScrapper; }
    public void setFechaScrapper(String fechaScrapper) { this.fechaScrapper = fechaScrapper; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
