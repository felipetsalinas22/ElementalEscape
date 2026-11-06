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

    private Herramienta herramienta = Herramienta.SELECCION;
    private TipoSeleccion tipoSel = TipoSeleccion.NINGUNO;
    private int indiceSel = -1;

    private boolean arrastrandoCamara = false;
    private boolean arrastrandoObjeto = false;
    private boolean objetoMovido = false;
    private boolean creando = false;
    private float crearInicioX;
    private float crearInicioY;
    private float crearFinX;
    private float crearFinY;
    private float ultimoMundoX;
    private float ultimoMundoY;

    private final List<String> historial = new ArrayList<>();
    private int posHistorial = 0;
    private String archivoActual = "";
    private boolean sucio = false;

    private String estado = "Editor listo. Elige una herramienta o selecciona un objeto.";
    private String confirmar = "";
    private boolean selectorAbierto = false;
    private List<GestorNiveles.Entrada> listaNiveles = new ArrayList<>();
    private int paginaSelector = 0;

    public PantallaEditor(Juego juego, Nivel nivelInicial) {
        super(juego);
        this.nivelInicial = nivelInicial;
        this.nivel = nivelInicial;
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
        } else {
            nivel = nivelInicial;
            nivel.normalizar();
            archivoActual = buscarArchivo(nivel);
        }
        encuadrarNivel();
        reiniciarHistorial();
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
        n.salidaFuego.set(810f, 30f, 60f, 80f);
        n.salidaAgua.set(880f, 30f, 60f, 80f);
        n.agregar(new Nivel.Plataforma(0f, 0f, n.ancho, 30f, "PIEDRA"));
        n.agregar(new Nivel.Plataforma(0f, 0f, 18f, n.alto, "MURO"));
        n.agregar(new Nivel.Plataforma(n.ancho - 18f, 0f, 18f, n.alto, "MURO"));
        n.agregar(new Nivel.Plataforma(0f, n.alto - 16f, n.ancho, 16f, "MURO"));
        n.agregar(new Nivel.Plataforma(360f, 160f, 200f, 20f, "PIEDRA"));
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
        mundoX = camX + (ui.mouseX - 480f) * zoom;
        mundoY = camY + (ui.mouseY - 270f) * zoom;

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
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) moverSeleccion(-paso, 0f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) moverSeleccion(paso, 0f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) moverSeleccion(0f, paso);
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) moverSeleccion(0f, -paso);
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
            } else if (esHerramientaRectangulo(herramienta)) {
                creando = true;
                crearInicioX = mundoX;
                crearInicioY = mundoY;
                crearFinX = mundoX;
                crearFinY = mundoY;
            } else {
                crearPunto(herramienta, mundoX, mundoY);
            }
        }

        if (creando) {
            crearFinX = mundoX;
            crearFinY = mundoY;
            if (!ui.clicMantenido) {
                finalizarCreacion();
                creando = false;
            }
        }

        if (arrastrandoObjeto) {
            if (ui.clicMantenido && (Math.abs(mundoX - ultimoMundoX) > 0.01f
                || Math.abs(mundoY - ultimoMundoY) > 0.01f)) {
                moverSeleccion(mundoX - ultimoMundoX, mundoY - ultimoMundoY);
                objetoMovido = true;
                ultimoMundoX = mundoX;
                ultimoMundoY = mundoY;
            }
            if (!ui.clicMantenido) {
                arrastrandoObjeto = false;
                if (objetoMovido) confirmarCambio();
            }
        }
    }

    private boolean esHerramientaRectangulo(Herramienta h) {
        return h == Herramienta.PLATAFORMA || h == Herramienta.MURO || h == Herramienta.LAVA
            || h == Herramienta.AGUA || h == Herramienta.PUERTA || h == Herramienta.PUENTE
            || h == Herramienta.MOVIL;
    }

    private void limitarCamara() {
        camX = MathUtils.clamp(camX, -100f, nivel.ancho + 100f);
        camY = MathUtils.clamp(camY, -50f, nivel.alto + 50f);
    }

    private void cambiarZoom(float factor) {
        zoom = MathUtils.clamp(zoom * factor, 0.30f, 3f);
        estado = "Zoom " + Math.round(100f / zoom) + "%";
    }

    private void encuadrarNivel() {
        camX = nivel.ancho / 2f;
        camY = nivel.alto / 2f;
        float ajuste = Math.min(960f / Math.max(1f, nivel.ancho),
            540f / Math.max(1f, nivel.alto));
        zoom = MathUtils.clamp(ajuste, 0.30f, 1.2f);
        limitarCamara();
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

    private void moverSeleccion(float dx, float dy) {
        dx = redondear(dx);
        dy = redondear(dy);
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
            default: break;
        }
        sucio = true;
    }

    private void moverRect(Rectangle r, float dx, float dy) {
        r.x = MathUtils.clamp(r.x + dx, 0f, Math.max(0f, nivel.ancho - r.width));
        r.y = MathUtils.clamp(r.y + dy, 0f, Math.max(0f, nivel.alto - r.height));
    }

    private float redondear(float valor) {
        return Math.round(valor / Configuracion.PASO_EDITOR) * Configuracion.PASO_EDITOR;
    }

    private void finalizarCreacion() {
        float x = redondear(Math.min(crearInicioX, crearFinX));
        float y = redondear(Math.min(crearInicioY, crearFinY));
        float ancho = Math.abs(crearFinX - crearInicioX);
        float alto = Math.abs(crearFinY - crearInicioY);
        boolean clic = ancho < 4f && alto < 4f;

        switch (herramienta) {
            case PLATAFORMA:
                if (clic) { ancho = 140f; alto = 20f; }
                nivel.agregar(new Nivel.Plataforma(x, y, Math.max(10f, ancho), Math.max(10f, alto), "PIEDRA"));
                tipoSel = TipoSeleccion.PLATAFORMA;
                indiceSel = nivel.plataformas.length - 1;
                break;
            case MURO:
                if (clic) { ancho = 24f; alto = 180f; }
                nivel.agregar(new Nivel.Plataforma(x, y, Math.max(10f, ancho), Math.max(10f, alto), "MURO"));
                tipoSel = TipoSeleccion.PLATAFORMA;
                indiceSel = nivel.plataformas.length - 1;
                break;
            case LAVA:
                if (clic) { ancho = 100f; alto = 14f; }
                nivel.agregar(new Nivel.Peligro(x, y, Math.max(10f, ancho), Math.max(8f, alto), true));
                tipoSel = TipoSeleccion.PELIGRO;
                indiceSel = nivel.peligros.length - 1;
                break;
            case AGUA:
                if (clic) { ancho = 100f; alto = 14f; }
                nivel.agregar(new Nivel.Peligro(x, y, Math.max(10f, ancho), Math.max(8f, alto), false));
                tipoSel = TipoSeleccion.PELIGRO;
                indiceSel = nivel.peligros.length - 1;
                break;
            case PUERTA:
                if (clic) { ancho = 20f; alto = 250f; }
                nivel.agregar(new Nivel.Puerta(x, y, Math.max(10f, ancho), Math.max(40f, alto),
                    siguienteId(), "ALGUNA", 0f, false));
                tipoSel = TipoSeleccion.PUERTA;
                indiceSel = nivel.puertas.length - 1;
                break;
            case PUENTE:
                if (clic) { ancho = 160f; alto = 16f; }
                nivel.agregar(new Nivel.Puerta(x, y, Math.max(20f, ancho), Math.max(8f, alto),
                    siguienteId(), "ALGUNA", 0f, true));
                tipoSel = TipoSeleccion.PUERTA;
                indiceSel = nivel.puertas.length - 1;
                break;
            case MOVIL:
                if (clic) { ancho = 110f; alto = 18f; }
                ancho = Math.max(20f, ancho);
                alto = Math.max(10f, alto);
                nivel.agregar(new Nivel.PlataformaMovil(x, y, ancho, alto,
                    Math.min(nivel.ancho - 20f, x + 200f), y, Configuracion.VELOCIDAD_PLATAFORMA_MOVIL, 0));
                tipoSel = TipoSeleccion.MOVIL;
                indiceSel = nivel.moviles.length - 1;
                break;
            default:
                return;
        }
        sucio = true;
        confirmarCambio();
        estado = "Objeto creado. Ajusta sus propiedades a la derecha o arrastralo.";
    }

    private void crearPunto(Herramienta h, float x, float y) {
        switch (h) {
            case GEMA:
                nivel.agregar(new Nivel.Gema(x - 11f, y - 11f));
                tipoSel = TipoSeleccion.GEMA;
                indiceSel = nivel.gemas.length - 1;
                break;
            case PLACA:
                nivel.agregar(new Nivel.Boton(x - 28f, y - 6f, siguienteId(), "AMBOS"));
                tipoSel = TipoSeleccion.BOTON;
                indiceSel = nivel.botones.length - 1;
                break;
            case PALANCA:
                nivel.agregar(new Nivel.Palanca(x - 14f, y - 22f, siguienteId(), "AMBOS"));
                tipoSel = TipoSeleccion.PALANCA;
                indiceSel = nivel.palancas.length - 1;
                break;
            case CAJA:
                nivel.agregar(new Nivel.Caja(x - 20f, y - 20f, false));
                tipoSel = TipoSeleccion.CAJA;
                indiceSel = nivel.cajas.length - 1;
                break;
            case ORIGEN_FUEGO:
                nivel.origenFuegoX = x - 15f;
                nivel.origenFuegoY = y;
                tipoSel = TipoSeleccion.ORIGEN_FUEGO;
                break;
            case ORIGEN_AGUA:
                nivel.origenAguaX = x - 15f;
                nivel.origenAguaY = y;
                tipoSel = TipoSeleccion.ORIGEN_AGUA;
                break;
            case SALIDA_FUEGO:
                nivel.salidaFuego.set(x - 30f, y, 60f, 80f);
                tipoSel = TipoSeleccion.SALIDA_FUEGO;
                break;
            case SALIDA_AGUA:
                nivel.salidaAgua.set(x - 30f, y, 60f, 80f);
                tipoSel = TipoSeleccion.SALIDA_AGUA;
                break;
            default:
                return;
        }
        sucio = true;
        confirmarCambio();
        estado = "Objeto creado. Revisa sus propiedades.";
    }

    private int siguienteId() {
        int max = 0;
        for (Nivel.Boton b : nivel.botones) max = Math.max(max, b.id);
        for (Nivel.Palanca p : nivel.palancas) max = Math.max(max, p.id);
        for (Nivel.Puerta p : nivel.puertas) max = Math.max(max, p.id);
        for (Nivel.PlataformaMovil m : nivel.moviles) max = Math.max(max, m.id);
        return max + 1;
    }

    private void eliminarSeleccion() {
        if (tipoSel == TipoSeleccion.NINGUNO) {
            estado = "No hay ningun objeto seleccionado.";
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
            default: break;
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
            nuevo.normalizar();
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
            nivel.normalizar();
            juego.getGestorNiveles().guardar(nivel, permitirSobrescribir);
            archivoActual = juego.getGestorNiveles().archivoDeNivel(nivel);
            sucio = false;
            estado = "Guardado en " + archivoActual + ".";
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
        listaNiveles = juego.getGestorNiveles().listar();
        paginaSelector = 0;
        selectorAbierto = true;
    }

    private void cargarEntrada(GestorNiveles.Entrada entrada) {
        if (entrada == null || entrada.nivel == null) {
            estado = entrada != null && entrada.error != null ? entrada.error : "Nivel invalido.";
            return;
        }
        try {
            nivel = juego.getGestorNiveles().cargar(entrada.archivo);
            archivoActual = entrada.archivo;
            tipoSel = TipoSeleccion.NINGUNO;
            indiceSel = -1;
            selectorAbierto = false;
            encuadrarNivel();
            reiniciarHistorial();
            estado = "Cargado " + archivoActual + ".";
        } catch (IOException e) {
            estado = e.getMessage();
        }
    }

    private void duplicarActual() {
        if (archivoActual.isEmpty()) {
            estado = "Guarda el nivel antes de duplicarlo.";
            return;
        }
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                if (texto == null || texto.trim().isEmpty()) return;
                try {
                    Nivel copia = juego.getGestorNiveles().duplicar(archivoActual, texto.trim());
                    estado = "Copia creada: " + copia.nombre + ".";
                } catch (IOException e) {
                    estado = e.getMessage();
                }
            }
            @Override public void canceled() { }
        }, "Nombre de la copia", nivel.nombre + " copia", "Nuevo nombre");
    }

    private void probarNivel() {
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
        cambiarPantalla(PantallaJuego.pruebaEditor(juego, nivel));
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

    private void dibujarPropiedadesNivel(float y) {
        Dibujo.texto(lote, fuente, "PROPIEDADES DEL NIVEL", 752f, 486f, Configuracion.DORADO, 0.72f);
        Dibujo.texto(lote, fuente, recortar(nivel.nombre, 24), 752f, 462f,
            Configuracion.TEXTO, 0.68f);
        y = 438f;
        if (ui.boton("EDITAR NOMBRE", 752f, y - 8f, 188f, 28f)) {
            pedirTexto("Nombre del nivel", nivel.nombre, new CambioTexto() {
                @Override public void aplicar(String texto) {
                    nivel.nombre = texto.trim().isEmpty() ? "Nuevo nivel" : texto.trim();
                }
            });
        }
        y -= 34f;
        if (ui.boton("EDITAR PISTA", 752f, y - 8f, 188f, 28f)) {
            pedirTexto("Pista del nivel", nivel.pista, new CambioTexto() {
                @Override public void aplicar(String texto) { nivel.pista = texto; }
            });
        }
        y -= 34f;
        if (ui.boton("AUTOR: " + recortar(nivel.autor, 14), 752f, y - 8f, 188f, 28f)) {
            pedirTexto("Autor del nivel", nivel.autor, new CambioTexto() {
                @Override public void aplicar(String texto) { nivel.autor = texto; }
            });
        }
        y -= 34f;
        filaCiclo("TEMA", nivel.tema, y, new Runnable() {
            @Override public void run() { nivel.tema = siguienteTema(nivel.tema); }
        });
        y -= 30f;
        filaNumero("ANCHO", nivel.ancho, y, 50f, new Ajuste() {
            @Override public void cambiar(float d) {
                nivel.ancho = MathUtils.clamp(nivel.ancho + d,
                    Configuracion.ANCHO_MINIMO_NIVEL, Configuracion.ANCHO_MAXIMO_NIVEL);
            }
        });
        y -= 30f;
        filaNumero("ALTO", nivel.alto, y, 50f, new Ajuste() {
            @Override public void cambiar(float d) {
                nivel.alto = MathUtils.clamp(nivel.alto + d,
                    Configuracion.ALTO_MINIMO_NIVEL, Configuracion.ALTO_MAXIMO_NIVEL);
            }
        });
        y -= 38f;
        Dibujo.texto(lote, fuente, "Elige una herramienta y", 752f, y,
            Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.texto(lote, fuente, "haz clic o arrastra en el area.", 752f, y - 18f,
            Configuracion.TEXTO_SUAVE, 0.60f);
    }

    private void dibujarPropiedadesPlataforma(float y) {
        Nivel.Plataforma o = nivel.plataformas[indiceSel];
        Dibujo.texto(lote, fuente, "PLATAFORMA", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaRectangulo(o.rect, 458f);
        filaCiclo("ESTILO", o.estilo, y, new Runnable() {
            @Override public void run() { o.estilo = siguienteEstilo(o.estilo); }
        });
        y -= 38f;
        if (ui.botonColor("ELIMINAR", 752f, y - 8f, 188f, 28f, Configuracion.ROJO)) {
            eliminarSeleccion();
        }
    }

    private void dibujarPropiedadesPeligro(float y) {
        Nivel.Peligro o = nivel.peligros[indiceSel];
        Dibujo.texto(lote, fuente, "PELIGRO", 752f, 486f, Configuracion.DORADO, 0.72f);
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
        y = filaRectangulo(o.rect, 458f);
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
                    if (fuego) nivel.origenFuegoX = MathUtils.clamp(nivel.origenFuegoX + d, 0f, nivel.ancho - 30f);
                    else nivel.origenAguaX = MathUtils.clamp(nivel.origenAguaX + d, 0f, nivel.ancho - 30f);
                }
            });
            y -= 30f;
            filaNumero("Y", fuego ? nivel.origenFuegoY : nivel.origenAguaY, y, pasoEdicion(), new Ajuste() {
                @Override public void cambiar(float d) {
                    if (fuego) nivel.origenFuegoY = MathUtils.clamp(nivel.origenFuegoY + d, 0f, nivel.alto - 46f);
                    else nivel.origenAguaY = MathUtils.clamp(nivel.origenAguaY + d, 0f, nivel.alto - 46f);
                }
            });
            y -= 34f;
            Dibujo.texto(lote, fuente, "Coloca el origen sobre una plataforma.", 752f, y,
                Configuracion.TEXTO_SUAVE, 0.58f);
        } else {
            Rectangle r = tipoSel == TipoSeleccion.SALIDA_FUEGO ? nivel.salidaFuego : nivel.salidaAgua;
            y = filaRectangulo(r, 458f);
            Dibujo.texto(lote, fuente, "La salida debe quedar libre.", 752f, y,
                Configuracion.TEXTO_SUAVE, 0.58f);
        }
    }

    private float filaRectangulo(Rectangle r, float y) {
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
        y -= 30f;
        filaNumero("ANCHO", r.width, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                r.width = MathUtils.clamp(r.width + d, 4f, nivel.ancho - r.x);
            }
        });
        y -= 30f;
        filaNumero("ALTO", r.height, y, pasoEdicion(), new Ajuste() {
            @Override public void cambiar(float d) {
                r.height = MathUtils.clamp(r.height + d, 4f, nivel.alto - r.y);
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
        String[] estilos = {"PIEDRA", "MADERA", "METAL", "MURO"};
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
    protected void dibujar(float delta) {
        ScreenUtils.clear(0.020f, 0.028f, 0.055f, 1f);

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

    private void dibujarMundo() {
        float visibleAncho = 960f * zoom;
        float visibleAlto = 540f * zoom;
        dibujante.fondo(nivel.tema, tiempo, camX - visibleAncho / 2f, camY - visibleAlto / 2f,
            visibleAncho, visibleAlto);

        float minX = Math.max(-100f, camX - visibleAncho / 2f);
        float maxX = Math.min(nivel.ancho + 100f, camX + visibleAncho / 2f);
        float minY = Math.max(-100f, camY - visibleAlto / 2f);
        float maxY = Math.min(nivel.alto + 100f, camY + visibleAlto / 2f);
        Color grid = new Color(0.45f, 0.60f, 0.85f, 0.09f);
        float primerX = (float) Math.floor(minX / 40f) * 40f;
        float primerY = (float) Math.floor(minY / 40f) * 40f;
        for (float x = primerX; x <= maxX; x += 40f) {
            Dibujo.rectangulo(lote, x, minY, 1f, maxY - minY, grid);
        }
        for (float y = primerY; y <= maxY; y += 40f) {
            Dibujo.rectangulo(lote, minX, y, maxX - minX, 1f, grid);
        }

        Dibujo.rectangulo(lote, 0f, 0f, nivel.ancho, 3f, Configuracion.DORADO);
        Dibujo.rectangulo(lote, 0f, nivel.alto - 3f, nivel.ancho, 3f, Configuracion.DORADO);
        Dibujo.rectangulo(lote, 0f, 0f, 3f, nivel.alto, Configuracion.DORADO);
        Dibujo.rectangulo(lote, nivel.ancho - 3f, 0f, 3f, nivel.alto, Configuracion.DORADO);

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

        if (creando) {
            float x = Math.min(crearInicioX, crearFinX);
            float y = Math.min(crearInicioY, crearFinY);
            float ancho = Math.max(1f, Math.abs(crearFinX - crearInicioX));
            float alto = Math.max(1f, Math.abs(crearFinY - crearInicioY));
            Dibujo.rectangulo(lote, x, y, ancho, alto, new Color(1f, 1f, 1f, 0.12f));
            marco(new Rectangle(x, y, ancho, alto), Configuracion.DORADO);
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
        dibujarBarraSuperior();
        dibujarPaleta();
        dibujarPropiedades();
        dibujarEstado();
        if (selectorAbierto) dibujarSelector();
        if (!confirmar.isEmpty()) dibujarConfirmacion();
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
        Dibujo.panel(lote, 90f, 62f, 780f, 416f, new Color(0.05f, 0.07f, 0.12f, 0.99f),
            new Color(0.42f, 0.55f, 0.78f, 1f));
        Dibujo.textoCentrado(lote, fuente, "CARGAR NIVEL GUARDADO", 480f, 452f,
            Configuracion.DORADO, 1.05f);

        if (listaNiveles.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, "No hay niveles personalizados en la carpeta.",
                480f, 300f, Configuracion.TEXTO_SUAVE, 0.74f);
        } else {
            int desde = paginaSelector * 7;
            for (int i = 0; i < 7 && desde + i < listaNiveles.size(); i++) {
                GestorNiveles.Entrada e = listaNiveles.get(desde + i);
                float y = 396f - i * 40f;
                Dibujo.panel(lote, 112f, y, 736f, 34f, new Color(0.06f, 0.08f, 0.14f, 0.98f),
                    new Color(0.26f, 0.36f, 0.52f, 1f));
                if (e.nivel == null) {
                    Dibujo.texto(lote, fuente, e.archivo + "  (archivo invalido)", 124f, y + 22f,
                        Configuracion.ROJO, 0.60f);
                    continue;
                }
                Dibujo.texto(lote, fuente, recortar(e.nivel.nombre, 42), 124f, y + 22f,
                    Configuracion.TEXTO, 0.68f);
                Dibujo.texto(lote, fuente, Nivel.nombreTema(e.nivel.tema), 480f, y + 22f,
                    Configuracion.TEXTO_SUAVE, 0.56f);
                if (ui.boton("CARGAR", 760f, y + 3f, 76f, 28f)) cargarEntrada(e);
            }
            int maxPagina = Math.max(0, (listaNiveles.size() - 1) / 7);
            if (ui.boton("<", 330f, 80f, 46f, 28f) && paginaSelector > 0) paginaSelector--;
            Dibujo.textoCentrado(lote, fuente, "Pagina " + (paginaSelector + 1) + " / " + (maxPagina + 1),
                480f, 100f, Configuracion.TEXTO_SUAVE, 0.62f);
            if (ui.boton(">", 584f, 80f, 46f, 28f) && paginaSelector < maxPagina) paginaSelector++;
        }
        if (ui.boton("CANCELAR", 400f, 76f, 160f, 30f)) selectorAbierto = false;
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
        } else if ("BORRAR_ARCHIVO".equals(confirmar)) {
            titulo = "BORRAR ARCHIVO";
            texto = "Se borrara " + archivoActual + " de la carpeta de niveles.";
        } else if ("SOBRESCRIBIR".equals(confirmar)) {
            titulo = "SOBRESCRIBIR";
            texto = "Ya existe un nivel con ese nombre. Sobrescribirlo?";
        }
        Dibujo.textoCentrado(lote, fuente, titulo, 480f, 332f, Configuracion.DORADO, 1.05f);
        Dibujo.textoCentrado(lote, fuente, texto, 480f, 282f, Configuracion.TEXTO, 0.72f);
        if (ui.botonColor("SI", 340f, 222f, 120f, 36f, Configuracion.VERDE)) {
            String accion = confirmar;
            confirmar = "";
            if ("SALIR".equals(accion)) salirDelEditor();
            else if ("NUEVO".equals(accion)) crearNuevo();
            else if ("BORRAR_ARCHIVO".equals(accion)) borrarArchivo();
            else if ("SOBRESCRIBIR".equals(accion)) guardar(true);
        }
        if (ui.boton("NO", 500f, 222f, 120f, 36f)) confirmar = "";
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return "";
        if (texto.length() <= maximo) return texto;
        return texto.substring(0, Math.max(0, maximo - 3)) + "...";
    }
}
