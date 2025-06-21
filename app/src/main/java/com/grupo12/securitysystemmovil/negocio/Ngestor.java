package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;

import com.grupo12.securitysystemmovil.dato.Dgestor;

public class Ngestor {
    private Dgestor dgestor;

    public Ngestor(Context context) {
        this.dgestor = new Dgestor(context);
    }

    public boolean guardarGestor(int id, String ci, String nombre, String apellido, int rol) {
        return dgestor.guardarGestor(id, ci, nombre, apellido, rol);
    }
}
