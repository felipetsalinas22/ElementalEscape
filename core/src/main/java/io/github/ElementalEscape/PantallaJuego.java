package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de juego. Sirve para campana, niveles sueltos, pruebas del editor y
 * modo online. En online el anfitrion simula y el invitado recibe Instantaneas.
 */
public class PantallaJuego extends PantallaBase {
    public enum Origen { CAMPANA, NIVELES, EDITOR, ONLINE }

    private final GestorRed red;
    private final Origen origen;
    private Nivel nivel;
    private int indiceNivel;
    private boolean campana;
    private Partida partida;

    private DibujanteJuego dibujante;
    private OrthographicCamera camaraMundo;
    private FitViewport vistaMundo;
    private float camX;
    private float camY;
    private float tiempo;
    private float transicion = 1f;

    private final Partida.Entrada entradaFuego = new Partida.Entrada();
    private final Partida.Entrada entradaAgua = new Partida.Entrada();
    private final Partida.Entrada entradaLocal = new Partida.Entrada();
    private boolean wPrevio;
    private boolean arribaPrevio;
    private boolean espacioPrevio;

    private float envioEntrada = 0f;
    private float envioEstado = 0f;
    private long versionEstadoAplicada = -1L;
    private long versionNivelAplicada = -1L;
    private int generacionRedVista = -1;
    private final List<String> eventosRed = new ArrayList<>();
    private boolean resultadoRegistrado = false;
    private boolean finalMostrado = false;
    private boolean saliendo = false;
    private String avisoRed = "";
    private float avisoRedTiempo = 0f;

    private PantallaJuego(Juego juego, Nivel nivel, int indiceNivel, boolean campana,
                          GestorRed red, Origen origen) {
        super(juego);
        this.nivel = nivel;
        this.indiceNivel = indiceNivel;
        this.campana = campana;
        this.red = red != null ? red : GestorRed.local();
        this.origen = origen;
    }

    public static PantallaJuego campana(Juego juego, int indice) {
        return new PantallaJuego(juego, NivelesIntegrados.crear(indice), indice, true,
            GestorRed.local(), Origen.CAMPANA);
    }

    public static PantallaJuego nivelSuelto(Juego juego, Nivel nivel, int indice, boolean campana) {
        return new PantallaJuego(juego, nivel, indice, campana, GestorRed.local(), Origen.NIVELES);
    }

    public static PantallaJuego pruebaEditor(Juego juego, Nivel nivel) {
        return new PantallaJuego(juego, nivel, 0, false, GestorRed.local(), Origen.EDITOR);
    }

    public static PantallaJuego online(Juego juego, GestorRed red, Nivel nivel,
                                       int indice, boolean campana) {
        return new PantallaJuego(juego, nivel, indice, campana, red, Origen.ONLINE);
    }

    @Override
    protected void alMostrar() {
        dibujante = new DibujanteJuego(lote, fuente);
        camaraMundo = new OrthographicCamera();
        vistaMundo = new FitViewport(960f, 540f, camaraMundo);
        vistaMundo.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        juego.getSonido().setActivo(juego.getAjustes().sonido);
        if (nivel != null) {
            cargarPartida(nivel, indiceNivel, campana);
        } else {
            camX = 480f;
            camY = 270f;
        }
    }

    @Override
    protected void alRedimensionar(int ancho, int alto) {
        if (vistaMundo != null) vistaMundo.update(ancho, alto, true);
    }

    private void cargarPartida(Nivel n, int indice, boolean esCampana) {
        nivel = n;
        indiceNivel = indice;
        campana = esCampana;
        partida = new Partida(n, indice, esCampana);
        partida.particulas.setActivas(juego.getAjustes().particulas);
        versionEstadoAplicada = -1L;
        resultadoRegistrado = false;
        finalMostrado = false;
        transicion = 1f;
        eventosRed.clear();
        camX = 480f;
        camY = 270f;
        centrarCamara(true);
        if (red.esAnfitrion() && red.conectado) {
            red.enviarNivel(nivel, indiceNivel, campana);
            red.enviarEstado(partida, juego.getProgreso());
        }
    }

