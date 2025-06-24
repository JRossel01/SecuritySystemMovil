package com.grupo12.securitysystemmovil;

import org.junit.Test;
import static org.junit.Assert.*;
import com.grupo12.securitysystemmovil.negocio.Nvelocidad;

public class NvelocidadTest {

    @Test
    public void testEsFrenoBruscoVerdadero() {
        Nvelocidad n = new Nvelocidad();
        boolean resultado = n.esFrenoBrusco(
                30f, 0.5f, 1000, 2000); // ↓ 29.5 km/h en 1s, casi detenido
        assertTrue(resultado);
    }

    @Test
    public void testFrenoNoEsBruscoPorVelocidadFinalAlta() {
        Nvelocidad n = new Nvelocidad();
        boolean resultado = n.esFrenoBrusco(
                40f, 20f, 1000, 2000); // ↓ 20 km/h en 1s, pero no se detuvo
        assertFalse(resultado);
    }

    @Test
    public void testFrenoNoEsBruscoPorTiempoLargo() {
        Nvelocidad n = new Nvelocidad();
        boolean resultado = n.esFrenoBrusco(
                40f, 0.5f, 1000, 4000); // ↓ se detuvo, pero en 3s
        assertFalse(resultado);
    }

    @Test
    public void testCambioPequenoNoEsBrusco() {
        Nvelocidad n = new Nvelocidad();
        boolean resultado = n.esFrenoBrusco(
                30f, 25f, 1000, 1500); // ↓ solo 5 km/h
        assertFalse(resultado);
    }

    @Test
    public void testFrenoBruscoConLimiteExacto() {
        Nvelocidad n = new Nvelocidad();
        boolean resultado = n.esFrenoBrusco(
                16f, 1f, 1000, 2000); // ↓ justo en el umbral
        assertFalse(resultado);
    }

}
