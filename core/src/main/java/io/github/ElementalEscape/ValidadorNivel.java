package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Revisa que un nivel tenga lo minimo necesario para poder jugarse.
 * La usa el editor antes de probar/guardar y tambien las pruebas automaticas.
 */
public final class ValidadorNivel {
    private ValidadorNivel() { }

    public static List<String> validar(Nivel n) {
        List<String> errores = new ArrayList<>();
        if (n == null) {
            errores.add("El nivel no existe.");
            return errores;
        }
        n.normalizar();

        if (n.nombre == null || n.nombre.trim().isEmpty()) errores.add("El nivel necesita un nombre.");
        if (n.ancho < Configuracion.ANCHO_MINIMO_NIVEL || n.ancho > Configuracion.ANCHO_MAXIMO_NIVEL)
            errores.add("El ancho debe estar entre " + (int) Configuracion.ANCHO_MINIMO_NIVEL
                + " y " + (int) Configuracion.ANCHO_MAXIMO_NIVEL + ".");
        if (n.alto < Configuracion.ALTO_MINIMO_NIVEL || n.alto > Configuracion.ALTO_MAXIMO_NIVEL)
            errores.add("El alto debe estar entre " + (int) Configuracion.ALTO_MINIMO_NIVEL
                + " y " + (int) Configuracion.ALTO_MAXIMO_NIVEL + ".");

        Rectangle zona = new Rectangle(0f, 0f, n.ancho, n.alto);
        if (!zona.contains(n.origenFuegoX, n.origenFuegoY)) errores.add("El origen de Fuego esta fuera del escenario.");
        if (!zona.contains(n.origenAguaX, n.origenAguaY)) errores.add("El origen de Agua esta fuera del escenario.");
        if (!zona.contains(n.salidaFuego)) errores.add("La salida de Fuego esta fuera del escenario.");
        if (!zona.contains(n.salidaAgua)) errores.add("La salida de Agua esta fuera del escenario.");
        if (n.salidaFuego.overlaps(n.salidaAgua)) errores.add("Las dos salidas no pueden estar superpuestas.");

        if (!tieneApoyo(n, n.origenFuegoX, n.origenFuegoY))
            errores.add("El origen de Fuego no tiene una plataforma justo debajo.");
        if (!tieneApoyo(n, n.origenAguaX, n.origenAguaY))
            errores.add("El origen de Agua no tiene una plataforma justo debajo.");

        for (Nivel.Puerta p : n.puertas) {
            if (p.id <= 0) errores.add("Hay una puerta sin identificador (id).");
            if (contarControles(n, p.id) == 0)
                errores.add("La puerta " + p.id + " no tiene ningun boton o palanca que la abra.");
            if (p.rect.width <= 0f || p.rect.height <= 0f)
                errores.add("Hay una puerta con tamano invalido.");
        }

        for (Nivel.Boton b : n.botones) {
            if (b.id <= 0) errores.add("Hay una placa sin identificador (id).");
            if (!elementoValido(b.elemento)) errores.add("Una placa tiene un elemento invalido: " + b.elemento);
        }
        for (Nivel.Palanca p : n.palancas) {
            if (p.id <= 0) errores.add("Hay una palanca sin identificador (id).");
            if (!elementoValido(p.elemento)) errores.add("Una palanca tiene un elemento invalido: " + p.elemento);
        }

        // Las secuencias deben tener ordenes 1, 2, 3... sin repetir dentro del grupo.
        List<Integer> grupos = new ArrayList<>();
        for (Nivel.Palanca p : n.palancas) {
            if (p.orden > 0 && !grupos.contains(p.grupo)) grupos.add(p.grupo);
        }
        for (int grupo : grupos) {
            Set<Integer> ordenes = new HashSet<>();
            for (Nivel.Palanca p : n.palancas) {
                if (p.grupo != grupo || p.orden <= 0) continue;
                if (!ordenes.add(p.orden))
                    errores.add("La secuencia " + grupo + " repite el orden " + p.orden + ".");
            }
            for (int i = 1; i <= ordenes.size(); i++) {
                if (!ordenes.contains(i))
                    errores.add("La secuencia " + grupo + " no tiene el orden " + i + ".");
            }
        }

        for (Nivel.Gema g : n.gemas) {
            if (!zona.contains(g.rect)) errores.add("Hay una gema fuera del escenario.");
            if (tocaSolido(n, g.rect)) errores.add("Hay una gema dentro de una plataforma.");
        }
        for (Nivel.PlataformaMovil m : n.moviles) {
            if (!zona.contains(m.rect)) errores.add("Una plataforma movil empieza fuera del escenario.");
            if (m.finX < 10f || m.finX > n.ancho - 10f || m.finY < 10f || m.finY > n.alto - 10f)
                errores.add("Una plataforma movil termina fuera del escenario.");
            if (m.velocidad <= 0f) errores.add("Una plataforma movil tiene velocidad 0.");
        }
        for (Nivel.Caja c : n.cajas) {
            if (!zona.contains(c.rect)) errores.add("Hay una caja fuera del escenario.");
            if (tocaSolido(n, c.rect)) errores.add("Hay una caja dentro de una plataforma.");
        }
        for (Nivel.Peligro p : n.peligros) {
            if (!zona.contains(p.rect)) errores.add("Hay un peligro fuera del escenario.");
        }

        // Las salidas deben ser alcanzables "a simple vista": no estan dentro de un muro.
        if (tocaSolido(n, n.salidaFuego)) errores.add("La salida de Fuego esta dentro de una plataforma.");
        if (tocaSolido(n, n.salidaAgua)) errores.add("La salida de Agua esta dentro de una plataforma.");

        return errores;
    }

