package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Base de todas las pantallas. Usa una resolucion virtual de 960x540 con
 * FitViewport: el juego se ve igual en cualquier ventana y el raton se convierte
 * automaticamente a coordenadas de interfaz.
 */
public abstract class PantallaBase implements Screen {
    protected final Juego juego;
    protected SpriteBatch lote;
    protected BitmapFont fuente;
    protected OrthographicCamera camaraUI;
    protected FitViewport vistaUI;
    protected Interfaz ui;
    protected boolean recursosListos = false;
    private Runnable accionPendiente = null;

    protected PantallaBase(Juego juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        crearRecursos();
        alMostrar();
    }

    protected void crearRecursos() {
        if (recursosListos) return;
        lote = new SpriteBatch();
        fuente = new BitmapFont();
        fuente.getData().markupEnabled = false;
        camaraUI = new OrthographicCamera(960f, 540f);
        camaraUI.position.set(480f, 270f, 0f);
        vistaUI = new FitViewport(960f, 540f, camaraUI);
        vistaUI.apply();
        camaraUI.update();
        Dibujo.iniciar();
        ui = new Interfaz(lote, fuente, vistaUI);
        recursosListos = true;
    }

    /** Las subclases crean aqui sus camaras o cargan datos. */
    protected void alMostrar() { }

    @Override
    public void render(float delta) {
        if (delta > 0.1f) delta = 0.1f;
        ui.actualizar();
        actualizar(delta);
        dibujar(delta);
        if (accionPendiente != null) {
            Runnable accion = accionPendiente;
            accionPendiente = null;
            accion.run();
        }
    }

    protected abstract void actualizar(float delta);

    protected abstract void dibujar(float delta);

    protected void aplicarVistaUI() {
        vistaUI.apply();
        camaraUI.update();
        lote.setProjectionMatrix(camaraUI.combined);
    }

    /** Fondo comun de menus: degradado, estrellas y linea inferior. */
    protected void fondoEstrellado(float tiempo, Color arriba, Color abajo) {
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, arriba);
        Dibujo.degradado(lote, 0f, 0f, 960f, 460f, abajo);
        for (int i = 0; i < 46; i++) {
            float x = (i * 149f + 37f) % 960f;
            float y = (i * 83f + 19f) % 540f;
            float pulso = 0.30f + 0.70f * (0.5f + 0.5f * (float) Math.sin(tiempo * 1.4f + i));
            Dibujo.circulo(lote, x, y, 2f + (i % 3),
                new Color(0.75f, 0.88f, 1f, 0.12f * pulso));
        }
        Dibujo.rectangulo(lote, 0f, 66f, 960f, 1f, new Color(0.35f, 0.45f, 0.70f, 0.20f));
    }

    /** Encabezado uniforme para pantallas de menu. */
    protected void tituloPantalla(String titulo, String subtitulo, Color color) {
        Dibujo.textoCentrado(lote, fuente, titulo, 480f, 500f, color, 1.65f);
        if (subtitulo != null && !subtitulo.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, subtitulo, 480f, 470f, Configuracion.TEXTO_SUAVE, 0.72f);
        }
    }

    @Override
    public void resize(int ancho, int alto) {
        if (vistaUI != null) vistaUI.update(ancho, alto, true);
        alRedimensionar(ancho, alto);
    }

    protected void alRedimensionar(int ancho, int alto) { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (lote != null) {
            lote.dispose();
            lote = null;
        }
        if (fuente != null) {
            fuente.dispose();
            fuente = null;
        }
        ui = null;
        recursosListos = false;
    }

    /** Atajo para volver al menu desde cualquier pantalla. */
    protected void irAlMenu() {
        cambiarPantalla(new MenuPrincipal(juego));
    }

    /** Ejecuta una accion despues de cerrar el lote de dibujo, evitando cambiar de pantalla mientras se dibuja. */
    protected void aplazar(Runnable accion) {
        if (accionPendiente == null) accionPendiente = accion;
    }

    protected void cambiarPantalla(final Screen pantalla) {
        aplazar(new Runnable() {
            @Override public void run() { juego.setScreen(pantalla); }
        });
    }

    protected static String textoONo(boolean valor) {
        return valor ? "SI" : "NO";
    }

    protected static float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    protected static String minutosSegundos(float segundos) {
        int total = Math.max(0, (int) segundos);
        int m = total / 60;
        int s = total % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    protected void salirAplicacion() {
        Gdx.app.exit();
    }
}
