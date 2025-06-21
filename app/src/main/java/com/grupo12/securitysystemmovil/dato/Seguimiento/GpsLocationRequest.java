package com.grupo12.securitysystemmovil.dato.Seguimiento;

public class GpsLocationRequest {
    private double latitud;
    private double longitud;
    private float velocidad;
    private int user_id;
    private int vehicle_id;
    private int trip_id;
    private String fecha;
    private String hora;

    public GpsLocationRequest(double latitud, double longitud, float velocidad, int user_id, int vehicle_id, int trip_id, String fecha, String hora) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.velocidad = velocidad;
        this.user_id = user_id;
        this.vehicle_id = vehicle_id;
        this.trip_id = trip_id;
        this.fecha = fecha;
        this.hora = hora;
    }
}
