package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Sonidos generados por codigo (sin archivos externos).
 * Si el equipo no tiene salida de audio, el juego sigue funcionando en silencio.
 * El hilo de audio es independiente para que writeSamples nunca bloquee el render.
 */
public class Sonido {
    private AudioDevice dispositivo;
    private volatile boolean activo = true;
    private volatile float volumenMaestro = 0.7f;
    private volatile boolean cerrado = false;
    private final LinkedBlockingQueue<float[]> cola = new LinkedBlockingQueue<>();
    private Thread hilo;

    public Sonido() {
        try {
            dispositivo = Gdx.audio.newAudioDevice(22050, true);
            hilo = new Thread(new Runnable() {
                @Override public void run() {
                    while (!cerrado) {
                        try {
                            float[] muestras = cola.take();
                            if (!activo || dispositivo == null) continue;
                            dispositivo.writeSamples(muestras, 0, muestras.length);
                            float[] silencio = new float[1100];
                            dispositivo.writeSamples(silencio, 0, silencio.length);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    }
                }
            }, "audio-elemental");
            hilo.setDaemon(true);
            hilo.start();
        } catch (Exception e) {
            dispositivo = null;
            Gdx.app.log("Sonido", "Audio no disponible: " + e.getMessage());
        }
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setVolumen(float volumen) {
        if (Float.isNaN(volumen)) volumen = 0.7f;
        this.volumenMaestro = Math.max(0f, Math.min(1f, volumen));
    }

    public boolean disponible() {
        return dispositivo != null;
    }

    public void reproducir(String nombre) {
        if (!activo || dispositivo == null || nombre == null) return;
        float frecuencia = 440f;
        float duracion = 0.10f;
        float volumen = 0.18f;
        if ("salto".equals(nombre)) { frecuencia = 620f; duracion = 0.08f; }
        else if ("aterrizar".equals(nombre)) { frecuencia = 180f; duracion = 0.07f; volumen = 0.12f; }
        else if ("gema".equals(nombre)) { frecuencia = 980f; duracion = 0.10f; volumen = 0.20f; }
        else if ("mecanismo".equals(nombre)) { frecuencia = 520f; duracion = 0.13f; }
        else if ("palanca".equals(nombre)) { frecuencia = 700f; duracion = 0.12f; }
        else if ("puerta".equals(nombre)) { frecuencia = 300f; duracion = 0.14f; }
        else if ("error".equals(nombre)) { frecuencia = 150f; duracion = 0.18f; }
        else if ("muerte".equals(nombre)) { frecuencia = 110f; duracion = 0.35f; volumen = 0.22f; }
        else if ("reaparecer".equals(nombre)) { frecuencia = 560f; duracion = 0.14f; }
        else if ("victoria".equals(nombre)) { frecuencia = 880f; duracion = 0.40f; volumen = 0.22f; }

        int muestras = Math.max(1, (int) (22050 * duracion));
        float[] datos = new float[muestras];
        float ganancia = volumen * volumenMaestro;
        for (int i = 0; i < muestras; i++) {
            float t = i / 22050f;
            float envolvente = 1f - (i / (float) muestras);
            envolvente *= envolvente;
            datos[i] = (float) Math.sin(2f * Math.PI * frecuencia * t) * envolvente * ganancia;
        }
        if (cola.size() < 12) cola.offer(datos);
    }

    public void cerrar() {
        cerrado = true;
        cola.clear();
        if (hilo != null) hilo.interrupt();
        try {
            if (dispositivo != null) dispositivo.dispose();
        } catch (Exception ignored) { }
        dispositivo = null;
    }
}
