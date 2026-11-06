package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.List;

/**
 * Un personaje (fuego o agua) con fisica de plataformas.
 * La fisica se resuelve primero en horizontal y despues en vertical,
 * que es la forma mas simple y estable para un juego 2D.
 */
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
    public float aplastado = 0f;       // 1 = aplastado, -1 = estirado
    public float tiempoReaparicion = 0f;
    public float tiempoCoyote = 0f;
    public float tiempoBufferSalto = 0f;
    public boolean estabaEnSuelo = false;

    // Estado exclusivamente visual: no cambia el cuerpo de colision ni la fisica.
    public float tiempoInteraccion = 0f;
    public boolean celebrando = false;
    public boolean derrotado = false;

    /** Evita contar dos veces la misma muerte (una por fisica y otra por mecanismos). */
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
        // La muerte se cuenta en Partida: si fue por un peligro, ahi se llama a
        // contarMuerte; si fue por caer al vacio, actualizarJugador la detecta.
        muerteContada = false;
        velX = 0f;
        velY = 0f;
        tiempoReaparicion = Configuracion.TIEMPO_REAPARICION;
    }

    /**
     * Devuelve true cuando la muerte actual ya fue registrada por quien la provoco.
     * Las caidas al vacio ocurren dentro de la fisica, por lo que llegan aqui sin contar.
     */
    public boolean tiempoReaparicionYaContado() {
        return muerteContada;
    }

    /** Permite que la partida registre una muerte producida dentro de la fisica. */
    public void marcarMuerteContada() {
        muerteContada = true;
    }

    public float velocidadMaxima() {
        return tipo == Tipo.FUEGO ? Configuracion.VELOCIDAD_FUEGO : Configuracion.VELOCIDAD_AGUA;
    }

    public float fuerzaSalto() {
        return tipo == Tipo.FUEGO ? Configuracion.FUERZA_SALTO_FUEGO : Configuracion.FUERZA_SALTO_AGUA;
    }

    /**
     * Actualiza un paso de fisica.
     * saltoPresionado es el instante en que se pulsa; saltoMantenido permite saltos mas altos.
     */
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

        // Salto con coyote time y buffer: se siente preciso sin ser complicado.
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

        // ---- Movimiento horizontal ---- (dt fijo, por eso es estable)
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

        // ---- Movimiento vertical ----
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
                // Resolver por el lado con menos solape (placas moviles, empujones, etc.).
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
        if (y < -90f) morir();
    }

    // Se ajusta al cargar un nivel de distinto tamano.
    public transient float anchoMundoActual = Configuracion.ANCHO_MUNDO;
}
