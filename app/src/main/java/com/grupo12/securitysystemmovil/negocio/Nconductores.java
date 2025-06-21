package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;

import com.grupo12.securitysystemmovil.dato.Dconductores;

public class Nconductores {
    private final Dconductores dconductores;

    public Nconductores(Context context) {
        this.dconductores = new Dconductores(context);
    }

    public boolean registrarConductor(int id, String ci, String nombre, String apellido, int rol, String rutaImagen, int activo) {
        return dconductores.guardarConductor(id, ci, nombre, apellido, rol, rutaImagen, activo);
    }
}
