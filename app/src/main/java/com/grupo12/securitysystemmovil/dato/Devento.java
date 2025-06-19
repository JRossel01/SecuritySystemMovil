package com.grupo12.securitysystemmovil.dato;

public class Devento {
    private int id;
    private String mensaje;
    private String fecha;
    private String hora;
    private double latitud;
    private double longitud;

    public Devento(int id, String mensaje, String fecha, String hora, double latitud, double longitud) {
        this.id = id;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.hora = hora;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Devento(String mensaje, String fecha, String hora, double latitud, double longitud) {
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.hora = hora;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public int getId() {
        return id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
}

