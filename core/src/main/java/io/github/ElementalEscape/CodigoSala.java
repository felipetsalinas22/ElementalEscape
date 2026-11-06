package io.github.ElementalEscape;

import java.util.Random;

/**
 * Codigo corto de sala. Se comparte con el otro jugador y se comprueba en la
 * handshake de red para que nadie entre a una partida por accidente.
 *
 * Importante: el codigo NO descubre la IP del anfitrion. El juego usa conexion
 * directa TCP, asi que tambien hay que compartir IP (o usar VPN/port forwarding).
 */
public final class CodigoSala {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private CodigoSala() { }

    public static String generar() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    public static String limpiar(String codigo) {
        if (codigo == null) return "";
        return codigo.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public static boolean valido(String codigo) {
        String limpio = limpiar(codigo);
        if (limpio.length() != 6) return false;
        for (int i = 0; i < limpio.length(); i++) {
            if (ALFABETO.indexOf(limpio.charAt(i)) < 0) return false;
        }
        return true;
    }
}
