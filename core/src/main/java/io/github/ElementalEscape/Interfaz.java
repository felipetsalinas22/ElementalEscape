package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Interfaz inmediata: cada pantalla dibuja sus botones y recibe true/false.
 * No usa scene2d, asi que no hay skins ni archivos externos.
 */
public class Interfaz {
    private final SpriteBatch lote;
    private final BitmapFont fuente;
    private final Viewport vista;
    private final Vector3 raton = new Vector3();

    public float mouseX;
    public float mouseY;
    public boolean clic;
    public boolean clicMantenido;

    private final Color COLOR_BOTON = new Color(0.16f, 0.20f, 0.31f, 0.98f);
    private final Color COLOR_BOTON_HOVER = new Color(0.23f, 0.29f, 0.44f, 1f);
    private final Color COLOR_BOTON_ACTIVO = new Color(0.25f, 0.45f, 0.58f, 1f);
    private final Color COLOR_BORDE = new Color(0.36f, 0.44f, 0.62f, 1f);
    private final Color COLOR_BORDE_HOVER = new Color(0.62f, 0.76f, 0.95f, 1f);

    public Interfaz(SpriteBatch lote, BitmapFont fuente, Viewport vista) {
        this.lote = lote;
        this.fuente = fuente;
        this.vista = vista;
    }

