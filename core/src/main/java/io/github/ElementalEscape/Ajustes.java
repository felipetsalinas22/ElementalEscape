package io.github.ElementalEscape;

/** Opciones del jugador. Se guardan en ajustes.json. */
public class Ajustes {
    public boolean sonido = true;
    public boolean musica = true;
    public boolean particulas = true;
    public boolean sacudida = true;
    public float volumen = 0.7f;
    public boolean pantallaCompleta = false;

    public void normalizar() {
        if (volumen < 0f) volumen = 0f;
        if (volumen > 1f) volumen = 1f;
        if (Float.isNaN(volumen)) volumen = 0.7f;
    }
}
