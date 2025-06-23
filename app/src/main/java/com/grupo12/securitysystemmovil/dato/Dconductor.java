package com.grupo12.securitysystemmovil.dato;

public class Dconductor {
    private int id;
    private String nombre;
    private String apellido;
    private String rutaImagen;


    public Dconductor() {
    }

    public Dconductor(int id, String nombre, String apellido, String foto) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rutaImagen = foto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getFoto() {
        return rutaImagen;
    }

    public void setFoto(String foto) {
        this.rutaImagen = foto;
    }

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }
}