    // =========================================================
    //                     ACTUALIZACION
    // =========================================================

    @Override
    protected void actualizar(float dt) {
        tiempo += dt;
        if (transicion > 0f) transicion = Math.max(0f, transicion - dt * 2.8f);
        if (avisoRedTiempo > 0f) {
            avisoRedTiempo -= dt;
            if (avisoRedTiempo <= 0f) avisoRed = "";
        }

        if (red.esCliente()) {
            sincronizarNivel();
            if (partida == null) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) salirDePantalla();
                return;
            }
            actualizarCliente(dt);
            return;
        }

        if (partida == null) return;
        actualizarLocalOAnfitrion(dt);
    }

    private void sincronizarNivel() {
        if (red.getVersionNivel() == versionNivelAplicada) return;
        Nivel remoto = red.getNivelRemoto();
        if (remoto == null) return;
        versionNivelAplicada = red.getVersionNivel();
        cargarPartida(remoto, red.getIndiceNivelRemoto(), red.isCampanaRemota());
        aviso("Nivel recibido del anfitrion.");
    }

    private void actualizarCliente(float dt) {
        if (red.generacion != generacionRedVista) {
            generacionRedVista = red.generacion;
            versionEstadoAplicada = -1L;
            if (red.conectado) aviso("Reconectado. Sincronizando partida...");
        }
        if (!red.conectado) {
            aviso("Conexion perdida. Reintentando...");
        }

        String comando;
        while ((comando = red.consumirComando()) != null) {
            if ("VOLVER".equals(comando)) {
                salirDePantalla();
                return;
            }
        }

        Instantanea s = red.getUltimaInstantanea();
        if (s != null && red.getVersionInstantanea() != versionEstadoAplicada) {
            versionEstadoAplicada = red.getVersionInstantanea();
            partida.aplicarInstantanea(s, Jugador.Tipo.AGUA);
            procesarEventos(s.eventos);
            if (s.completado && !resultadoRegistrado) {
                resultadoRegistrado = true;
                if (s.resultado != null && (origen == Origen.CAMPANA || origen == Origen.ONLINE)) {
                    juego.getProgreso().registrar(s.nivel, s.resultado);
                    juego.guardarProgreso();
                }
                if (s.nivel >= Configuracion.TOTAL_NIVELES - 1 && (origen == Origen.CAMPANA || origen == Origen.ONLINE)) {
                    finalMostrado = true;
                }
            }
            if (s.derrota) resultadoRegistrado = true;
        }

        leerEntradaCliente();
        if (!partida.completado && !partida.derrota && !partida.pausado) {
            partida.predecirJugador(Jugador.Tipo.AGUA, dt, entradaLocal);
        }
        partida.particulas.actualizar(dt);

        envioEntrada += dt;
        if (envioEntrada >= 1f / Configuracion.FRECUENCIA_ENTRADA && red.conectado) {
            envioEntrada = 0f;
            red.enviarEntrada(entradaLocal);
            entradaLocal.saltoPresionado = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !partida.completado && !partida.derrota) {
            red.enviarComando("PAUSA");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && (partida.derrota || partida.completado)) {
            red.enviarComando("REINICIAR");
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            && partida.completado) {
            red.enviarComando("SIGUIENTE");
        }

        actualizarCamara(dt);
    }

    private void actualizarLocalOAnfitrion(float dt) {
        String comando;
        while ((comando = red.consumirComando()) != null) procesarComando(comando);

        leerEntradasLocal();
        if (red.esAnfitrion()) {
            if (red.conectado) {
                Partida.Entrada remota = red.consumirEntradaRemota();
                entradaAgua.izquierda = remota.izquierda;
                entradaAgua.derecha = remota.derecha;
                entradaAgua.saltoMantenido = remota.saltoMantenido;
                entradaAgua.saltoPresionado = remota.saltoPresionado;
            } else {
                entradaAgua.limpiar();
            }
        }

        partida.actualizar(dt, entradaFuego, entradaAgua);
        String[] eventos = partida.eventosActuales();
        for (String evento : eventos) {
            sonar(evento);
            eventosRed.add(evento);
        }

        if (partida.completado && !resultadoRegistrado) {
            resultadoRegistrado = true;
            if (campana && origen != Origen.EDITOR) {
                juego.getProgreso().registrar(indiceNivel, partida.resultado);
                juego.guardarProgreso();
            }
            if (indiceNivel >= Configuracion.TOTAL_NIVELES - 1
                && (origen == Origen.CAMPANA || origen == Origen.ONLINE)) {
                finalMostrado = true;
            }
        }

        if (red.esAnfitrion()) {
            if (red.generacion != generacionRedVista) {
                generacionRedVista = red.generacion;
                versionEstadoAplicada = -1L;
                if (red.conectado) {
                    red.enviarNivel(nivel, indiceNivel, campana);
                    eventosRed.clear();
                }
            }
            envioEstado += dt;
            if (red.conectado && envioEstado >= 1f / Configuracion.FRECUENCIA_ESTADO) {
                envioEstado = 0f;
                red.enviarEstado(partida, juego.getProgreso(),
                    eventosRed.toArray(new String[0]));
                eventosRed.clear();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !partida.completado && !partida.derrota) {
            partida.pausado = !partida.pausado;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && (partida.derrota || partida.completado)) {
            reiniciarPartida();
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            && partida.completado) {
            avanzarNivel();
        }

        actualizarCamara(dt);
    }

    private void procesarComando(String comando) {
        if (comando == null) return;
        if ("PAUSA".equals(comando)) {
            if (partida != null && !partida.completado && !partida.derrota) {
                partida.pausado = !partida.pausado;
            }
        } else if ("REINICIAR".equals(comando)) {
            reiniciarPartida();
        } else if ("SIGUIENTE".equals(comando)) {
            if (partida != null && partida.completado) avanzarNivel();
        } else if ("SALIR".equals(comando)) {
            salirDePantalla();
        } else if ("VOLVER".equals(comando)) {
            salirDePantalla();
        }
    }

    private void leerEntradasLocal() {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean arriba = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean espacio = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean izq = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean der = Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        entradaFuego.izquierda = a;
        entradaFuego.derecha = d;
        entradaFuego.saltoMantenido = w;
        entradaFuego.saltoPresionado = w && !wPrevio;

        if (red.esAnfitrion()) {
            // El agua viene de la red; no se toca con las flechas locales.
        } else {
            entradaAgua.izquierda = izq;
            entradaAgua.derecha = der;
            entradaAgua.saltoMantenido = arriba;
            entradaAgua.saltoPresionado = arriba && !arribaPrevio;
        }

        wPrevio = w;
        arribaPrevio = arriba;
        espacioPrevio = espacio;
    }

    private void leerEntradaCliente() {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean arriba = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean espacio = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        entradaLocal.izquierda = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        entradaLocal.derecha = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        entradaLocal.saltoMantenido = w || arriba || espacio;
        entradaLocal.saltoPresionado = entradaLocal.saltoMantenido && !(wPrevio || arribaPrevio || espacioPrevio);
        wPrevio = w;
        arribaPrevio = arriba;
        espacioPrevio = espacio;
    }

    private void actualizarCamara(float dt) {
        if (partida == null) return;
        float objetivoX = (partida.fuego.x + partida.agua.x) / 2f + Configuracion.ANCHO_PERSONAJE / 2f;
        float objetivoY = (partida.fuego.y + partida.agua.y) / 2f + Configuracion.ALTO_PERSONAJE / 2f;
        float mitadAncho = 480f;
        float mitadAlto = 270f;
        if (nivel.ancho <= mitadAncho * 2f) objetivoX = nivel.ancho / 2f;
        else objetivoX = MathUtils.clamp(objetivoX, mitadAncho, nivel.ancho - mitadAncho);
        if (nivel.alto <= mitadAlto * 2f) objetivoY = nivel.alto / 2f;
        else objetivoY = MathUtils.clamp(objetivoY, mitadAlto, nivel.alto - mitadAlto);

        float suavizado = Math.min(1f, dt * 6.5f);
        camX += (objetivoX - camX) * suavizado;
        camY += (objetivoY - camY) * suavizado;

        float sacudida = 0f;
        if (juego.getAjustes().sacudida) sacudida = partida.sacudida;
        float sx = (float) Math.sin(tiempo * 71f) * sacudida;
        float sy = (float) Math.cos(tiempo * 63f) * sacudida * 0.7f;
        camaraMundo.position.set(camX + sx, camY + sy, 0f);
        camaraMundo.update();
    }

    private void centrarCamara(boolean instantaneo) {
        if (nivel == null) return;
        float x = nivel.ancho / 2f;
        float y = nivel.alto / 2f;
        if (instantaneo) { camX = x; camY = y; }
        if (camaraMundo != null) {
            camaraMundo.position.set(camX, camY, 0f);
            camaraMundo.update();
        }
    }

    // =========================================================
    //                       ACCIONES
    // =========================================================

    private void reiniciarPartida() {
        if (nivel == null) return;
        cargarPartida(nivel, indiceNivel, campana);
        if (red.esAnfitrion() && red.conectado) {
            red.enviarNivel(nivel, indiceNivel, campana);
        }
        aviso("Nivel reiniciado.");
    }

    private void avanzarNivel() {
        if (origen == Origen.EDITOR) {
            salirDePantalla();
            return;
        }
        if (origen == Origen.NIVELES) {
            salirDePantalla();
            return;
        }
        if (indiceNivel + 1 < Configuracion.TOTAL_NIVELES && campana) {
            cargarPartida(NivelesIntegrados.crear(indiceNivel + 1), indiceNivel + 1, true);
            aviso("Nivel " + (indiceNivel + 1) + " de " + Configuracion.TOTAL_NIVELES);
        } else {
            finalMostrado = true;
        }
    }

    private void salirDePantalla() {
        if (saliendo) return;
        saliendo = true;
        if (red.esAnfitrion()) red.enviarComando("VOLVER");
        red.cerrar();
        if (origen == Origen.EDITOR) {
            cambiarPantalla(new PantallaEditor(juego, nivel));
        } else if (origen == Origen.NIVELES) {
            cambiarPantalla(new PantallaNiveles(juego));
        } else if (origen == Origen.ONLINE) {
            cambiarPantalla(new PantallaOnline(juego));
        } else {
            cambiarPantalla(new MenuPrincipal(juego));
        }
    }

    private void aviso(String texto) {
        avisoRed = texto;
        avisoRedTiempo = 2.4f;
    }

    private void sonar(String evento) {
        juego.getSonido().reproducir(evento);
    }

    private void procesarEventos(String[] eventos) {
        if (eventos == null) return;
        for (String evento : eventos) {
            sonar(evento);
            if (partida == null) continue;
            if ("gema".equals(evento)) {
                partida.particulas.chispas(partida.agua.x + 15f, partida.agua.y + 23f,
                    Configuracion.VERDE, 8);
            } else if ("muerte".equals(evento)) {
                partida.particulas.chispas(partida.agua.x + 15f, partida.agua.y + 23f,
                    Configuracion.ROJO, 14);
            } else if ("victoria".equals(evento)) {
                partida.particulas.chispas(partida.agua.x + 15f, partida.agua.y + 23f,
                    Configuracion.DORADO, 20);
            } else if ("aterrizar".equals(evento)) {
                partida.particulas.polvo(partida.agua.x + 15f, partida.agua.y, 4);
            }
        }
    }

    // =========================================================
    //                       DIBUJO
    // =========================================================

    @Override
    protected void dibujar(float delta) {
        if (partida == null) {
            dibujarEspera();
            return;
        }

        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        vistaMundo.apply();
        camaraMundo.update();
        lote.setProjectionMatrix(camaraMundo.combined);
        lote.begin();
        float vx = camaraMundo.position.x - 480f;
        float vy = camaraMundo.position.y - 270f;
        dibujante.fondo(nivel.tema, tiempo, vx, vy, 960f, 540f);
        dibujante.mundo(partida, tiempo, red.esCliente());
        lote.end();

        aplicarVistaUI();
        lote.begin();
        dibujante.hud(partida, red, juego, red.esCliente());

        if (partida.completado) {
            if (finalMostrado) dibujarFinal();
            else dibujarResultados();
        } else if (partida.derrota) {
            dibujarDerrota();
        } else if (partida.pausado) {
            dibujarPausa();
        }

        if (transicion > 0f) {
            Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, transicion));
        }

        if (avisoRedTiempo > 0f && !avisoRed.isEmpty()) {
            Dibujo.panel(lote, 300f, 112f, 360f, 34f, new Color(0.04f, 0.06f, 0.10f, 0.94f),
                new Color(0.35f, 0.55f, 0.75f, 0.95f));
            Dibujo.textoCentrado(lote, fuente, avisoRed, 480f, 135f, Configuracion.TEXTO, 0.70f);
        }

        if (red.esCliente() && !red.conectado) {
            Dibujo.panel(lote, 300f, 150f, 360f, 70f, new Color(0.10f, 0.03f, 0.04f, 0.94f),
                Configuracion.ROJO);
            Dibujo.textoCentrado(lote, fuente, "SIN CONEXION", 480f, 200f, Configuracion.ROJO, 0.90f);
            Dibujo.textoCentrado(lote, fuente, "El juego reintenta solo. ESC para salir.",
                480f, 172f, Configuracion.TEXTO, 0.65f);
        }
        lote.end();
    }

    private void dibujarEspera() {
        aplicarVistaUI();
        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        lote.begin();
        Dibujo.degradado(lote, 0f, 0f, 960f, 540f, new Color(0.08f, 0.06f, 0.18f, 1f));
        Dibujo.textoCentrado(lote, fuente, "SALA " + (red.codigo.isEmpty() ? "" : red.codigo),
            480f, 350f, Configuracion.DORADO, 1.4f);
        Dibujo.textoCentrado(lote, fuente, "Esperando al anfitrion...", 480f, 310f,
            Configuracion.TEXTO, 1.0f);
        Dibujo.textoCentrado(lote, fuente, red.getEstado(), 480f, 275f,
            Configuracion.TEXTO_SUAVE, 0.72f);
        Dibujo.textoCentrado(lote, fuente, "IP del anfitrion y puerto " + GestorRed.PUERTO,
            480f, 240f, Configuracion.TEXTO_SUAVE, 0.64f);
        if (ui.boton("SALIR", 400f, 170f, 160f, 42f)) salirDePantalla();
        lote.end();
    }

    private void dibujarPausa() {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.62f));
        Dibujo.panel(lote, 280f, 118f, 400f, 304f, new Color(0.05f, 0.07f, 0.12f, 0.98f),
            new Color(0.40f, 0.55f, 0.78f, 1f));
        Dibujo.textoCentrado(lote, fuente, "PAUSA", 480f, 380f, Configuracion.DORADO, 1.5f);
        Dibujo.textoCentrado(lote, fuente, "Nivel " + (indiceNivel + 1) + " - " + nivel.nombre,
            480f, 350f, Configuracion.TEXTO_SUAVE, 0.68f);
        if (ui.boton("CONTINUAR", 320f, 282f, 320f, 42f)) accionPausa();
        if (ui.boton("REINICIAR NIVEL", 320f, 232f, 320f, 42f)) accionReiniciar();
        if (ui.boton("AJUSTES", 320f, 182f, 320f, 42f)) cambiarPantalla(new PantallaAjustes(juego));
        if (ui.boton("VOLVER AL MENU", 320f, 132f, 320f, 42f)) accionSalir();
        Dibujo.textoCentrado(lote, fuente, "ESC para continuar", 480f, 112f,
            Configuracion.TEXTO_SUAVE, 0.58f);
    }

    private void dibujarResultados() {
        Puntuacion.ResultadoNivel r = partida.resultado;
        if (r == null) return;
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.60f));
        // Los protagonistas enmarcan el resultado y refuerzan la direccion artistica.
        Dibujo.brillo(lote, 82f, 250f, 180f, new Color(1f, 0.34f, 0.10f, 0.13f));
        Dibujo.brillo(lote, 878f, 250f, 180f, new Color(0.20f, 0.60f, 1f, 0.13f));
        dibujante.dibujarAvatarMenu(82f, 210f, true, tiempo, 1.45f);
        dibujante.dibujarAvatarMenu(878f, 210f, false, tiempo, 1.45f);
        Dibujo.panel(lote, 132f, 54f, 696f, 432f, new Color(0.05f, 0.07f, 0.12f, 0.99f),
            new Color(0.45f, 0.62f, 0.85f, 1f));
        Dibujo.rectangulo(lote, 132f, 484f, 696f, 2f, new Color(0.75f, 0.62f, 0.25f, 0.72f));
        Dibujo.brillo(lote, 480f, 452f, 360f, new Color(1f, 0.82f, 0.35f, 0.08f));
        Dibujo.textoCentrado(lote, fuente, "NIVEL COMPLETADO", 480f, 456f, Configuracion.DORADO, 1.35f);
        Dibujo.textoCentrado(lote, fuente, nivel.nombre, 480f, 430f, Configuracion.TEXTO, 0.82f);
        Dibujo.textoCentrado(lote, fuente, "Tiempo " + minutosSegundos(r.tiempo), 480f, 406f,
            Configuracion.TEXTO_SUAVE, 0.70f);

        Dibujo.linea(lote, 250f, 390f, 710f, 390f, 1f, new Color(0.35f, 0.44f, 0.60f, 0.45f));
        Dibujo.circulo(lote, 270f, 365f, 16f, Configuracion.FUEGO);
        Dibujo.texto(lote, fuente, "FUEGO", 292f, 372f, Configuracion.FUEGO_CLARO, 0.82f);
        Dibujo.circulo(lote, 510f, 365f, 16f, Configuracion.AGUA);
        Dibujo.texto(lote, fuente, "AGUA", 532f, 372f, Configuracion.AGUA_CLARA, 0.82f);

        filaResultado("Puntos", r.puntosFuego, r.puntosAgua, 336f, true);
        filaResultado("Gemas", r.gemasFuego, r.gemasAgua, 306f, false);
        filaResultado("Mecanismos", r.mecanismosFuego, r.mecanismosAgua, 276f, false);
        filaResultado("Muertes", r.muertesFuego, r.muertesAgua, 246f, false);
        filaResultado("Bono tiempo", r.bonusFuego, r.bonusAgua, 216f, false);

        String ganador = r.ganador == 0 ? "EMPATE" : (r.ganador == 1 ? "GANA FUEGO" : "GANA AGUA");
        Dibujo.textoCentrado(lote, fuente, ganador + "   (" + r.puntosFuego + " - " + r.puntosAgua + ")",
            480f, 184f, r.ganador == 0 ? Configuracion.DORADO : Configuracion.VERDE, 0.95f);
        if (r.todasGemas) {
            Dibujo.textoCentrado(lote, fuente, "Todas las gemas recogidas: bono +"
                + Configuracion.PUNTOS_BONO_TODAS_GEMAS + " para cada uno", 480f, 160f,
                Configuracion.DORADO, 0.64f);
        }

        if (origen == Origen.EDITOR) {
            if (ui.boton("VOLVER AL EDITOR", 340f, 82f, 280f, 40f)) accionSalir();
        } else if (origen == Origen.NIVELES) {
            if (ui.boton("VOLVER A NIVELES", 320f, 82f, 200f, 40f)) accionSalir();
            if (ui.boton("REPETIR", 530f, 82f, 130f, 40f)) accionReiniciar();
        } else if (indiceNivel + 1 < Configuracion.TOTAL_NIVELES || !campana) {
            if (ui.boton("SIGUIENTE NIVEL", 300f, 82f, 220f, 40f)) accionAvanzar();
            if (ui.boton("REPETIR", 530f, 82f, 130f, 40f)) accionReiniciar();
        } else {
            if (ui.boton("VER RANKING FINAL", 330f, 82f, 220f, 40f)) accionAvanzar();
            if (ui.boton("REPETIR", 560f, 82f, 100f, 40f)) accionReiniciar();
        }
        if (ui.boton("MENU", 120f, 82f, 140f, 40f)) accionSalir();
        Dibujo.textoCentrado(lote, fuente, "ENTER continua  -  R reinicia", 480f, 68f,
            Configuracion.TEXTO_SUAVE, 0.55f);
    }

    private void filaResultado(String etiqueta, int fuego, int agua, float y, boolean grande) {
        Dibujo.texto(lote, fuente, etiqueta, 270f, y, Configuracion.TEXTO_SUAVE, 0.68f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(fuego), 470f, y,
            grande ? Configuracion.FUEGO_CLARO : Configuracion.TEXTO, grande ? 0.90f : 0.70f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(agua), 710f, y,
            grande ? Configuracion.AGUA_CLARA : Configuracion.TEXTO, grande ? 0.90f : 0.70f);
    }

    private void dibujarDerrota() {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.64f));
        Dibujo.panel(lote, 280f, 130f, 400f, 280f, new Color(0.08f, 0.04f, 0.06f, 0.98f),
            Configuracion.ROJO);
        Dibujo.textoCentrado(lote, fuente, "SIN VIDAS", 480f, 370f, Configuracion.ROJO, 1.45f);
        Dibujo.textoCentrado(lote, fuente, "Se quedaron sin vidas en el nivel.", 480f, 336f,
            Configuracion.TEXTO, 0.74f);
        Dibujo.textoCentrado(lote, fuente, "Fuego: " + partida.vidasFuego + "   Agua: " + partida.vidasAgua,
            480f, 306f, Configuracion.TEXTO_SUAVE, 0.70f);
        if (ui.boton("REINTENTAR", 320f, 240f, 320f, 42f)) accionReiniciar();
        if (ui.boton("VOLVER", 320f, 190f, 320f, 42f)) accionSalir();
        Dibujo.textoCentrado(lote, fuente, "R para reintentar", 480f, 166f,
            Configuracion.TEXTO_SUAVE, 0.58f);
    }

    private void dibujarFinal() {
        int fuego;
        int agua;
        int completados;
        int ganadosFuego;
        int ganadosAgua;
        if (red.esCliente() && red.getUltimaInstantanea() != null) {
            Instantanea s = red.getUltimaInstantanea();
            fuego = s.totalesFuego;
            agua = s.totalesAgua;
            completados = s.nivelesCompletados;
            ganadosFuego = s.nivelesGanadosFuego;
            ganadosAgua = s.nivelesGanadosAgua;
        } else {
            Progreso p = juego.getProgreso();
            fuego = p.puntosTotales(Jugador.Tipo.FUEGO);
            agua = p.puntosTotales(Jugador.Tipo.AGUA);
            completados = p.nivelesCompletados();
            ganadosFuego = p.nivelesGanados(Jugador.Tipo.FUEGO);
            ganadosAgua = p.nivelesGanados(Jugador.Tipo.AGUA);
        }
        int ganador = fuego == agua ? 0 : (fuego > agua ? 1 : 2);
        String texto = ganador == 0 ? "EMPATE PERFECTO" : (ganador == 1 ? "GANA FUEGO" : "GANA AGUA");

        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0f, 0f, 0f, 0.68f));
        Dibujo.panel(lote, 130f, 58f, 700f, 424f, new Color(0.05f, 0.06f, 0.12f, 0.99f),
            new Color(0.55f, 0.68f, 0.95f, 1f));
        Dibujo.textoCentrado(lote, fuente, "CAMPANA COMPLETADA", 480f, 448f, Configuracion.DORADO, 1.55f);
        Dibujo.textoCentrado(lote, fuente, texto, 480f, 406f,
            ganador == 0 ? Configuracion.DORADO : Configuracion.VERDE, 1.2f);

        Dibujo.panel(lote, 190f, 236f, 260f, 132f, new Color(0.10f, 0.07f, 0.06f, 0.90f),
            Configuracion.FUEGO);
        Dibujo.panel(lote, 510f, 236f, 260f, 132f, new Color(0.05f, 0.09f, 0.15f, 0.90f),
            Configuracion.AGUA);
        Dibujo.textoCentrado(lote, fuente, "FUEGO", 320f, 342f, Configuracion.FUEGO_CLARO, 0.90f);
        Dibujo.textoCentrado(lote, fuente, String.valueOf(fuego), 320f, 302f, Configuracion.TEXTO, 1.8f);
        Dibujo.textoCentrado(lote, fuente, "niveles ganados: " + ganadosFuego, 320f, 268f,
            Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.textoCentrado(lote, fuente, "AGUA", 640f, 342f, Configuracion.AGUA_CLARA, 0.90f);
        Dibujo.textoCentrado(lote, fuente, String.valueOf(agua), 640f, 302f, Configuracion.TEXTO, 1.8f);
        Dibujo.textoCentrado(lote, fuente, "niveles ganados: " + ganadosAgua, 640f, 268f,
            Configuracion.TEXTO_SUAVE, 0.62f);

        Dibujo.textoCentrado(lote, fuente, completados + " / " + Configuracion.TOTAL_NIVELES
            + " niveles completados", 480f, 200f, Configuracion.TEXTO, 0.78f);
        if (ui.boton("VOLVER AL MENU", 340f, 100f, 280f, 44f)) accionSalir();
        Dibujo.textoCentrado(lote, fuente, "Gracias por jugar Elemental Escape", 480f, 78f,
            Configuracion.TEXTO_SUAVE, 0.62f);
    }

    // =========================================================
    //                   ACCIONES DE INTERFAZ
    // =========================================================

    private void accionPausa() {
        if (red.esCliente()) {
            red.enviarComando("PAUSA");
        } else {
            aplazar(new Runnable() {
                @Override public void run() { partida.pausado = false; }
            });
        }
    }

    private void accionReiniciar() {
        if (red.esCliente()) {
            red.enviarComando("REINICIAR");
        } else {
            aplazar(new Runnable() {
                @Override public void run() { reiniciarPartida(); }
            });
        }
    }

    private void accionAvanzar() {
        if (red.esCliente()) {
            red.enviarComando("SIGUIENTE");
        } else {
            aplazar(new Runnable() {
                @Override public void run() { avanzarNivel(); }
            });
        }
    }

    private void accionSalir() {
        if (red.esCliente()) red.enviarComando("SALIR");
        aplazar(new Runnable() {
            @Override public void run() { salirDePantalla(); }
        });
    }

    @Override
    public void dispose() {
        // Si se sale por AJUSTES/MENU o se cierra la ventana, no dejamos el
        // socket ni el hilo de red vivos en segundo plano.
        if (red != null && !red.esLocal()) red.cerrar();
        super.dispose();
    }
}
