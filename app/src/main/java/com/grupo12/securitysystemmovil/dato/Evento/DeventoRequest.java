package com.grupo12.securitysystemmovil.dato.Evento;

public class DeventoRequest {
    private String mensaje;
    private String tipo;
    private String nivel;
    private String fecha;
    private String hora;
    private double latitud;
    private double longitud;
    private int user_id;
    private Integer vehicle_id;
    private Integer trip_id;

    public DeventoRequest(String mensaje, String tipo, String nivel, String fecha, String hora,
                         double latitud, double longitud, int user_id, Integer vehicle_id, Integer trip_id) {
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.nivel = nivel;
        this.fecha = fecha;
        this.hora = hora;
        this.latitud = latitud;
        this.longitud = longitud;
        this.user_id = user_id;
        this.vehicle_id = vehicle_id;
        this.trip_id = trip_id;
    }

}