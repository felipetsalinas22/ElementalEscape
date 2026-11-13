package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Editor de niveles completo, dibujado con interfaz inmediata.
 *
 * - Click izquierdo: seleccionar, colocar o arrastrar (segun la herramienta).
 * - Click derecho / central: mover la camara.
 * - Rueda no disponible: usa + y - para zoom, F para encuadrar.
 * - Ctrl+Z / Ctrl+Y: deshacer y rehacer.
 * - Supr: borrar el objeto seleccionado.
 *
 * Los cambios se guardan como JSON en la carpeta externa del usuario.
 */
public class PantallaEditor extends PantallaBase {
    private enum Herramienta {
        SELECCION, PLATAFORMA, MURO, LAVA, AGUA, GEMA, PLACA, PALANCA, PUERTA,
        PUENTE, MOVIL, CAJA, ORIGEN_FUEGO, ORIGEN_AGUA, SALIDA_FUEGO, SALIDA_AGUA
    }

    private enum TipoSeleccion {
        NINGUNO, PLATAFORMA, PELIGRO, GEMA, BOTON, PALANCA, PUERTA, MOVIL, CAJA,
        ORIGEN_FUEGO, ORIGEN_AGUA, SALIDA_FUEGO, SALIDA_AGUA
    }

    private interface Ajuste { void cambiar(float delta); }
    private interface CambioTexto { void aplicar(String texto); }

    private static final String[] TEMAS = {
        "TEMPLO", "CAVERNA", "CIELO", "HIELO", "VOLCAN", "RUINAS",
        "LABORATORIO", "BOSQUE", "COSMOS", "FINAL"
    };

    private Nivel nivel;
    private final Nivel nivelInicial;
    private String archivoRestaurar;
    private boolean sucioRestaurar;
    private List<String> historialRestaurar;
    private int posHistorialRestaurar = -1;
    private DibujanteJuego dibujante;
    private OrthographicCamera camara;
    private Json json;
    private float tiempo = 0f;
    private float zoom = 1f;
    private float camX = 480f;
    private float camY = 270f;
    private float mouseAnteriorX;
    private float mouseAnteriorY;
    private float mundoX;
    private float mundoY;

    private static final float HUD_H = Configuracion.ALTURA_HUD;
    private static final float VISTA_W = Configuracion.ANCHO_VISTA_JUEGO;
    private static final float VISTA_H = Configuracion.ALTO_VISTA_JUEGO;

    private Herramienta herramienta = Herramienta.SELECCION;
    private TipoSeleccion tipoSel = TipoSeleccion.NINGUNO;
    private int indiceSel = -1;

    private boolean arrastrandoCamara = false;
    private boolean arrastrandoObjeto = false;
    private boolean objetoMovido = false;
    private float ultimoMundoX;
    private float ultimoMundoY;

    private final List<String> historial = new ArrayList<>();
    private int posHistorial = 0;
    private String archivoActual = "";
    private boolean sucio = false;

    private String estado = "Editor listo. Elige una herramienta o selecciona un objeto.";
    private String confirmar = "";
    private float pendienteAncho;
    private float pendienteAlto;
    private int pendientesFueraCount;
    private boolean selectorAbierto = false;
    private boolean esNivelHistoria = false;
    private List<GestorNiveles.Entrada> listaNiveles = new ArrayList<>();
    private int paginaSelector = 0;
    private int pestanaSelector = 0; // 0 = Mis niveles, 1 = Historia

    public PantallaEditor(Juego juego, Nivel nivelInicial) {
        super(juego);
        this.nivelInicial = nivelInicial;
        this.nivel = nivelInicial;
    }

    private PantallaEditor(Juego juego, Nivel nivelInicial, String archivo, boolean sucio,
                           List<String> historial, int posHistorial) {
        super(juego);
        this.nivelInicial = nivelInicial;
        this.nivel = nivelInicial;
        this.archivoRestaurar = archivo;
        this.sucioRestaurar = sucio;
        this.historialRestaurar = historial == null
            ? null : new ArrayList<>(historial);
        this.posHistorialRestaurar = posHistorial;
    }

    /** Vuelve al editor conservando archivo, cambios y historial tras una prueba. */
    public static PantallaEditor desdePrueba(Juego juego, Nivel nivel, String archivo,
                                             boolean sucio, List<String> historial, int posHistorial) {
        return new PantallaEditor(juego, nivel, archivo, sucio, historial, posHistorial);
    }

    @Override
    protected void alMostrar() {
        dibujante = new DibujanteJuego(lote, fuente);
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
        camara = new OrthographicCamera();
        camara.setToOrtho(false, 960f, 540f);

        if (nivelInicial == null) {
            nivel = nivelNuevo();
            archivoActual = "";
            esNivelHistoria = false;
        } else {
            nivel = nivelInicial;
            nivel.normalizarEditor();
            archivoActual = archivoRestaurar != null ? archivoRestaurar : buscarArchivo(nivel);
            esNivelHistoria = archivoActual.isEmpty() && nivelInicial != null;
        }
        encuadrarNivel();
        if (historialRestaurar != null && !historialRestaurar.isEmpty()) {
            historial.clear();
            historial.addAll(historialRestaurar);
            posHistorial = Math.max(0, Math.min(posHistorialRestaurar, historial.size() - 1));
            sucio = sucioRestaurar;
        } else {
            reiniciarHistorial();
        }
        estado = archivoActual.isEmpty()
            ? "Nivel nuevo. Guardalo cuando este listo."
            : "Editando " + archivoActual + ". Guarda con el boton GUARDAR.";
    }

    private Nivel nivelNuevo() {
        Nivel n = new Nivel();
        n.nombre = "Nuevo nivel";
        n.autor = "Tu nombre";
        n.tema = "TEMPLO";
        n.pista = "Lleguen los dos a sus portales.";
        n.ancho = 960f;
        n.alto = 540f;
        n.origenFuegoX = 70f;
        n.origenAguaX = 120f;
        n.origenFuegoY = 30f;
        n.origenAguaY = 30f;
        n.salidaFuego.set(810f, 30f, Configuracion.ANCHO_SALIDA_EDITOR, Configuracion.ALTO_SALIDA_EDITOR);
        n.salidaAgua.set(880f, 30f, Configuracion.ANCHO_SALIDA_EDITOR, Configuracion.ALTO_SALIDA_EDITOR);
        Nivel.Plataforma piso = new Nivel.Plataforma(0f, 0f, n.ancho, 30f, "PIEDRA");
        piso.limite = true;
        n.agregar(piso);
        Nivel.Plataforma paredIzquierda = new Nivel.Plataforma(0f, 0f, 18f, n.alto, "MURO");
        paredIzquierda.limite = true;
        n.agregar(paredIzquierda);
        Nivel.Plataforma paredDerecha = new Nivel.Plataforma(n.ancho - 18f, 0f, 18f, n.alto, "MURO");
        paredDerecha.limite = true;
        n.agregar(paredDerecha);
        Nivel.Plataforma techo = new Nivel.Plataforma(0f, n.alto - 16f, n.ancho, 16f, "MURO");
        techo.limite = true;
        n.agregar(techo);
        n.agregar(new Nivel.Plataforma(360f, 160f,
            Configuracion.ANCHO_PLATAFORMA_EDITOR, Configuracion.ALTO_PLATAFORMA_EDITOR, "PIEDRA"));
        n.agregar(new Nivel.Gema(445f, 200f));
        return n;
    }

    private String buscarArchivo(Nivel n) {
        if (n == null) return "";
        for (GestorNiveles.Entrada e : juego.getGestorNiveles().listar()) {
            if (e.nivel != null && e.nivel.nombre != null
                && e.nivel.nombre.trim().equalsIgnoreCase(n.nombre.trim())) {
                return e.archivo;
            }
        }
        return "";
    }

    // =========================================================
    //                     ACTUALIZACION
    // =========================================================

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        float localY = MathUtils.clamp(ui.mouseY, 0f, VISTA_H);
        mundoX = camX + (ui.mouseX - VISTA_W / 2f) * zoom;
        mundoY = camY + (localY - VISTA_H / 2f) * zoom;

