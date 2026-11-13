package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.List;

public class Jugador {
    public enum Tipo { FUEGO, AGUA }

    public final Tipo tipo;
    public float x, y;
    public float ancho = Configuracion.ANCHO_PERSONAJE;
    public float alto = Configuracion.ALTO_PERSONAJE;
    public float velX = 0f, velY = 0f;
    public boolean enSuelo = false;
    public boolean vivo = true;
    public boolean mirandoDerecha = true;
    public float tiempoAnim = 0f;
    public float aplastado = 0f;
    public float tiempoReaparicion = 0f;
    public float tiempoCoyote = 0f;
    public float tiempoBufferSalto = 0f;
    public boolean estabaEnSuelo = false;

    public float tiempoInteraccion = 0f;
    public boolean celebrando = false;
    public boolean derrotado = false;

    private boolean muerteContada = false;

    public float origenX, origenY;

    private final Rectangle cuerpo = new Rectangle();

    public Jugador(Tipo tipo, float origenX, float origenY) {
        this.tipo = tipo;
        this.origenX = origenX;
        this.origenY = origenY;
        this.x = origenX;
        this.y = origenY;
    }

    public Rectangle cuerpo() {
        cuerpo.set(x, y, ancho, alto);
        return cuerpo;
    }

    public void reaparecer() {
        x = origenX;
        y = origenY;
        velX = 0f;
        velY = 0f;
        vivo = true;
        tiempoReaparicion = 0f;
        aplastado = 0f;
        enSuelo = false;
        tiempoCoyote = 0f;
        tiempoBufferSalto = 0f;
        muerteContada = false;
        tiempoInteraccion = 0f;
        celebrando = false;
        derrotado = false;
    }

    public void morir() {
        if (!vivo) return;
        vivo = false;

        muerteContada = false;
        velX = 0f;
        velY = 0f;
        tiempoReaparicion = Configuracion.TIEMPO_REAPARICION;
    }

    public boolean tiempoReaparicionYaContado() {
        return muerteContada;
    }

    public void marcarMuerteContada() {
        muerteContada = true;
    }

    public float velocidadMaxima() {
        return tipo == Tipo.FUEGO ? Configuracion.VELOCIDAD_FUEGO : Configuracion.VELOCIDAD_AGUA;
    }

    public float fuerzaSalto() {
        return tipo == Tipo.FUEGO ? Configuracion.FUERZA_SALTO_FUEGO : Configuracion.FUERZA_SALTO_AGUA;
    }

    public void actualizar(float dt, boolean izquierda, boolean derecha, boolean saltoMantenido,
                           boolean saltoPresionado, List<Rectangle> solidos) {
        tiempoAnim += dt;
        if (tiempoInteraccion > 0f) tiempoInteraccion = Math.max(0f, tiempoInteraccion - dt);
        if (aplastado > 0f) aplastado = Math.max(0f, aplastado - dt * 4f);
        if (aplastado < 0f) aplastado = Math.min(0f, aplastado + dt * 4f);

        if (!vivo) {
            tiempoReaparicion -= dt;
            velX = 0f;
            velY = 0f;
            return;
        }

        estabaEnSuelo = enSuelo;
        if (enSuelo) tiempoCoyote = Configuracion.TIEMPO_COYOTE;
        else tiempoCoyote -= dt;

        if (saltoPresionado) tiempoBufferSalto = Configuracion.TIEMPO_BUFFER_SALTO;
        else tiempoBufferSalto -= dt;

        float direccion = 0f;
        if (izquierda) direccion -= 1f;
        if (derecha) direccion += 1f;
        velX = direccion * velocidadMaxima();
        if (direccion != 0f) mirandoDerecha = direccion > 0f;

        if (tiempoBufferSalto > 0f && tiempoCoyote > 0f) {
            velY = fuerzaSalto();
            enSuelo = false;
            tiempoCoyote = 0f;
            tiempoBufferSalto = 0f;
            aplastado = -0.7f;
        }

        velY -= Configuracion.GRAVEDAD * dt;
        if (velY > 0f && !saltoMantenido) {
            velY -= Configuracion.GRAVEDAD * Configuracion.CORTE_SALTO * dt;
        }
        if (velY < -Configuracion.VELOCIDAD_CAIDA_MAXIMA) {
            velY = -Configuracion.VELOCIDAD_CAIDA_MAXIMA;
        }

        float xAntes = x;
        x += velX * dt;
        for (int i = 0; i < solidos.size(); i++) {
            Rectangle s = solidos.get(i);
            if (cuerpo().overlaps(s)) {
                if (velX > 0f) x = s.x - ancho;
                else if (velX < 0f) x = s.x + s.width;
                else if (xAntes + ancho <= s.x + 0.5f) x = s.x - ancho;
                else x = s.x + s.width;
                velX = 0f;
            }
        }

        float yAntes = y;
        float velYAntes = velY;
        y += velY * dt;
        enSuelo = false;
        for (int i = 0; i < solidos.size(); i++) {
            Rectangle s = solidos.get(i);
            if (!cuerpo().overlaps(s)) continue;

            boolean veniaDeArriba = velYAntes <= 0f && yAntes >= s.y + s.height - 1f;
            boolean golpeoCabeza = velYAntes > 0f && yAntes + alto <= s.y + 1f;

            if (veniaDeArriba) {
                if (velYAntes < -520f) aplastado = 1f;
                y = s.y + s.height;
                velY = 0f;
                enSuelo = true;
            } else if (golpeoCabeza) {
                y = s.y - alto;
                velY = 0f;
            } else {

                float solapeAbajo = (s.y + s.height) - y;
                float solapeArriba = (y + alto) - s.y;
                if (solapeAbajo <= solapeArriba) {
                    y = s.y + s.height;
                    enSuelo = true;
                } else {
                    y = s.y - alto;
                }
                velY = 0f;
            }
        }

        if (x < 0f) x = 0f;
        if (x > anchoMundoActual - ancho) x = Math.max(0f, anchoMundoActual - ancho);
        if (y + alto > altoMundoActual) {
            y = Math.max(0f, altoMundoActual - alto);
            if (velY > 0f) velY = 0f;
        }
        if (y < -90f) morir();
    }

    public transient float anchoMundoActual = Configuracion.ANCHO_MUNDO;
    public transient float altoMundoActual = Configuracion.ALTO_MUNDO;
}
