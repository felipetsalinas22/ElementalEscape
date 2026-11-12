package io.github.ElementalEscape;

import com.badlogic.gdx.graphics.Color;

/**
 * Todos los valores importantes del juego estan aca.
 * Si durante la exposicion quieren que un personaje salte mas alto,
 * cambien FUERZA_SALTO_FUEGO o FUERZA_SALTO_AGUA y listo.
 */
public final class Configuracion {
    private Configuracion() { }

    // ---------------- Fisica ----------------
    public static final float VELOCIDAD_PERSONAJE = 300f;
    public static final float FUERZA_SALTO = 820f;
    public static final float GRAVEDAD = 2200f;
    public static final float VELOCIDAD_CAIDA_MAXIMA = 900f;

    // Pequenas diferencias para que cada elemento se sienta distinto, pero equilibrado.
    public static final float VELOCIDAD_FUEGO = 300f;
    public static final float VELOCIDAD_AGUA = 312f;
    public static final float FUERZA_SALTO_FUEGO = 830f;
    public static final float FUERZA_SALTO_AGUA = 802f;

    public static final float ANCHO_PERSONAJE = 30f;
    public static final float ALTO_PERSONAJE = 46f;
    public static final float TIEMPO_COYOTE = 0.09f;       // salto justo despues de dejar el borde
    public static final float TIEMPO_BUFFER_SALTO = 0.10f; // salto pulsado un poco antes de caer
    public static final float CORTE_SALTO = 2.4f;          // soltar el salto acorta el salto
    public static final float PASO_FISICA = 1f / 120f;
    public static final int MAX_PASOS_POR_FOTOGRAMA = 8;

    public static final float VELOCIDAD_PLATAFORMA_MOVIL = 80f;
    public static final float VELOCIDAD_CAJA = 120f;
    public static final float ANCHO_CAJA = 40f;
    public static final float ALTO_CAJA = 40f;

    // ---------------- Dimensiones ----------------
    public static final float ANCHO_MUNDO = 960f;
    public static final float ALTO_MUNDO = 540f;
    public static final float ANCHO_MINIMO_NIVEL = 640f;
    public static final float ALTO_MINIMO_NIVEL = 400f;
    public static final float ANCHO_MAXIMO_NIVEL = 2400f;
    public static final float ALTO_MAXIMO_NIVEL = 900f;
    public static final float PASO_EDITOR = 10f;

    /**
     * Margen superior reservado para el HUD en pantalla. Ningun objeto jugable
     * importante (plataforma alcanzable, gema, mecanismo, puerta, salida, origen)
     * debe colocarse dentro de esta franja.
     */
    public static final float MARGEN_HUD_SUPERIOR = 76f;
    /** Margen inferior para textos/pistas del HUD. */
    public static final float MARGEN_HUD_INFERIOR = 48f;

    // Medidas fijas de los objetos del editor. No se editan desde la interfaz:
    // el editor coloca siempre objetos de estos tamanos y el JSON los normaliza.
    public static final float ANCHO_PLATAFORMA_EDITOR = 140f;
    public static final float ALTO_PLATAFORMA_EDITOR = 20f;
    public static final float ANCHO_MURO_EDITOR = 24f;
    public static final float ALTO_MURO_EDITOR = 180f;
    public static final float ANCHO_PELIGRO_EDITOR = 100f;
    public static final float ALTO_PELIGRO_EDITOR = 14f;
    public static final float ANCHO_PUERTA_EDITOR = 20f;
    public static final float ALTO_PUERTA_EDITOR = 250f;
    public static final float ANCHO_PUENTE_EDITOR = 160f;
    public static final float ALTO_PUENTE_EDITOR = 16f;
    public static final float ANCHO_MOVIL_EDITOR = 110f;
    public static final float ALTO_MOVIL_EDITOR = 18f;
    public static final float ANCHO_GEMA_EDITOR = 22f;
    public static final float ALTO_GEMA_EDITOR = 22f;
    public static final float ANCHO_BOTON_EDITOR = 56f;
    public static final float ALTO_BOTON_EDITOR = 12f;
    public static final float ANCHO_PALANCA_EDITOR = 28f;
    public static final float ALTO_PALANCA_EDITOR = 44f;
    public static final float ANCHO_CAJA_EDITOR = 40f;
    public static final float ALTO_CAJA_EDITOR = 40f;
    public static final float ANCHO_SALIDA_EDITOR = 60f;
    public static final float ALTO_SALIDA_EDITOR = 80f;
    public static final float ANCHO_ORIGEN_EDITOR = 30f;
    public static final float ALTO_ORIGEN_EDITOR = 46f;

    // ---------------- Puntuacion ----------------
    public static final int PUNTOS_COLECCIONABLE = 100;
    public static final int PUNTOS_MECANISMO = 25;
    public static final int PUNTOS_COMPLETAR_NIVEL = 300;
    public static final int PUNTOS_BONO_TODAS_GEMAS = 100;
    public static final int PENALIZACION_MUERTE = 50;
    public static final int BONO_TIEMPO_MAXIMO = 300;
    public static final float TIEMPO_LIMITE_NIVEL = 180f;
    public static final float SEGUNDOS_PUNTOS_TIEMPO = 2f;
    public static final int VIDAS_INICIALES = 3;
    public static final int TOTAL_NIVELES = 8;

    public static final float TIEMPO_SALIDA = 1.2f;
    public static final float TIEMPO_REAPARICION = 1.0f;
    public static final float DURACION_TRANSICION = 0.45f;

    // ---------------- Red ----------------
    public static final int PUERTO_PREDETERMINADO = 54555;
    public static final int VERSION_PROTOCOLO = 2;
    public static final float FRECUENCIA_ESTADO = 20f;
    public static final float FRECUENCIA_ENTRADA = 30f;
    public static final float TIEMPO_DESCONEXION = 6f;
    public static final String NOMBRE_JUEGO = "Elemental Escape";

    // ---------------- Colores ----------------
    public static final Color FUEGO = new Color(0.98f, 0.32f, 0.12f, 1f);
    public static final Color FUEGO_CLARO = new Color(1f, 0.72f, 0.22f, 1f);
    public static final Color AGUA = new Color(0.16f, 0.55f, 0.98f, 1f);
    public static final Color AGUA_CLARA = new Color(0.55f, 0.88f, 1f, 1f);
    public static final Color FONDO_OSCURO = new Color(0.035f, 0.045f, 0.085f, 1f);
    public static final Color PANEL = new Color(0.09f, 0.11f, 0.18f, 0.94f);
    public static final Color PANEL_CLARO = new Color(0.14f, 0.17f, 0.26f, 0.96f);
    public static final Color TEXTO = new Color(0.94f, 0.95f, 1f, 1f);
    public static final Color TEXTO_SUAVE = new Color(0.66f, 0.72f, 0.86f, 1f);
    public static final Color DORADO = new Color(1f, 0.83f, 0.35f, 1f);
    public static final Color VERDE = new Color(0.35f, 0.9f, 0.55f, 1f);
    public static final Color ROJO = new Color(1f, 0.32f, 0.35f, 1f);
    public static final Color VIOLETA = new Color(0.62f, 0.42f, 0.95f, 1f);
}
