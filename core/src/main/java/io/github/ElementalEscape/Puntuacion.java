package io.github.ElementalEscape;

/**
 * Puntuacion de UNA partida de nivel.
 * Reglas (todas en Configuracion):
 *   +100 por gema recogida (para quien la recoge)
 *   +25 por activar una placa/palanca por primera vez
 *   +300 por terminar el nivel (los dos)
 *   +bonus de tiempo (hasta 300 para cada uno)
 *   +100 a cada uno si se recogen todas las gemas
 *   -50 por muerte (nunca deja el puntaje del nivel por debajo de 0)
 */
public class Puntuacion {
    public int puntosFuego;
    public int puntosAgua;
    public int gemasFuego;
    public int gemasAgua;
    public int muertesFuego;
    public int muertesAgua;
    public int mecanismosFuego;
    public int mecanismosAgua;
    public int bonusFuego;
    public int bonusAgua;
    public int totalGemas;
    public float tiempo;
    public boolean todasLasGemas;
    private boolean finalizada;

    public Puntuacion() { }

    public Puntuacion(int totalGemas) {
        this.totalGemas = totalGemas;
    }

    /** Deja la puntuacion lista para una nueva partida del mismo nivel. */
    public void reiniciar(int totalGemas) {
        puntosFuego = 0;
        puntosAgua = 0;
        gemasFuego = 0;
        gemasAgua = 0;
        muertesFuego = 0;
        muertesAgua = 0;
        mecanismosFuego = 0;
        mecanismosAgua = 0;
        bonusFuego = 0;
        bonusAgua = 0;
        this.totalGemas = totalGemas;
        tiempo = 0f;
        todasLasGemas = false;
        finalizada = false;
    }

    public void sumarGema(Jugador.Tipo tipo) {
        if (tipo == Jugador.Tipo.FUEGO) {
            puntosFuego += Configuracion.PUNTOS_COLECCIONABLE;
            gemasFuego++;
        } else {
            puntosAgua += Configuracion.PUNTOS_COLECCIONABLE;
            gemasAgua++;
        }
    }

    public void sumarMecanismo(Jugador.Tipo tipo) {
        if (tipo == Jugador.Tipo.FUEGO) {
            puntosFuego += Configuracion.PUNTOS_MECANISMO;
            mecanismosFuego++;
        } else {
            puntosAgua += Configuracion.PUNTOS_MECANISMO;
            mecanismosAgua++;
        }
    }

    public void sumarMuerte(Jugador.Tipo tipo) {
        if (tipo == Jugador.Tipo.FUEGO) {
            muertesFuego++;
            puntosFuego = Math.max(0, puntosFuego - Configuracion.PENALIZACION_MUERTE);
        } else {
            muertesAgua++;
            puntosAgua = Math.max(0, puntosAgua - Configuracion.PENALIZACION_MUERTE);
        }
    }

    /** Se llama una sola vez, al terminar el nivel. */
    public ResultadoNivel terminar(float tiempoNivel) {
        this.tiempo = tiempoNivel;
        if (finalizada) return crearResultado();
        finalizada = true;

        float restante = Math.max(0f, Configuracion.TIEMPO_LIMITE_NIVEL - tiempoNivel);
        int bonoTiempo = Math.min(Configuracion.BONO_TIEMPO_MAXIMO,
            Math.round(restante * Configuracion.SEGUNDOS_PUNTOS_TIEMPO));
        bonusFuego = bonoTiempo;
        bonusAgua = bonoTiempo;

        todasLasGemas = totalGemas > 0 && gemasFuego + gemasAgua >= totalGemas;
        int bonoGemas = todasLasGemas ? Configuracion.PUNTOS_BONO_TODAS_GEMAS : 0;

        puntosFuego += Configuracion.PUNTOS_COMPLETAR_NIVEL + bonusFuego + bonoGemas;
        puntosAgua += Configuracion.PUNTOS_COMPLETAR_NIVEL + bonusAgua + bonoGemas;
        return crearResultado();
    }

    private ResultadoNivel crearResultado() {
        ResultadoNivel r = new ResultadoNivel();
        r.completado = true;
        r.puntosFuego = puntosFuego;
        r.puntosAgua = puntosAgua;
        r.gemasFuego = gemasFuego;
        r.gemasAgua = gemasAgua;
        r.muertesFuego = muertesFuego;
        r.muertesAgua = muertesAgua;
        r.mecanismosFuego = mecanismosFuego;
        r.mecanismosAgua = mecanismosAgua;
        r.bonusFuego = bonusFuego;
        r.bonusAgua = bonusAgua;
        r.tiempo = tiempo;
        r.todasGemas = todasLasGemas;
        r.ganador = puntosFuego > puntosAgua ? 1 : (puntosAgua > puntosFuego ? 2 : 0);
        return r;
    }

    /** Datos que se guardan en el progreso y se muestran en la pantalla de resultados. */
    public static class ResultadoNivel {
        public boolean completado;
        public int puntosFuego;
        public int puntosAgua;
        public int gemasFuego;
        public int gemasAgua;
        public int muertesFuego;
        public int muertesAgua;
        public int mecanismosFuego;
        public int mecanismosAgua;
        public int bonusFuego;
        public int bonusAgua;
        public float tiempo;
        public boolean todasGemas;
        public int ganador; // 0 empate, 1 fuego, 2 agua

        public ResultadoNivel() { }

        public int total() { return puntosFuego + puntosAgua; }
        public int diferencia() { return Math.abs(puntosFuego - puntosAgua); }
    }
}
