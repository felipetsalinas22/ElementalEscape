package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.Arrays;

/**
 * Datos de un nivel. No guarda estado de la partida: eso lo hace Partida.
 * Asi el mismo nivel se puede jugar, editar, guardar en JSON y sincronizar por red
 * sin que queden "gemas tomadas" o "puertas abiertas" pegadas en los datos.
 */
public class Nivel {
    public static final int FORMATO_ACTUAL = 1;

    // ---------- Datos generales ----------
    public int formato = FORMATO_ACTUAL;
    public String nombre = "Nuevo nivel";
    public float ancho = Configuracion.ANCHO_MUNDO;
    public float alto = Configuracion.ALTO_MUNDO;
    public String tema = "TEMPLO";
    public String pista = "Lleguen los dos a sus portales.";
    public String autor = "Elemental Escape";

    public float origenFuegoX = 80f;
    public float origenFuegoY = 30f;
    public float origenAguaX = 130f;
    public float origenAguaY = 30f;
    public Rectangle salidaFuego = new Rectangle(820f, 30f, 60f, 80f);
    public Rectangle salidaAgua = new Rectangle(890f, 30f, 60f, 80f);

    // ---------- Objetos ----------
    public Plataforma[] plataformas = new Plataforma[0];
    public Peligro[] peligros = new Peligro[0];
    public Gema[] gemas = new Gema[0];
    public Boton[] botones = new Boton[0];
    public Palanca[] palancas = new Palanca[0];
    public Puerta[] puertas = new Puerta[0];
    public PlataformaMovil[] moviles = new PlataformaMovil[0];
    public Caja[] cajas = new Caja[0];

    public Nivel() { }

    // =========================================================
    //                    CLASES DE OBJETOS
    // =========================================================

    /** Plataforma o pared solida. El estilo solo cambia el dibujo. */
    public static class Plataforma {
        public Rectangle rect = new Rectangle();
        public String estilo = "PIEDRA";
        /** Los bordes automaticos del escenario conservan su tamano completo. */
        public boolean limite = false;

        public Plataforma() { }

        public Plataforma(float x, float y, float ancho, float alto, String estilo) {
            rect.set(x, y, ancho, alto);
            this.estilo = estilo;
        }
    }

    /** Zona peligrosa. lava=true mata al agua; lava=false mata al fuego. */
    public static class Peligro {
        public Rectangle rect = new Rectangle();
        public boolean lava = true;

        public Peligro() { }

        public Peligro(float x, float y, float ancho, float alto, boolean lava) {
            rect.set(x, y, ancho, alto);
            this.lava = lava;
        }
    }

    /** Coleccionable. Si es secreta vale un poco mas y brilla distinto. */
    public static class Gema {
        public Rectangle rect = new Rectangle();
        public boolean secreta = false;

        public Gema() { }

        public Gema(float x, float y) {
            rect.set(x, y, 22f, 22f);
        }
    }

    /**
     * Placa de presion: se mantiene activa mientras alguien (o una caja) la pisa.
     * elemento: AMBOS, FUEGO o AGUA.
     */
    public static class Boton {
        public Rectangle rect = new Rectangle(0f, 0f, 56f, 12f);
        public int id = 1;
        public String elemento = "AMBOS";

        public Boton() { }

        public Boton(float x, float y, int id, String elemento) {
            rect.set(x, y, 56f, 12f);
            this.id = id;
            this.elemento = elemento;
        }
    }

    /**
     * Palanca de un solo uso. orden > 0 la vuelve parte de una secuencia:
     * solo se puede activar cuando las anteriores del mismo grupo ya estan activas.
     */
    public static class Palanca {
        public Rectangle rect = new Rectangle(0f, 0f, 28f, 44f);
        public int id = 1;
        public String elemento = "AMBOS";
        public int orden = 0;
        public int grupo = 1;

        public Palanca() { }

        public Palanca(float x, float y, int id, String elemento) {
            rect.set(x, y, 28f, 44f);
            this.id = id;
            this.elemento = elemento;
        }
    }

    /**
     * Puerta: bloquea el paso mientras esta cerrada.
     * modo=TODAS exige que todos los controles de su id esten activos.
     * duracion > 0 la mantiene abierta unos segundos al activarse.
     * puente=true hace lo contrario: es solida cuando esta activa (puente de energia).
     */
    public static class Puerta {
        public Rectangle rect = new Rectangle(0f, 0f, 18f, 250f);
        public int id = 1;
        public String modo = "ALGUNA";
        public float duracion = 0f;
        public boolean puente = false;

        public Puerta() { }

        public Puerta(float x, float y, float ancho, float alto, int id, String modo, float duracion, boolean puente) {
            rect.set(x, y, ancho, alto);
            this.id = id;
            this.modo = modo;
            this.duracion = duracion;
            this.puente = puente;
        }
    }

    /** Plataforma que va y vuelve entre su posicion inicial y (finX, finY). */
    public static class PlataformaMovil {
        public Rectangle rect = new Rectangle(0f, 0f, 100f, 18f);
        public float finX = 300f;
        public float finY = 0f;
        public float velocidad = Configuracion.VELOCIDAD_PLATAFORMA_MOVIL;
        public int id = 0; // 0 = siempre se mueve; >0 = necesita el control de ese id

