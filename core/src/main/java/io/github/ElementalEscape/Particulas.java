package io.github.ElementalEscape;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/** Sistema de particulas muy liviano (sin librerias externas). */
public class Particulas {
    private static final int MAXIMAS = 420;

    private static class Particula {
        float x, y, velX, velY, vida, vidaInicial, tamano;
        Color color = new Color();
        boolean brillo;
        boolean gravedad;
    }

    private final Particula[] lista = new Particula[MAXIMAS];
    private int cantidad = 0;
    private boolean activas = true;

    public Particulas() {
        for (int i = 0; i < MAXIMAS; i++) lista[i] = new Particula();
    }

    public void setActivas(boolean activas) { this.activas = activas; }

    public void limpiar() { cantidad = 0; }

    public void crear(float x, float y, float velX, float velY, float vida, float tamano,
                      Color color, boolean brillo, boolean gravedad) {
        if (!activas || cantidad >= MAXIMAS) return;
        Particula p = lista[cantidad++];
        p.x = x; p.y = y; p.velX = velX; p.velY = velY;
        p.vida = vida; p.vidaInicial = vida; p.tamano = tamano;
        p.color.set(color); p.brillo = brillo; p.gravedad = gravedad;
    }

    public void chispas(float x, float y, Color color, int cuantas) {
        for (int i = 0; i < cuantas; i++) {
            float angulo = MathUtils.random(0f, MathUtils.PI2);
            float rapidez = MathUtils.random(30f, 120f);
            crear(x, y, MathUtils.cos(angulo) * rapidez, MathUtils.sin(angulo) * rapidez,
                MathUtils.random(0.35f, 0.8f), MathUtils.random(2f, 5f), color, true, true);
        }
    }

    public void polvo(float x, float y, int cuantas) {
        for (int i = 0; i < cuantas; i++) {
            crear(x + MathUtils.random(-10f, 10f), y, MathUtils.random(-45f, 45f),
                MathUtils.random(15f, 65f), MathUtils.random(0.3f, 0.6f), MathUtils.random(3f, 7f),
                new Color(0.8f, 0.8f, 0.9f, 0.35f), false, false);
        }
    }

    public void gotas(float x, float y, Color color, int cuantas) {
        for (int i = 0; i < cuantas; i++) {
            crear(x + MathUtils.random(-8f, 8f), y, MathUtils.random(-35f, 35f),
                MathUtils.random(20f, 90f), MathUtils.random(0.3f, 0.7f), MathUtils.random(2f, 4f),
                color, false, true);
        }
    }

    public void actualizar(float dt) {
        for (int i = cantidad - 1; i >= 0; i--) {
            Particula p = lista[i];
            p.vida -= dt;
            if (p.vida <= 0f) {
                lista[i] = lista[cantidad - 1];
                lista[cantidad - 1] = p;
                cantidad--;
                continue;
            }
            p.x += p.velX * dt;
            p.y += p.velY * dt;
            if (p.gravedad) p.velY -= 260f * dt;
        }
    }

    public void dibujar(SpriteBatch lote) {
        for (int i = 0; i < cantidad; i++) {
            Particula p = lista[i];
            float alfa = Math.max(0f, Math.min(1f, p.vida / p.vidaInicial));
            p.color.a = alfa * 0.9f;
            float tamano = p.tamano * (0.45f + alfa * 0.55f);
            if (p.brillo) Dibujo.brillo(lote, p.x, p.y, tamano * 4f, p.color);
            else Dibujo.circulo(lote, p.x - tamano / 2f, p.y - tamano / 2f, tamano, p.color);
        }
    }

    public int getCantidad() { return cantidad; }
}