    public void actualizar() {
        raton.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        vista.unproject(raton);
        mouseX = raton.x;
        mouseY = raton.y;
        clic = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        clicMantenido = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    public boolean dentro(float x, float y, float ancho, float alto) {
        return mouseX >= x && mouseX <= x + ancho && mouseY >= y && mouseY <= y + alto;
    }

    public boolean boton(String texto, float x, float y, float ancho, float alto) {
        boolean hover = dentro(x, y, ancho, alto);
        boolean pulsado = hover && clic;
        if (pulsado) clic = false;
        dibujarBoton(texto, x, y, ancho, alto, hover, false);
        return pulsado;
    }

    public boolean botonActivo(String texto, float x, float y, float ancho, float alto) {
        boolean hover = dentro(x, y, ancho, alto);
        boolean pulsado = hover && clic;
        if (pulsado) clic = false;
        dibujarBoton(texto, x, y, ancho, alto, hover, true);
        return pulsado;
    }

    public boolean botonColor(String texto, float x, float y, float ancho, float alto, Color color) {
        boolean hover = dentro(x, y, ancho, alto);
        boolean pulsado = hover && clic;
        if (pulsado) clic = false;
        Color base = color.cpy();
        if (hover) base.lerp(Color.WHITE, 0.16f);
        if (hover) Dibujo.brillo(lote, x + ancho / 2f, y + alto / 2f, ancho * 0.62f,
            new Color(1f, 1f, 1f, 0.10f));
        Dibujo.panel(lote, x, y, ancho, alto, base, hover ? COLOR_BORDE_HOVER : color.cpy().lerp(Color.WHITE, 0.25f));
        Dibujo.rectangulo(lote, x, y, Math.min(4f, ancho * 0.08f), alto,
            new Color(1f, 1f, 1f, hover ? 0.55f : 0.22f));
        Dibujo.textoCentrado(lote, fuente, texto, x + ancho / 2f, y + alto / 2f + 5f,
            new Color(1f, 1f, 1f, 0.98f), escalaTextoQueCabe(texto, ancho, 0.92f, 0.62f));
        return pulsado;
    }

    private void dibujarBoton(String texto, float x, float y, float ancho, float alto,
                              boolean hover, boolean activo) {
        Color relleno = activo ? COLOR_BOTON_ACTIVO : (hover ? COLOR_BOTON_HOVER : COLOR_BOTON);
        Color borde = hover ? COLOR_BORDE_HOVER : COLOR_BORDE;
        if (hover) Dibujo.brillo(lote, x + ancho / 2f, y + alto / 2f, ancho * 0.62f,
            new Color(0.55f, 0.78f, 1f, 0.11f));
        Dibujo.rectangulo(lote, x + 2f, y - 3f, ancho, alto, new Color(0f, 0f, 0f, 0.35f));
        Dibujo.panel(lote, x, y, ancho, alto, relleno, borde);
        // Acento vertical: refuerza la jerarquia y hace visible el area del boton.
        Dibujo.rectangulo(lote, x, y, Math.min(4f, ancho * 0.08f), alto,
            hover ? new Color(0.72f, 0.92f, 1f, 0.92f) : new Color(0.42f, 0.58f, 0.82f, 0.55f));
        Dibujo.rectangulo(lote, x + 5f, y + alto - 1f, Math.max(0f, ancho - 7f), 1f,
            new Color(1f, 1f, 1f, hover ? 0.22f : 0.10f));
        Dibujo.textoCentrado(lote, fuente, texto, x + ancho / 2f, y + alto / 2f + 5f,
            Configuracion.TEXTO, escalaTextoQueCabe(texto, ancho, 0.92f, 0.58f));
    }

    /** Reduce la escala solo cuando el texto no cabe: evita letras cortadas. */
    private float escalaTextoQueCabe(String texto, float anchoBoton, float escalaDeseada, float minimo) {
        if (texto == null || texto.isEmpty()) return escalaDeseada;
        float disponible = Math.max(12f, anchoBoton - 26f);
        float anchoTexto = Dibujo.anchoTexto(fuente, texto, escalaDeseada);
        if (anchoTexto <= disponible) return escalaDeseada;
        return Math.max(minimo, escalaDeseada * disponible / anchoTexto);
    }

    /** Interruptor on/off con etiqueta. Devuelve el nuevo valor. */
    public boolean interruptor(String etiqueta, boolean valor, float x, float y, float ancho, float alto) {
        boolean hover = dentro(x, y, ancho, alto);
        boolean pulsado = hover && clic;
        if (pulsado) clic = false;
        Dibujo.panel(lote, x, y, ancho, alto, hover ? COLOR_BOTON_HOVER : COLOR_BOTON,
            hover ? COLOR_BORDE_HOVER : COLOR_BORDE);
        Dibujo.texto(lote, fuente, etiqueta, x + 12f, y + alto / 2f + 5f, Configuracion.TEXTO, 0.82f);
        float cw = 44f;
        float cx = x + ancho - cw - 12f;
        float cy = y + alto / 2f - 10f;
        Dibujo.rectanguloRedondeado(lote, cx, cy, cw, 20f, 10f,
            valor ? Configuracion.VERDE : new Color(0.28f, 0.32f, 0.42f, 1f));
        Dibujo.circulo(lote, valor ? cx + cw - 18f : cx + 2f, cy + 2f, 16f, Color.WHITE);
        if (pulsado) return !valor;
        return valor;
    }

    public void panel(float x, float y, float ancho, float alto) {
        Dibujo.panel(lote, x, y, ancho, alto, Configuracion.PANEL,
            new Color(0.28f, 0.36f, 0.52f, 1f));
    }

    public void panelClaro(float x, float y, float ancho, float alto) {
        Dibujo.panel(lote, x, y, ancho, alto, Configuracion.PANEL_CLARO,
            new Color(0.34f, 0.43f, 0.62f, 1f));
    }

    public void titulo(String texto, float centroX, float y, float escala, Color color) {
        Dibujo.textoCentrado(lote, fuente, texto, centroX, y, color, escala);
    }

    public void texto(String texto, float x, float y, Color color, float escala) {
        Dibujo.texto(lote, fuente, texto, x, y, color, escala);
    }

    public void textoCentrado(String texto, float centroX, float y, Color color, float escala) {
        Dibujo.textoCentrado(lote, fuente, texto, centroX, y, color, escala);
    }

    public void textoDerecha(String texto, float derechaX, float y, Color color, float escala) {
        float ancho = Dibujo.anchoTexto(fuente, texto, escala);
        Dibujo.texto(lote, fuente, texto, derechaX - ancho, y, color, escala);
    }

    public void barra(float x, float y, float ancho, float alto, float proporcion, Color color) {
        Dibujo.rectanguloRedondeado(lote, x, y, ancho, alto, alto / 2f, new Color(0.08f, 0.10f, 0.16f, 0.9f));
        float w = Math.max(0f, Math.min(1f, proporcion)) * ancho;
        if (w > 2f) Dibujo.rectanguloRedondeado(lote, x, y, w, alto, alto / 2f, color);
    }

    public void separador(float x, float y, float ancho) {
        Dibujo.rectangulo(lote, x, y, ancho, 1f, new Color(0.35f, 0.42f, 0.58f, 0.45f));
    }

    public void etiqueta(String texto, float x, float y) {
        Dibujo.texto(lote, fuente, texto, x, y, Configuracion.TEXTO_SUAVE, 0.72f);
    }

    public SpriteBatch getLote() { return lote; }
    public BitmapFont getFuente() { return fuente; }
}
