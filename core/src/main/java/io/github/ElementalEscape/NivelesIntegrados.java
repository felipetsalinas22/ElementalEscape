package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * Campana de 8 niveles cargada desde archivos JSON del editor en
 * core/src/main/resources/niveles. Los niveles editados por el usuario quedan
 * como niveles principales de la historia.
 *
 * Orden: 01 El despertar elemental, 02 Ruedas y cerrojos, 03 Torres enfrentadas,
 * 04 Ascenso entrelazado, 05 Salas del sello, 06 Galerias cruzadas,
 * 07 Nucleo sincronizado, 08 Santuario de los elementos.
 */
public final class NivelesIntegrados {
    private static final Json JSON = new Json();
    private static final String[] ARCHIVOS = {
        "niveles/01 El despertar elemental.json",
        "niveles/02 Ruedas y cerrojos.json",
        "niveles/03 Torres enfrentadas.json",
        "niveles/04 Ascenso entrelazado.json",
        "niveles/05 Salas del sello.json",
        "niveles/06 Galerias cruzadas.json",
        "niveles/07 Nucleo sincronizado.json",
        "niveles/08 Santuario de los elementos.json",
    };

    private NivelesIntegrados() { }

    public static Nivel[] todos() {
        Nivel[] niveles = new Nivel[ARCHIVOS.length];
        for (int i = 0; i < ARCHIVOS.length; i++) niveles[i] = crear(i);
        return niveles;
    }

    public static Nivel crear(int indice) {
        if (indice < 0 || indice >= ARCHIVOS.length) return crear(0);
        FileHandle archivo = Gdx.files.internal(ARCHIVOS[indice]);
        Nivel n = JSON.fromJson(Nivel.class, archivo.readString("UTF-8"));
        if (n == null) throw new RuntimeException("Nivel JSON vacio: " + ARCHIVOS[indice]);
        n.normalizarEditor();
        return n;
    }

    public static String nombre(int indice) {
        return crear(indice).nombre;
    }
}
