package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Menu principal. La composicion usa a los dos personajes como protagonistas,
 * botones grandes con areas de clic claras y un panel de progreso legible.
 */
public class MenuPrincipal extends PantallaBase {
    private float tiempo = 0f;
    private float entrada = 0f;
    private DibujanteJuego arte;

    public MenuPrincipal(Juego juego) {
        super(juego);
    }

    @Override
    protected void alMostrar() {
        arte = new DibujanteJuego(lote, fuente);
    }

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        entrada = Math.min(1f, entrada + delta * 1.6f);

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            continuarCampana();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            cambiarPantalla(new PantallaNiveles(juego));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            cambiarPantalla(new PantallaOnline(juego));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            cambiarPantalla(new PantallaEditor(juego, null));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            cambiarPantalla(new PantallaRanking(juego));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            cambiarPantalla(new PantallaAjustes(juego));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            salirAplicacion();
        }
    }

    @Override
    protected void dibujar(float delta) {
        aplicarVistaUI();
        ScreenUtils.clear(0.02f, 0.03f, 0.065f, 1f);
        lote.begin();
        dibujarFondo();
        dibujarTitulo();
        dibujarBotones();
        dibujarProgreso();
        dibujarPie();
        lote.end();
    }

    private void dibujarFondo() {
        // Cielo nocturno con una direccion artistica comun al resto del juego.
        Dibujo.rectangulo(lote, 0f, 0f, 960f, 540f, new Color(0.02f, 0.03f, 0.07f, 1f));
        Dibujo.degradado(lote, 0f, 0f, 960f, 500f, new Color(0.13f, 0.09f, 0.24f, 1f));

        // Estrellas y polvo magico.
        for (int i = 0; i < 66; i++) {
            float x = (i * 149f + 37f) % 960f;
            float y = (i * 83f + 19f) % 540f;
            float pulso = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(tiempo * 1.5f + i));
            Dibujo.circulo(lote, x, y, 1.5f + (i % 3),
                new Color(0.78f, 0.88f, 1f, 0.10f * pulso));
        }

        // Arcos del templo, muy oscuros para no competir con el titulo.
        for (int i = 0; i < 5; i++) {
            float cx = 90f + i * 195f;
            float base = 112f;
            float h = 300f + (i % 2) * 32f;
            Dibujo.rectangulo(lote, cx - 8f, base, 16f, h, new Color(0.10f, 0.12f, 0.20f, 0.56f));
            Dibujo.circulo(lote, cx - 64f, base + h - 42f, 128f,
                new Color(0.10f, 0.12f, 0.20f, 0.42f));
            Dibujo.rectangulo(lote, cx - 8f, base, 16f, 3f, new Color(0.35f, 0.42f, 0.58f, 0.25f));
        }

        // Auras elementales: encuadran a los personajes sin tapar los botones.
        float flotarF = (float) Math.sin(tiempo * 1.2f) * 7f;
        Dibujo.brillo(lote, 155f, 430f + flotarF, 220f, new Color(1f, 0.28f, 0.08f, 0.18f));
        Dibujo.anillo(lote, 155f, 428f + flotarF, 200f, new Color(1f, 0.52f, 0.16f, 0.12f));
        float flotarA = (float) Math.sin(tiempo * 1.4f + 1.6f) * 7f;
        Dibujo.brillo(lote, 805f, 430f + flotarA, 220f, new Color(0.16f, 0.55f, 1f, 0.17f));
        Dibujo.anillo(lote, 805f, 428f + flotarA, 200f, new Color(0.35f, 0.80f, 1f, 0.12f));

        // Linea de horizonte y niebla inferior.
        Dibujo.rectangulo(lote, 0f, 112f, 960f, 1f, new Color(0.42f, 0.54f, 0.78f, 0.20f));
        Dibujo.degradado(lote, 0f, 0f, 960f, 130f, new Color(0.02f, 0.04f, 0.09f, 1f));
        for (int i = 0; i < 8; i++) {
            float x = (i * 143f + tiempo * 7f) % 1000f - 20f;
            Dibujo.elipse(lote, x, 90f + (i % 3) * 12f, 180f, 18f,
                new Color(0.24f, 0.34f, 0.56f, 0.06f));
        }
    }

    private void dibujarTitulo() {
        float alpha = 0.35f + entrada * 0.65f;
        Dibujo.brillo(lote, 480f, 470f, 520f, new Color(1f, 0.78f, 0.36f, 0.10f));
        Dibujo.textoCentrado(lote, fuente, "ELEMENTAL", 480f, 510f,
            new Color(1f, 1f, 1f, alpha), 2.15f);
        Dibujo.textoCentrado(lote, fuente, "ESCAPE", 480f, 468f,
            new Color(1f, 0.78f, 0.35f, alpha), 2.15f);
        Dibujo.linea(lote, 330f, 452f, 450f, 452f, 2f,
            new Color(1f, 0.60f, 0.20f, 0.72f));
        Dibujo.linea(lote, 510f, 452f, 630f, 452f, 2f,
            new Color(0.36f, 0.78f, 1f, 0.72f));
        Dibujo.circulo(lote, 480f, 452f, 8f, new Color(1f, 0.84f, 0.40f, 0.90f));
        Dibujo.textoCentrado(lote, fuente, "COOPERATIVO PARA DOS JUGADORES", 480f, 438f,
            new Color(0.74f, 0.84f, 1f, alpha), 0.72f);

        // Los dos personajes en pose de presentacion. Quedan por encima de los
        // botones para que sus pies y sombras no se recorten.
        Dibujo.elipse(lote, 105f, 416f, 100f, 16f, new Color(0f, 0f, 0f, 0.35f));
        Dibujo.elipse(lote, 755f, 416f, 100f, 16f, new Color(0f, 0f, 0f, 0.35f));
        arte.dibujarAvatarMenu(155f, 424f, true, tiempo, 2.1f);
        arte.dibujarAvatarMenu(805f, 424f, false, tiempo, 2.1f);
    }

    private void dibujarBotones() {
        // Una sola columna centrada: el mismo rectangulo se usa para dibujar y
        // para detectar el clic, por lo que no hay desfases al redimensionar.
        final float centroX = 480f;
        final float ancho = 360f;
        final float alto = 38f;
        final float separacion = 5f;
        final float x = centroX - ancho / 2f;
        final float inicioY = 352f;

        String[] etiquetas = {
            "CONTINUAR CAMPANA",
            "SELECCION DE NIVELES",
            "MULTIJUGADOR EN LINEA",
            "EDITOR DE NIVELES",
            "RANKING Y PROGRESO",
            "AJUSTES",
            "SALIR"
        };
        for (int i = 0; i < etiquetas.length; i++) {
            float y = inicioY - i * (alto + separacion);
            boolean pulsado = ui.boton(etiquetas[i], x, y, ancho, alto);
            dibujarTecla(String.valueOf(i + 1), x + 12f, y + (alto - 24f) / 2f);
            if (pulsado) {
                switch (i) {
                    case 0: continuarCampana(); break;
                    case 1: cambiarPantalla(new PantallaNiveles(juego)); break;
                    case 2: cambiarPantalla(new PantallaOnline(juego)); break;
                    case 3: cambiarPantalla(new PantallaEditor(juego, null)); break;
                    case 4: cambiarPantalla(new PantallaRanking(juego)); break;
                    case 5: cambiarPantalla(new PantallaAjustes(juego)); break;
                    case 6: salirAplicacion(); break;
                    default: break;
                }
                return;
            }
        }
    }

    private void dibujarTecla(String texto, float x, float y) {
        Dibujo.panel(lote, x, y, 24f, 24f, new Color(0.08f, 0.11f, 0.18f, 0.92f),
            new Color(0.35f, 0.45f, 0.62f, 0.60f));
        Dibujo.textoCentrado(lote, fuente, texto, x + 12f, y + 14f,
            new Color(0.70f, 0.82f, 1f, 0.72f), 0.62f);
    }

    private void dibujarProgreso() {
        Progreso progreso = juego.getProgreso();
        int completados = progreso.nivelesCompletados();
        int fuego = progreso.puntosTotales(Jugador.Tipo.FUEGO);
        int agua = progreso.puntosTotales(Jugador.Tipo.AGUA);

        Dibujo.panel(lote, 300f, 396f, 360f, 34f,
            new Color(0.045f, 0.065f, 0.115f, 0.94f),
            new Color(0.28f, 0.39f, 0.58f, 0.95f));
        Dibujo.rectangulo(lote, 300f, 396f, 3f, 34f, Configuracion.DORADO);
        ui.texto(completados + " / " + Configuracion.TOTAL_NIVELES + " completados",
            312f, 421f, Configuracion.TEXTO, 0.62f);
        ui.barra(410f, 406f, 120f, 6f,
            completados / (float) Configuracion.TOTAL_NIVELES, Configuracion.DORADO);
        ui.textoDerecha("Fuego " + fuego + "   Agua " + agua, 648f, 421f,
            Configuracion.TEXTO_SUAVE, 0.60f);
    }

    private void dibujarPie() {
        Dibujo.textoCentrado(lote, fuente, "Fuego: A / D / W     Agua: Flechas     ESC: pausa",
            480f, 42f, new Color(0.68f, 0.78f, 0.94f, 0.88f), 0.64f);
        Dibujo.textoCentrado(lote, fuente, "v1.2  -  mundos por capas  -  editor y online incluidos",
            480f, 22f, new Color(0.48f, 0.58f, 0.76f, 0.78f), 0.55f);
    }

    private void continuarCampana() {
        int indice = juego.getProgreso().siguienteNivel();
        cambiarPantalla(PantallaJuego.campana(juego, indice));
    }
}
