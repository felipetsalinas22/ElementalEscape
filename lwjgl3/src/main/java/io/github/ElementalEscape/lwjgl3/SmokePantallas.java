package io.github.ElementalEscape.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import io.github.ElementalEscape.Juego;
import java.nio.ByteBuffer;
import io.github.ElementalEscape.MenuPrincipal;
import io.github.ElementalEscape.PantallaAjustes;
import io.github.ElementalEscape.PantallaEditor;
import io.github.ElementalEscape.PantallaJuego;
import io.github.ElementalEscape.PantallaNiveles;
import io.github.ElementalEscape.PantallaOnline;
import io.github.ElementalEscape.PantallaRanking;

/**
 * Prueba de humo de las pantallas con un contexto OpenGL real.
 *
 * Recorre menu, niveles, online, ranking, ajustes, editor y juego local.
 * No simula clics: sirve para detectar excepciones al crear, mostrar, dibujar
 * y cerrar cada pantalla. Se ejecuta con: gradlew :lwjgl3:pruebasUI
 */
public class SmokePantallas extends Juego {
    private float acumulado = 0f;
    private int etapa = 0;
    private boolean terminado = false;

    @Override
    public void create() {
        super.create();
        System.out.println("[UI] menu principal");
    }

    @Override
    public void render() {
        super.render();
        if (terminado) return;
        acumulado += Math.min(Gdx.graphics.getDeltaTime(), 0.25f);
        if (acumulado < 0.8f) return;
        acumulado = 0f;
        capturar(etapa);
        etapa++;
        try {
            switch (etapa) {
                case 1:
                    setScreen(new PantallaNiveles(this));
                    System.out.println("[UI] seleccion de niveles");
                    break;
                case 2:
                    setScreen(new PantallaOnline(this));
                    System.out.println("[UI] lobby online");
                    break;
                case 3:
                    setScreen(new PantallaRanking(this));
                    System.out.println("[UI] ranking");
                    break;
                case 4:
                    setScreen(new PantallaAjustes(this));
                    System.out.println("[UI] ajustes");
                    break;
                case 5:
                    setScreen(new PantallaEditor(this, null));
                    System.out.println("[UI] editor");
                    break;
                case 6:
                    setScreen(PantallaJuego.campana(this, 0));
                    System.out.println("[UI] juego local");
                    break;
                case 7:
                    setScreen(new MenuPrincipal(this));
                    System.out.println("[UI] vuelta al menu");
                    break;
                default:
                    terminado = true;
                    System.out.println("[UI] OK: todas las pantallas se abrieron y cerraron");
                    Gdx.app.exit();
                    break;
            }
        } catch (Throwable t) {
            System.err.println("[UI] ERROR en la etapa " + etapa + ": " + t);
            t.printStackTrace();
            Gdx.app.exit();
        }
    }

    private static Pixmap voltearVertical(Pixmap original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        Pixmap salida = new Pixmap(ancho, alto, Pixmap.Format.RGBA8888);
        ByteBuffer entrada = original.getPixels();
        ByteBuffer destino = salida.getPixels();
        int bytesPorFila = ancho * 4;
        byte[] fila = new byte[bytesPorFila];
        for (int y = 0; y < alto; y++) {
            entrada.position((alto - 1 - y) * bytesPorFila);
            entrada.get(fila, 0, bytesPorFila);
            destino.position(y * bytesPorFila);
            destino.put(fila, 0, bytesPorFila);
        }
        destino.position(0);
        return salida;
    }

    private void capturar(int indice) {
        String[] nombres = {
            "00-menu", "01-niveles", "02-online", "03-ranking", "04-ajustes",
            "05-editor", "06-juego-local", "07-menu-final"
        };
        if (indice < 0 || indice >= nombres.length) return;
        try {
            String destino = System.getProperty("capturas", "../lwjgl3/build/capturas");
            FileHandle carpeta = Gdx.files.local(destino);
            carpeta.mkdirs();
            Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0,
                Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            // glReadPixels devuelve la imagen de abajo hacia arriba; se corrige
            // antes de guardar para que las capturas se puedan revisar.
            Pixmap normal = voltearVertical(pixmap);
            pixmap.dispose();
            FileHandle archivo = carpeta.child(nombres[indice] + ".png");
            PixmapIO.writePNG(archivo, normal);
            normal.dispose();
            System.out.println("[UI] captura " + archivo.path());
        } catch (Throwable t) {
            System.err.println("[UI] no se pudo guardar la captura: " + t.getMessage());
        }
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Elemental Escape - prueba de pantallas");
        config.setWindowedMode(960, 540);
        config.setForegroundFPS(60);
        config.useVsync(false);
        new Lwjgl3Application(new SmokePantallas(), config);
    }
}