    public static boolean esValido(Nivel n) {
        return validar(n).isEmpty();
    }

    /**
     * Comprobacion conservadora de recorrido: construye las superficies
     * pisables (piso, plataformas, extremos de plataformas moviles y puentes)
     * y verifica que las salidas y los mecanismos sean alcanzables con saltos
     * posibles. Ignora puertas cerradas y peligros: valida la geometria, no la
     * solucion exacta de cada puzzle.
     */
    public static List<String> validarConectividad(Nivel n) {
        List<String> problemas = new ArrayList<>();
        if (n == null) {
            problemas.add("El nivel no existe.");
            return problemas;
        }
        n.normalizar();
        List<Superficie> superficies = superficies(n);
        if (superficies.isEmpty()) {
            problemas.add("El nivel no tiene superficies pisables.");
            return problemas;
        }

        int fuego = superficieBajo(superficies, n.origenFuegoX, n.origenFuegoY);
        int agua = superficieBajo(superficies, n.origenAguaX, n.origenAguaY);
        if (fuego < 0) problemas.add("Fuego no aparece sobre una superficie pisable.");
        if (agua < 0) problemas.add("Agua no aparece sobre una superficie pisable.");
        if (fuego < 0 || agua < 0) return problemas;

        boolean[] desdeFuego = recorrer(superficies, fuego);
        boolean[] desdeAgua = recorrer(superficies, agua);

        comprobarDestino(problemas, superficies, desdeFuego, desdeAgua,
            n.salidaFuego, "La salida de Fuego no es alcanzable desde su origen.");
        comprobarDestino(problemas, superficies, desdeFuego, desdeAgua,
            n.salidaAgua, "La salida de Agua no es alcanzable desde su origen.");

        for (Nivel.Boton b : n.botones) {
            comprobarControl(problemas, superficies, desdeFuego, desdeAgua,
                b.rect, "Una placa");
        }
        for (Nivel.Palanca p : n.palancas) {
            comprobarControl(problemas, superficies, desdeFuego, desdeAgua,
                p.rect, "Una palanca");
        }
        return problemas;
    }

    private static void comprobarDestino(List<String> problemas, List<Superficie> superficies,
                                         boolean[] desdeFuego, boolean[] desdeAgua,
                                         Rectangle destino, String mensaje) {
        int indice = superficieBajo(superficies, destino.x + destino.width / 2f, destino.y);
        if (indice < 0) {
            problemas.add(mensaje + " (no hay piso debajo).");
        } else if (!desdeFuego[indice] && !desdeAgua[indice]) {
            problemas.add(mensaje);
        }
    }

    private static void comprobarControl(List<String> problemas, List<Superficie> superficies,
                                         boolean[] desdeFuego, boolean[] desdeAgua,
                                         Rectangle control, String tipo) {
        int indice = superficieBajo(superficies, control.x + control.width / 2f, control.y);
        if (indice < 0) {
            problemas.add(tipo + " no tiene una superficie pisable debajo.");
        } else if (!desdeFuego[indice] && !desdeAgua[indice]) {
            problemas.add(tipo + " quedo aislada del recorrido.");
        }
    }

