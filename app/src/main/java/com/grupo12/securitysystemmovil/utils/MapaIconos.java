package com.grupo12.securitysystemmovil.utils;

import android.graphics.*;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class MapaIconos {

    public static BitmapDescriptor IconoParada(String texto) {
        int tamaño = 80;
        Bitmap bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fondo = new Paint();
        fondo.setColor(Color.parseColor("#0A0A0A")); // color celeste
        fondo.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 3.3f, fondo);

        Paint textoPaint = new Paint();
        textoPaint.setColor(Color.WHITE);
        textoPaint.setTextSize(25f);
        textoPaint.setAntiAlias(true);
        textoPaint.setTextAlign(Paint.Align.CENTER);
        textoPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Rect bounds = new Rect();
        textoPaint.getTextBounds(texto, 0, texto.length(), bounds);
        float y = tamaño / 2f - bounds.exactCenterY();

        canvas.drawText(texto, tamaño / 2f, y, textoPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static BitmapDescriptor IconoOrigen() {
        int tamaño = 80; // Tamaño total del ícono
        Bitmap bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Borde Gris
        Paint borde = new Paint();
        borde.setColor(Color.parseColor("#515151"));
        borde.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 3.8f, borde);

        // Círculo gris (plomo) interior
        Paint fondo = new Paint();
        fondo.setColor(Color.parseColor("#666666")); // gris oscuro
        fondo.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 4f, fondo);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static BitmapDescriptor IconoUbicacionActual() {
        int tamaño = 80;
        Bitmap bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 🔵 Círculo blanco semitransparente
        Paint fondo = new Paint();
        fondo.setColor(Color.WHITE);
        fondo.setAlpha(180); // Transparencia (0-255)
        fondo.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 2.2f, fondo);

        // 🔷 Flecha azul
        Paint paintFlecha = new Paint();
        paintFlecha.setColor(Color.parseColor("#2196F3")); // Azul estilo Google
        paintFlecha.setAntiAlias(true);

        Path path = new Path();
        path.moveTo(tamaño / 2f, tamaño / 5f); // punta
        path.lineTo(tamaño * 4f / 5f, tamaño * 3.5f / 5f); // base derecha
        path.lineTo(tamaño / 2f, tamaño * 4.2f / 5f); // centro base
        path.lineTo(tamaño * 1f / 5f, tamaño * 3.5f / 5f); // base izquierda
        path.close();

        canvas.drawPath(path, paintFlecha);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