        if (selectorAbierto || !confirmar.isEmpty()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                selectorAbierto = false;
                confirmar = "";
            }
            mouseAnteriorX = ui.mouseX;
            mouseAnteriorY = ui.mouseY;
            return;
        }

        actualizarTeclado();
        actualizarRaton();
        mouseAnteriorX = ui.mouseX;
        mouseAnteriorY = ui.mouseY;
    }

    private void actualizarTeclado() {
        boolean control = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (control && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            deshacer();
            return;
        }
        if (control && (Gdx.input.isKeyJustPressed(Input.Keys.Y)
            || Gdx.input.isKeyJustPressed(Input.Keys.R))) {
            rehacer();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            pedirSalir();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)
            || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            eliminarSeleccion();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) encuadrarNivel();
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) cambiarZoom(1.15f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) cambiarZoom(1f / 1.15f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            estado = "La cuadricula esta siempre visible para ayudarte a alinear.";
        }

        // Flechas: mueven el objeto seleccionado o la camara.
        float paso = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
            ? 50f : Configuracion.PASO_EDITOR;
        if (tipoSel != TipoSeleccion.NINGUNO) {
            boolean movido = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) movido = moverSeleccion(-paso, 0f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) movido = moverSeleccion(paso, 0f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) movido = moverSeleccion(0f, paso);
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) movido = moverSeleccion(0f, -paso);
            if (movido) confirmarCambio();
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) camX -= paso;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camX += paso;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) camY += paso;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) camY -= paso;
        }
    }

    private void actualizarRaton() {
        boolean enCanvas = ui.dentro(150f, 60f, 590f, 440f);

        // Camara con boton derecho o central.
        if (enCanvas && (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
            || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE))) {
            arrastrandoCamara = true;
            mouseAnteriorX = ui.mouseX;
            mouseAnteriorY = ui.mouseY;
        }
        if (arrastrandoCamara) {
            boolean sigue = Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
                || Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
            if (sigue) {
                camX -= (ui.mouseX - mouseAnteriorX) * zoom;
                camY += (ui.mouseY - mouseAnteriorY) * zoom;
                limitarCamara();
            } else {
                arrastrandoCamara = false;
            }
            mouseAnteriorX = ui.mouseX;
            mouseAnteriorY = ui.mouseY;
            return;
        }

        if (ui.clic && enCanvas) {
            ui.clic = false;
            if (herramienta == Herramienta.SELECCION) {
                seleccionarEn(mundoX, mundoY);
                if (tipoSel != TipoSeleccion.NINGUNO) {
                    arrastrandoObjeto = true;
                    objetoMovido = false;
                    ultimoMundoX = mundoX;
                    ultimoMundoY = mundoY;
                }
            } else {
                crearObjeto(herramienta, mundoX, mundoY);
            }
        }

        if (arrastrandoObjeto) {
            if (ui.clicMantenido && (Math.abs(mundoX - ultimoMundoX) > 0.01f
                || Math.abs(mundoY - ultimoMundoY) > 0.01f)) {
                if (moverSeleccion(mundoX - ultimoMundoX, mundoY - ultimoMundoY)) {
                    objetoMovido = true;
                }
                ultimoMundoX = mundoX;
                ultimoMundoY = mundoY;
            }
            if (!ui.clicMantenido) {
                arrastrandoObjeto = false;
                if (objetoMovido) confirmarCambio();
            }
        }
    }

    private void limitarCamara() {
        float hx = VISTA_W * zoom / 2f;
        float hy = VISTA_H * zoom / 2f;
        if (nivel.ancho <= hx * 2f) camX = nivel.ancho / 2f;
        else camX = MathUtils.clamp(camX, hx, nivel.ancho - hx);
        if (nivel.alto <= hy * 2f) camY = nivel.alto / 2f;
        else camY = MathUtils.clamp(camY, hy, nivel.alto - hy);
    }

    private void cambiarZoom(float factor) {
        zoom = MathUtils.clamp(zoom * factor, 0.30f, 3f);
        estado = "Zoom " + Math.round(100f / zoom) + "%";
    }

    private void encuadrarNivel() {
        float ajuste = Math.min(VISTA_W / Math.max(1f, nivel.ancho),
            VISTA_H / Math.max(1f, nivel.alto));
        zoom = MathUtils.clamp(ajuste, 0.25f, 3f);
        camX = nivel.ancho / 2f;
        camY = nivel.alto / 2f;
        limitarCamara();
        estado = "Vista general. F encuadra, +/- ajusta el zoom.";
    }

    // =========================================================
    //                    SELECCION Y EDICION
    // =========================================================

    private void seleccionarEn(float x, float y) {
        tipoSel = TipoSeleccion.NINGUNO;
        indiceSel = -1;
        Rectangle punto = new Rectangle(x - 3f, y - 3f, 6f, 6f);

        if (contiene(nivel.salidaFuego, punto)) { tipoSel = TipoSeleccion.SALIDA_FUEGO; return; }
        if (contiene(nivel.salidaAgua, punto)) { tipoSel = TipoSeleccion.SALIDA_AGUA; return; }
        if (cercaOrigen(nivel.origenFuegoX, nivel.origenFuegoY, punto)) {
            tipoSel = TipoSeleccion.ORIGEN_FUEGO;
            return;
        }
        if (cercaOrigen(nivel.origenAguaX, nivel.origenAguaY, punto)) {
            tipoSel = TipoSeleccion.ORIGEN_AGUA;
            return;
        }

        for (int i = nivel.palancas.length - 1; i >= 0; i--) {
            if (contiene(nivel.palancas[i].rect, punto)) {
                tipoSel = TipoSeleccion.PALANCA; indiceSel = i; return;
            }
        }
        for (int i = nivel.botones.length - 1; i >= 0; i--) {
            if (contiene(nivel.botones[i].rect, punto)) {
                tipoSel = TipoSeleccion.BOTON; indiceSel = i; return;
            }
        }
        for (int i = nivel.gemas.length - 1; i >= 0; i--) {
            if (contiene(nivel.gemas[i].rect, punto)) {
                tipoSel = TipoSeleccion.GEMA; indiceSel = i; return;
            }
        }
        for (int i = nivel.puertas.length - 1; i >= 0; i--) {
            if (contiene(nivel.puertas[i].rect, punto)) {
                tipoSel = TipoSeleccion.PUERTA; indiceSel = i; return;
            }
        }
        for (int i = nivel.cajas.length - 1; i >= 0; i--) {
            if (contiene(nivel.cajas[i].rect, punto)) {
                tipoSel = TipoSeleccion.CAJA; indiceSel = i; return;
            }
        }
        for (int i = nivel.moviles.length - 1; i >= 0; i--) {
            if (contiene(nivel.moviles[i].rect, punto)) {
                tipoSel = TipoSeleccion.MOVIL; indiceSel = i; return;
            }
        }
        for (int i = nivel.peligros.length - 1; i >= 0; i--) {
            if (contiene(nivel.peligros[i].rect, punto)) {
                tipoSel = TipoSeleccion.PELIGRO; indiceSel = i; return;
            }
        }
        for (int i = nivel.plataformas.length - 1; i >= 0; i--) {
            if (contiene(nivel.plataformas[i].rect, punto)) {
                tipoSel = TipoSeleccion.PLATAFORMA; indiceSel = i; return;
            }
        }
    }

    private boolean contiene(Rectangle r, Rectangle punto) {
        if (r == null) return false;
        return r.x - 3f <= punto.x && r.x + r.width + 3f >= punto.x
            && r.y - 3f <= punto.y && r.y + r.height + 3f >= punto.y;
    }

    private boolean cercaOrigen(float x, float y, Rectangle punto) {
        return punto.overlaps(new Rectangle(x - 4f, y - 4f,
            Configuracion.ANCHO_PERSONAJE + 8f, Configuracion.ALTO_PERSONAJE + 8f));
    }

    private boolean moverSeleccion(float dx, float dy) {
        if (tipoSel == TipoSeleccion.PLATAFORMA && nivel.plataformas[indiceSel].limite) {
            estado = "Los bordes del escenario son fijos.";
            return false;
        }
        switch (tipoSel) {
            case PLATAFORMA: moverRect(nivel.plataformas[indiceSel].rect, dx, dy); break;
            case PELIGRO: moverRect(nivel.peligros[indiceSel].rect, dx, dy); break;
            case GEMA: moverRect(nivel.gemas[indiceSel].rect, dx, dy); break;
            case BOTON: moverRect(nivel.botones[indiceSel].rect, dx, dy); break;
            case PALANCA: moverRect(nivel.palancas[indiceSel].rect, dx, dy); break;
            case PUERTA: moverRect(nivel.puertas[indiceSel].rect, dx, dy); break;
            case CAJA: moverRect(nivel.cajas[indiceSel].rect, dx, dy); break;
            case MOVIL: {
                Nivel.PlataformaMovil m = nivel.moviles[indiceSel];
                moverRect(m.rect, dx, dy);
                m.finX = MathUtils.clamp(m.finX + dx, 10f, nivel.ancho - 10f);
                m.finY = MathUtils.clamp(m.finY + dy, 10f, nivel.alto - 10f);
                break;
            }
            case ORIGEN_FUEGO:
                nivel.origenFuegoX = MathUtils.clamp(nivel.origenFuegoX + dx, 0f,
                    nivel.ancho - Configuracion.ANCHO_PERSONAJE);
                nivel.origenFuegoY = MathUtils.clamp(nivel.origenFuegoY + dy, 0f,
                    nivel.alto - Configuracion.ALTO_PERSONAJE);
                break;
            case ORIGEN_AGUA:
                nivel.origenAguaX = MathUtils.clamp(nivel.origenAguaX + dx, 0f,
                    nivel.ancho - Configuracion.ANCHO_PERSONAJE);
                nivel.origenAguaY = MathUtils.clamp(nivel.origenAguaY + dy, 0f,
                    nivel.alto - Configuracion.ALTO_PERSONAJE);
                break;
            case SALIDA_FUEGO: moverRect(nivel.salidaFuego, dx, dy); break;
            case SALIDA_AGUA: moverRect(nivel.salidaAgua, dx, dy); break;
            default: return false;
        }
        sucio = true;
        return true;
    }

    private void moverRect(Rectangle r, float dx, float dy) {
        r.x = MathUtils.clamp(r.x + dx, 0f, Math.max(0f, nivel.ancho - r.width));
        r.y = MathUtils.clamp(r.y + dy, 0f, Math.max(0f, nivel.alto - r.height));
    }

    private float redondear(float valor) {
        return Math.round(valor / Configuracion.PASO_EDITOR) * Configuracion.PASO_EDITOR;
    }

    private void crearObjeto(Herramienta h, float x, float y) {
        Rectangle r = rectHerramienta(h, x, y);
        if (r == null) return;
        r.x = redondear(r.x);
        r.y = redondear(r.y);
        colocarDentro(r);

        switch (h) {
            case PLATAFORMA:
                nivel.agregar(new Nivel.Plataforma(r.x, r.y, r.width, r.height, "PIEDRA"));
                tipoSel = TipoSeleccion.PLATAFORMA;
                indiceSel = nivel.plataformas.length - 1;
                break;
            case MURO:
                nivel.agregar(new Nivel.Plataforma(r.x, r.y, r.width, r.height, "MURO"));
                tipoSel = TipoSeleccion.PLATAFORMA;
                indiceSel = nivel.plataformas.length - 1;
                break;
            case LAVA:
                nivel.agregar(new Nivel.Peligro(r.x, r.y, r.width, r.height, true));
                tipoSel = TipoSeleccion.PELIGRO;
                indiceSel = nivel.peligros.length - 1;
                break;
            case AGUA:
                nivel.agregar(new Nivel.Peligro(r.x, r.y, r.width, r.height, false));
                tipoSel = TipoSeleccion.PELIGRO;
                indiceSel = nivel.peligros.length - 1;
                break;
            case GEMA:
                nivel.agregar(new Nivel.Gema(r.x, r.y));
                tipoSel = TipoSeleccion.GEMA;
                indiceSel = nivel.gemas.length - 1;
                break;
            case PLACA:
                nivel.agregar(new Nivel.Boton(r.x, r.y, idParaControlNuevo(), "AMBOS"));
                tipoSel = TipoSeleccion.BOTON;
                indiceSel = nivel.botones.length - 1;
                break;
            case PALANCA:
                nivel.agregar(new Nivel.Palanca(r.x, r.y, idParaControlNuevo(), "AMBOS"));
                tipoSel = TipoSeleccion.PALANCA;
                indiceSel = nivel.palancas.length - 1;
                break;
            case PUERTA:
                nivel.agregar(new Nivel.Puerta(r.x, r.y, r.width, r.height,
                    idParaPuertaNueva(), "ALGUNA", 0f, false));
                tipoSel = TipoSeleccion.PUERTA;
                indiceSel = nivel.puertas.length - 1;
                break;
            case PUENTE:
                nivel.agregar(new Nivel.Puerta(r.x, r.y, r.width, r.height,
                    idParaPuertaNueva(), "ALGUNA", 0f, true));
                tipoSel = TipoSeleccion.PUERTA;
                indiceSel = nivel.puertas.length - 1;
                break;
            case MOVIL:
                nivel.agregar(new Nivel.PlataformaMovil(r.x, r.y, r.width, r.height,
                    Math.min(nivel.ancho - r.width, r.x + 200f), r.y,
                    Configuracion.VELOCIDAD_PLATAFORMA_MOVIL, 0));
                tipoSel = TipoSeleccion.MOVIL;
                indiceSel = nivel.moviles.length - 1;
                break;
            case CAJA:
                nivel.agregar(new Nivel.Caja(r.x, r.y, false));
                tipoSel = TipoSeleccion.CAJA;
                indiceSel = nivel.cajas.length - 1;
                break;
            case ORIGEN_FUEGO:
                nivel.origenFuegoX = r.x;
                nivel.origenFuegoY = r.y;
                tipoSel = TipoSeleccion.ORIGEN_FUEGO;
                indiceSel = -1;
                break;
            case ORIGEN_AGUA:
                nivel.origenAguaX = r.x;
                nivel.origenAguaY = r.y;
                tipoSel = TipoSeleccion.ORIGEN_AGUA;
                indiceSel = -1;
                break;
            case SALIDA_FUEGO:
                nivel.salidaFuego.set(r);
                tipoSel = TipoSeleccion.SALIDA_FUEGO;
                indiceSel = -1;
                break;
            case SALIDA_AGUA:
                nivel.salidaAgua.set(r);
                tipoSel = TipoSeleccion.SALIDA_AGUA;
                indiceSel = -1;
                break;
            default:
                return;
        }
        sucio = true;
        confirmarCambio();
        estado = "Objeto creado con medidas fijas. Arrastralo para moverlo.";
    }

    private Rectangle rectHerramienta(Herramienta h, float x, float y) {
        switch (h) {
            case PLATAFORMA:
                return new Rectangle(x - Configuracion.ANCHO_PLATAFORMA_EDITOR / 2f, y,
                    Configuracion.ANCHO_PLATAFORMA_EDITOR, Configuracion.ALTO_PLATAFORMA_EDITOR);
            case MURO:
                return new Rectangle(x - Configuracion.ANCHO_MURO_EDITOR / 2f, y,
                    Configuracion.ANCHO_MURO_EDITOR, Configuracion.ALTO_MURO_EDITOR);
            case LAVA:
            case AGUA:
                return new Rectangle(x - Configuracion.ANCHO_PELIGRO_EDITOR / 2f, y,
                    Configuracion.ANCHO_PELIGRO_EDITOR, Configuracion.ALTO_PELIGRO_EDITOR);
            case GEMA:
                return new Rectangle(x - Configuracion.ANCHO_GEMA_EDITOR / 2f,
                    y - Configuracion.ALTO_GEMA_EDITOR / 2f,
                    Configuracion.ANCHO_GEMA_EDITOR, Configuracion.ALTO_GEMA_EDITOR);
            case PLACA:
                return new Rectangle(x - Configuracion.ANCHO_BOTON_EDITOR / 2f,
                    y - Configuracion.ALTO_BOTON_EDITOR / 2f,
                    Configuracion.ANCHO_BOTON_EDITOR, Configuracion.ALTO_BOTON_EDITOR);
            case PALANCA:
                return new Rectangle(x - Configuracion.ANCHO_PALANCA_EDITOR / 2f,
                    y - Configuracion.ALTO_PALANCA_EDITOR / 2f,
                    Configuracion.ANCHO_PALANCA_EDITOR, Configuracion.ALTO_PALANCA_EDITOR);
            case PUERTA:
                return new Rectangle(x - Configuracion.ANCHO_PUERTA_EDITOR / 2f, y,
                    Configuracion.ANCHO_PUERTA_EDITOR, Configuracion.ALTO_PUERTA_EDITOR);
            case PUENTE:
                return new Rectangle(x - Configuracion.ANCHO_PUENTE_EDITOR / 2f, y,
                    Configuracion.ANCHO_PUENTE_EDITOR, Configuracion.ALTO_PUENTE_EDITOR);
            case MOVIL:
                return new Rectangle(x - Configuracion.ANCHO_MOVIL_EDITOR / 2f, y,
                    Configuracion.ANCHO_MOVIL_EDITOR, Configuracion.ALTO_MOVIL_EDITOR);
            case CAJA:
                return new Rectangle(x - Configuracion.ANCHO_CAJA_EDITOR / 2f,
                    y - Configuracion.ALTO_CAJA_EDITOR / 2f,
                    Configuracion.ANCHO_CAJA_EDITOR, Configuracion.ALTO_CAJA_EDITOR);
            case ORIGEN_FUEGO:
            case ORIGEN_AGUA:
                return new Rectangle(x - Configuracion.ANCHO_ORIGEN_EDITOR / 2f, y,
                    Configuracion.ANCHO_ORIGEN_EDITOR, Configuracion.ALTO_ORIGEN_EDITOR);
            case SALIDA_FUEGO:
            case SALIDA_AGUA:
                return new Rectangle(x - Configuracion.ANCHO_SALIDA_EDITOR / 2f, y,
                    Configuracion.ANCHO_SALIDA_EDITOR, Configuracion.ALTO_SALIDA_EDITOR);
            default:
                return null;
        }
    }

    private void colocarDentro(Rectangle r) {
        r.x = MathUtils.clamp(r.x, 0f, Math.max(0f, nivel.ancho - r.width));
        r.y = MathUtils.clamp(r.y, 0f, Math.max(0f, nivel.alto - r.height));
    }

    private int siguienteId() {
        int max = 0;
        for (Nivel.Boton b : nivel.botones) max = Math.max(max, b.id);
        for (Nivel.Palanca p : nivel.palancas) max = Math.max(max, p.id);
        for (Nivel.Puerta p : nivel.puertas) max = Math.max(max, p.id);
        for (Nivel.PlataformaMovil m : nivel.moviles) max = Math.max(max, m.id);
        return max + 1;
    }

    /**
     * Al colocar un control nuevo, si hay una puerta que todavia no tiene
     * ningun boton ni palanca, se reutiliza su ID. Asi el primer control y la
     * primera puerta quedan enlazados sin que el usuario tenga que ajustar IDs.
     */
    private int idParaControlNuevo() {
        for (Nivel.Puerta p : nivel.puertas) {
            if (p.id > 0 && contarControles(p.id) == 0) return p.id;
        }
        return siguienteId();
    }

    /** Al colocar una puerta nueva, reutiliza el ID de un control huerfano. */
    private int idParaPuertaNueva() {
        for (Nivel.Boton b : nivel.botones) {
            if (b.id > 0 && contarPuertas(b.id) == 0) return b.id;
        }
        for (Nivel.Palanca p : nivel.palancas) {
            if (p.id > 0 && contarPuertas(p.id) == 0) return p.id;
        }
        return siguienteId();
    }

    private int contarControles(int id) {
        int total = 0;
        for (Nivel.Boton b : nivel.botones) if (b.id == id) total++;
        for (Nivel.Palanca p : nivel.palancas) if (p.id == id) total++;
        return total;
    }

    private int contarPuertas(int id) {
        int total = 0;
        for (Nivel.Puerta p : nivel.puertas) if (p.id == id) total++;
        return total;
    }

    private void eliminarSeleccion() {
        if (tipoSel == TipoSeleccion.NINGUNO) {
            estado = "No hay ningun objeto seleccionado.";
            return;
        }
        if (tipoSel == TipoSeleccion.PLATAFORMA && nivel.plataformas[indiceSel].limite) {
            estado = "Los bordes del escenario no se pueden borrar.";
            return;
        }
        if (tipoSel == TipoSeleccion.ORIGEN_FUEGO || tipoSel == TipoSeleccion.ORIGEN_AGUA
            || tipoSel == TipoSeleccion.SALIDA_FUEGO || tipoSel == TipoSeleccion.SALIDA_AGUA) {
            estado = "Los origenes y las salidas son obligatorios; muevelos, no los borres.";
            return;
        }
        switch (tipoSel) {
            case PLATAFORMA: nivel.plataformas = quitar(nivel.plataformas, indiceSel); break;
            case PELIGRO: nivel.peligros = quitar(nivel.peligros, indiceSel); break;
            case GEMA: nivel.gemas = quitar(nivel.gemas, indiceSel); break;
            case BOTON: nivel.botones = quitar(nivel.botones, indiceSel); break;
            case PALANCA: nivel.palancas = quitar(nivel.palancas, indiceSel); break;
            case PUERTA: nivel.puertas = quitar(nivel.puertas, indiceSel); break;
            case MOVIL: nivel.moviles = quitar(nivel.moviles, indiceSel); break;
            case CAJA: nivel.cajas = quitar(nivel.cajas, indiceSel); break;
            default:
                estado = "Ese objeto no se puede eliminar.";
                return;
        }
        tipoSel = TipoSeleccion.NINGUNO;
        indiceSel = -1;
        sucio = true;
        confirmarCambio();
        estado = "Objeto eliminado.";
    }

    private static <T> T[] quitar(T[] arreglo, int indice) {
        if (indice < 0 || indice >= arreglo.length) return arreglo;
        T[] nuevo = java.util.Arrays.copyOf(arreglo, arreglo.length - 1);
        System.arraycopy(arreglo, indice + 1, nuevo, indice, arreglo.length - indice - 1);
        return nuevo;
    }

    // =========================================================
    //                       HISTORIAL
    // =========================================================

    private String instantanea() {
        return json.toJson(nivel);
    }

    private void reiniciarHistorial() {
        historial.clear();
        posHistorial = 0;
        historial.add(instantanea());
        sucio = false;
    }

    private void confirmarCambio() {
        while (historial.size() > posHistorial + 1) {
            historial.remove(historial.size() - 1);
        }
        historial.add(instantanea());
        posHistorial = historial.size() - 1;
        while (historial.size() > 80) {
            historial.remove(0);
            posHistorial--;
        }
        sucio = true;
    }

    private void deshacer() {
        if (posHistorial <= 0) {
            estado = "No hay mas cambios para deshacer.";
            return;
        }
        posHistorial--;
        restaurar(historial.get(posHistorial));
        estado = "Deshecho.";
    }

    private void rehacer() {
        if (posHistorial >= historial.size() - 1) {
            estado = "No hay cambios para rehacer.";
            return;
        }
        posHistorial++;
        restaurar(historial.get(posHistorial));
        estado = "Rehecho.";
    }

    private void restaurar(String texto) {
        try {
            Nivel nuevo = json.fromJson(Nivel.class, texto);
            if (nuevo == null) return;
            nuevo.normalizarEditor();
            nivel = nuevo;
            tipoSel = TipoSeleccion.NINGUNO;
            indiceSel = -1;
            sucio = true;
        } catch (Exception e) {
            estado = "No se pudo restaurar el historial: " + e.getMessage();
        }
    }

    // =========================================================
    //                     GUARDAR / CARGAR
    // =========================================================

    private void guardar(boolean permitirSobrescribir) {
        try {
            nivel.normalizarEditor();
            GestorNiveles gestor = juego.getGestorNiveles();
            if (esNivelHistoria || archivoActual.isEmpty()) {
                // Nivel de historia o nuevo: guardar en carpeta de usuario
                String existente = gestor.archivoConNombre(nivel.nombre);
                if (!existente.isEmpty() && !permitirSobrescribir) {
                    estado = "Ya existe \"" + nivel.nombre + "\". Sobrescribe o cambia el nombre.";
                    confirmar = "SOBRESCRIBIR";
                    return;
                }
                boolean eraHistoria = esNivelHistoria;
                archivoActual = gestor.guardar(nivel, permitirSobrescribir);
                esNivelHistoria = false;
                if (eraHistoria) {
                    estado = "Guardado como " + archivoActual + ". La campaña usará esta versión.";
                }
            } else {
                String otro = gestor.archivoConNombre(nivel.nombre);
                if (!otro.isEmpty() && !otro.equalsIgnoreCase(archivoActual)) {
                    estado = "Ya existe otro nivel llamado \"" + nivel.nombre + "\". Cambia el nombre.";
                    confirmar = "";
                    return;
                }
                archivoActual = gestor.guardarEn(nivel, archivoActual);
            }
            sucio = false;
            estado = "Guardado en " + archivoActual + ".";
            confirmar = "";
        } catch (IOException e) {
            String mensaje = e.getMessage() == null ? "Error al guardar." : e.getMessage();
            if (!permitirSobrescribir && (mensaje.contains("Ya existe")
                || mensaje.contains("archivo"))) {
                confirmar = "SOBRESCRIBIR";
                estado = mensaje;
            } else {
                estado = mensaje;
            }
        }
    }

    private void pedirNuevo() {
        if (sucio) {
            confirmar = "NUEVO";
        } else {
            crearNuevo();
        }
    }

    private void crearNuevo() {
        nivel = nivelNuevo();
        archivoActual = "";
        tipoSel = TipoSeleccion.NINGUNO;
        indiceSel = -1;
        sucio = false;
        encuadrarNivel();
        reiniciarHistorial();
        estado = "Nivel nuevo creado.";
        confirmar = "";
    }

    private void pedirSalir() {
        if (sucio) confirmar = "SALIR";
        else salirDelEditor();
    }

    private void salirDelEditor() {
        cambiarPantalla(new PantallaNiveles(juego));
    }

    private void pedirBorrarArchivo() {
        if (archivoActual.isEmpty()) {
            estado = "Este nivel todavia no esta guardado.";
            return;
        }
        confirmar = "BORRAR_ARCHIVO";
    }

    private void borrarArchivo() {
        try {
            juego.getGestorNiveles().eliminar(archivoActual);
            estado = "Archivo " + archivoActual + " eliminado. El nivel sigue abierto como borrador.";
            archivoActual = "";
            sucio = true;
            confirmar = "";
        } catch (IOException e) {
            estado = e.getMessage();
        }
    }

    private void abrirSelector() {
        if (sucio) {
            confirmar = "CARGAR";
            estado = "Hay cambios sin guardar. Confirma para abrir otro nivel.";
            return;
        }
        abrirSelectorAhora();
    }

    private void abrirSelectorAhora() {
        recargarListaSelector();
        paginaSelector = 0;
        pestanaSelector = 0;
        selectorAbierto = true;
        confirmar = "";
    }

    private void cargarEntrada(GestorNiveles.Entrada entrada) {
        if (entrada == null || entrada.nivel == null) {
            estado = entrada != null && entrada.error != null ? entrada.error : "Nivel invalido.";
            return;
        }
        try {
            if (entrada.archivo != null && entrada.archivo.startsWith("HISTORIA:")) {
                // Nivel de la historia: cargar como copia editable
                Nivel copia = json.fromJson(Nivel.class, json.toJson(entrada.nivel));
                if (copia == null) throw new IOException("No se pudo copiar el nivel de la historia.");
                copia.normalizarEditor();
                nivel = copia;
                archivoActual = "";
                esNivelHistoria = true;
                estado = "Historia \"" + recortar(copia.nombre, 24) + "\" cargada. Guardar crea copia para campaña.";
            } else {
                nivel = juego.getGestorNiveles().cargar(entrada.archivo);
                archivoActual = entrada.archivo;
                esNivelHistoria = false;
                estado = "Cargado: " + recortar(nivel.nombre, 28) + " (" + archivoActual + ").";
            }
            tipoSel = TipoSeleccion.NINGUNO;
            indiceSel = -1;
            sucio = false;
            selectorAbierto = false;
            confirmar = "";
            encuadrarNivel();
            reiniciarHistorial();
        } catch (Exception e) {
            estado = e.getMessage() == null ? "No se pudo cargar el nivel." : e.getMessage();
        }
    }

    private void duplicarActual() {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                if (texto == null || texto.trim().isEmpty()) return;
                try {
                    Nivel copia = juego.getGestorNiveles().duplicarNivel(nivel, texto.trim());
                    estado = "Copia creada: " + copia.nombre + " (" +
                        juego.getGestorNiveles().archivoConNombre(copia.nombre) + ").";
                } catch (IOException e) {
                    estado = e.getMessage();
                }
            }
            @Override public void canceled() { }
        }, "Nombre de la copia", nivel.nombre + " copia", "Nuevo nombre");
    }

    private void probarNivel() {
        nivel.normalizarEditor();
        List<String> errores = ValidadorNivel.validar(nivel);
        if (!errores.isEmpty()) {
            StringBuilder sb = new StringBuilder("No se puede probar: ");
            for (int i = 0; i < Math.min(3, errores.size()); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(errores.get(i));
            }
            estado = sb.toString();
            return;
        }
        cambiarPantalla(PantallaJuego.pruebaEditor(juego, nivel, archivoActual, sucio,
            historial, posHistorial));
    }

    // =========================================================
    //                     PROPIEDADES
    // =========================================================

    private void pedirTexto(String titulo, String valor, final CambioTexto cambio) {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                cambio.aplicar(texto == null ? "" : texto);
                confirmarCambio();
            }
            @Override public void canceled() { }
        }, titulo, valor == null ? "" : valor, "");
    }

    private void filaNumero(String etiqueta, float valor, float y, float paso, final Ajuste ajuste) {
        Dibujo.texto(lote, fuente, etiqueta, 752f, y, Configuracion.TEXTO_SUAVE, 0.64f);
        Dibujo.textoDerecha(lote, fuente, formatoNumero(valor), 834f, y, Configuracion.TEXTO, 0.66f);
        if (ui.boton("-", 842f, y - 6f, 26f, 22f)) {
            ajuste.cambiar(-paso);
            confirmarCambio();
        }
        if (ui.boton("+", 874f, y - 6f, 26f, 22f)) {
            ajuste.cambiar(paso);
            confirmarCambio();
        }
    }

    private void filaCiclo(String etiqueta, String valor, float y, final Runnable cambio) {
        Dibujo.texto(lote, fuente, etiqueta, 752f, y, Configuracion.TEXTO_SUAVE, 0.64f);
        if (ui.boton(valor, 812f, y - 7f, 128f, 26f)) {
            cambio.run();
            confirmarCambio();
        }
    }

    private void filaToggle(String etiqueta, boolean valor, float y, final AjusteBooleano cambio) {
        boolean nuevo = ui.interruptor(etiqueta, valor, 750f, y - 10f, 190f, 32f);
        if (nuevo != valor) {
            cambio.cambiar(nuevo);
            confirmarCambio();
        }
    }

    private interface AjusteBooleano { void cambiar(boolean valor); }

    private static String formatoNumero(float valor) {
        if (Math.abs(valor - Math.round(valor)) < 0.01f) return String.valueOf(Math.round(valor));
        return String.format(java.util.Locale.US, "%.1f", valor);
    }

    private float pasoEdicion() {
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) return 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) return 50f;
        return Configuracion.PASO_EDITOR;
    }

    private void dibujarPropiedades() {
        Dibujo.panel(lote, 742f, 60f, 210f, 440f, new Color(0.05f, 0.07f, 0.12f, 0.98f),
            new Color(0.30f, 0.40f, 0.58f, 1f));
        float y = 470f;
        switch (tipoSel) {
            case NINGUNO: dibujarPropiedadesNivel(y); break;
            case PLATAFORMA: dibujarPropiedadesPlataforma(y); break;
            case PELIGRO: dibujarPropiedadesPeligro(y); break;
            case GEMA: dibujarPropiedadesGema(y); break;
            case BOTON: dibujarPropiedadesBoton(y); break;
            case PALANCA: dibujarPropiedadesPalanca(y); break;
            case PUERTA: dibujarPropiedadesPuerta(y); break;
            case MOVIL: dibujarPropiedadesMovil(y); break;
            case CAJA: dibujarPropiedadesCaja(y); break;
            case ORIGEN_FUEGO:
            case ORIGEN_AGUA:
            case SALIDA_FUEGO:
            case SALIDA_AGUA:
                dibujarPropiedadesPunto(y);
                break;
            default: break;
        }
    }

    private void dibujarPropiedadesNivel(float yTop) {
        Dibujo.texto(lote, fuente, "DATOS DEL NIVEL", 752f, 486f, Configuracion.DORADO, 0.72f);
        float y = 458f;

        // Nombre
        Dibujo.texto(lote, fuente, "NOMBRE", 752f, y, Configuracion.TEXTO_SUAVE, 0.54f);
        Dibujo.panel(lote, 750f, y - 32f, 192f, 26f, new Color(0.08f, 0.10f, 0.16f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.texto(lote, fuente, recortar(nivel.nombre, 24), 758f, y - 15f, Configuracion.TEXTO, 0.62f);
        if (ui.boton("CAMBIAR", 750f, y - 62f, 192f, 24f)) {
            pedirTexto("Nombre del nivel", nivel.nombre, new CambioTexto() {
                @Override public void aplicar(String texto) {
                    nivel.nombre = texto.trim().isEmpty() ? "Nuevo nivel" : texto.trim();
                    confirmarCambio();
                    estado = "Nombre actualizado.";
                }
            });
        }
        y -= 72f;

        // Autor
        Dibujo.texto(lote, fuente, "AUTOR", 752f, y, Configuracion.TEXTO_SUAVE, 0.54f);
        Dibujo.panel(lote, 750f, y - 32f, 192f, 26f, new Color(0.08f, 0.10f, 0.16f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.texto(lote, fuente, recortar(nivel.autor == null ? "" : nivel.autor, 24), 758f, y - 15f,
            Configuracion.TEXTO, 0.62f);
        if (ui.boton("CAMBIAR", 750f, y - 62f, 192f, 24f)) {
            pedirTexto("Autor del nivel", nivel.autor, new CambioTexto() {
                @Override public void aplicar(String texto) {
                    nivel.autor = texto == null ? "" : texto.trim();
                    confirmarCambio();
                    estado = "Autor actualizado.";
                }
            });
        }
        y -= 72f;

        // Pista
        Dibujo.texto(lote, fuente, "PISTA", 752f, y, Configuracion.TEXTO_SUAVE, 0.54f);
        Dibujo.panel(lote, 750f, y - 32f, 192f, 26f, new Color(0.08f, 0.10f, 0.16f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.texto(lote, fuente, recortar(nivel.pista == null ? "" : nivel.pista, 24), 758f, y - 15f,
            Configuracion.TEXTO, 0.58f);
        if (ui.boton("CAMBIAR", 750f, y - 62f, 192f, 24f)) {
            pedirTexto("Pista del nivel", nivel.pista, new CambioTexto() {
                @Override public void aplicar(String texto) {
                    nivel.pista = texto == null ? "" : texto;
                    confirmarCambio();
                    estado = "Pista actualizada.";
                }
            });
        }
        y -= 72f;

        // Tema
        Dibujo.texto(lote, fuente, "TEMA", 752f, y, Configuracion.TEXTO_SUAVE, 0.54f);
        Dibujo.panel(lote, 750f, y - 32f, 192f, 26f, new Color(0.08f, 0.10f, 0.16f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.texto(lote, fuente, Nivel.nombreTema(nivel.tema), 758f, y - 15f, Configuracion.TEXTO, 0.60f);
        if (ui.boton("<  " + nivel.tema + "  >", 750f, y - 62f, 192f, 24f)) {
            nivel.tema = siguienteTema(nivel.tema);
            confirmarCambio();
            estado = "Tema: " + Nivel.nombreTema(nivel.tema);
        }
        y -= 74f;

        Dibujo.texto(lote, fuente, "ESCENARIO", 752f, y, Configuracion.TEXTO_SUAVE, 0.54f);
        y -= 22f;
        filaNumero("ANCHO", nivel.ancho, y, 40f, new Ajuste() {
            @Override public void cambiar(float d) { cambiarAnchoNivel(d); }
        });
        y -= 30f;
        filaNumero("ALTO", nivel.alto, y, 40f, new Ajuste() {
            @Override public void cambiar(float d) { cambiarAltoNivel(d); }
        });
        y -= 34f;

        if (esNivelHistoria || archivoActual.startsWith("HISTORIA:")) {
            Dibujo.texto(lote, fuente, "Nivel de historia", 752f, y, Configuracion.DORADO, 0.58f);
            Dibujo.texto(lote, fuente, "Al guardar crea copia editable", 752f, y - 16f, Configuracion.TEXTO_SUAVE, 0.50f);
            y -= 28f;
        } else if (!archivoActual.isEmpty()) {
            Dibujo.texto(lote, fuente, "Archivo: " + recortar(archivoActual, 18), 752f, y, Configuracion.TEXTO_SUAVE, 0.50f);
            y -= 14f;
        }
        Dibujo.texto(lote, fuente, "F encuadra, +/- zoom", 752f, y - 6f, Configuracion.TEXTO_SUAVE, 0.52f);
        Dibujo.texto(lote, fuente, "HUD fijo, mundo debajo", 752f, y - 20f, Configuracion.TEXTO_SUAVE, 0.52f);
    }


    private void cambiarAnchoNivel(float d) {
        float nuevo = MathUtils.clamp(nivel.ancho + d, Configuracion.ANCHO_MINIMO_NIVEL, Configuracion.ANCHO_MAXIMO_NIVEL);
        aplicarTamanoNivel(nuevo, nivel.alto);
    }

    private void cambiarAltoNivel(float d) {
        float nuevo = MathUtils.clamp(nivel.alto + d, Configuracion.ALTO_MINIMO_NIVEL, Configuracion.ALTO_MAXIMO_NIVEL);
        aplicarTamanoNivel(nivel.ancho, nuevo);
    }

    private void aplicarTamanoNivel(float nuevoAncho, float nuevoAlto) {
        if (Math.abs(nuevoAncho - nivel.ancho) < 0.01f && Math.abs(nuevoAlto - nivel.alto) < 0.01f) return;
        java.util.List<String> fuera = nivel.objetosFuera(nuevoAncho, nuevoAlto);
        if (fuera != null && !fuera.isEmpty()) {
            pendienteAncho = nuevoAncho;
            pendienteAlto = nuevoAlto;
            pendientesFueraCount = fuera.size();
            confirmar = "REDIMENSIONAR";
            return;
        }
        ejecutarRedimension(nuevoAncho, nuevoAlto, false);
    }

    private void ejecutarRedimension(float nuevoAncho, float nuevoAlto, boolean reubico) {
        nivel.ancho = nuevoAncho;
        nivel.alto = nuevoAlto;
        nivel.ajustarBordes();
        nivel.normalizarEditor();
        encuadrarNivel();
        confirmarCambio();
        estado = "Escenario " + Math.round(nivel.ancho) + "x" + Math.round(nivel.alto)
            + (reubico ? ". Objetos reubicados." : ".");
    }

    private void dibujarPropiedadesPlataforma(float y) {
        Nivel.Plataforma o = nivel.plataformas[indiceSel];
        Dibujo.texto(lote, fuente, o.limite ? "BORDE DEL ESCENARIO" : "PLATAFORMA",
            752f, 486f, Configuracion.DORADO, 0.72f);
        if (o.limite) {
            Dibujo.texto(lote, fuente, "Borde fijo del recinto.", 752f, 452f,
                Configuracion.TEXTO, 0.64f);
            Dibujo.texto(lote, fuente, "No se mueve ni se borra.", 752f, 430f,
                Configuracion.TEXTO_SUAVE, 0.60f);
            return;
        }
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 8f, 8f);
        if ("MURO".equalsIgnoreCase(o.estilo)) {
            Dibujo.texto(lote, fuente, "ESTILO: MURO", 752f, y,
                Configuracion.TEXTO_SUAVE, 0.64f);
            y -= 30f;
        } else {
            filaCiclo("ESTILO", o.estilo, y, new Runnable() {
                @Override public void run() { o.estilo = siguienteEstilo(o.estilo); }
            });
            y -= 38f;
        }
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesPeligro(float y) {
        Nivel.Peligro o = nivel.peligros[indiceSel];
        Dibujo.texto(lote, fuente, "PELIGRO", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 8f, 6f);
        filaToggle("ES LAVA", o.lava, y, new AjusteBooleano() {
            @Override public void cambiar(boolean v) { o.lava = v; }
        });
        y -= 38f;
        Dibujo.texto(lote, fuente, o.lava ? "Mata al Agua." : "Mata al Fuego.", 752f, y,
            Configuracion.TEXTO_SUAVE, 0.62f);
        y -= 30f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesGema(float y) {
        Nivel.Gema o = nivel.gemas[indiceSel];
        Dibujo.texto(lote, fuente, "GEMA", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 10f, 10f);
        filaToggle("SECRETA", o.secreta, y, new AjusteBooleano() {
            @Override public void cambiar(boolean v) { o.secreta = v; }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesBoton(float y) {
        Nivel.Boton o = nivel.botones[indiceSel];
        Dibujo.texto(lote, fuente, "PLACA DE PRESION", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 16f, 8f);
        filaNumero("ID", o.id, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.id = Math.max(1, Math.round(o.id + d)); }
        });
        y -= 30f;
        filaCiclo("ELEMENTO", o.elemento, y, new Runnable() {
            @Override public void run() { o.elemento = siguienteElemento(o.elemento); }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesPalanca(float y) {
        Nivel.Palanca o = nivel.palancas[indiceSel];
        Dibujo.texto(lote, fuente, "PALANCA", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 12f, 12f);
        filaNumero("ID", o.id, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.id = Math.max(1, Math.round(o.id + d)); }
        });
        y -= 30f;
        filaCiclo("ELEMENTO", o.elemento, y, new Runnable() {
            @Override public void run() { o.elemento = siguienteElemento(o.elemento); }
        });
        y -= 30f;
        filaNumero("ORDEN", o.orden, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.orden = Math.max(0, Math.round(o.orden + d)); }
        });
        y -= 30f;
        filaNumero("GRUPO", o.grupo, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.grupo = Math.max(1, Math.round(o.grupo + d)); }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesPuerta(float y) {
        Nivel.Puerta o = nivel.puertas[indiceSel];
        Dibujo.texto(lote, fuente, o.puente ? "PUENTE DE ENERGIA" : "PUERTA",
            752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 8f, 8f);
        filaNumero("ID", o.id, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.id = Math.max(1, Math.round(o.id + d)); }
        });
        y -= 30f;
        filaCiclo("MODO", o.modo, y, new Runnable() {
            @Override public void run() { o.modo = "TODAS".equalsIgnoreCase(o.modo) ? "ALGUNA" : "TODAS"; }
        });
        y -= 30f;
        filaNumero("DURACION", o.duracion, y, 0.5f, new Ajuste() {
            @Override public void cambiar(float d) { o.duracion = Math.max(0f, o.duracion + d); }
        });
        y -= 30f;
        filaToggle("ES PUENTE", o.puente, y, new AjusteBooleano() {
            @Override public void cambiar(boolean v) { o.puente = v; }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesMovil(float y) {
        Nivel.PlataformaMovil o = nivel.moviles[indiceSel];
        Dibujo.texto(lote, fuente, "PLATAFORMA MOVIL", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 16f, 8f);
        filaNumero("FIN X", o.finX, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                o.finX = MathUtils.clamp(o.finX + d, 10f, nivel.ancho - 10f);
            }
        });
        y -= 30f;
        filaNumero("FIN Y", o.finY, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                o.finY = MathUtils.clamp(o.finY + d, 10f, nivel.alto - 10f);
            }
        });
        y -= 30f;
        filaNumero("VELOCIDAD", o.velocidad, y, 5f, new Ajuste() {
            @Override public void cambiar(float d) { o.velocidad = Math.max(5f, o.velocidad + d); }
        });
        y -= 30f;
        filaNumero("ID CONTROL", o.id, y, 1f, new Ajuste() {
            @Override public void cambiar(float d) { o.id = Math.max(0, Math.round(o.id + d)); }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesCaja(float y) {
        Nivel.Caja o = nivel.cajas[indiceSel];
        Dibujo.texto(lote, fuente, "CAJA", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaPosicion(o.rect, 458f);
        y = filaTamano(o.rect, y, 12f, 12f);
        filaToggle("PESADA", o.pesada, y, new AjusteBooleano() {
            @Override public void cambiar(boolean v) { o.pesada = v; }
        });
        y -= 38f;
        Dibujo.texto(lote, fuente, o.pesada ? "Necesita los dos para moverse." : "Uno solo puede moverla.",
            752f, y, Configuracion.TEXTO_SUAVE, 0.58f);
        y -= 30f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesPunto(float y) {
        Dibujo.texto(lote, fuente, "PUNTO DE NIVEL", 752f, 486f, Configuracion.DORADO, 0.72f);
        if (tipoSel == TipoSeleccion.ORIGEN_FUEGO || tipoSel == TipoSeleccion.ORIGEN_AGUA) {
            boolean fuego = tipoSel == TipoSeleccion.ORIGEN_FUEGO;
            y = 458f;
            filaNumero("X", fuego ? nivel.origenFuegoX : nivel.origenAguaX, y, pasoEdicion(), new Ajuste() {
                @Override public void cambiar(float d) {
                    if (fuego) nivel.origenFuegoX = MathUtils.clamp(nivel.origenFuegoX + d, 0f,
                        Math.max(0f, nivel.ancho - Configuracion.ANCHO_PERSONAJE));
                    else nivel.origenAguaX = MathUtils.clamp(nivel.origenAguaX + d, 0f,
                        Math.max(0f, nivel.ancho - Configuracion.ANCHO_PERSONAJE));
                }
            });
            y -= 30f;
            filaNumero("Y", fuego ? nivel.origenFuegoY : nivel.origenAguaY, y, pasoEdicion(), new Ajuste() {
                @Override public void cambiar(float d) {
                    if (fuego) nivel.origenFuegoY = MathUtils.clamp(nivel.origenFuegoY + d, 0f,
                        Math.max(0f, nivel.alto - Configuracion.ALTO_PERSONAJE));
                    else nivel.origenAguaY = MathUtils.clamp(nivel.origenAguaY + d, 0f,
                        Math.max(0f, nivel.alto - Configuracion.ALTO_PERSONAJE));
                }
            });
            y -= 34f;
            Dibujo.texto(lote, fuente, "Coloca el origen sobre una plataforma.", 752f, y,
                Configuracion.TEXTO_SUAVE, 0.58f);
        } else {
            Rectangle r = tipoSel == TipoSeleccion.SALIDA_FUEGO ? nivel.salidaFuego : nivel.salidaAgua;
            y = filaPosicion(r, 458f);
            y = filaTamano(r, y, 20f, 20f);
            Dibujo.texto(lote, fuente, "La salida debe quedar libre.", 752f, y,
                Configuracion.TEXTO_SUAVE, 0.58f);
        }
    }

    private float filaPosicion(Rectangle r, float y) {
        filaNumero("X", r.x, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                r.x = MathUtils.clamp(r.x + d, 0f, Math.max(0f, nivel.ancho - r.width));
            }
        });
        y -= 30f;
        filaNumero("Y", r.y, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                r.y = MathUtils.clamp(r.y + d, 0f, Math.max(0f, nivel.alto - r.height));
            }
        });
        return y - 34f;
    }

    private float filaTamano(Rectangle r, float y, float minW, float minH) {
        filaNumero("ANCHO", r.width, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                float nuevo = Math.max(minW, r.width + d);
                nuevo = Math.min(nuevo, Math.max(minW, nivel.ancho - r.x));
                r.width = nuevo;
                r.x = MathUtils.clamp(r.x, 0f, Math.max(0f, nivel.ancho - r.width));
            }
        });
        y -= 30f;
        filaNumero("ALTO", r.height, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                float nuevo = Math.max(minH, r.height + d);
                nuevo = Math.min(nuevo, Math.max(minH, nivel.alto - r.y));
                r.height = nuevo;
                r.y = MathUtils.clamp(r.y, 0f, Math.max(0f, nivel.alto - r.height));
            }
        });
        return y - 34f;
    }

    private static String siguienteTema(String actual) {
        for (int i = 0; i < TEMAS.length; i++) {
            if (TEMAS[i].equalsIgnoreCase(actual)) return TEMAS[(i + 1) % TEMAS.length];
        }
        return TEMAS[0];
    }

    private static String siguienteEstilo(String actual) {
        String[] estilos = {"PIEDRA", "MADERA", "METAL"};
        for (int i = 0; i < estilos.length; i++) {
            if (estilos[i].equalsIgnoreCase(actual)) return estilos[(i + 1) % estilos.length];
        }
        return "PIEDRA";
    }

    private static String siguienteElemento(String actual) {
        String[] elementos = {"AMBOS", "FUEGO", "AGUA"};
        for (int i = 0; i < elementos.length; i++) {
            if (elementos[i].equalsIgnoreCase(actual)) return elementos[(i + 1) % elementos.length];
        }
        return "AMBOS";
    }

    // =========================================================
    //                       DIBUJO
    // =========================================================

    @Override
    protected void alRedimensionar(int ancho, int alto) {
        super.alRedimensionar(ancho, alto);
    }

    @Override
    protected void dibujar(float delta) {
        ScreenUtils.clear(0.020f, 0.028f, 0.055f, 1f);

        aplicarVistaMundo();
        camara.position.set(camX, camY, 0f);
        camara.zoom = zoom;
        camara.update();
        lote.setProjectionMatrix(camara.combined);
        lote.begin();
        dibujarMundo();
        lote.end();

        aplicarVistaUI();
        lote.begin();
        dibujarInterfaz();
        lote.end();
    }

    private void aplicarVistaMundo() {
        if (vistaUI == null) return;
        int hudPx = Math.round(vistaUI.getScreenHeight() * (HUD_H / 540f));
        vistaUI.apply();
        com.badlogic.gdx.Gdx.gl.glViewport(vistaUI.getScreenX(), vistaUI.getScreenY(),
            vistaUI.getScreenWidth(), Math.max(1, vistaUI.getScreenHeight() - hudPx));
        camara.viewportWidth = VISTA_W;
        camara.viewportHeight = VISTA_H;
    }

    private void dibujarMundo() {
        float visibleAncho = VISTA_W * zoom;
        float visibleAlto = VISTA_H * zoom;
        dibujante.fondo(nivel.tema, tiempo, camX - visibleAncho / 2f - 200f, camY - visibleAlto / 2f - 200f,
            visibleAncho + 400f, visibleAlto + 400f);

        Color fuera = new Color(0.01f, 0.02f, 0.04f, 0.88f);
        Dibujo.rectangulo(lote, camX - visibleAncho, camY - visibleAlto, visibleAncho * 3f, visibleAlto, fuera);
        Dibujo.rectangulo(lote, camX - visibleAncho, nivel.alto, visibleAncho * 3f, visibleAlto, fuera);
        Dibujo.rectangulo(lote, camX - visibleAncho, 0f, visibleAncho, nivel.alto, fuera);
        Dibujo.rectangulo(lote, nivel.ancho, 0f, visibleAncho, nivel.alto, fuera);

        float minX = Math.max(0f, camX - visibleAncho / 2f);
        float maxX = Math.min(nivel.ancho, camX + visibleAncho / 2f);
        float minY = Math.max(0f, camY - visibleAlto / 2f);
        float maxY = Math.min(nivel.alto, camY + visibleAlto / 2f);
        Color grid = new Color(0.45f, 0.60f, 0.85f, 0.09f);
        float primerX = (float) Math.floor(minX / 40f) * 40f;
        float primerY = (float) Math.floor(minY / 40f) * 40f;
        for (float x = primerX; x <= maxX; x += 40f) {
            Dibujo.rectangulo(lote, x, minY, 1f, maxY - minY, grid);
        }
        for (float y = primerY; y <= maxY; y += 40f) {
            Dibujo.rectangulo(lote, minX, y, maxX - minX, 1f, grid);
        }

        Dibujo.rectangulo(lote, 0f, 0f, nivel.ancho, 4f, Configuracion.DORADO);
        Dibujo.rectangulo(lote, 0f, nivel.alto - 4f, nivel.ancho, 4f, Configuracion.DORADO);
        Dibujo.rectangulo(lote, 0f, 0f, 4f, nivel.alto, Configuracion.DORADO);
        Dibujo.rectangulo(lote, nivel.ancho - 4f, 0f, 4f, nivel.alto, Configuracion.DORADO);

        for (Nivel.Plataforma p : nivel.plataformas) dibujante.dibujarPlataforma(p.rect, p.estilo);
        for (Nivel.Peligro p : nivel.peligros) dibujante.dibujarPeligro(p.rect, p.lava, tiempo);
        for (int i = 0; i < nivel.puertas.length; i++) {
            Nivel.Puerta p = nivel.puertas[i];
            dibujante.dibujarPuerta(p.rect, false, p.puente, tiempo);
        }
        for (Nivel.PlataformaMovil m : nivel.moviles) {
            Dibujo.linea(lote, m.rect.x + m.rect.width / 2f, m.rect.y + m.rect.height / 2f,
                m.finX + m.rect.width / 2f, m.finY + m.rect.height / 2f, 1.5f,
                new Color(0.55f, 0.80f, 1f, 0.45f));
            Dibujo.circulo(lote, m.finX + m.rect.width / 2f - 3f, m.finY + m.rect.height / 2f - 3f,
                6f, Configuracion.AGUA_CLARA);
            dibujante.dibujarMovil(m.rect, tiempo);
        }
        for (Nivel.Boton b : nivel.botones) dibujante.dibujarBoton(b.rect, b.elemento, false, tiempo);
        for (Nivel.Palanca p : nivel.palancas) dibujante.dibujarPalanca(p.rect, p.elemento, false, tiempo);
        for (Nivel.Caja c : nivel.cajas) dibujante.dibujarCaja(c.rect, c.pesada);
        for (Nivel.Gema g : nivel.gemas) dibujante.dibujarGema(g.rect, g.secreta, tiempo);
        dibujante.dibujarSalida(nivel.salidaFuego, true, false, tiempo);
        dibujante.dibujarSalida(nivel.salidaAgua, false, false, tiempo);

        dibujarOrigen(nivel.origenFuegoX, nivel.origenFuegoY, Configuracion.FUEGO, "F");
        dibujarOrigen(nivel.origenAguaX, nivel.origenAguaY, Configuracion.AGUA, "A");

        if (herramienta != Herramienta.SELECCION && ui.dentro(150f, 60f, 590f, 440f)) {
            Rectangle fantasma = rectHerramienta(herramienta, mundoX, mundoY);
            if (fantasma != null) {
                Dibujo.rectangulo(lote, fantasma.x, fantasma.y, fantasma.width, fantasma.height,
                    new Color(1f, 1f, 1f, 0.13f));
                marco(fantasma, Configuracion.DORADO);
                if (herramienta == Herramienta.MOVIL) {
                    Dibujo.linea(lote, fantasma.x + fantasma.width / 2f,
                        fantasma.y + fantasma.height / 2f,
                        Math.min(nivel.ancho - fantasma.width, fantasma.x + 200f) + fantasma.width / 2f,
                        fantasma.y + fantasma.height / 2f, 1.5f, new Color(0.55f, 0.80f, 1f, 0.45f));
                }
            }
        }

        Rectangle seleccion = rectSeleccion();
        if (seleccion != null) marco(seleccion, Configuracion.DORADO);
    }

    private void dibujarOrigen(float x, float y, Color color, String letra) {
        Dibujo.rectanguloRedondeado(lote, x, y, Configuracion.ANCHO_PERSONAJE,
            Configuracion.ALTO_PERSONAJE, 9f, color);
        Dibujo.texto(lote, fuente, letra, x + 10f, y + 28f, new Color(1f, 1f, 1f, 0.9f), 0.68f);
    }

    private Rectangle rectSeleccion() {
        switch (tipoSel) {
            case PLATAFORMA: return nivel.plataformas[indiceSel].rect;
            case PELIGRO: return nivel.peligros[indiceSel].rect;
            case GEMA: return nivel.gemas[indiceSel].rect;
            case BOTON: return nivel.botones[indiceSel].rect;
            case PALANCA: return nivel.palancas[indiceSel].rect;
            case PUERTA: return nivel.puertas[indiceSel].rect;
            case MOVIL: return nivel.moviles[indiceSel].rect;
            case CAJA: return nivel.cajas[indiceSel].rect;
            case ORIGEN_FUEGO: return new Rectangle(nivel.origenFuegoX, nivel.origenFuegoY,
                Configuracion.ANCHO_PERSONAJE, Configuracion.ALTO_PERSONAJE);
            case ORIGEN_AGUA: return new Rectangle(nivel.origenAguaX, nivel.origenAguaY,
                Configuracion.ANCHO_PERSONAJE, Configuracion.ALTO_PERSONAJE);
            case SALIDA_FUEGO: return nivel.salidaFuego;
            case SALIDA_AGUA: return nivel.salidaAgua;
            default: return null;
        }
    }

    private void marco(Rectangle r, Color color) {
        float grosor = Math.max(1.5f, 2f * zoom);
        Dibujo.rectangulo(lote, r.x - grosor, r.y - grosor, r.width + grosor * 2f, grosor, color);
        Dibujo.rectangulo(lote, r.x - grosor, r.y + r.height, r.width + grosor * 2f, grosor, color);
        Dibujo.rectangulo(lote, r.x - grosor, r.y, grosor, r.height, color);
        Dibujo.rectangulo(lote, r.x + r.width, r.y, grosor, r.height, color);
    }

    private void dibujarInterfaz() {
        boolean modal = selectorAbierto || !confirmar.isEmpty();
        if (modal) {
            // Fondo visible pero sin interacción: no limpiamos ui.clic para que
            // los botones del diálogo activo puedan recibirlo.
            dibujarBarraSuperiorFondo();
            dibujarPaletaFondo();
            dibujarPropiedadesFondo();
            dibujarEstado();
            if (selectorAbierto && confirmar.isEmpty()) dibujarSelector();
            if (!confirmar.isEmpty()) dibujarConfirmacion();
        } else {
            dibujarBarraSuperior();
            dibujarPaleta();
            dibujarPropiedades();
            dibujarEstado();
        }
    }

    private void dibujarBarraSuperiorFondo() {
        Dibujo.rectangulo(lote, 0f, 500f, 960f, 40f, new Color(0.035f, 0.05f, 0.09f, 0.98f));
        Dibujo.rectangulo(lote, 0f, 499f, 960f, 1f, new Color(0.35f, 0.48f, 0.68f, 0.55f));
    }

    private void dibujarPaletaFondo() {
        Dibujo.panel(lote, 8f, 60f, 136f, 432f, new Color(0.035f, 0.05f, 0.09f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.textoCentrado(lote, fuente, "HERRAMIENTAS", 76f, 480f, Configuracion.DORADO, 0.62f);
    }

    private void dibujarPropiedadesFondo() {
        Dibujo.panel(lote, 742f, 60f, 210f, 440f, new Color(0.05f, 0.07f, 0.12f, 0.98f),
            new Color(0.30f, 0.40f, 0.58f, 1f));
    }

    private void dibujarBarraSuperior() {
        Dibujo.rectangulo(lote, 0f, 500f, 960f, 40f, new Color(0.035f, 0.05f, 0.09f, 0.98f));
        Dibujo.rectangulo(lote, 0f, 499f, 960f, 1f, new Color(0.35f, 0.48f, 0.68f, 0.55f));
        float x = 10f;
        if (ui.boton("NUEVO", x, 506f, 76f, 28f)) pedirNuevo();
        x += 82f;
        if (ui.boton("CARGAR", x, 506f, 76f, 28f)) abrirSelector();
        x += 82f;
        if (ui.boton("GUARDAR", x, 506f, 76f, 28f)) guardar(false);
        x += 82f;
        if (ui.boton("DUPLICAR", x, 506f, 82f, 28f)) duplicarActual();
        x += 88f;
        if (ui.boton("BORRAR", x, 506f, 76f, 28f)) pedirBorrarArchivo();
        x += 82f;
        if (ui.boton("PROBAR", x, 506f, 76f, 28f)) probarNivel();
        x += 82f;
        if (ui.boton("DESHACER", x, 506f, 82f, 28f)) deshacer();
        x += 88f;
        if (ui.boton("REHACER", x, 506f, 82f, 28f)) rehacer();
        x += 88f;
        if (ui.boton("SALIR", x, 506f, 70f, 28f)) pedirSalir();
    }

    private void dibujarPaleta() {
        Dibujo.panel(lote, 8f, 60f, 136f, 432f, new Color(0.035f, 0.05f, 0.09f, 0.98f),
            new Color(0.28f, 0.38f, 0.54f, 1f));
        Dibujo.textoCentrado(lote, fuente, "HERRAMIENTAS", 76f, 480f, Configuracion.DORADO, 0.62f);
        Herramienta[] lista = Herramienta.values();
        for (int i = 0; i < lista.length; i++) {
            // 24 px por fila: las 16 herramientas quedan dentro del panel y
            // por encima de la barra de estado inferior.
            float y = 450f - i * 24f;
            String texto = nombreHerramienta(lista[i]);
            if (ui.botonActivo(texto, 14f, y, 124f, 23f) && herramienta != lista[i]) {
                herramienta = lista[i];
                tipoSel = TipoSeleccion.NINGUNO;
                indiceSel = -1;
                estado = "Herramienta: " + texto + ".";
            }
        }
    }

    private static String nombreHerramienta(Herramienta h) {
        switch (h) {
            case SELECCION: return "SELECCION";
            case PLATAFORMA: return "PLATAFORMA";
            case MURO: return "MURO";
            case LAVA: return "LAVA";
            case AGUA: return "AGUA";
            case GEMA: return "GEMA";
            case PLACA: return "PLACA";
            case PALANCA: return "PALANCA";
            case PUERTA: return "PUERTA";
            case PUENTE: return "PUENTE";
            case MOVIL: return "MOVIL";
            case CAJA: return "CAJA";
            case ORIGEN_FUEGO: return "ORIGEN FUEGO";
            case ORIGEN_AGUA: return "ORIGEN AGUA";
            case SALIDA_FUEGO: return "SALIDA FUEGO";
            case SALIDA_AGUA: return "SALIDA AGUA";
            default: return h.name();
        }
    }

    private void dibujarEstado() {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 56f, new Color(0.025f, 0.035f, 0.06f, 0.99f));
        Dibujo.rectangulo(lote, 0f, 55f, 960f, 1f, new Color(0.32f, 0.44f, 0.62f, 0.55f));
        Dibujo.texto(lote, fuente, recortar(nivel.nombre, 44) + (sucio ? "  *sin guardar" : ""),
            12f, 38f, sucio ? Configuracion.DORADO : Configuracion.TEXTO, 0.66f);
        Dibujo.texto(lote, fuente, "Herramienta: " + nombreHerramienta(herramienta)
            + "   Zoom " + Math.round(100f / zoom) + "%   Mouse " + Math.round(mundoX) + "," + Math.round(mundoY),
            12f, 17f, Configuracion.TEXTO_SUAVE, 0.58f);
        Dibujo.textoDerecha(lote, fuente, recortar(estado, 70), 948f, 38f,
            Configuracion.TEXTO_SUAVE, 0.56f);
        Dibujo.textoDerecha(lote, fuente, "Ctrl+Z deshacer   Ctrl+Y rehacer   F encuadrar   +/- zoom",
            948f, 17f, new Color(0.50f, 0.60f, 0.76f, 1f), 0.52f);
    }

    private void dibujarSelector() {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.62f));
        Dibujo.panel(lote, 70f, 48f, 820f, 444f, new Color(0.05f, 0.07f, 0.12f, 0.99f),
            new Color(0.42f, 0.55f, 0.78f, 1f));
        Dibujo.textoCentrado(lote, fuente, "CARGAR NIVEL", 480f, 468f, Configuracion.DORADO, 1.05f);

        // Pestañas
        boolean tabMis = pestanaSelector == 0;
        boolean tabHis = pestanaSelector == 1;
        if (ui.botonColor("MIS NIVELES", 140f, 432f, 180f, 30f,
            tabMis ? new Color(0.18f, 0.40f, 0.70f, 1f) : new Color(0.10f, 0.14f, 0.22f, 1f))) {
            if (!tabMis) { pestanaSelector = 0; paginaSelector = 0; recargarListaSelector(); }
        }
        if (ui.botonColor("HISTORIA", 330f, 432f, 180f, 30f,
            tabHis ? new Color(0.18f, 0.40f, 0.70f, 1f) : new Color(0.10f, 0.14f, 0.22f, 1f))) {
            if (!tabHis) { pestanaSelector = 1; paginaSelector = 0; recargarListaSelector(); }
        }
        Dibujo.texto(lote, fuente, tabMis
                ? "Niveles guardados en tu carpeta. Se abren para editar."
                : "Niveles de la campaña. Se abren como copia editable; al guardar se superpone en campaña.",
            140f, 418f, Configuracion.TEXTO_SUAVE, 0.54f);

        // Lista filtrada
        List<GestorNiveles.Entrada> visibles = filtrarSelector(pestanaSelector);
        if (visibles.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, tabMis
                    ? "No hay niveles guardados en tu carpeta."
                    : "No hay niveles de historia disponibles.",
                480f, 280f, Configuracion.TEXTO_SUAVE, 0.74f);
        } else {
            int porPagina = 6;
            int desde = paginaSelector * porPagina;
            int maxPagina = Math.max(0, (visibles.size() - 1) / porPagina);
            if (paginaSelector > maxPagina) paginaSelector = maxPagina;
            for (int i = 0; i < porPagina && desde + i < visibles.size(); i++) {
                GestorNiveles.Entrada e = visibles.get(desde + i);
                float y = 368f - i * 44f;
                Dibujo.panel(lote, 100f, y, 760f, 38f, new Color(0.06f, 0.08f, 0.14f, 0.98f),
                    new Color(0.26f, 0.36f, 0.52f, 1f));
                if (e.nivel == null) {
                    Dibujo.texto(lote, fuente, recortar(e.archivo, 40) + "  (invalido)", 114f, y + 24f,
                        Configuracion.ROJO, 0.60f);
                } else {
                    Dibujo.texto(lote, fuente, recortar(e.nivel.nombre, 32), 114f, y + 24f,
                        Configuracion.TEXTO, 0.70f);
                    Dibujo.texto(lote, fuente, Nivel.nombreTema(e.nivel.tema), 420f, y + 24f,
                        Configuracion.TEXTO_SUAVE, 0.56f);
                    String tag = (e.archivo != null && e.archivo.startsWith("HISTORIA:")) ? "Historia" : "Personal";
                    Dibujo.texto(lote, fuente, tag, 640f, y + 24f, Configuracion.TEXTO_SUAVE, 0.54f);
                }
                if (e.nivel != null) {
                    if (ui.boton("ABRIR", 760f, y + 5f, 84f, 28f)) {
                        cargarEntrada(e);
                        return;
                    }
                }
            }
            if (ui.boton("<", 340f, 94f, 50f, 28f) && paginaSelector > 0) paginaSelector--;
            Dibujo.textoCentrado(lote, fuente, "Página " + (paginaSelector + 1) + " / " + (maxPagina + 1),
                480f, 114f, Configuracion.TEXTO_SUAVE, 0.62f);
            if (ui.boton(">", 570f, 94f, 50f, 28f) && paginaSelector < maxPagina) paginaSelector++;
        }

        if (ui.boton("CERRAR", 390f, 56f, 180f, 30f)) selectorAbierto = false;
    }

    private void recargarListaSelector() {
        List<GestorNiveles.Entrada> base = new ArrayList<>();
        base.addAll(juego.getGestorNiveles().listar());
        for (Nivel h : NivelesIntegrados.todos()) {
            GestorNiveles.Entrada e = new GestorNiveles.Entrada();
            e.archivo = "HISTORIA:" + h.nombre;
            e.nivel = h;
            e.error = null;
            base.add(e);
        }
        Collections.sort(base, new Comparator<GestorNiveles.Entrada>() {
            @Override
            public int compare(GestorNiveles.Entrada a, GestorNiveles.Entrada b) {
                String na = a.nivel != null ? a.nivel.nombre : a.archivo;
                String nb = b.nivel != null ? b.nivel.nombre : b.archivo;
                return na.compareToIgnoreCase(nb);
            }
        });
        listaNiveles = base;
    }

    private List<GestorNiveles.Entrada> filtrarSelector(int pestana) {
        List<GestorNiveles.Entrada> out = new ArrayList<>();
        for (GestorNiveles.Entrada e : listaNiveles) {
            boolean esHistoria = e.archivo != null && e.archivo.startsWith("HISTORIA:");
            if (pestana == 1 && esHistoria) out.add(e);
            else if (pestana == 0 && !esHistoria) out.add(e);
        }
        return out;
    }

    private void dibujarConfirmacion() {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.64f));
        Dibujo.panel(lote, 270f, 176f, 420f, 190f, new Color(0.07f, 0.05f, 0.08f, 0.99f),
            Configuracion.DORADO);
        String titulo = "CONFIRMAR";
        String texto = "";
        if ("SALIR".equals(confirmar)) {
            titulo = "SALIR DEL EDITOR";
            texto = "Hay cambios sin guardar. Salir igualmente?";
        } else if ("NUEVO".equals(confirmar)) {
            titulo = "NUEVO NIVEL";
            texto = "Hay cambios sin guardar. Crear un nivel nuevo?";
        } else if ("CARGAR".equals(confirmar)) {
            titulo = "CARGAR OTRO NIVEL";
            texto = "Hay cambios sin guardar. Abrir la lista de niveles?";
        } else if ("BORRAR_ARCHIVO".equals(confirmar)) {
            titulo = "BORRAR ARCHIVO";
            texto = "Se borrara " + archivoActual + " de la carpeta de niveles.";
        } else if ("SOBRESCRIBIR".equals(confirmar)) {
            titulo = "SOBRESCRIBIR";
            texto = "Ya existe un nivel con ese nombre. Sobrescribirlo?";
        } else if ("REDIMENSIONAR".equals(confirmar)) {
            titulo = "REDIMENSIONAR ESCENARIO";
            texto = "Hay " + pendientesFueraCount + " objetos fuera al reducir. Se reubicaran dentro. Continuar?";
        }
        Dibujo.textoCentrado(lote, fuente, titulo, 480f, 332f, Configuracion.DORADO, 1.05f);
        Dibujo.textoCentrado(lote, fuente, texto, 480f, 282f, Configuracion.TEXTO, 0.72f);
        if (ui.botonColor("SI", 340f, 222f, 120f, 36f, Configuracion.VERDE)) {
            String accion = confirmar;
            confirmar = "";
            if ("SALIR".equals(accion)) salirDelEditor();
            else if ("NUEVO".equals(accion)) crearNuevo();
            else if ("CARGAR".equals(accion)) abrirSelectorAhora();
            else if ("BORRAR_ARCHIVO".equals(accion)) borrarArchivo();
            else if ("SOBRESCRIBIR".equals(accion)) guardar(true);
            else if ("REDIMENSIONAR".equals(accion)) ejecutarRedimension(pendienteAncho, pendienteAlto, true);
        }
        if (ui.boton("NO", 500f, 222f, 120f, 36f)) confirmar = "";
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return "";
        if (texto.length() <= maximo) return texto;
        return texto.substring(0, Math.max(0, maximo - 3)) + "...";
    }
}
