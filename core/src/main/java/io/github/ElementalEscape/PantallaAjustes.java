package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

/** Opciones de audio, efectos, video y borrado del progreso. */
public class PantallaAjustes extends PantallaBase {
    private float tiempo = 0f;
    private boolean confirmarBorrado = false;
    private String mensaje = "";
    private float mensajeTiempo = 0f;

    public PantallaAjustes(Juego juego) {
        super(juego);
    }

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        if (mensajeTiempo > 0f) {
            mensajeTiempo -= delta;
            if (mensajeTiempo <= 0f) mensaje = "";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) irAlMenu();
    }

    @Override
    protected void dibujar(float delta) {
        aplicarVistaUI();
        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        lote.begin();
        fondoEstrellado(tiempo, new Color(0.025f, 0.035f, 0.075f, 1f),
            new Color(0.08f, 0.07f, 0.17f, 1f));
        tituloPantalla("AJUSTES", "Se guardan automaticamente en ajustes.json", Configuracion.DORADO);

        Ajustes a = juego.getAjustes();
        Dibujo.panel(lote, 130f, 104f, 700f, 340f, new Color(0.05f, 0.07f, 0.12f, 0.97f),
            new Color(0.32f, 0.43f, 0.62f, 1f));

        boolean sonido = ui.interruptor("SONIDO Y EFECTOS", a.sonido, 160f, 388f, 640f, 38f);
        if (sonido != a.sonido) {
            a.sonido = sonido;
            juego.getSonido().setActivo(sonido);
            juego.guardarAjustes();
        }

        boolean particulas = ui.interruptor("PARTICULAS", a.particulas, 160f, 342f, 640f, 38f);
        if (particulas != a.particulas) {
            a.particulas = particulas;
            juego.guardarAjustes();
        }

        boolean sacudida = ui.interruptor("SACUDIDA DE CAMARA", a.sacudida, 160f, 296f, 640f, 38f);
        if (sacudida != a.sacudida) {
            a.sacudida = sacudida;
            juego.guardarAjustes();
        }

        boolean pantalla = ui.interruptor("PANTALLA COMPLETA", a.pantallaCompleta, 160f, 250f, 640f, 38f);
        if (pantalla != a.pantallaCompleta) {
            a.pantallaCompleta = pantalla;
            juego.guardarAjustes();
            aplazar(new Runnable() {
                @Override public void run() { juego.aplicarPantallaCompleta(true); }
            });
        }

        Dibujo.texto(lote, fuente, "VOLUMEN", 172f, 218f, Configuracion.TEXTO, 0.70f);
        ui.barra(280f, 210f, 380f, 12f, a.volumen, Configuracion.AGUA);
        Dibujo.texto(lote, fuente, Math.round(a.volumen * 100f) + "%", 666f, 218f,
            Configuracion.TEXTO_SUAVE, 0.62f);
        if (ui.boton("-", 700f, 202f, 40f, 30f) && a.volumen > 0f) {
            a.volumen = Math.max(0f, a.volumen - 0.1f);
            juego.guardarAjustes();
        }
        if (ui.boton("+", 748f, 202f, 40f, 30f) && a.volumen < 1f) {
            a.volumen = Math.min(1f, a.volumen + 0.1f);
            juego.guardarAjustes();
        }

        Dibujo.linea(lote, 160f, 186f, 800f, 186f, 1f, new Color(0.35f, 0.44f, 0.60f, 0.45f));
        Dibujo.texto(lote, fuente, "Progreso guardado en la carpeta del usuario .elemental-escape",
            172f, 162f, Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.texto(lote, fuente, "Los niveles personalizados viven en .elemental-escape/niveles",
            172f, 142f, Configuracion.TEXTO_SUAVE, 0.62f);

        String textoBoton = confirmarBorrado ? "CONFIRMAR: BORRAR TODO EL PROGRESO" : "BORRAR PROGRESO";
        if (ui.botonColor(textoBoton, 480f, 112f, 300f, 36f,
            confirmarBorrado ? Configuracion.ROJO : new Color(0.34f, 0.20f, 0.24f, 1f))) {
            if (!confirmarBorrado) {
                confirmarBorrado = true;
                mostrarMensaje("Pulsa otra vez para confirmar. Esto no borra los niveles.");
            } else {
                juego.reiniciarProgreso();
                confirmarBorrado = false;
                mostrarMensaje("Progreso de la campana borrado.");
            }
        }

        if (!mensaje.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, mensaje, 480f, 84f, Configuracion.DORADO, 0.64f);
        }
        if (ui.boton("VOLVER AL MENU", 24f, 20f, 190f, 34f)) irAlMenu();
        lote.end();
    }

    private void mostrarMensaje(String texto) {
        mensaje = texto;
        mensajeTiempo = 3.4f;
    }
}
