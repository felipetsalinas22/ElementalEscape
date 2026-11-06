package io.github.ElementalEscape;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Dibuja el mundo, los personajes y el HUD con SpriteBatch.
 *
 * Toda la direccion artistica esta generada por codigo: fondos por capas,
 * parallax, siluetas, particulas ambientales y personajes humanoides con
 * animaciones. El dibujo nunca modifica las dimensiones de colision.
 */
public class DibujanteJuego {
    private final SpriteBatch lote;
    private final BitmapFont fuente;

    private static final Color SOMBRA = new Color(0f, 0f, 0f, 0.30f);
    private static final Color PIEDRA = new Color(0.29f, 0.33f, 0.42f, 1f);
    private static final Color PIEDRA_CLARA = new Color(0.49f, 0.56f, 0.69f, 1f);
    private static final Color MADERA = new Color(0.43f, 0.26f, 0.14f, 1f);
    private static final Color MADERA_CLARA = new Color(0.72f, 0.49f, 0.25f, 1f);
    private static final Color METAL = new Color(0.26f, 0.33f, 0.42f, 1f);
    private static final Color METAL_CLARA = new Color(0.58f, 0.73f, 0.84f, 1f);
    private static final Color MURO = new Color(0.17f, 0.19f, 0.27f, 1f);
    private static final Color MURO_CLARO = new Color(0.34f, 0.39f, 0.51f, 1f);

    /** Paleta de fondo por tema. Mantiene una direccion artistica comun. */
    private static final class Paleta {
        Color arriba;
        Color abajo;
        Color lejos;
        Color medio;
        Color cerca;
        Color acento;
    }

    public DibujanteJuego(SpriteBatch lote, BitmapFont fuente) {
        this.lote = lote;
        this.fuente = fuente;
    }

    // =========================================================
    //                       FONDOS
    // =========================================================

    public void fondo(String tema, float t, float x, float y, float ancho, float alto) {
        Paleta p = paleta(tema);
        float parallaxLejos = positivo(x * 0.08f, ancho);
        float parallaxMedio = positivo(x * 0.18f, ancho);
        float parallaxCerca = positivo(x * 0.32f, ancho);
        float olas = -y * 0.045f;

        Dibujo.rectangulo(lote, x, y, ancho, alto, p.arriba);
        Dibujo.degradado(lote, x, y, ancho, alto * 0.94f, p.abajo);
        Dibujo.brillo(lote, x + ancho * 0.50f, y + alto * 0.30f + olas,
            ancho * 0.95f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.10f));

