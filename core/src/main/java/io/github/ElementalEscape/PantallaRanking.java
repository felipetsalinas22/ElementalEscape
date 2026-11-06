package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.List;

/** Ranking competitivo: totales, ganador por nivel y logros. */
public class PantallaRanking extends PantallaBase {
    private float tiempo = 0f;

    public PantallaRanking(Juego juego) {
        super(juego);
    }

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) irAlMenu();
    }

    @Override
    protected void dibujar(float delta) {
        aplicarVistaUI();
        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        lote.begin();
        fondoEstrellado(tiempo, new Color(0.025f, 0.035f, 0.075f, 1f),
            new Color(0.12f, 0.07f, 0.18f, 1f));
        tituloPantalla("RANKING DE LA CAMPANA", "Puntos de cada jugador en los diez niveles", Configuracion.DORADO);

        Progreso progreso = juego.getProgreso();
        int fuego = progreso.puntosTotales(Jugador.Tipo.FUEGO);
        int agua = progreso.puntosTotales(Jugador.Tipo.AGUA);
        int ganador = progreso.ganadorFinal();

        Dibujo.panel(lote, 30f, 386f, 260f, 64f, new Color(0.10f, 0.06f, 0.06f, 0.97f),
            Configuracion.FUEGO);
        Dibujo.texto(lote, fuente, "FUEGO", 48f, 428f, Configuracion.FUEGO_CLARO, 0.72f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(fuego), 272f, 416f,
            Configuracion.TEXTO, 1.30f);
        Dibujo.texto(lote, fuente, "niveles ganados: " + progreso.nivelesGanados(Jugador.Tipo.FUEGO),
            48f, 398f, Configuracion.TEXTO_SUAVE, 0.58f);

        Dibujo.panel(lote, 670f, 386f, 260f, 64f, new Color(0.05f, 0.08f, 0.14f, 0.97f),
            Configuracion.AGUA);
        Dibujo.texto(lote, fuente, "AGUA", 688f, 428f, Configuracion.AGUA_CLARA, 0.72f);
        Dibujo.textoDerecha(lote, fuente, String.valueOf(agua), 912f, 416f,
            Configuracion.TEXTO, 1.30f);
        Dibujo.texto(lote, fuente, "niveles ganados: " + progreso.nivelesGanados(Jugador.Tipo.AGUA),
            688f, 398f, Configuracion.TEXTO_SUAVE, 0.58f);

        String textoGanador = ganador == 0 ? "EMPATE" : (ganador == 1 ? "GANA FUEGO" : "GANA AGUA");
        Color colorGanador = ganador == 0 ? Configuracion.DORADO
            : (ganador == 1 ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA);
        Dibujo.panel(lote, 310f, 386f, 340f, 64f, new Color(0.05f, 0.07f, 0.12f, 0.97f),
            colorGanador);
        Dibujo.textoCentrado(lote, fuente, textoGanador, 480f, 426f, colorGanador, 1.10f);
        Dibujo.textoCentrado(lote, fuente, progreso.nivelesCompletados() + " / "
            + Configuracion.TOTAL_NIVELES + " niveles completados", 480f, 402f,
            Configuracion.TEXTO_SUAVE, 0.62f);

        // Tabla por nivel.
        Dibujo.panel(lote, 30f, 84f, 560f, 288f, new Color(0.05f, 0.07f, 0.12f, 0.96f),
            new Color(0.30f, 0.40f, 0.58f, 1f));
        Dibujo.texto(lote, fuente, "NIVEL", 48f, 350f, Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.textoDerecha(lote, fuente, "FUEGO", 300f, 350f, Configuracion.FUEGO_CLARO, 0.60f);
        Dibujo.textoDerecha(lote, fuente, "AGUA", 390f, 350f, Configuracion.AGUA_CLARA, 0.60f);
        Dibujo.textoDerecha(lote, fuente, "TIEMPO", 470f, 350f, Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.textoDerecha(lote, fuente, "GANADOR", 572f, 350f, Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.linea(lote, 44f, 342f, 576f, 342f, 1f, new Color(0.35f, 0.44f, 0.60f, 0.40f));

        for (int i = 0; i < Configuracion.TOTAL_NIVELES; i++) {
            float y = 326f - i * 25f;
            Puntuacion.ResultadoNivel r = progreso.niveles[i];
            boolean hizo = r != null && r.completado;
            Color color = hizo ? Configuracion.TEXTO : new Color(0.42f, 0.46f, 0.56f, 1f);
            Dibujo.texto(lote, fuente, (i + 1) + ". " + recortar(NivelesIntegrados.nombre(i), 26),
                48f, y, color, 0.60f);
            Dibujo.textoDerecha(lote, fuente, hizo ? String.valueOf(r.puntosFuego) : "-",
                300f, y, hizo ? Configuracion.FUEGO_CLARO : color, 0.60f);
            Dibujo.textoDerecha(lote, fuente, hizo ? String.valueOf(r.puntosAgua) : "-",
                390f, y, hizo ? Configuracion.AGUA_CLARA : color, 0.60f);
            Dibujo.textoDerecha(lote, fuente, hizo ? minutosSegundos(r.tiempo) : "-",
                470f, y, color, 0.60f);
            String g = !hizo ? "-" : (r.ganador == 0 ? "Empate" : (r.ganador == 1 ? "Fuego" : "Agua"));
            Dibujo.textoDerecha(lote, fuente, g, 572f, y,
                !hizo ? color : (r.ganador == 0 ? Configuracion.DORADO
                    : (r.ganador == 1 ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA)), 0.58f);
        }

        // Logros y records.
        Dibujo.panel(lote, 610f, 84f, 320f, 288f, new Color(0.05f, 0.07f, 0.12f, 0.96f),
            new Color(0.30f, 0.40f, 0.58f, 1f));
        Dibujo.texto(lote, fuente, "LOGROS", 628f, 350f, Configuracion.DORADO, 0.78f);
        List<String> logros = progreso.listaLogros();
        if (logros.isEmpty()) {
            Dibujo.texto(lote, fuente, "Todavia no hay logros.", 628f, 324f,
                Configuracion.TEXTO_SUAVE, 0.62f);
        } else {
            // Cinco logros caben sin invadir la zona de records.
            for (int i = 0; i < logros.size() && i < 5; i++) {
                Dibujo.rectanguloRotado(lote, 630f, 316f - i * 24f, 9f, 9f, 4.5f, 4.5f, 45f,
                    Configuracion.DORADO);
                Dibujo.texto(lote, fuente, logros.get(i), 646f, 324f - i * 24f,
                    Configuracion.TEXTO, 0.64f);
            }
        }
        Dibujo.linea(lote, 628f, 186f, 912f, 186f, 1f, new Color(0.35f, 0.44f, 0.60f, 0.40f));
        Dibujo.texto(lote, fuente, "Mejor tiempo: " + minutosSegundos(progreso.mejorTiempo()),
            628f, 166f, Configuracion.TEXTO, 0.66f);
        Dibujo.texto(lote, fuente, "Empates: " + progreso.empates(), 628f, 144f,
            Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.texto(lote, fuente, "Logros: " + logros.size(), 628f, 122f,
            Configuracion.TEXTO_SUAVE, 0.62f);

        if (ui.boton("JUGAR SIGUIENTE", 360f, 30f, 190f, 36f)) {
            cambiarPantalla(PantallaJuego.campana(juego, progreso.siguienteNivel()));
        }
        if (ui.boton("VOLVER AL MENU", 24f, 20f, 190f, 34f)) irAlMenu();
        lote.end();
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return "";
        if (texto.length() <= maximo) return texto;
        return texto.substring(0, Math.max(0, maximo - 3)) + "...";
    }
}
