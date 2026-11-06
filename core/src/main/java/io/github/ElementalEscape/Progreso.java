package io.github.ElementalEscape;

import java.util.ArrayList;
import java.util.List;

/** Progreso de la campana: resultados por nivel, puntos y logros. */
public class Progreso {
    public int version = 1;
    public Puntuacion.ResultadoNivel[] niveles = new Puntuacion.ResultadoNivel[Configuracion.TOTAL_NIVELES];
    public String[] logros = new String[0];

    public Progreso() {
        for (int i = 0; i < niveles.length; i++) niveles[i] = new Puntuacion.ResultadoNivel();
    }

    public void normalizar() {
        if (niveles == null || niveles.length != Configuracion.TOTAL_NIVELES) {
            Puntuacion.ResultadoNivel[] nuevo = new Puntuacion.ResultadoNivel[Configuracion.TOTAL_NIVELES];
            for (int i = 0; i < nuevo.length; i++) {
                nuevo[i] = niveles != null && i < niveles.length && niveles[i] != null
                    ? niveles[i] : new Puntuacion.ResultadoNivel();
            }
            niveles = nuevo;
        }
        for (int i = 0; i < niveles.length; i++) if (niveles[i] == null) niveles[i] = new Puntuacion.ResultadoNivel();
        if (logros == null) logros = new String[0];
    }

    /**
     * Guarda el resultado de un nivel.
     * Se queda el mejor intento (suma de ambos personajes) para que reiniciar
     * un nivel no permita sumar puntos infinitos.
     */
    public void registrar(int indiceNivel, Puntuacion.ResultadoNivel resultado) {
        normalizar();
        if (indiceNivel < 0 || indiceNivel >= niveles.length || resultado == null) return;
        Puntuacion.ResultadoNivel actual = niveles[indiceNivel];
        if (!actual.completado || resultado.total() > actual.total()) {
            niveles[indiceNivel] = resultado;
        }
        evaluarLogros();
    }

    public boolean estaCompletado(int indice) {
        return indice >= 0 && indice < niveles.length && niveles[indice].completado;
    }

    public int nivelesCompletados() {
        int total = 0;
        for (Puntuacion.ResultadoNivel r : niveles) if (r.completado) total++;
        return total;
    }

    public int puntosTotales(Jugador.Tipo tipo) {
        int total = 0;
        for (Puntuacion.ResultadoNivel r : niveles) {
            if (!r.completado) continue;
            total += tipo == Jugador.Tipo.FUEGO ? r.puntosFuego : r.puntosAgua;
        }
        return total;
    }

    public int nivelesGanados(Jugador.Tipo tipo) {
        int total = 0;
        for (Puntuacion.ResultadoNivel r : niveles) {
            if (r.completado && r.ganador == (tipo == Jugador.Tipo.FUEGO ? 1 : 2)) total++;
        }
        return total;
    }

    public int empates() {
        int total = 0;
        for (Puntuacion.ResultadoNivel r : niveles) if (r.completado && r.ganador == 0) total++;
        return total;
    }

    /** 0 = empate, 1 = gana fuego, 2 = gana agua. */
    public int ganadorFinal() {
        int fuego = puntosTotales(Jugador.Tipo.FUEGO);
        int agua = puntosTotales(Jugador.Tipo.AGUA);
        if (fuego == agua) return 0;
        return fuego > agua ? 1 : 2;
    }

    public float mejorTiempo() {
        float mejor = Float.MAX_VALUE;
        for (Puntuacion.ResultadoNivel r : niveles) {
            if (r.completado && r.tiempo > 0f && r.tiempo < mejor) mejor = r.tiempo;
        }
        return mejor == Float.MAX_VALUE ? 0f : mejor;
    }

    public int siguienteNivel() {
        for (int i = 0; i < niveles.length; i++) if (!niveles[i].completado) return i;
        return niveles.length - 1;
    }

    public boolean todosCompletados() {
        return nivelesCompletados() == niveles.length;
    }

    public boolean tieneLogro(String nombre) {
        for (String logro : logros) if (logro.equals(nombre)) return true;
        return false;
    }

    public void agregarLogro(String nombre) {
        if (tieneLogro(nombre)) return;
        String[] nuevo = new String[logros.length + 1];
        System.arraycopy(logros, 0, nuevo, 0, logros.length);
        nuevo[logros.length] = nombre;
        logros = nuevo;
    }

    private void evaluarLogros() {
        int completados = nivelesCompletados();
        if (completados >= 1) agregarLogro("Primer paso");
        if (completados >= 10) agregarLogro("Maestria elemental");
        for (Puntuacion.ResultadoNivel r : niveles) {
            if (!r.completado) continue;
            if (r.todasGemas) agregarLogro("Coleccionista");
            if (r.muertesFuego == 0 && r.muertesAgua == 0) agregarLogro("Sin un rasguno");
            if (r.tiempo > 0f && r.tiempo <= 45f) agregarLogro("Velocista");
        }
        if (nivelesGanados(Jugador.Tipo.FUEGO) >= 5) agregarLogro("Corazon de fuego");
        if (nivelesGanados(Jugador.Tipo.AGUA) >= 5) agregarLogro("Corazon de agua");
        if (completados == niveles.length && ganadorFinal() == 0) agregarLogro("Empate perfecto");
    }

    public List<String> listaLogros() {
        List<String> lista = new ArrayList<>();
        for (String l : logros) lista.add(l);
        return lista;
    }
}
