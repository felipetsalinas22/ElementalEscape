package io.github.ElementalEscape;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

/**
 * Punto de entrada LibGDX y estado compartido entre pantallas:
 * progreso, ajustes, niveles personalizados y sonido generado.
 */
public class Juego extends Game {
    private Progreso progreso;
    private Ajustes ajustes;
    private GestorProgreso gestorProgreso;
    private GestorNiveles gestorNiveles;
    private Sonido sonido;

    @Override
    public void create() {
        Dibujo.iniciar();
        gestorProgreso = GestorProgreso.predeterminado();
        gestorNiveles = GestorNiveles.predeterminado();
        progreso = gestorProgreso.cargarProgreso();
        ajustes = gestorProgreso.cargarAjustes();
        sonido = new Sonido();
        sonido.setActivo(ajustes.sonido);
        sonido.setVolumen(ajustes.volumen);
        aplicarPantallaCompleta(true);
        setScreen(new MenuPrincipal(this));
    }

    public Progreso getProgreso() {
        if (progreso == null) progreso = new Progreso();
        return progreso;
    }

    public Ajustes getAjustes() {
        if (ajustes == null) ajustes = new Ajustes();
        return ajustes;
    }

    public GestorProgreso getGestorProgreso() { return gestorProgreso; }
    public GestorNiveles getGestorNiveles() { return gestorNiveles; }
    public Sonido getSonido() { return sonido; }

    public void guardarProgreso() {
        if (gestorProgreso != null) gestorProgreso.guardarProgreso(getProgreso());
    }

    public void guardarAjustes() {
        if (gestorProgreso != null) gestorProgreso.guardarAjustes(getAjustes());
        if (sonido != null) {
            sonido.setActivo(getAjustes().sonido);
            sonido.setVolumen(getAjustes().volumen);
        }
    }

    public void aplicarPantallaCompleta(boolean cambiarAhora) {
        if (!cambiarAhora) return;
        try {
            if (getAjustes().pantallaCompleta) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                Gdx.graphics.setWindowedMode(960, 540);
            }
        } catch (Exception e) {
            Gdx.app.log("Video", "No se pudo cambiar el modo de pantalla: " + e.getMessage());
        }
    }

    public void reiniciarProgreso() {
        progreso = new Progreso();
        guardarProgreso();
    }

    @Override
    public void dispose() {
        if (screen != null) screen.dispose();
        if (sonido != null) sonido.cerrar();
        Dibujo.liberar();
        super.dispose();
    }
}
