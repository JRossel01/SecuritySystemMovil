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

    public static Bitmap IconoUbicacionActual() {
        int tamaño = 70; // tamaño reducido
        Bitmap bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 🔵 Fondo blanco semitransparente
        Paint fondo = new Paint();
        fondo.setColor(Color.WHITE);
        fondo.setAlpha(180);
        fondo.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 2.2f, fondo);

        // 🔵 Círculo azul más pequeño (representa la ubicación)
        Paint circuloAzul = new Paint();
        circuloAzul.setColor(Color.parseColor("#2196F3")); // azul
        circuloAzul.setAntiAlias(true);
        canvas.drawCircle(tamaño / 2f, tamaño / 2f, tamaño / 4f, circuloAzul); // círculo azul centrado

        return bitmap;
    }

}