        if ("VOLCAN".equals(tema)) fondoVolcan(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("CAVERNA".equals(tema)) fondoCaverna(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("BOSQUE".equals(tema)) fondoBosque(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("LABORATORIO".equals(tema)) fondoLaboratorio(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("HIELO".equals(tema)) fondoHielo(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("CIELO".equals(tema)) fondoCielo(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else if ("COSMOS".equals(tema) || "FINAL".equals(tema)) fondoCosmos(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, olas);
        else fondoTemplo(t, x, y, ancho, alto, p, parallaxLejos, parallaxMedio, parallaxCerca, olas);

        // Polvo ambiental muy tenue: da profundidad sin competir con el juego.
        for (int i = 0; i < 34; i++) {
            float px = x + positivo(i * 137f + t * (4f + i % 5), ancho + 120f) - 60f;
            float py = y + 40f + ((i * 71f + t * (3f + i % 3)) % Math.max(1f, alto - 80f));
            float a = 0.05f + 0.05f * (0.5f + 0.5f * (float) Math.sin(t * 1.3f + i));
            Dibujo.circulo(lote, px, py, 2f + (i % 3), new Color(p.acento.r, p.acento.g, p.acento.b, a));
        }

        // Vineta y linea de horizonte: enmarcan la accion y mejoran contraste.
        Dibujo.degradado(lote, x, y + alto * 0.78f, ancho, alto * 0.22f, new Color(0f, 0f, 0f, 0.40f));
        Dibujo.rectangulo(lote, x, y + 4f, ancho, 1f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.16f));
        Dibujo.rectangulo(lote, x, y, ancho, 10f, new Color(0f, 0f, 0f, 0.24f));
    }

    private Paleta paleta(String tema) {
        Paleta p = new Paleta();
        p.arriba = new Color(0.035f, 0.045f, 0.085f, 1f);
        p.abajo = new Color(0.08f, 0.10f, 0.17f, 1f);
        p.lejos = new Color(0.10f, 0.14f, 0.24f, 1f);
        p.medio = new Color(0.16f, 0.22f, 0.34f, 1f);
        p.cerca = new Color(0.24f, 0.31f, 0.44f, 1f);
        p.acento = new Color(0.55f, 0.78f, 1f, 1f);

        if ("CAVERNA".equals(tema)) {
            p.arriba.set(0.025f, 0.055f, 0.075f, 1f);
            p.abajo.set(0.08f, 0.17f, 0.19f, 1f);
            p.lejos.set(0.06f, 0.16f, 0.18f, 1f);
            p.medio.set(0.10f, 0.26f, 0.27f, 1f);
            p.cerca.set(0.16f, 0.37f, 0.36f, 1f);
            p.acento.set(0.30f, 1f, 0.82f, 1f);
        } else if ("CIELO".equals(tema)) {
            p.arriba.set(0.06f, 0.09f, 0.23f, 1f);
            p.abajo.set(0.32f, 0.47f, 0.66f, 1f);
            p.lejos.set(0.16f, 0.27f, 0.47f, 1f);
            p.medio.set(0.35f, 0.52f, 0.70f, 1f);
            p.cerca.set(0.55f, 0.72f, 0.86f, 1f);
            p.acento.set(0.75f, 0.90f, 1f, 1f);
        } else if ("HIELO".equals(tema)) {
            p.arriba.set(0.035f, 0.09f, 0.17f, 1f);
            p.abajo.set(0.16f, 0.34f, 0.47f, 1f);
            p.lejos.set(0.12f, 0.27f, 0.40f, 1f);
            p.medio.set(0.24f, 0.47f, 0.62f, 1f);
            p.cerca.set(0.48f, 0.70f, 0.82f, 1f);
            p.acento.set(0.65f, 0.95f, 1f, 1f);
        } else if ("VOLCAN".equals(tema)) {
            p.arriba.set(0.11f, 0.035f, 0.03f, 1f);
            p.abajo.set(0.34f, 0.10f, 0.05f, 1f);
            p.lejos.set(0.27f, 0.09f, 0.06f, 1f);
            p.medio.set(0.45f, 0.16f, 0.07f, 1f);
            p.cerca.set(0.62f, 0.26f, 0.10f, 1f);
            p.acento.set(1f, 0.52f, 0.12f, 1f);
        } else if ("RUINAS".equals(tema)) {
            p.arriba.set(0.075f, 0.075f, 0.06f, 1f);
            p.abajo.set(0.24f, 0.21f, 0.14f, 1f);
            p.lejos.set(0.17f, 0.16f, 0.12f, 1f);
            p.medio.set(0.28f, 0.26f, 0.18f, 1f);
            p.cerca.set(0.42f, 0.37f, 0.25f, 1f);
            p.acento.set(0.96f, 0.80f, 0.42f, 1f);
        } else if ("LABORATORIO".equals(tema)) {
            p.arriba.set(0.025f, 0.045f, 0.095f, 1f);
            p.abajo.set(0.07f, 0.16f, 0.25f, 1f);
            p.lejos.set(0.08f, 0.18f, 0.27f, 1f);
            p.medio.set(0.11f, 0.28f, 0.37f, 1f);
            p.cerca.set(0.17f, 0.42f, 0.50f, 1f);
            p.acento.set(0.28f, 0.90f, 0.95f, 1f);
        } else if ("BOSQUE".equals(tema)) {
            p.arriba.set(0.025f, 0.07f, 0.055f, 1f);
            p.abajo.set(0.08f, 0.22f, 0.13f, 1f);
            p.lejos.set(0.06f, 0.18f, 0.12f, 1f);
            p.medio.set(0.10f, 0.30f, 0.17f, 1f);
            p.cerca.set(0.16f, 0.42f, 0.23f, 1f);
            p.acento.set(0.50f, 1f, 0.58f, 1f);
        } else if ("COSMOS".equals(tema) || "FINAL".equals(tema)) {
            p.arriba.set(0.035f, 0.025f, 0.10f, 1f);
            p.abajo.set(0.13f, 0.08f, 0.29f, 1f);
            p.lejos.set(0.11f, 0.09f, 0.25f, 1f);
            p.medio.set(0.22f, 0.15f, 0.44f, 1f);
            p.cerca.set(0.38f, 0.26f, 0.62f, 1f);
            p.acento.set(0.80f, 0.62f, 1f, 1f);
        } else {
            // Templo: piedra calida, incienso y luz dorada.
            p.arriba.set(0.06f, 0.08f, 0.12f, 1f);
            p.abajo.set(0.22f, 0.19f, 0.15f, 1f);
            p.lejos.set(0.16f, 0.16f, 0.17f, 1f);
            p.medio.set(0.27f, 0.25f, 0.24f, 1f);
            p.cerca.set(0.43f, 0.38f, 0.31f, 1f);
            p.acento.set(1f, 0.78f, 0.36f, 1f);
        }
        return p;
    }

    private void fondoTemplo(float t, float x, float y, float ancho, float alto, Paleta p,
                             float lejos, float medio, float cerca, float olas) {
        // Arcos lejanos y columnas partidas.
        for (int i = 0; i < 7; i++) {
            float cx = x + positivo(i * 185f - lejos, ancho + 220f) - 110f;
            float base = y + 78f + olas;
            float altura = 230f + (i % 3) * 48f;
            Dibujo.rectangulo(lote, cx - 5f, base, 10f, altura, new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.56f));
            Dibujo.circulo(lote, cx - 58f, base + altura - 46f, 116f,
                new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.42f));
            Dibujo.rectangulo(lote, cx - 44f, base + altura - 40f, 88f, 4f,
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.10f));
        }
        for (int i = 0; i < 6; i++) {
            float cx = x + positivo(i * 230f - medio, ancho + 320f) - 160f;
            float base = y + 32f + olas;
            float h = 185f + (i % 2) * 65f;
            Dibujo.rectangulo(lote, cx - 16f, base, 32f, h, new Color(p.medio.r, p.medio.g, p.medio.b, 0.72f));
            Dibujo.rectangulo(lote, cx - 23f, base + h - 10f, 46f, 10f, new Color(p.cerca.r, p.cerca.g, p.cerca.b, 0.72f));
            Dibujo.rectangulo(lote, cx - 23f, base, 46f, 10f, new Color(p.cerca.r, p.cerca.g, p.cerca.b, 0.72f));
            for (int j = 0; j < 4; j++) {
                Dibujo.rectangulo(lote, cx - 15f, base + 30f + j * 40f, 30f, 2f,
                    new Color(0f, 0f, 0f, 0.18f));
            }
            // Antorchas y sus halos.
            float pulso = 0.5f + 0.5f * (float) Math.sin(t * 2.4f + i);
            Dibujo.brillo(lote, cx, base + h * 0.62f, 58f + pulso * 10f,
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.12f));
            Dibujo.circulo(lote, cx - 4f, base + h * 0.62f - 8f, 8f + pulso * 3f,
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.82f));
        }
        // Enredaderas colgantes y runas tenues.
        for (int i = 0; i < 9; i++) {
            float cx = x + positivo(i * 127f - cerca, ancho + 160f) - 80f;
            float h = 35f + (i % 4) * 22f;
            Dibujo.rectangulo(lote, cx, y + alto - 54f, 2f, h, new Color(0.18f, 0.42f, 0.24f, 0.24f));
            Dibujo.circulo(lote, cx - 4f, y + alto - 56f - h, 10f, new Color(0.24f, 0.55f, 0.28f, 0.20f));
        }
    }

    private void fondoVolcan(float t, float x, float y, float ancho, float alto, Paleta p,
                             float lejos, float medio, float olas) {
        // Montanas y cono volcanico geometrico.
        for (int i = 0; i < 6; i++) {
            float cx = x + positivo(i * 225f - lejos, ancho + 280f) - 140f;
            float base = y + 60f + olas;
            float lado = 220f + (i % 3) * 75f;
            Dibujo.rectanguloRotado(lote, cx - lado / 2f, base - lado / 2f, lado, lado,
                lado / 2f, lado / 2f, 45f, new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.78f));
        }
        float cx = x + ancho * 0.58f - medio * 0.30f;
        Dibujo.rectanguloRotado(lote, cx - 245f, y + 25f - 120f, 510f, 510f,
            255f, 255f, 45f, new Color(p.medio.r, p.medio.g, p.medio.b, 0.95f));
        Dibujo.rectanguloRotado(lote, cx - 110f, y + 106f, 220f, 220f,
            110f, 110f, 45f, new Color(0.10f, 0.035f, 0.025f, 0.95f));
        // Rio de lava en el fondo.
        for (int i = 0; i < 8; i++) {
            float rx = x + positivo(i * 150f - medio * 0.6f, ancho + 200f) - 100f;
            float ry = y + 28f + (i % 3) * 12f + (float) Math.sin(t * 2f + i) * 3f;
            Dibujo.rectangulo(lote, rx, ry, 130f, 8f, new Color(1f, 0.25f, 0.04f, 0.42f));
            Dibujo.rectangulo(lote, rx + 14f, ry + 2f, 74f, 3f, new Color(1f, 0.72f, 0.20f, 0.52f));
            Dibujo.brillo(lote, rx + 65f, ry + 4f, 120f, new Color(1f, 0.25f, 0.04f, 0.12f));
        }
        // Humo y brasas.
        for (int i = 0; i < 24; i++) {
            float ex = x + positivo(i * 61f + t * (8f + i % 7), ancho + 80f) - 40f;
            float ey = y + 30f + ((i * 53f + t * (12f + i % 5)) % (alto - 70f));
            float a = 0.16f + 0.20f * (0.5f + 0.5f * (float) Math.sin(t * 4f + i));
            Dibujo.circulo(lote, ex, ey, 2f + (i % 4), new Color(1f, 0.46f + (i % 3) * 0.12f, 0.12f, a));
        }
        for (int i = 0; i < 12; i++) {
            float hx = x + ancho * 0.55f + (float) Math.sin(t * 0.4f + i) * 90f - 30f;
            float hy = y + alto * 0.54f + i * 18f + (float) Math.sin(t * 0.9f + i) * 8f;
            Dibujo.circulo(lote, hx, hy, 34f + (i % 3) * 14f, new Color(0.22f, 0.12f, 0.11f, 0.09f));
        }
    }

    private void fondoCaverna(float t, float x, float y, float ancho, float alto, Paleta p,
                              float lejos, float medio, float olas) {
        // Techo irregular y estalactitas.
        Dibujo.rectangulo(lote, x, y + alto - 52f, ancho, 52f, new Color(0.015f, 0.025f, 0.035f, 0.75f));
        for (int i = 0; i < 18; i++) {
            float cx = x + positivo(i * 73f - lejos, ancho + 100f) - 30f;
            float h = 20f + (i % 5) * 14f;
            Dibujo.rectanguloRotado(lote, cx - 10f, y + alto - h, 20f, h, 10f, 0f, 45f,
                new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.80f));
        }
        // Paredes laterales y rocas lejanas.
        for (int i = 0; i < 7; i++) {
            float cx = x + positivo(i * 190f - medio, ancho + 250f) - 120f;
            Dibujo.circulo(lote, cx, y + 45f + olas, 260f + (i % 3) * 45f,
                new Color(p.medio.r, p.medio.g, p.medio.b, 0.30f));
        }
        // Cristales luminosos por grupos.
        for (int i = 0; i < 13; i++) {
            float cx = x + positivo(i * 101f - lejos * 0.7f, ancho + 80f) - 40f;
            float baseY = y + 28f + (i % 4) * 34f;
            float tam = 12f + (i % 4) * 5f;
            Color c = (i % 3 == 0) ? new Color(0.45f, 0.80f, 1f, 0.85f)
                : (i % 3 == 1) ? new Color(0.30f, 1f, 0.78f, 0.85f)
                : new Color(0.76f, 0.55f, 1f, 0.82f);
            Dibujo.brillo(lote, cx, baseY + tam, 32f, new Color(c.r, c.g, c.b, 0.15f));
            Dibujo.rectanguloRotado(lote, cx - tam / 2f, baseY, tam, tam * 2.4f,
                tam / 2f, 0f, (i % 2 == 0) ? 8f : -9f, c);
            Dibujo.rectanguloRotado(lote, cx - 2f, baseY + tam * 0.3f, 4f, tam * 1.4f,
                2f, 0f, (i % 2 == 0) ? 8f : -9f, new Color(1f, 1f, 1f, 0.35f));
        }
        // Goteo y niebla baja.
        for (int i = 0; i < 22; i++) {
            float cx = x + positivo(i * 89f + t * (5f + i % 4), ancho + 100f) - 50f;
            float cy = y + ((i * 47f + t * 18f) % (alto - 80f));
            Dibujo.circulo(lote, cx, cy, 2f + (i % 2), new Color(0.45f, 0.88f, 1f, 0.24f));
        }
        Dibujo.degradado(lote, x, y + 20f, ancho, 90f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.10f));
    }

    private void fondoBosque(float t, float x, float y, float ancho, float alto, Paleta p,
                             float lejos, float medio, float olas) {
        // Rayos de luz.
        for (int i = 0; i < 7; i++) {
            float cx = x + 80f + i * 150f - medio * 0.25f;
            Dibujo.rectanguloRotado(lote, cx, y + 130f + olas, 34f, alto * 0.90f,
                17f, 0f, 14f, new Color(0.75f, 1f, 0.62f, 0.035f));
        }
        // Troncos y copas en tres capas.
        for (int capa = 0; capa < 3; capa++) {
            Color color = capa == 0 ? p.lejos : (capa == 1 ? p.medio : p.cerca);
            float desplazamiento = capa == 0 ? lejos : (capa == 1 ? medio : lejos * 1.7f);
            int cantidad = capa == 0 ? 8 : (capa == 1 ? 7 : 5);
            for (int i = 0; i < cantidad; i++) {
                float cx = x + positivo(i * (170f + capa * 24f) - desplazamiento, ancho + 220f) - 110f;
                float tronco = 18f + capa * 7f;
                float altura = 120f + (i % 4) * 38f + capa * 38f;
                Dibujo.rectangulo(lote, cx - tronco / 2f, y + 20f + olas, tronco, altura,
                    new Color(color.r * 0.55f, color.g * 0.55f, color.b * 0.55f, 0.70f));
                Dibujo.circulo(lote, cx - 54f, y + 68f + altura, 112f + capa * 14f,
                    new Color(color.r, color.g, color.b, 0.22f + capa * 0.10f));
                Dibujo.circulo(lote, cx + 20f, y + 42f + altura, 82f + capa * 10f,
                    new Color(color.r * 0.80f, color.g, color.b * 0.80f, 0.20f + capa * 0.08f));
            }
        }
        // Luciernagas.
        for (int i = 0; i < 26; i++) {
            float cx = x + positivo(i * 97f + (float) Math.sin(t * 0.6f + i) * 28f, ancho);
            float cy = y + 90f + ((i * 47f) % 270f) + (float) Math.sin(t * 1.4f + i) * 12f;
            float pulso = 0.5f + 0.5f * (float) Math.sin(t * 3f + i * 1.7f);
            Dibujo.brillo(lote, cx, cy, 15f, new Color(0.88f, 1f, 0.50f, 0.30f * pulso));
            Dibujo.circulo(lote, cx, cy, 3f, new Color(0.92f, 1f, 0.63f, 0.65f * pulso));
        }
    }

    private void fondoLaboratorio(float t, float x, float y, float ancho, float alto, Paleta p,
                                  float lejos, float medio, float olas) {
        // Rejilla de pared y paneles.
        for (int i = 0; i < 14; i++) {
            float cx = x + i * 80f - medio * 0.10f;
            Dibujo.rectangulo(lote, cx, y + 40f, 1f, alto - 80f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.06f));
        }
        for (int i = 0; i < 7; i++) {
            float cy = y + 60f + i * 70f;
            Dibujo.rectangulo(lote, x, cy, ancho, 1f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.06f));
        }
        // Tuberias y maquinas.
        for (int i = 0; i < 8; i++) {
            float cx = x + positivo(i * 155f - lejos, ancho + 180f) - 90f;
            float anchoTubo = 7f + (i % 3) * 4f;
            Dibujo.rectangulo(lote, cx - anchoTubo / 2f, y + 44f, anchoTubo, 250f + (i % 4) * 38f,
                new Color(p.medio.r, p.medio.g, p.medio.b, 0.56f));
            Dibujo.rectangulo(lote, cx - 20f, y + 250f + (i % 4) * 38f, 40f, 9f,
                new Color(p.cerca.r, p.cerca.g, p.cerca.b, 0.55f));
            for (int j = 0; j < 3; j++) {
                Dibujo.circulo(lote, cx - 5f, y + 85f + j * 62f, 10f,
                    new Color(p.acento.r, p.acento.g, p.acento.b, 0.30f));
            }
        }
        // Pantallas con datos falsos, pero con lectura visual clara.
        for (int i = 0; i < 4; i++) {
            float sx = x + 102f + i * 238f - medio * 0.15f;
            float sy = y + 292f + (i % 2) * 54f + olas;
            Dibujo.panel(lote, sx, sy, 112f, 62f, new Color(0.04f, 0.09f, 0.12f, 0.78f),
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.35f));
            for (int j = 0; j < 4; j++) {
                float w = 26f + ((i + j) * 17 % 54);
                Dibujo.rectangulo(lote, sx + 10f, sy + 12f + j * 10f, w, 3f,
                    new Color(p.acento.r, p.acento.g, p.acento.b, 0.22f + j * 0.08f));
            }
        }
        // Cables y chispas.
        for (int i = 0; i < 10; i++) {
            float cx = x + 50f + i * 96f - lejos * 0.2f;
            Dibujo.linea(lote, cx, y + 44f, cx + 28f, y + 170f + (i % 3) * 20f, 2f,
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.12f));
        }
        for (int i = 0; i < 14; i++) {
            float cx = x + positivo(i * 131f + t * 9f, ancho);
            float cy = y + 400f - ((t * (15f + i % 4) + i * 37f) % 220f);
            Dibujo.circulo(lote, cx, cy, 2f, new Color(0.55f, 0.95f, 1f, 0.30f));
        }
    }

    private void fondoHielo(float t, float x, float y, float ancho, float alto, Paleta p,
                            float lejos, float medio, float olas) {
        // Aurora.
        for (int i = 0; i < 4; i++) {
            float cy = y + 310f + i * 36f + olas;
            Dibujo.elipse(lote, x + 40f - medio * 0.10f, cy, ancho * 0.80f, 34f + i * 18f,
                new Color(p.acento.r, p.acento.g, p.acento.b, 0.055f + i * 0.018f));
        }
        // Montanas de hielo estratificadas.
        for (int i = 0; i < 9; i++) {
            float cx = x + positivo(i * 155f - lejos, ancho + 220f) - 100f;
            float lado = 160f + (i % 4) * 48f;
            Dibujo.rectanguloRotado(lote, cx - lado / 2f, y + 40f - lado / 2f, lado, lado,
                lado / 2f, lado / 2f, 45f, new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.58f));
        }
        for (int i = 0; i < 7; i++) {
            float cx = x + positivo(i * 205f - medio, ancho + 260f) - 120f;
            float lado = 110f + (i % 3) * 45f;
            Dibujo.rectanguloRotado(lote, cx - lado / 2f, y + 26f - lado / 2f, lado, lado,
                lado / 2f, lado / 2f, 45f, new Color(p.medio.r, p.medio.g, p.medio.b, 0.50f));
        }
        // Cristales y copos.
        for (int i = 0; i < 12; i++) {
            float cx = x + positivo(i * 111f - medio * 0.4f, ancho + 80f) - 40f;
            float cy = y + 54f + (i % 5) * 44f;
            Dibujo.rectanguloRotado(lote, cx - 5f, cy, 10f, 28f, 5f, 0f, (i % 2 == 0) ? 22f : -18f,
                new Color(0.60f, 0.90f, 1f, 0.25f));
        }
        for (int i = 0; i < 34; i++) {
            float cx = x + positivo(i * 83f + t * (5f + i % 4), ancho + 60f) - 30f;
            float cy = y + ((i * 67f + t * 11f) % alto);
            Dibujo.circulo(lote, cx, cy, 1.5f + (i % 3), new Color(0.85f, 0.96f, 1f, 0.30f));
        }
    }

    private void fondoCielo(float t, float x, float y, float ancho, float alto, Paleta p,
                            float lejos, float medio, float olas) {
        // Sol y nubes en capas.
        Dibujo.brillo(lote, x + 760f - lejos * 0.10f, y + 390f + olas, 260f,
            new Color(1f, 0.92f, 0.62f, 0.14f));
        for (int capa = 0; capa < 3; capa++) {
            float desplazamiento = capa == 0 ? lejos : (capa == 1 ? medio : medio * 1.7f);
            int nubes = capa == 0 ? 6 : (capa == 1 ? 5 : 4);
            for (int i = 0; i < nubes; i++) {
                float cx = x + positivo(i * 205f - desplazamiento, ancho + 300f) - 150f;
                float cy = y + 150f + capa * 56f + (i % 3) * 34f + olas * (capa + 1);
                float anchoNube = 120f + (i % 3) * 46f;
                Color c = capa == 0 ? new Color(1f, 1f, 1f, 0.10f)
                    : (capa == 1 ? new Color(0.86f, 0.93f, 1f, 0.16f)
                    : new Color(0.95f, 0.98f, 1f, 0.24f));
                Dibujo.elipse(lote, cx, cy, anchoNube, 28f + capa * 8f, c);
                Dibujo.circulo(lote, cx + anchoNube * 0.24f, cy + 6f, 34f + capa * 7f, c);
            }
        }
        // Islas flotantes lejanas.
        for (int i = 0; i < 3; i++) {
            float cx = x + 120f + i * 330f - medio * 0.18f;
            float cy = y + 170f + (i % 2) * 90f + olas;
            Dibujo.elipse(lote, cx, cy, 130f, 20f, new Color(p.medio.r, p.medio.g, p.medio.b, 0.26f));
            Dibujo.rectanguloRotado(lote, cx + 20f, cy - 60f, 70f, 78f, 35f, 0f, 45f,
                new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.30f));
            Dibujo.circulo(lote, cx + 20f, cy + 8f, 9f, new Color(0.25f, 0.55f, 0.25f, 0.42f));
        }
        // Aves.
        for (int i = 0; i < 6; i++) {
            float cx = x + positivo(i * 167f + t * 10f, ancho + 100f) - 50f;
            float cy = y + 360f + (i % 3) * 36f + (float) Math.sin(t * 1.5f + i) * 7f;
            Dibujo.linea(lote, cx - 8f, cy, cx, cy + 5f, 1.2f, new Color(0.20f, 0.30f, 0.45f, 0.42f));
            Dibujo.linea(lote, cx, cy + 5f, cx + 8f, cy, 1.2f, new Color(0.20f, 0.30f, 0.45f, 0.42f));
        }
    }

    private void fondoCosmos(float t, float x, float y, float ancho, float alto, Paleta p,
                             float lejos, float medio, float olas) {
        // Nebulosas.
        for (int i = 0; i < 5; i++) {
            float cx = x + 130f + i * 210f - lejos * 0.15f;
            float cy = y + 190f + (i % 3) * 82f + olas;
            Dibujo.elipse(lote, cx, cy, 250f + (i % 2) * 120f, 80f + (i % 3) * 24f,
                new Color(p.medio.r, p.medio.g, p.medio.b, 0.075f));
        }
        // Planeta anillado.
        float px = x + 760f - medio * 0.10f;
        float py = y + 370f + olas;
        Dibujo.brillo(lote, px, py, 230f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.11f));
        Dibujo.circulo(lote, px - 55f, py - 55f, 110f, new Color(p.medio.r, p.medio.g, p.medio.b, 0.52f));
        Dibujo.anillo(lote, px, py, 190f, new Color(p.acento.r, p.acento.g, p.acento.b, 0.32f));
        Dibujo.rectanguloRotado(lote, px - 115f, py - 12f, 230f, 7f, 115f, 3.5f, -16f,
            new Color(p.acento.r, p.acento.g, p.acento.b, 0.24f));
        // Estrellas y constelaciones.
        for (int i = 0; i < 70; i++) {
            float sx = x + ((i * 137f + 23f) % Math.max(1f, ancho));
            float sy = y + ((i * 83f + 41f) % Math.max(1f, alto));
            float pulso = 0.30f + 0.70f * (0.5f + 0.5f * (float) Math.sin(t * 1.8f + i));
            float tam = 1.5f + (i % 3) * 1.2f;
            Dibujo.circulo(lote, sx, sy, tam, new Color(0.85f, 0.90f, 1f, 0.24f * pulso));
            if (i % 11 == 0) {
                Dibujo.linea(lote, sx - 4f, sy, sx + 4f, sy, 0.8f,
                    new Color(1f, 1f, 1f, 0.26f * pulso));
                Dibujo.linea(lote, sx, sy - 4f, sx, sy + 4f, 0.8f,
                    new Color(1f, 1f, 1f, 0.26f * pulso));
            }
        }
        // Obeliscos flotantes del templo final.
        for (int i = 0; i < 3; i++) {
            float cx = x + 90f + i * 320f - lejos * 0.2f;
            Dibujo.rectanguloRotado(lote, cx - 16f, y + 50f, 32f, 170f + i * 28f, 16f, 0f, 45f,
                new Color(p.lejos.r, p.lejos.g, p.lejos.b, 0.42f));
        }
    }

    // =========================================================
    //                       MUNDO
    // =========================================================

    public void mundo(Partida p, float t, boolean esCliente) {
        for (Nivel.Plataforma plataforma : p.nivel.plataformas) {
            dibujarPlataforma(plataforma.rect, plataforma.estilo);
        }
        for (Nivel.Peligro peligro : p.nivel.peligros) {
            dibujarPeligro(peligro.rect, peligro.lava, t);
        }
        for (int i = 0; i < p.nivel.puertas.length; i++) {
            Nivel.Puerta puerta = p.nivel.puertas[i];
            dibujarPuerta(puerta.rect, p.puertasAbiertas[i], puerta.puente, t);
        }
        for (Partida.MovilEstado m : p.moviles) {
            dibujarMovil(m.rect, t);
        }
        for (Nivel.Boton b : p.nivel.botones) {
            boolean activo = false;
            for (int i = 0; i < p.nivel.botones.length; i++) {
                if (p.nivel.botones[i] == b) { activo = p.botonesActivos[i]; break; }
            }
            dibujarBoton(b.rect, b.elemento, activo, t);
        }
        for (int i = 0; i < p.nivel.palancas.length; i++) {
            dibujarPalanca(p.nivel.palancas[i].rect, p.nivel.palancas[i].elemento,
                p.palancasActivas[i], t);
        }
        for (Partida.CajaEstado c : p.cajas) {
            dibujarCaja(c.rect, c.datos.pesada);
        }
        for (int i = 0; i < p.nivel.gemas.length; i++) {
            if (!p.gemasTomadas[i]) dibujarGema(p.nivel.gemas[i].rect, p.nivel.gemas[i].secreta, t);
        }
        dibujarSalida(p.nivel.salidaFuego, true, p.tiempoSalida > 0f, t);
        dibujarSalida(p.nivel.salidaAgua, false, p.tiempoSalida > 0f, t);

        if (p.fuego.vivo) dibujarJugador(p.fuego, t);
        if (p.agua.vivo) dibujarJugador(p.agua, t);
        p.particulas.dibujar(lote);
    }

    public void dibujarPlataforma(Rectangle r, String estilo) {
        Color cuerpo = PIEDRA;
        Color luz = PIEDRA_CLARA;
        if ("MADERA".equals(estilo)) { cuerpo = MADERA; luz = MADERA_CLARA; }
        else if ("METAL".equals(estilo)) { cuerpo = METAL; luz = METAL_CLARA; }
        else if ("MURO".equals(estilo)) { cuerpo = MURO; luz = MURO_CLARO; }

        // Sombra, cuerpo y remate superior: lectura clara de superficie pisable.
        Dibujo.rectangulo(lote, r.x + 4f, r.y - 5f, r.width, r.height, new Color(0f, 0f, 0f, 0.26f));
        Dibujo.rectangulo(lote, r.x, r.y, r.width, r.height, cuerpo);
        Dibujo.rectangulo(lote, r.x, r.y + r.height - Math.min(6f, r.height * 0.32f),
            r.width, Math.min(6f, r.height * 0.32f), luz);
        Dibujo.rectangulo(lote, r.x, r.y + r.height - 1f, r.width, 1f,
            new Color(1f, 1f, 1f, 0.24f));
        Dibujo.rectangulo(lote, r.x, r.y, Math.min(4f, r.width * 0.18f), r.height,
            new Color(1f, 1f, 1f, 0.10f));
        Dibujo.rectangulo(lote, r.x + r.width - Math.min(3f, r.width * 0.15f), r.y,
            Math.min(3f, r.width * 0.15f), r.height, new Color(0f, 0f, 0f, 0.16f));

        // Motivos de material.
        if (r.width > 34f && r.height > 8f) {
            if ("MADERA".equals(estilo)) {
                for (float x = r.x + 18f; x < r.x + r.width - 8f; x += 34f) {
                    Dibujo.rectangulo(lote, x, r.y + 2f, 2f, r.height - 4f,
                        new Color(0.25f, 0.13f, 0.07f, 0.55f));
                    Dibujo.rectangulo(lote, x + 9f, r.y + r.height - 4f, 8f, 2f,
                        new Color(1f, 0.80f, 0.48f, 0.18f));
                }
            } else if ("METAL".equals(estilo)) {
                for (float x = r.x + 16f; x < r.x + r.width - 10f; x += 42f) {
                    Dibujo.circulo(lote, x, r.y + r.height / 2f - 2f, 4f,
                        new Color(0.10f, 0.14f, 0.18f, 0.70f));
                    Dibujo.rectangulo(lote, x + 7f, r.y + r.height - 4f, 18f, 2f,
                        new Color(0.55f, 0.88f, 1f, 0.14f));
                }
            } else if ("PIEDRA".equals(estilo)) {
                Dibujo.linea(lote, r.x + 10f, r.y + r.height * 0.55f, r.x + 18f,
                    r.y + r.height * 0.35f, 1.5f, new Color(0f, 0f, 0f, 0.25f));
                Dibujo.linea(lote, r.x + r.width * 0.55f, r.y + r.height * 0.65f,
                    r.x + r.width * 0.62f, r.y + r.height * 0.30f, 1.5f,
                    new Color(0f, 0f, 0f, 0.22f));
                // Musgo sutil en el borde superior.
                for (float x = r.x + 8f; x < r.x + r.width - 6f; x += 29f) {
                    Dibujo.circulo(lote, x, r.y + r.height - 2f, 5f,
                        new Color(0.30f, 0.58f, 0.29f, 0.16f));
                }
            }
        }
    }

    public void dibujarPeligro(Rectangle r, boolean lava, float t) {
        Color base = lava ? new Color(0.85f, 0.18f, 0.04f, 1f) : new Color(0.06f, 0.31f, 0.72f, 1f);
        Color claro = lava ? new Color(1f, 0.64f, 0.12f, 0.95f) : new Color(0.35f, 0.84f, 1f, 0.90f);
        Dibujo.brillo(lote, r.x + r.width / 2f, r.y + r.height / 2f,
            Math.max(r.width * 1.22f, 70f), new Color(base.r, base.g, base.b, 0.18f));
        Dibujo.rectangulo(lote, r.x - 3f, r.y - 2f, r.width + 6f, r.height + 5f,
            new Color(base.r, base.g, base.b, 0.28f));
        Dibujo.rectangulo(lote, r.x, r.y, r.width, r.height, base);
        int segmentos = Math.max(2, (int) (r.width / 18f));
        for (int i = 0; i < segmentos; i++) {
            float x = r.x + i * (r.width / segmentos);
            float onda = (float) Math.sin(t * 4f + i * 0.9f) * 2.5f;
            Dibujo.rectangulo(lote, x, r.y + r.height - 5f + onda,
                r.width / segmentos + 2f, 4f, claro);
            if (!lava) {
                Dibujo.circulo(lote, x + 5f, r.y + 4f + (i % 2) * 4f, 3f,
                    new Color(0.70f, 0.94f, 1f, 0.45f));
            }
        }
        for (int i = 0; i < 6; i++) {
            float x = r.x + ((t * (12f + i * 3f) + i * 31f) % Math.max(1f, r.width));
            float y = r.y + r.height + 2f + (i % 3) * 4f + (float) Math.sin(t * 2f + i) * 2f;
            Dibujo.circulo(lote, x, y, 3f + (i % 3), new Color(claro.r, claro.g, claro.b, 0.45f));
        }
    }

    public void dibujarGema(Rectangle r, boolean secreta, float t) {
        float cx = r.x + r.width / 2f;
        float cy = r.y + r.height / 2f + (float) Math.sin(t * 3f + r.x * 0.03f) * 4f;
        Color color = secreta ? Configuracion.DORADO : new Color(0.30f, 0.95f, 0.58f, 1f);
        Color brillo = secreta ? new Color(1f, 0.95f, 0.62f, 1f) : new Color(0.75f, 1f, 0.86f, 1f);
        Dibujo.brillo(lote, cx, cy, secreta ? 58f : 46f, new Color(color.r, color.g, color.b, 0.27f));
        Dibujo.rectanguloRotado(lote, cx - 10f, cy - 10f, 20f, 20f, 10f, 10f, 45f, color);
        Dibujo.rectanguloRotado(lote, cx - 4f, cy - 4f, 8f, 8f, 4f, 4f, 45f, brillo);
        Dibujo.linea(lote, cx, cy - 24f, cx, cy - 13f, 1.5f,
            new Color(brillo.r, brillo.g, brillo.b, 0.25f));
        if (secreta) {
            for (int i = 0; i < 4; i++) {
                float ang = t * 2f + i * MathUtils.PI / 2f;
                Dibujo.circulo(lote, cx + MathUtils.cos(ang) * 18f - 2f,
                    cy + MathUtils.sin(ang) * 18f - 2f, 4f, new Color(1f, 0.9f, 0.5f, 0.8f));
            }
        }
    }

    public void dibujarBoton(Rectangle r, String elemento, boolean activo, float t) {
        Color color = colorElemento(elemento);
        float y = r.y + (activo ? -2f : 0f);
        Dibujo.rectangulo(lote, r.x - 3f, r.y - 2f, r.width + 6f, r.height + 4f,
            new Color(0.10f, 0.12f, 0.18f, 1f));
        Dibujo.rectangulo(lote, r.x, y, r.width, r.height,
            activo ? color : color.cpy().lerp(Color.BLACK, 0.28f));
        Dibujo.rectangulo(lote, r.x + 4f, y + r.height - 3f, r.width - 8f, 3f,
            activo ? new Color(1f, 1f, 1f, 0.75f) : new Color(1f, 1f, 1f, 0.25f));
        Dibujo.rectangulo(lote, r.x + 4f, y + 3f, r.width - 8f, 2f,
            new Color(0f, 0f, 0f, 0.25f));
        if (activo) Dibujo.brillo(lote, r.x + r.width / 2f, y + r.height / 2f,
            r.width * 1.7f, new Color(color.r, color.g, color.b, 0.24f));
        else Dibujo.brillo(lote, r.x + r.width / 2f, y + r.height / 2f,
            r.width * 1.2f, new Color(color.r, color.g, color.b, 0.08f));
    }

    public void dibujarPalanca(Rectangle r, String elemento, boolean activo, float t) {
        Color color = colorElemento(elemento);
        float cx = r.x + r.width / 2f;
        float baseY = r.y + 6f;
        Dibujo.rectangulo(lote, r.x + 2f, r.y, r.width - 4f, 8f,
            new Color(0.15f, 0.18f, 0.26f, 1f));
        Dibujo.rectangulo(lote, r.x + 6f, r.y + 6f, r.width - 12f, 2f,
            new Color(1f, 1f, 1f, 0.16f));
        Dibujo.rectangulo(lote, cx - 2f, baseY, 4f, r.height * 0.55f,
            new Color(0.55f, 0.61f, 0.72f, 1f));
        float ang = activo ? 32f : -28f;
        float hx = cx + MathUtils.sinDeg(ang) * r.height * 0.52f;
        float hy = baseY + MathUtils.cosDeg(ang) * r.height * 0.52f;
        Dibujo.linea(lote, cx, baseY, hx, hy, 6f, new Color(0.72f, 0.78f, 0.88f, 1f));
        Dibujo.circulo(lote, hx - 6f, hy - 6f, 12f, color);
        Dibujo.circulo(lote, hx - 3f, hy - 3f, 5f, new Color(1f, 1f, 1f, 0.52f));
        if (activo) Dibujo.brillo(lote, hx, hy, 46f, new Color(color.r, color.g, color.b, 0.35f));
        else Dibujo.brillo(lote, hx, hy, 26f, new Color(color.r, color.g, color.b, 0.12f));
    }

    public void dibujarPuerta(Rectangle r, boolean abierta, boolean puente, float t) {
        if (puente) {
            if (abierta) {
                Dibujo.rectangulo(lote, r.x, r.y - 2f, r.width, r.height + 4f,
                    new Color(0.20f, 0.85f, 0.95f, 0.22f));
                Dibujo.rectangulo(lote, r.x, r.y + r.height - 4f, r.width, 4f,
                    new Color(0.55f, 1f, 1f, 0.90f));
                for (float x = r.x; x < r.x + r.width; x += 26f) {
                    Dibujo.linea(lote, x, r.y, x, r.y + r.height, 1.5f,
                        new Color(0.45f, 0.95f, 1f, 0.35f));
                }
            } else {
                for (float x = r.x; x < r.x + r.width; x += 22f) {
                    Dibujo.rectangulo(lote, x, r.y + r.height / 2f - 1f, 12f, 2f,
                        new Color(0.35f, 0.65f, 0.80f, 0.20f));
                }
            }
            return;
        }

        if (abierta) {
            Dibujo.brillo(lote, r.x + r.width / 2f, r.y + r.height / 2f,
                r.width * 2.3f, new Color(0.35f, 0.90f, 1f, 0.16f));
            Dibujo.rectangulo(lote, r.x, r.y, 5f, r.height, new Color(0.55f, 0.86f, 0.95f, 0.55f));
            Dibujo.rectangulo(lote, r.x + r.width - 5f, r.y, 5f, r.height,
                new Color(0.55f, 0.86f, 0.95f, 0.55f));
            for (int i = 0; i < 5; i++) {
                float y = r.y + 20f + i * (r.height - 40f) / 4f;
                Dibujo.rectangulo(lote, r.x + 4f, y, r.width - 8f, 1.5f,
                    new Color(0.45f, 0.95f, 1f, 0.22f));
            }
            return;
        }

        Dibujo.rectangulo(lote, r.x - 3f, r.y, r.width + 6f, r.height,
            new Color(0.08f, 0.10f, 0.16f, 1f));
        Dibujo.rectangulo(lote, r.x, r.y, r.width, r.height,
            new Color(0.30f, 0.22f, 0.20f, 1f));
        for (float y = r.y + 8f; y < r.y + r.height - 8f; y += 22f) {
            Dibujo.rectangulo(lote, r.x + 3f, y, r.width - 6f, 3f,
                new Color(0.72f, 0.50f, 0.22f, 0.65f));
        }
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 3f + r.x);
        Dibujo.linea(lote, r.x + r.width / 2f, r.y + 20f,
            r.x + r.width / 2f, r.y + r.height - 20f, 2f,
            new Color(1f, 0.55f, 0.20f, 0.50f + pulso * 0.35f));
        Dibujo.anillo(lote, r.x + r.width / 2f, r.y + r.height / 2f, r.width * 1.4f,
            new Color(1f, 0.60f, 0.20f, 0.10f));
    }

    public void dibujarMovil(Rectangle r, float t) {
        Dibujo.rectangulo(lote, r.x + 3f, r.y - 4f, r.width, r.height, SOMBRA);
        Dibujo.brillo(lote, r.x + r.width / 2f, r.y + r.height / 2f, r.width * 1.2f,
            new Color(0.55f, 0.45f, 0.95f, 0.12f));
        Dibujo.rectangulo(lote, r.x, r.y, r.width, r.height, new Color(0.36f, 0.30f, 0.56f, 1f));
        Dibujo.rectangulo(lote, r.x, r.y + r.height - 4f, r.width, 4f,
            new Color(0.75f, 0.68f, 1f, 1f));
        for (float x = r.x + 12f; x < r.x + r.width - 10f; x += 24f) {
            Dibujo.circulo(lote, x, r.y + r.height / 2f - 1f, 4f,
                new Color(0.62f, 0.55f, 0.90f, 0.80f));
        }
        for (int i = 0; i < 3; i++) {
            float px = r.x + 12f + ((t * (8f + i * 2f) + i * 23f) % Math.max(1f, r.width - 24f));
            Dibujo.circulo(lote, px, r.y + r.height - 9f, 2f,
                new Color(0.85f, 0.78f, 1f, 0.55f));
        }
    }

    public void dibujarCaja(Rectangle r, boolean pesada) {
        Dibujo.rectangulo(lote, r.x + 3f, r.y - 3f, r.width, r.height, SOMBRA);
        Dibujo.rectangulo(lote, r.x, r.y, r.width, r.height,
            pesada ? new Color(0.34f, 0.39f, 0.50f, 1f) : MADERA);
        Dibujo.rectangulo(lote, r.x + 3f, r.y + 3f, r.width - 6f, r.height - 6f,
            pesada ? new Color(0.48f, 0.54f, 0.64f, 1f) : MADERA_CLARA);
        Dibujo.linea(lote, r.x + 5f, r.y + 5f, r.x + r.width - 5f, r.y + r.height - 5f,
            2f, new Color(0.15f, 0.12f, 0.10f, 0.55f));
        Dibujo.linea(lote, r.x + r.width - 5f, r.y + 5f, r.x + 5f, r.y + r.height - 5f,
            2f, new Color(0.15f, 0.12f, 0.10f, 0.55f));
        if (pesada) {
            Dibujo.rectangulo(lote, r.x + r.width / 2f - 6f, r.y + r.height / 2f - 6f,
                12f, 12f, new Color(0.90f, 0.82f, 0.52f, 1f));
            Dibujo.brillo(lote, r.x + r.width / 2f, r.y + r.height / 2f, 30f,
                new Color(1f, 0.85f, 0.35f, 0.14f));
        }
    }

    public void dibujarSalida(Rectangle r, boolean fuego, boolean activa, float t) {
        Color color = fuego ? Configuracion.FUEGO : Configuracion.AGUA;
        Color claro = fuego ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA;
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 3f + (fuego ? 0f : 1.4f));
        Dibujo.brillo(lote, r.x + r.width / 2f, r.y + r.height / 2f,
            r.height * (1.7f + pulso * 0.35f), new Color(color.r, color.g, color.b, activa ? 0.36f : 0.18f));
        Dibujo.rectanguloRedondeado(lote, r.x, r.y, r.width, r.height, 14f,
            new Color(0.10f, 0.12f, 0.19f, 1f));
        Dibujo.rectanguloRedondeado(lote, r.x + 5f, r.y + 5f, r.width - 10f, r.height - 10f, 12f,
            new Color(color.r, color.g, color.b, 0.38f + pulso * 0.20f));
        for (int i = 0; i < 4; i++) {
            float ang = t * (1.2f + i * 0.15f) + i * 1.57f;
            float cx = r.x + r.width / 2f + MathUtils.cos(ang) * r.width * 0.20f;
            float cy = r.y + r.height / 2f + MathUtils.sin(ang) * r.height * 0.22f;
            Dibujo.circulo(lote, cx - 5f, cy - 5f, 10f,
                new Color(claro.r, claro.g, claro.b, 0.75f));
        }
        Dibujo.rectangulo(lote, r.x + r.width / 2f - 2f, r.y + 8f, 4f,
            r.height - 16f, new Color(claro.r, claro.g, claro.b, 0.55f));
        if (activa) {
            Dibujo.anillo(lote, r.x + r.width / 2f, r.y + r.height / 2f,
                r.height * (1.1f + pulso * 0.25f), new Color(1f, 1f, 1f, 0.55f));
        }
    }

    // =========================================================
    //                    PERSONAJES
    // =========================================================

    public void dibujarJugador(Jugador p, float t) {
        dibujarHumanoide(p.x, p.y, p.ancho, p.alto, p.tipo, t, p.enSuelo,
            p.velX, p.velY, p.mirandoDerecha, p.tiempoInteraccion > 0f,
            p.celebrando, p.derrotado, p.aplastado, 1f);
    }

    /** Retrato animado para el menu: no necesita una Partida ni altera colisiones. */
    public void dibujarAvatarMenu(float centroX, float baseY, boolean fuego, float t, float escala) {
        float ancho = Configuracion.ANCHO_PERSONAJE * escala;
        float alto = Configuracion.ALTO_PERSONAJE * escala;
        dibujarHumanoide(centroX - ancho / 2f, baseY, ancho, alto,
            fuego ? Jugador.Tipo.FUEGO : Jugador.Tipo.AGUA, t, true,
            0f, 0f, true, false, fuego, false, 0f, escala);
    }

    /**
     * Personaje humanoide dibujado por piezas. Todo queda dentro del espiritu
     * geometrico del juego, pero con cabeza, torso, brazos, piernas y estados.
     */
    private void dibujarHumanoide(float x, float y, float ancho, float alto, Jugador.Tipo tipo,
                                  float t, boolean enSuelo, float velX, float velY,
                                  boolean mirandoDerecha, boolean interactuando,
                                  boolean celebrando, boolean derrotado, float aplastado,
                                  float escala) {
        float s = Math.max(0.55f, escala);
        float cx = x + ancho / 2f;
        float base = y;
        boolean caminando = enSuelo && Math.abs(velX) > 12f;
        boolean saltando = !enSuelo && velY > 40f;
        boolean cayendo = !enSuelo && velY < -40f;
        float fase = caminando ? (float) Math.sin(t * 13f) : 0f;
        float respiracion = enSuelo && !caminando ? (float) Math.sin(t * 3f) * 1.2f : 0f;
        float rebote = celebrando ? Math.abs((float) Math.sin(t * 6f)) * 7f * s : 0f;
        float inclinacion = caminando ? fase * 2.5f * s : 0f;

        Color baseColor = tipo == Jugador.Tipo.FUEGO ? Configuracion.FUEGO : Configuracion.AGUA;
        Color claro = tipo == Jugador.Tipo.FUEGO ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA;
        if (derrotado) {
            baseColor = baseColor.cpy().lerp(new Color(0.20f, 0.22f, 0.28f, 1f), 0.62f);
            claro = claro.cpy().lerp(new Color(0.35f, 0.38f, 0.46f, 1f), 0.55f);
        }
        Color oscuro = baseColor.cpy().lerp(Color.BLACK, 0.48f);
        Color rostro = new Color(0.055f, 0.065f, 0.10f, 1f);
        if (tipo == Jugador.Tipo.AGUA) rostro = new Color(0.035f, 0.075f, 0.14f, 1f);

        float cuerpoAncho = 17f * s;
        float cuerpoAlto = 19f * s;
        float cabezaDiam = 16f * s;
        float cabezaCY = base + alto * 0.73f + rebote;
        float torsoY = base + alto * 0.28f + rebote;
        float hombroY = torsoY + cuerpoAlto * 0.82f;

        // Sombra y aura elemental: no se confunden con el cuerpo fisico.
        Dibujo.elipse(lote, cx - 18f * s, base - 4f * s, 36f * s, 8f * s, SOMBRA);
        Dibujo.brillo(lote, cx, base + alto * 0.50f + rebote, alto * 2.4f,
            new Color(baseColor.r, baseColor.g, baseColor.b, 0.15f));

        if (Math.abs(velX) > 80f && enSuelo) {
            float estela = mirandoDerecha ? -1f : 1f;
            Dibujo.rectanguloRedondeado(lote, cx + estela * 18f * s - 7f * s,
                torsoY - 2f * s, 14f * s, cuerpoAlto * 0.8f, 7f * s,
                new Color(baseColor.r, baseColor.g, baseColor.b, 0.10f));
        }

        // Piernas: zancada alterna; en el aire quedan recogidas.
        float zancada = caminando ? fase * 5f * s : 0f;
        float piernaIzqX = cx - 5f * s + zancada * 0.55f;
        float piernaDerX = cx + 5f * s - zancada * 0.55f;
        float pieIzqY = base + (saltando ? 5f * s : 0f) + Math.max(0f, zancada) * 0.15f;
        float pieDerY = base + (saltando ? 5f * s : 0f) + Math.max(0f, -zancada) * 0.15f;
        if (derrotado) {
            piernaIzqX -= 3f * s;
            piernaDerX += 3f * s;
        }
        Dibujo.linea(lote, cx - 5f * s, torsoY + 2f * s, piernaIzqX, pieIzqY + 4f * s,
            5f * s, oscuro);
        Dibujo.linea(lote, cx + 5f * s, torsoY + 2f * s, piernaDerX, pieDerY + 4f * s,
            5f * s, oscuro);
        Dibujo.rectanguloRedondeado(lote, piernaIzqX - 5f * s, pieIzqY, 10f * s, 4f * s,
            2f * s, baseColor.cpy().lerp(Color.BLACK, 0.30f));
        Dibujo.rectanguloRedondeado(lote, piernaDerX - 5f * s, pieDerY, 10f * s, 4f * s,
            2f * s, baseColor.cpy().lerp(Color.BLACK, 0.30f));

        // Torso con placas y nucleo.
        Dibujo.rectanguloRedondeado(lote, cx - cuerpoAncho / 2f + inclinacion, torsoY,
            cuerpoAncho, cuerpoAlto, 6f * s, baseColor);
        Dibujo.rectanguloRedondeado(lote, cx - cuerpoAncho / 2f + 2f * s + inclinacion,
            torsoY + 3f * s, cuerpoAncho - 4f * s, cuerpoAlto - 7f * s, 4f * s,
            oscuro.cpy().lerp(baseColor, 0.32f));
        Dibujo.rectangulo(lote, cx - 7f * s + inclinacion, torsoY + cuerpoAlto - 4f * s,
            14f * s, 2f * s, new Color(1f, 1f, 1f, 0.20f));
        // Cinturon.
        Dibujo.rectangulo(lote, cx - cuerpoAncho / 2f + inclinacion, torsoY + 4f * s,
            cuerpoAncho, 2.5f * s, new Color(0.10f, 0.12f, 0.17f, 0.80f));
        // Nucleo brillante.
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 4f + (tipo == Jugador.Tipo.FUEGO ? 0f : 1.2f));
        Dibujo.brillo(lote, cx + inclinacion, torsoY + cuerpoAlto * 0.54f, 26f * s,
            new Color(claro.r, claro.g, claro.b, 0.24f + pulso * 0.10f));
        if (tipo == Jugador.Tipo.FUEGO) {
            Dibujo.rectanguloRotado(lote, cx - 4f * s + inclinacion, torsoY + cuerpoAlto * 0.46f,
                8f * s, 8f * s, 4f * s, 4f * s, 45f, claro);
        } else {
            Dibujo.circulo(lote, cx - 4f * s + inclinacion, torsoY + cuerpoAlto * 0.46f,
                8f * s, claro);
            Dibujo.circulo(lote, cx - 1.5f * s + inclinacion, torsoY + cuerpoAlto * 0.50f,
                3.5f * s, new Color(1f, 1f, 1f, 0.58f));
        }

        // Brazos: caminar, salto, interaccion, victoria o derrota.
        float brazoSwing = caminando ? -fase * 7f * s : 0f;
        float manoIzqX = cx - 13f * s + (mirandoDerecha ? 0f : 2f * s);
        float manoDerX = cx + 13f * s + (mirandoDerecha ? 0f : -2f * s);
        float manoIzqY = hombroY - 10f * s - brazoSwing;
        float manoDerY = hombroY - 10f * s + brazoSwing;
        if (saltando) {
            manoIzqX = cx - 14f * s;
            manoDerX = cx + 14f * s;
            manoIzqY = hombroY + 4f * s;
            manoDerY = hombroY + 3f * s;
        } else if (cayendo) {
            manoIzqX = cx - 16f * s;
            manoDerX = cx + 16f * s;
            manoIzqY = hombroY - 2f * s;
            manoDerY = hombroY - 2f * s;
        } else if (interactuando) {
            float dir = mirandoDerecha ? 1f : -1f;
            manoIzqX = cx + dir * 19f * s;
            manoDerX = cx + dir * 23f * s;
            manoIzqY = hombroY - 7f * s;
            manoDerY = hombroY - 2f * s;
        } else if (celebrando) {
            manoIzqX = cx - 15f * s;
            manoDerX = cx + 15f * s;
            manoIzqY = hombroY + 12f * s;
            manoDerY = hombroY + 10f * s;
        } else if (derrotado) {
            manoIzqX = cx - 12f * s;
            manoDerX = cx + 12f * s;
            manoIzqY = hombroY - 16f * s;
            manoDerY = hombroY - 16f * s;
        }
        Dibujo.linea(lote, cx - 7f * s + inclinacion, hombroY, manoIzqX, manoIzqY,
            4.5f * s, baseColor.cpy().lerp(Color.BLACK, 0.16f));
        Dibujo.linea(lote, cx + 7f * s + inclinacion, hombroY, manoDerX, manoDerY,
            4.5f * s, baseColor.cpy().lerp(Color.BLACK, 0.16f));
        Dibujo.circulo(lote, manoIzqX - 3.5f * s, manoIzqY - 3.5f * s, 7f * s, claro);
        Dibujo.circulo(lote, manoDerX - 3.5f * s, manoDerY - 3.5f * s, 7f * s, claro);
        if (interactuando) {
            float dir = mirandoDerecha ? 1f : -1f;
            Dibujo.circulo(lote, cx + dir * 23f * s, hombroY - 5f * s, 7f * s,
                new Color(1f, 1f, 1f, 0.45f));
        }

        // Cabeza, rostro y ojos.
        Dibujo.rectangulo(lote, cx - 3f * s + inclinacion, cabezaCY - 12f * s,
            6f * s, 5f * s, oscuro); // cuello
        Dibujo.circulo(lote, cx - cabezaDiam / 2f + inclinacion, cabezaCY - cabezaDiam / 2f,
            cabezaDiam, baseColor);
        Dibujo.circulo(lote, cx - cabezaDiam * 0.33f + inclinacion, cabezaCY - cabezaDiam * 0.30f,
            cabezaDiam * 0.66f, new Color(claro.r, claro.g, claro.b, 0.55f));
        // Rostro oscuro: lectura humana, sin perder el elemento.
        Dibujo.elipse(lote, cx - cabezaDiam * 0.37f + inclinacion, cabezaCY - cabezaDiam * 0.34f,
            cabezaDiam * 0.74f, cabezaDiam * 0.62f, rostro);
        float ojoY = cabezaCY + cabezaDiam * 0.02f;
        float ojoSep = cabezaDiam * 0.20f;
        float mirada = mirandoDerecha ? 1f : -1f;
        Dibujo.circulo(lote, cx - ojoSep - 2.8f * s + inclinacion + mirada * 1.2f * s,
            ojoY - 2.8f * s, 5.6f * s, Color.WHITE);
        Dibujo.circulo(lote, cx + ojoSep - 2.8f * s + inclinacion + mirada * 1.2f * s,
            ojoY - 2.8f * s, 5.6f * s, Color.WHITE);
        float pupilaY = ojoY - 1.4f * s;
        Dibujo.circulo(lote, cx - ojoSep - 1.6f * s + inclinacion + mirada * 1.5f * s,
            pupilaY - 1.5f * s, 3f * s, new Color(0.05f, 0.06f, 0.09f, 1f));
        Dibujo.circulo(lote, cx + ojoSep - 1.6f * s + inclinacion + mirada * 1.5f * s,
            pupilaY - 1.5f * s, 3f * s, new Color(0.05f, 0.06f, 0.09f, 1f));
        if (derrotado) {
            // Parpados caidos.
            Dibujo.rectangulo(lote, cx - ojoSep - 3f * s + inclinacion, ojoY - 0.5f * s,
                6f * s, 2.2f * s, new Color(0.05f, 0.06f, 0.09f, 0.75f));
            Dibujo.rectangulo(lote, cx + ojoSep - 3f * s + inclinacion, ojoY - 0.5f * s,
                6f * s, 2.2f * s, new Color(0.05f, 0.06f, 0.09f, 0.75f));
        }
        // Boca.
        float bocaY = cabezaCY - cabezaDiam * 0.22f;
        if (celebrando) {
            Dibujo.circulo(lote, cx - 3f * s + inclinacion, bocaY - 3f * s, 6f * s,
                new Color(0.10f, 0.06f, 0.07f, 0.85f));
        } else {
            Dibujo.rectanguloRedondeado(lote, cx - 4f * s + inclinacion, bocaY,
                8f * s, 1.8f * s, 0.9f * s, new Color(0.85f, 0.85f, 0.90f, 0.52f));
        }

        // Cabello elemental.
        if (tipo == Jugador.Tipo.FUEGO) {
            for (int i = 0; i < 6; i++) {
                float fx = cx + inclinacion - 9f * s + i * 3.6f * s;
                float altoLlama = (11f + (float) Math.sin(t * 8f + i * 1.3f) * 5f) * s;
                Dibujo.rectanguloRotado(lote, fx - 2.6f * s, cabezaCY + cabezaDiam * 0.20f,
                    5.2f * s, altoLlama, 2.6f * s, 0f, (i - 2) * 8f,
                    i % 2 == 0 ? claro : baseColor);
            }
            Dibujo.circulo(lote, cx + inclinacion, cabezaCY + cabezaDiam * 0.18f,
                8f * s, new Color(1f, 0.90f, 0.48f, 0.78f));
            for (int i = 0; i < 4; i++) {
                float ang = t * 2.6f + i * 1.57f;
                Dibujo.circulo(lote, cx + MathUtils.cos(ang) * 16f * s - 2f * s,
                    cabezaCY + 8f * s + MathUtils.sin(ang) * 12f * s, 3f * s,
                    new Color(1f, 0.80f, 0.28f, 0.50f));
            }
        } else {
            // Melena fluida de agua, por encima de los ojos para no taparlos.
            Dibujo.circulo(lote, cx - 8f * s + inclinacion, cabezaCY + 8f * s, 15f * s,
                new Color(0.12f, 0.44f, 0.90f, 0.92f));
            Dibujo.circulo(lote, cx + 8f * s + inclinacion, cabezaCY + 9f * s, 15f * s,
                new Color(0.12f, 0.44f, 0.90f, 0.92f));
            for (int i = 0; i < 4; i++) {
                float bx = cx + (i - 1.5f) * 7f * s + inclinacion;
                float by = cabezaCY + 14f * s + (float) Math.sin(t * 3f + i) * 4f * s;
                Dibujo.circulo(lote, bx - 4f * s, by - 4f * s, 8f * s,
                    new Color(0.55f, 0.88f, 1f, 0.72f));
            }
            for (int i = 0; i < 3; i++) {
                float bx = cx + (i - 1) * 12f * s + (float) Math.sin(t * 2.2f + i) * 6f * s;
                float by = cabezaCY - 12f * s + i * 5f * s;
                Dibujo.circulo(lote, bx - 2f * s, by - 2f * s, 4f * s,
                    new Color(0.75f, 0.96f, 1f, 0.55f));
            }
        }

        // Pequenas particulas del personaje.
        for (int i = 0; i < 3; i++) {
            float px = cx + (float) Math.sin(t * 3f + i * 2.1f) * 17f * s;
            float py = base + 8f * s + ((t * (18f + i * 4f) + i * 19f) % (38f * s));
            Dibujo.circulo(lote, px, py, (1.8f + i * 0.6f) * s,
                new Color(claro.r, claro.g, claro.b, 0.22f));
        }
    }

    // =========================================================
    //                         HUD
    // =========================================================

    public void hud(Partida p, GestorRed red, Juego juego, boolean esCliente) {
        if (p == null) return;

        // Barra superior izquierda: nivel, nombre y tiempo.
        Dibujo.panel(lote, 12f, 472f, 298f, 56f, new Color(0.04f, 0.06f, 0.11f, 0.92f),
            new Color(0.30f, 0.42f, 0.60f, 0.95f));
        Dibujo.rectangulo(lote, 12f, 472f, 4f, 56f, Configuracion.DORADO);
        Dibujo.texto(lote, fuente, "NIVEL " + (p.indiceNivel + 1) + "/" + Configuracion.TOTAL_NIVELES,
            26f, 512f, Configuracion.DORADO, 0.82f);
        Dibujo.texto(lote, fuente, p.nivel.nombre, 26f, 490f, Configuracion.TEXTO, 0.78f);
        Dibujo.textoDerecha(lote, fuente, p.textoTiempo(), 292f, 512f, Configuracion.TEXTO_SUAVE, 0.78f);
        Dibujo.textoDerecha(lote, fuente, Nivel.nombreTema(p.nivel.tema), 292f, 490f,
            new Color(0.55f, 0.72f, 0.90f, 1f), 0.62f);

        // Marcador central: dos columnas con nucleo de color.
        float panelX = 318f;
        Dibujo.panel(lote, panelX, 472f, 338f, 56f, new Color(0.04f, 0.06f, 0.11f, 0.92f),
            new Color(0.30f, 0.42f, 0.60f, 0.95f));
        Dibujo.rectangulo(lote, panelX + 11f, 484f, 3f, 32f, Configuracion.FUEGO);
        Dibujo.circulo(lote, panelX + 21f, 496f, 13f, Configuracion.FUEGO);
        Dibujo.texto(lote, fuente, "FUEGO", panelX + 40f, 512f, Configuracion.FUEGO_CLARO, 0.68f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(p.puntuacion.puntosFuego),
            panelX + 152f, 512f, Configuracion.TEXTO, 0.88f);
        Dibujo.texto(lote, fuente, "gemas " + p.puntuacion.gemasFuego, panelX + 40f, 489f,
            Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.texto(lote, fuente, "vidas " + p.vidasFuego, panelX + 112f, 489f,
            Configuracion.TEXTO_SUAVE, 0.60f);

        Dibujo.linea(lote, panelX + 169f, 480f, panelX + 169f, 522f, 1f,
            new Color(0.35f, 0.42f, 0.58f, 0.35f));

        Dibujo.rectangulo(lote, panelX + 179f, 484f, 3f, 32f, Configuracion.AGUA);
        Dibujo.circulo(lote, panelX + 189f, 496f, 13f, Configuracion.AGUA);
        Dibujo.texto(lote, fuente, "AGUA", panelX + 208f, 512f, Configuracion.AGUA_CLARA, 0.68f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(p.puntuacion.puntosAgua),
            panelX + 320f, 512f, Configuracion.TEXTO, 0.88f);
        Dibujo.texto(lote, fuente, "gemas " + p.puntuacion.gemasAgua, panelX + 208f, 489f,
            Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.texto(lote, fuente, "vidas " + p.vidasAgua, panelX + 280f, 489f,
            Configuracion.TEXTO_SUAVE, 0.60f);

        // Panel derecho: gemas, red y pausa.
        Dibujo.panel(lote, 666f, 472f, 282f, 56f, new Color(0.04f, 0.06f, 0.11f, 0.92f),
            new Color(0.30f, 0.42f, 0.60f, 0.95f));
        Dibujo.rectangulo(lote, 944f, 472f, 4f, 56f, Configuracion.VERDE);
        Dibujo.texto(lote, fuente, "GEMAS " + p.gemasRecogidas() + "/" + p.nivel.gemas.length,
            680f, 512f, Configuracion.TEXTO, 0.76f);
        String redTexto = "LOCAL";
        Color redColor = Configuracion.VERDE;
        if (red != null) {
            if (red.modo == GestorRed.Modo.SERVIDOR) {
                redTexto = red.conectado ? "SALA " + red.codigo + "  CONECTADO" : "SALA " + red.codigo + "  ESPERANDO";
                redColor = red.conectado ? Configuracion.VERDE : Configuracion.DORADO;
            } else if (red.modo == GestorRed.Modo.CLIENTE) {
                redTexto = red.conectado ? "CLIENTE CONECTADO" : "RECONECTANDO...";
                redColor = red.conectado ? Configuracion.VERDE : Configuracion.ROJO;
            }
        }
        Dibujo.texto(lote, fuente, redTexto, 680f, 489f, redColor, 0.60f);
        Dibujo.textoDerecha(lote, fuente, "ESC pausa", 934f, 512f, Configuracion.TEXTO_SUAVE, 0.58f);
        Dibujo.textoDerecha(lote, fuente, esCliente ? "Tu: Agua"
            : (red != null && red.modo == GestorRed.Modo.LOCAL ? "2 jugadores" : "Tu: Fuego"),
            934f, 489f, Configuracion.TEXTO_SUAVE, 0.58f);

        // Mensaje central inferior y pista del nivel.
        if (p.mensaje != null && !p.mensaje.isEmpty() && p.mensajeTiempo > 0f) {
            float ancho = Math.min(620f, Dibujo.anchoTexto(fuente, p.mensaje, 0.85f) + 50f);
            Dibujo.panel(lote, 480f - ancho / 2f, 66f, ancho, 34f,
                new Color(0.04f, 0.06f, 0.11f, 0.94f), new Color(0.38f, 0.52f, 0.72f, 0.95f));
            Dibujo.rectangulo(lote, 480f - ancho / 2f + 10f, 70f, 3f, 26f, Configuracion.DORADO);
            Dibujo.textoCentrado(lote, fuente, p.mensaje, 480f, 89f, Configuracion.TEXTO, 0.85f);
        }
        if (p.nivel.pista != null && !p.nivel.pista.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, p.nivel.pista, 480f, 28f,
                new Color(0.72f, 0.82f, 0.96f, 0.88f), 0.66f);
        }
    }

    public void textoCentrado(String texto, float x, float y, Color color, float escala) {
        Dibujo.textoCentrado(lote, fuente, texto, x, y, color, escala);
    }

    public void texto(String texto, float x, float y, Color color, float escala) {
        Dibujo.texto(lote, fuente, texto, x, y, color, escala);
    }

    public static Color colorElemento(String elemento) {
        if ("FUEGO".equalsIgnoreCase(elemento)) return Configuracion.FUEGO;
        if ("AGUA".equalsIgnoreCase(elemento)) return Configuracion.AGUA;
        return Configuracion.VIOLETA;
    }

    private static float positivo(float valor, float modulo) {
        if (modulo <= 0f) return 0f;
        float r = valor % modulo;
        return r < 0f ? r + modulo : r;
    }
}