        public PlataformaMovil() { }

        public PlataformaMovil(float x, float y, float ancho, float alto, float finX, float finY,
                               float velocidad, int id) {
            rect.set(x, y, ancho, alto);
            this.finX = finX;
            this.finY = finY;
            this.velocidad = velocidad;
            this.id = id;
        }
    }

    /** Caja empujable. Las pesadas necesitan que los dos personajes empujen. */
    public static class Caja {
        public Rectangle rect = new Rectangle(0f, 0f, Configuracion.ANCHO_CAJA, Configuracion.ALTO_CAJA);
        public boolean pesada = false;

        public Caja() { }

        public Caja(float x, float y, boolean pesada) {
            rect.set(x, y, Configuracion.ANCHO_CAJA, Configuracion.ALTO_CAJA);
            this.pesada = pesada;
        }
    }

    // =========================================================
    //                    AYUDAS
    // =========================================================

    /** Corrige datos incompletos o invalidos despues de cargar un JSON. */
    public void normalizar() {
        if (nombre == null || nombre.trim().isEmpty()) nombre = "Nuevo nivel";
        if (tema == null || tema.trim().isEmpty()) tema = "TEMPLO";
        if (pista == null) pista = "";
        if (autor == null) autor = "";
        ancho = limitar(ancho, Configuracion.ANCHO_MINIMO_NIVEL, Configuracion.ANCHO_MAXIMO_NIVEL);
        alto = limitar(alto, Configuracion.ALTO_MINIMO_NIVEL, Configuracion.ALTO_MAXIMO_NIVEL);
        if (salidaFuego == null) salidaFuego = new Rectangle(ancho - 140f, 30f, 60f, 80f);
        if (salidaAgua == null) salidaAgua = new Rectangle(ancho - 70f, 30f, 60f, 80f);
        if (plataformas == null) plataformas = new Plataforma[0];
        if (peligros == null) peligros = new Peligro[0];
        if (gemas == null) gemas = new Gema[0];
        if (botones == null) botones = new Boton[0];
        if (palancas == null) palancas = new Palanca[0];
        if (puertas == null) puertas = new Puerta[0];
        if (moviles == null) moviles = new PlataformaMovil[0];
        if (cajas == null) cajas = new Caja[0];
        formato = FORMATO_ACTUAL;
    }

    private static float limitar(float valor, float minimo, float maximo) {
        if (Float.isNaN(valor) || Float.isInfinite(valor)) return minimo;
        return Math.max(minimo, Math.min(maximo, valor));
    }

    /**
     * Normaliza un nivel personalizado con las medidas fijas del editor.
     * Los bordes automaticos del escenario son la unica excepcion: conservan
     * el ancho/alto del nivel para que el recinto siga cerrado.
     *
     * Las medidas no se editan desde la interfaz; esta funcion las aplica al
     * cargar y al guardar para que el JSON tambien quede siempre normalizado.
     */
    public void normalizarEditor() {
        normalizar();

        for (Plataforma p : plataformas) {
            if (p == null || p.rect == null) continue;
            if (p.limite || esBorde(p)) {
                p.limite = true;
                limitarRect(p.rect);
                continue;
            }
            if ("MURO".equalsIgnoreCase(p.estilo)) {
                p.rect.setSize(Configuracion.ANCHO_MURO_EDITOR, Configuracion.ALTO_MURO_EDITOR);
            } else {
                p.rect.setSize(Configuracion.ANCHO_PLATAFORMA_EDITOR, Configuracion.ALTO_PLATAFORMA_EDITOR);
            }
            limitarRect(p.rect);
        }
        for (Peligro p : peligros) {
            if (p == null || p.rect == null) continue;
            p.rect.setSize(Configuracion.ANCHO_PELIGRO_EDITOR, Configuracion.ALTO_PELIGRO_EDITOR);
            limitarRect(p.rect);
        }
        for (Gema g : gemas) {
            if (g == null || g.rect == null) continue;
            g.rect.setSize(Configuracion.ANCHO_GEMA_EDITOR, Configuracion.ALTO_GEMA_EDITOR);
            limitarRect(g.rect);
        }
        for (Boton b : botones) {
            if (b == null || b.rect == null) continue;
            b.rect.setSize(Configuracion.ANCHO_BOTON_EDITOR, Configuracion.ALTO_BOTON_EDITOR);
            limitarRect(b.rect);
        }
        for (Palanca p : palancas) {
            if (p == null || p.rect == null) continue;
            p.rect.setSize(Configuracion.ANCHO_PALANCA_EDITOR, Configuracion.ALTO_PALANCA_EDITOR);
            limitarRect(p.rect);
        }
        for (Puerta p : puertas) {
            if (p == null || p.rect == null) continue;
            if (p.puente) {
                p.rect.setSize(Configuracion.ANCHO_PUENTE_EDITOR, Configuracion.ALTO_PUENTE_EDITOR);
            } else {
                p.rect.setSize(Configuracion.ANCHO_PUERTA_EDITOR, Configuracion.ALTO_PUERTA_EDITOR);
            }
            limitarRect(p.rect);
        }
        for (PlataformaMovil m : moviles) {
            if (m == null || m.rect == null) continue;
            m.rect.setSize(Configuracion.ANCHO_MOVIL_EDITOR, Configuracion.ALTO_MOVIL_EDITOR);
            limitarRect(m.rect);
            m.finX = limitar(m.finX, 10f, Math.max(10f, ancho - 10f));
            m.finY = limitar(m.finY, 10f, Math.max(10f, alto - 10f));
        }
        for (Caja c : cajas) {
            if (c == null || c.rect == null) continue;
            c.rect.setSize(Configuracion.ANCHO_CAJA_EDITOR, Configuracion.ALTO_CAJA_EDITOR);
            limitarRect(c.rect);
        }

        salidaFuego.setSize(Configuracion.ANCHO_SALIDA_EDITOR, Configuracion.ALTO_SALIDA_EDITOR);
        salidaAgua.setSize(Configuracion.ANCHO_SALIDA_EDITOR, Configuracion.ALTO_SALIDA_EDITOR);
        limitarRect(salidaFuego);
        limitarRect(salidaAgua);
        origenFuegoX = limitar(origenFuegoX, 0f, Math.max(0f, ancho - Configuracion.ANCHO_ORIGEN_EDITOR));
        origenFuegoY = limitar(origenFuegoY, 0f, Math.max(0f, alto - Configuracion.ALTO_ORIGEN_EDITOR));
        origenAguaX = limitar(origenAguaX, 0f, Math.max(0f, ancho - Configuracion.ANCHO_ORIGEN_EDITOR));
        origenAguaY = limitar(origenAguaY, 0f, Math.max(0f, alto - Configuracion.ALTO_ORIGEN_EDITOR));
    }