    private static List<Superficie> superficies(Nivel n) {
        List<Superficie> lista = new ArrayList<>();
        for (Nivel.Plataforma p : n.plataformas) {
            if (p.rect.width >= 8f && p.rect.height > 4f) {
                lista.add(new Superficie(p.rect.x, p.rect.x + p.rect.width,
                    p.rect.y + p.rect.height));
            }
        }
        for (Nivel.PlataformaMovil m : n.moviles) {
            lista.add(new Superficie(m.rect.x, m.rect.x + m.rect.width,
                m.rect.y + m.rect.height));
            float fx = m.finX;
            float fy = m.finY;
            if (Math.abs(fx - m.rect.x) > 1f || Math.abs(fy - m.rect.y) > 1f) {
                lista.add(new Superficie(fx, fx + m.rect.width, fy + m.rect.height));
            }
        }
        for (Nivel.Puerta p : n.puertas) {
            if (p.puente) {
                lista.add(new Superficie(p.rect.x, p.rect.x + p.rect.width,
                    p.rect.y + p.rect.height));
            }
        }
        return lista;
    }

    private static boolean[] recorrer(List<Superficie> superficies, int inicio) {
        boolean[] visto = new boolean[superficies.size()];
        int[] cola = new int[superficies.size()];
        int cabeza = 0;
        int colaFinal = 0;
        cola[colaFinal++] = inicio;
        visto[inicio] = true;
        while (cabeza < colaFinal) {
            int actual = cola[cabeza++];
            for (int i = 0; i < superficies.size(); i++) {
                if (visto[i]) continue;
                if (puedeSaltar(superficies.get(actual), superficies.get(i))) {
                    visto[i] = true;
                    cola[colaFinal++] = i;
                }
            }
        }
        return visto;
    }

    private static boolean puedeSaltar(Superficie a, Superficie b) {
        float hueco = Math.max(0f, Math.max(a.x1, b.x1) - Math.min(a.x2, b.x2));
        float subida = b.y - a.y;
        if (subida > 152f) return false;
        if (subida >= -1f) {
            // Salto: cuanto mas alto el destino, menos distancia horizontal queda.
            float maximo = 224f - Math.max(0f, subida) * 0.46f;
            return hueco <= Math.max(24f, maximo);
        }
        // Caida: la gravedad permite cubrir algo mas de distancia horizontal.
        return hueco <= 300f;
    }

    private static int superficieBajo(List<Superficie> superficies, float x, float y) {
        int mejor = -1;
        float mejorY = -Float.MAX_VALUE;
        for (int i = 0; i < superficies.size(); i++) {
            Superficie s = superficies.get(i);
            if (x < s.x1 - 1f || x > s.x2 + 1f) continue;
            if (s.y > y + 3f) continue;
            if (s.y > mejorY) {
                mejorY = s.y;
                mejor = i;
            }
        }
        return mejor;
    }

    private static final class Superficie {
        final float x1;
        final float x2;
        final float y;

        Superficie(float x1, float x2, float y) {
            this.x1 = x1;
            this.x2 = x2;
            this.y = y;
        }
    }

    private static boolean elementoValido(String elemento) {
        return elemento == null || elemento.equalsIgnoreCase("AMBOS")
            || elemento.equalsIgnoreCase("FUEGO") || elemento.equalsIgnoreCase("AGUA");
    }

    private static int contarControles(Nivel n, int id) {
        int total = 0;
        for (Nivel.Boton b : n.botones) if (b.id == id) total++;
        for (Nivel.Palanca p : n.palancas) if (p.id == id) total++;
        return total;
    }

    private static boolean tocaSolido(Nivel n, Rectangle r) {
        Rectangle prueba = new Rectangle(r.x + 1f, r.y + 1f, Math.max(1f, r.width - 2f), Math.max(1f, r.height - 2f));
        for (Nivel.Plataforma p : n.plataformas) if (prueba.overlaps(p.rect)) return true;
        return false;
    }

    private static boolean tieneApoyo(Nivel n, float x, float y) {
        for (Nivel.Plataforma p : n.plataformas) {
            if (x + Configuracion.ANCHO_PERSONAJE * 0.5f < p.rect.x) continue;
            if (x + Configuracion.ANCHO_PERSONAJE * 0.5f > p.rect.x + p.rect.width) continue;
            if (Math.abs(p.rect.y + p.rect.height - y) <= 2.5f) return true;
        }
        return false;
    }
}
