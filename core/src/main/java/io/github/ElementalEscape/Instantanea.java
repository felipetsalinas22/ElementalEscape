package io.github.ElementalEscape;

/**
 * "Foto" del estado de una partida que el anfitrion envia al invitado.
 * Incluye a los dos jugadores, mecanismos, cajas, plataformas moviles,
 * puntajes y resultado. Asi las dos computadoras ven exactamente lo mismo.
 */
public class Instantanea {
    public int version = Configuracion.VERSION_PROTOCOLO;
    public int nivel;
    public float tiempo;
    public float tiempoSalida;
    public boolean completado;
    public boolean derrota;
    public boolean pausado;
    public int vidasFuego;
    public int vidasAgua;

    public float[] jugadorX = new float[2];
    public float[] jugadorY = new float[2];
    public float[] jugadorVelX = new float[2];
    public float[] jugadorVelY = new float[2];
    public boolean[] jugadorVivo = new boolean[2];
    public boolean[] jugadorDerecha = new boolean[2];
    public float[] jugadorAnim = new float[2];
    public float[] jugadorAplastado = new float[2];

    public boolean[] gemasTomadas = new boolean[0];
    public boolean[] botonesActivos = new boolean[0];
    public boolean[] palancasActivas = new boolean[0];
    public boolean[] puertasAbiertas = new boolean[0];
    public float[] puertasTiempo = new float[0];
    public float[] cajasX = new float[0];
    public float[] cajasY = new float[0];
    public float[] movilesX = new float[0];
    public float[] movilesY = new float[0];

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
    public boolean todasLasGemas;
    public float tiempoPuntuacion;
    public Puntuacion.ResultadoNivel resultado = new Puntuacion.ResultadoNivel();

    // Progreso acumulado del anfitrion, para que el ranking sea igual en las dos PCs.
    public int totalesFuego;
    public int totalesAgua;
    public int nivelesCompletados;
    public int nivelesGanadosFuego;
    public int nivelesGanadosAgua;

    public String mensaje = "";
    /** Eventos de sonido/particulas ocurridos desde la foto anterior. */
    public String[] eventos = new String[0];
}