    private void limitarRect(Rectangle r) {
        r.width = Math.max(1f, r.width);
        r.height = Math.max(1f, r.height);
        r.x = Math.max(0f, Math.min(r.x, Math.max(0f, ancho - r.width)));
        r.y = Math.max(0f, Math.min(r.y, Math.max(0f, alto - r.height)));
    }

    private boolean esBorde(Plataforma p) {
        Rectangle r = p.rect;
        float e = 0.6f;
        boolean suelo = Math.abs(r.x) < e && Math.abs(r.y) < e
            && Math.abs(r.width - ancho) < e && Math.abs(r.height - 30f) < e;
        boolean izquierda = Math.abs(r.x) < e && Math.abs(r.y) < e
            && Math.abs(r.width - 18f) < e && Math.abs(r.height - alto) < e;
        boolean derecha = Math.abs(r.x + r.width - ancho) < e && Math.abs(r.y) < e
            && Math.abs(r.width - 18f) < e && Math.abs(r.height - alto) < e;
        boolean techo = Math.abs(r.x) < e && Math.abs(r.y + r.height - alto) < e
            && Math.abs(r.width - ancho) < e && Math.abs(r.height - 16f) < e;
        return suelo || izquierda || derecha || techo;
    }

    public void agregar(Plataforma objeto) { plataformas = agregar(plataformas, objeto); }
    public void agregar(Peligro objeto) { peligros = agregar(peligros, objeto); }
    public void agregar(Gema objeto) { gemas = agregar(gemas, objeto); }
    public void agregar(Boton objeto) { botones = agregar(botones, objeto); }
    public void agregar(Palanca objeto) { palancas = agregar(palancas, objeto); }
    public void agregar(Puerta objeto) { puertas = agregar(puertas, objeto); }
    public void agregar(PlataformaMovil objeto) { moviles = agregar(moviles, objeto); }
    public void agregar(Caja objeto) { cajas = agregar(cajas, objeto); }

    private static <T> T[] agregar(T[] arreglo, T objeto) {
        T[] nuevo = Arrays.copyOf(arreglo, arreglo.length + 1);
        nuevo[arreglo.length] = objeto;
        return nuevo;
    }

    public boolean esDeFuego(String elemento) { return "FUEGO".equalsIgnoreCase(elemento); }
    public boolean esDeAgua(String elemento) { return "AGUA".equalsIgnoreCase(elemento); }
    public boolean esDeAmbos(String elemento) { return elemento == null || elemento.equalsIgnoreCase("AMBOS"); }

    public static String nombreTema(String tema) {
        if ("CAVERNA".equals(tema)) return "Caverna de cristal";
        if ("CIELO".equals(tema)) return "Templo en las nubes";
        if ("HIELO".equals(tema)) return "Caverna helada";
        if ("VOLCAN".equals(tema)) return "Corazon del volcan";
        if ("RUINAS".equals(tema)) return "Ruinas olvidadas";
        if ("LABORATORIO".equals(tema)) return "Laboratorio elemental";
        if ("BOSQUE".equals(tema)) return "Bosque antiguo";
        if ("COSMOS".equals(tema)) return "Frontera cosmica";
        if ("FINAL".equals(tema)) return "Santuario de los elementos";
        return "Templo elemental";
    }
}
