package io.github.ElementalEscape;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Dibujo con SpriteBatch y texturas generadas por codigo.
 * No usamos ShapeRenderer porque en algunas placas AMD hace crashear el juego.
 */
public final class Dibujo {
    private Dibujo() { }

    public static Texture texturaBlanca;
    public static Texture texturaCirculo;
    public static Texture texturaBrillo;
    public static Texture texturaAnillo;
    public static Texture texturaDegradado;
    private static final GlyphLayout MEDIDOR = new GlyphLayout();

    public static void iniciar() {
        if (texturaBlanca != null) return;

        Pixmap blanco = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        blanco.setColor(Color.WHITE);
        blanco.fill();
        texturaBlanca = new Texture(blanco);
        blanco.dispose();

        texturaCirculo = crearCirculo(64);
        texturaBrillo = crearBrillo(128);
        texturaAnillo = crearAnillo(128);
        texturaDegradado = crearDegradado();
    }

    private static Texture crearCirculo(int lado) {
        Pixmap p = new Pixmap(lado, lado, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(Color.WHITE);
        p.fillCircle(lado / 2, lado / 2, lado / 2 - 1);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    /** Circulo con centro solido y borde suave (sirve para luces). */
    private static Texture crearBrillo(int lado) {
        Pixmap p = new Pixmap(lado, lado, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        float centro = lado / 2f;
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                float dx = (x - centro) / centro;
                float dy = (y - centro) / centro;
                float distancia = (float) Math.sqrt(dx * dx + dy * dy);
                float alfa = Math.max(0f, 1f - distancia);
                alfa = alfa * alfa;
                p.setColor(1f, 1f, 1f, alfa);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture crearAnillo(int lado) {
        Pixmap p = new Pixmap(lado, lado, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        float centro = lado / 2f;
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                float dx = (x - centro) / centro;
                float dy = (y - centro) / centro;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float alfa = 1f - Math.abs(d - 0.8f) / 0.16f;
                if (alfa < 0f) alfa = 0f;
                p.setColor(1f, 1f, 1f, alfa);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    /** Degradado vertical: arriba opaco, abajo transparente. */
    private static Texture crearDegradado() {
        int alto = 64;
        Pixmap p = new Pixmap(2, alto, Pixmap.Format.RGBA8888);
        for (int y = 0; y < alto; y++) {
            float alfa = 1f - (y / (float) (alto - 1));
            p.setColor(1f, 1f, 1f, alfa * alfa);
            p.drawLine(0, y, 1, y);
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public static void rectangulo(SpriteBatch lote, float x, float y, float ancho, float alto, Color color) {
        lote.setColor(color);
        lote.draw(texturaBlanca, x, y, ancho, alto);
        lote.setColor(Color.WHITE);
    }

    public static void rectanguloRotado(SpriteBatch lote, float x, float y, float ancho, float alto,
                                        float origenX, float origenY, float grados, Color color) {
        lote.setColor(color);
        lote.draw(texturaBlanca, x, y, origenX, origenY, ancho, alto, 1f, 1f, grados,
            0, 0, 1, 1, false, false);
        lote.setColor(Color.WHITE);
    }

    public static void circulo(SpriteBatch lote, float x, float y, float diametro, Color color) {
        lote.setColor(color);
        lote.draw(texturaCirculo, x, y, diametro, diametro);
        lote.setColor(Color.WHITE);
    }

    public static void elipse(SpriteBatch lote, float x, float y, float ancho, float alto, Color color) {
        lote.setColor(color);
        lote.draw(texturaCirculo, x, y, ancho, alto);
        lote.setColor(Color.WHITE);
    }

    public static void brillo(SpriteBatch lote, float centroX, float centroY, float diametro, Color color) {
        lote.setColor(color);
        lote.draw(texturaBrillo, centroX - diametro / 2f, centroY - diametro / 2f, diametro, diametro);
        lote.setColor(Color.WHITE);
    }

    public static void anillo(SpriteBatch lote, float centroX, float centroY, float diametro, Color color) {
        lote.setColor(color);
        lote.draw(texturaAnillo, centroX - diametro / 2f, centroY - diametro / 2f, diametro, diametro);
        lote.setColor(Color.WHITE);
    }

    /** Degradado que ocupa un rectangulo. Opaco arriba, transparente abajo. */
    public static void degradado(SpriteBatch lote, float x, float y, float ancho, float alto, Color color) {
        lote.setColor(color);
        lote.draw(texturaDegradado, x, y, ancho, alto);
        lote.setColor(Color.WHITE);
    }

    /** Rectangulo con esquinas redondeadas hecho con un rectangulo y dos circulos por lado. */
    public static void rectanguloRedondeado(SpriteBatch lote, float x, float y, float ancho, float alto,
                                            float radio, Color color) {
        if (radio <= 0f) { rectangulo(lote, x, y, ancho, alto, color); return; }
        radio = Math.min(radio, Math.min(ancho, alto) / 2f);
        rectangulo(lote, x + radio, y, ancho - radio * 2f, alto, color);
        rectangulo(lote, x, y + radio, radio, alto - radio * 2f, color);
        rectangulo(lote, x + ancho - radio, y + radio, radio, alto - radio * 2f, color);
        circulo(lote, x, y + alto - radio * 2f, radio * 2f, color);
        circulo(lote, x + ancho - radio * 2f, y + alto - radio * 2f, radio * 2f, color);
        circulo(lote, x, y, radio * 2f, color);
        circulo(lote, x + ancho - radio * 2f, y, radio * 2f, color);
    }

    /** Panel de interfaz con borde. */
    public static void panel(SpriteBatch lote, float x, float y, float ancho, float alto, Color relleno, Color borde) {
        rectanguloRedondeado(lote, x, y, ancho, alto, 12f, borde);
        rectanguloRedondeado(lote, x + 2f, y + 2f, ancho - 4f, alto - 4f, 10f, relleno);
    }

    public static void linea(SpriteBatch lote, float x1, float y1, float x2, float y2, float grosor, Color color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float largo = (float) Math.sqrt(dx * dx + dy * dy);
        if (largo < 0.01f) return;
        float grados = (float) Math.toDegrees(Math.atan2(dy, dx));
        lote.setColor(color);
        lote.draw(texturaBlanca, x1, y1 - grosor / 2f, 0, grosor / 2f, largo, grosor, 1f, 1f, grados,
            0, 0, 1, 1, false, false);
        lote.setColor(Color.WHITE);
    }

    public static void texto(SpriteBatch lote, BitmapFont fuente, String texto, float x, float y, Color color) {
        texto(lote, fuente, texto, x, y, color, 1f);
    }

    public static void texto(SpriteBatch lote, BitmapFont fuente, String texto, float x, float y, Color color,
                             float escala) {
        fuente.getData().setScale(escala);
        fuente.setColor(0f, 0f, 0f, 0.55f);
        fuente.draw(lote, texto, x + 1.5f, y - 1.5f);
        fuente.setColor(color);
        fuente.draw(lote, texto, x, y);
    }

    public static void textoCentrado(SpriteBatch lote, BitmapFont fuente, String texto, float centroX, float y,
                                     Color color, float escala) {
        fuente.getData().setScale(escala);
        MEDIDOR.setText(fuente, texto);
        float x = centroX - MEDIDOR.width / 2f;
        texto(lote, fuente, texto, x, y, color, escala);
    }

    /** Dibuja el texto terminando exactamente en derechaX. */
    public static void textoDerecha(SpriteBatch lote, BitmapFont fuente, String texto, float derechaX, float y,
                                    Color color, float escala) {
        float ancho = anchoTexto(fuente, texto, escala);
        texto(lote, fuente, texto, derechaX - ancho, y, color, escala);
    }

    public static float anchoTexto(BitmapFont fuente, String texto, float escala) {
        fuente.getData().setScale(escala);
        MEDIDOR.setText(fuente, texto);
        return MEDIDOR.width;
    }

    public static void liberar() {
        if (texturaBlanca != null) texturaBlanca.dispose();
        if (texturaCirculo != null) texturaCirculo.dispose();
        if (texturaBrillo != null) texturaBrillo.dispose();
        if (texturaAnillo != null) texturaAnillo.dispose();
        if (texturaDegradado != null) texturaDegradado.dispose();
        texturaBlanca = null;
        texturaCirculo = null;
        texturaBrillo = null;
        texturaAnillo = null;
        texturaDegradado = null;
    }
}
