package io.github.ElementalEscape;

import java.util.Random;

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
