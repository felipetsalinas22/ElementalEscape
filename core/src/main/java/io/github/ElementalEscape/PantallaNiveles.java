package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Seleccion de niveles.
 * Pestana de campana con desbloqueo progresivo y pestana de niveles creados
 * por el usuario (leidos de la carpeta JSON externa).
 */
public class PantallaNiveles extends PantallaBase {
    private int pestana = 0;
    private float tiempo = 0f;
    private List<GestorNiveles.Entrada> personalizados = new ArrayList<>();
    private int pagina = 0;
    private static final int POR_PAGINA = 6;

    public PantallaNiveles(Juego juego) {
        super(juego);
    }

    @Override
    protected void alMostrar() {
        recargarPersonalizados();
    }

    private void recargarPersonalizados() {
        personalizados = juego.getGestorNiveles().listar();
        int maxPagina = Math.max(0, (personalizados.size() - 1) / POR_PAGINA);
        if (pagina > maxPagina) pagina = maxPagina;
    }

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) irAlMenu();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) && pestana == 1 && pagina > 0) pagina--;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) && pestana == 1) {
            int maxPagina = Math.max(0, (personalizados.size() - 1) / POR_PAGINA);
            if (pagina < maxPagina) pagina++;
        }
    }

    @Override
    protected void dibujar(float delta) {
        aplicarVistaUI();
        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        lote.begin();
        fondoEstrellado(tiempo, new Color(0.025f, 0.035f, 0.075f, 1f),
            new Color(0.10f, 0.07f, 0.20f, 1f));
        tituloPantalla("SELECCION DE NIVELES", "Elijan una sala y coordinen sus elementos", Configuracion.DORADO);

        if (ui.botonActivo("CAMPANA", 330f, 432f, 130f, 30f) && pestana != 0) {
            pestana = 0;
        }
        if (ui.botonActivo("MIS NIVELES", 468f, 432f, 140f, 30f) && pestana != 1) {
            pestana = 1;
            recargarPersonalizados();
        }
        if (ui.boton("EDITOR", 616f, 432f, 100f, 30f)) {
            cambiarPantalla(new PantallaEditor(juego, null));
        }

        if (pestana == 0) dibujarCampana();
        else dibujarPersonalizados();

        if (ui.boton("VOLVER AL MENU", 24f, 20f, 190f, 34f)) irAlMenu();
        Dibujo.textoDerecha(lote, fuente, "ESC vuelve al menu", 936f, 42f,
            Configuracion.TEXTO_SUAVE, 0.62f);
        lote.end();
    }

    private void dibujarCampana() {
        Progreso progreso = juego.getProgreso();
        int completados = progreso.nivelesCompletados();
        float ancho = 172f;
        float alto = 150f;
        float inicioX = 24f;

        for (int i = 0; i < Configuracion.TOTAL_NIVELES; i++) {
            int columna = i % 5;
            int fila = i / 5;
            float x = inicioX + columna * (ancho + 12f);
            float y = fila == 0 ? 260f : 98f;
            boolean desbloqueado = i <= completados;
            boolean completado = progreso.estaCompletado(i);
            Puntuacion.ResultadoNivel resultado = progreso.niveles[i];

            Color borde = completado ? Configuracion.VERDE
                : (desbloqueado ? new Color(0.42f, 0.54f, 0.76f, 1f)
                : new Color(0.22f, 0.25f, 0.34f, 1f));
            Color relleno = completado ? new Color(0.05f, 0.12f, 0.10f, 0.96f)
                : (desbloqueado ? new Color(0.07f, 0.09f, 0.16f, 0.97f)
                : new Color(0.05f, 0.06f, 0.10f, 0.92f));
            Dibujo.panel(lote, x, y, ancho, alto, relleno, borde);

            Dibujo.circulo(lote, x + 20f, y + alto - 22f, 28f,
                desbloqueado ? Configuracion.DORADO : new Color(0.30f, 0.33f, 0.42f, 1f));
            Dibujo.textoCentrado(lote, fuente, String.valueOf(i + 1), x + 34f, y + alto - 14f,
                new Color(0.08f, 0.08f, 0.12f, 1f), 0.82f);

            Dibujo.texto(lote, fuente, recortar(NivelesIntegrados.nombre(i), 20),
                x + 10f, y + alto - 54f, desbloqueado ? Configuracion.TEXTO
                    : new Color(0.45f, 0.48f, 0.58f, 1f), 0.68f);
            Dibujo.texto(lote, fuente, Nivel.nombreTema(NivelesIntegrados.crear(i).tema),
                x + 10f, y + alto - 74f, Configuracion.TEXTO_SUAVE, 0.55f);

            if (completado && resultado != null) {
                Dibujo.texto(lote, fuente, "Mejor: " + resultado.puntosFuego + " - " + resultado.puntosAgua,
                    x + 10f, y + 52f, Configuracion.TEXTO, 0.62f);
                String ganador = resultado.ganador == 0 ? "Empate"
                    : (resultado.ganador == 1 ? "Gano Fuego" : "Gano Agua");
                Dibujo.texto(lote, fuente, ganador, x + 10f, y + 34f, Configuracion.VERDE, 0.58f);
                int estrellas = resultado.todasGemas ? 3 : 2;
                for (int e = 0; e < 3; e++) {
                    Color c = e < estrellas ? Configuracion.DORADO
                        : new Color(0.30f, 0.34f, 0.44f, 1f);
                    Dibujo.rectanguloRotado(lote, x + ancho - 54f + e * 15f, y + 42f, 9f, 9f,
                        4.5f, 4.5f, 45f, c);
                }
            } else if (desbloqueado) {
                Dibujo.texto(lote, fuente, "Disponible", x + 10f, y + 42f, Configuracion.DORADO, 0.66f);
            } else {
                Dibujo.texto(lote, fuente, "Completa el nivel " + completados, x + 10f, y + 42f,
                    new Color(0.50f, 0.54f, 0.64f, 1f), 0.55f);
            }

            if (desbloqueado) {
                Dibujo.texto(lote, fuente, "JUGAR", x + 10f, y + 16f, Configuracion.VERDE, 0.62f);
                if (zonaClic(x, y, ancho, alto)) {
                    cambiarPantalla(PantallaJuego.nivelSuelto(juego,
                        NivelesIntegrados.crear(i), i, true));
                }
            }
        }

        Dibujo.textoCentrado(lote, fuente,
            completados + " / " + Configuracion.TOTAL_NIVELES + " niveles completados",
            480f, 74f, Configuracion.TEXTO_SUAVE, 0.70f);
    }

    private void dibujarPersonalizados() {
        if (personalizados.isEmpty()) {
            Dibujo.panel(lote, 190f, 200f, 580f, 160f, new Color(0.06f, 0.08f, 0.14f, 0.97f),
                new Color(0.35f, 0.45f, 0.62f, 1f));
            Dibujo.textoCentrado(lote, fuente, "TODAVIA NO HAY NIVELES PERSONALIZADOS",
                480f, 310f, Configuracion.TEXTO, 0.90f);
            Dibujo.textoCentrado(lote, fuente,
                "Crea uno en el editor y se guardara como JSON en tu carpeta de usuario.",
                480f, 280f, Configuracion.TEXTO_SUAVE, 0.66f);
            if (ui.boton("ABRIR EDITOR", 380f, 228f, 200f, 38f)) {
                cambiarPantalla(new PantallaEditor(juego, null));
            }
            return;
        }

        int desde = pagina * POR_PAGINA;
        for (int i = 0; i < POR_PAGINA && desde + i < personalizados.size(); i++) {
            GestorNiveles.Entrada entrada = personalizados.get(desde + i);
            float y = 374f - i * 52f;
            Dibujo.panel(lote, 34f, y, 892f, 44f, new Color(0.06f, 0.08f, 0.14f, 0.96f),
                new Color(0.30f, 0.40f, 0.58f, 1f));
            if (entrada.nivel == null) {
                Dibujo.texto(lote, fuente, entrada.archivo, 50f, y + 27f, Configuracion.ROJO, 0.68f);
                Dibujo.texto(lote, fuente, entrada.error, 220f, y + 27f,
                    Configuracion.TEXTO_SUAVE, 0.58f);
                continue;
            }
            Nivel n = entrada.nivel;
            Dibujo.texto(lote, fuente, recortar(n.nombre, 34), 50f, y + 27f,
                Configuracion.TEXTO, 0.76f);
            Dibujo.texto(lote, fuente, Nivel.nombreTema(n.tema), 430f, y + 27f,
                Configuracion.TEXTO_SUAVE, 0.60f);
            Dibujo.texto(lote, fuente, "Autor: " + (n.autor == null || n.autor.isEmpty() ? "-" : n.autor),
                620f, y + 27f, Configuracion.TEXTO_SUAVE, 0.58f);
            if (ui.boton("JUGAR", 760f, y + 7f, 74f, 30f)) {
                cambiarPantalla(PantallaJuego.nivelSuelto(juego, n, 0, false));
            }
            if (ui.boton("EDITAR", 840f, y + 7f, 74f, 30f)) {
                cambiarPantalla(new PantallaEditor(juego, n));
            }
        }

        int maxPagina = Math.max(0, (personalizados.size() - 1) / POR_PAGINA);
        if (ui.boton("<", 330f, 72f, 50f, 30f) && pagina > 0) pagina--;
        Dibujo.textoCentrado(lote, fuente, "Pagina " + (pagina + 1) + " / " + (maxPagina + 1),
            480f, 92f, Configuracion.TEXTO_SUAVE, 0.64f);
        if (ui.boton(">", 580f, 72f, 50f, 30f) && pagina < maxPagina) pagina++;
    }

    /** Click manual sobre una tarjeta: consume el clic para que no active otra cosa. */
    private boolean zonaClic(float x, float y, float ancho, float alto) {
        if (ui.clic && ui.dentro(x, y, ancho, alto)) {
            ui.clic = false;
            return true;
        }
        return false;
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return "";
        if (texto.length() <= maximo) return texto;
        return texto.substring(0, Math.max(0, maximo - 3)) + "...";
    }
}
