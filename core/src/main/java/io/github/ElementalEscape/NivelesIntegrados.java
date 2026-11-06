package io.github.ElementalEscape;

/**
 * Los diez niveles del modo campana.
 * Estan escritos en codigo para que sean faciles de leer y modificar.
 * Los niveles creados por el usuario se guardan aparte, en archivos JSON.
 *
 * Convencion: el piso mide 30 px de alto. Las plataformas se indican con su
 * esquina inferior izquierda y su tamano. Un salto alcanza unos 150 px de alto
 * y unos 220 px de distancia, por eso los saltos importantes son mas cortos.
 */
public final class NivelesIntegrados {
    private NivelesIntegrados() { }

    public static Nivel[] todos() {
        Nivel[] niveles = new Nivel[Configuracion.TOTAL_NIVELES];
        for (int i = 0; i < niveles.length; i++) niveles[i] = crear(i);
        return niveles;
    }

    public static Nivel crear(int indice) {
        switch (indice) {
            case 0: return nivel1();
            case 1: return nivel2();
            case 2: return nivel3();
            case 3: return nivel4();
            case 4: return nivel5();
            case 5: return nivel6();
            case 6: return nivel7();
            case 7: return nivel8();
            case 8: return nivel9();
            case 9: return nivel10();
            default: return nivel1();
        }
    }

    public static String nombre(int indice) {
        return crear(indice).nombre;
    }

    // =========================================================
    //                        NIVELES
    // =========================================================

    /** 1. Introduccion: saltar, una palanca y una puerta. */
    private static Nivel nivel1() {
        Nivel n = base("El despertar elemental", "TEMPLO",
            "Fuego debe activar la palanca roja. Despues, los dos cruzan la puerta.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(815f, 30f, 60f, 80f);
        n.salidaAgua.set(875f, 30f, 60f, 80f);

        plataforma(n, 90f, 90f, 100f, 20f, "PIEDRA");
        plataforma(n, 210f, 90f, 140f, 20f, "PIEDRA");
        plataforma(n, 430f, 170f, 150f, 20f, "PIEDRA");
        plataforma(n, 650f, 90f, 140f, 20f, "PIEDRA");

        palanca(n, 125f, 110f, 1, "FUEGO");
        puertaBloqueo(n, 760f, 1, "ALGUNA", 0f);
        peligroAgua(n, 530f, 30f, 70f, 14f);

        gema(n, 255f, 128f);
        gema(n, 485f, 208f);
        gema(n, 700f, 128f);
        gema(n, 620f, 60f);
        return n;
    }

    /** 2. Dos caminos separados y palancas que se ayudan mutuamente. */
    private static Nivel nivel2() {
        Nivel n = base("Caminos separados", "CAVERNA",
            "Cada palanca abre la puerta del OTRO camino. Necesitan las dos.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 900f; n.origenAguaY = 30f;
        n.salidaFuego.set(385f, 30f, 60f, 80f);
        n.salidaAgua.set(515f, 30f, 60f, 80f);

        // Muro central que separa los caminos.
        plataforma(n, 460f, 30f, 40f, 494f, "MURO");

        // Camino de fuego (izquierda). Su palanca abre la puerta derecha.
        plataforma(n, 100f, 90f, 120f, 18f, "PIEDRA");
        palanca(n, 135f, 108f, 1, "FUEGO");
        peligroAgua(n, 240f, 30f, 70f, 14f);
        plataforma(n, 180f, 210f, 110f, 18f, "PIEDRA");
        gema(n, 220f, 238f);
        gema(n, 300f, 128f);
        puertaBloqueo(n, 340f, 2, "ALGUNA", 0f);

        // Camino de agua (derecha). Su palanca abre la puerta izquierda.
        plataforma(n, 740f, 90f, 120f, 18f, "PIEDRA");
        palanca(n, 775f, 108f, 2, "AGUA");
        peligroLava(n, 650f, 30f, 70f, 14f);
        plataforma(n, 670f, 210f, 110f, 18f, "PIEDRA");
        gema(n, 710f, 238f);
        gema(n, 620f, 128f);
        puertaBloqueo(n, 600f, 1, "ALGUNA", 0f);

        // Repisas altas: ruta alternativa con una gema secreta en cada lado.
        plataforma(n, 40f, 300f, 80f, 18f, "MADERA");
        plataforma(n, 840f, 300f, 80f, 18f, "MADERA");
        gemaSecreta(n, 70f, 330f);
        gemaSecreta(n, 880f, 330f);
        return n;
    }

    /** 3. Dos placas elementales simultaneas y una puerta temporizada. */
    private static Nivel nivel3() {
        Nivel n = base("El templo de los interruptores", "TEMPLO",
            "Las dos placas deben estar pisadas a la vez. La puerta queda abierta 6 segundos.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 105f; n.origenAguaY = 30f;
        n.salidaFuego.set(760f, 30f, 60f, 80f);
        n.salidaAgua.set(850f, 30f, 60f, 80f);

        plataforma(n, 90f, 110f, 150f, 20f, "PIEDRA");
        boton(n, 130f, 130f, 1, "FUEGO");
        plataforma(n, 330f, 150f, 160f, 20f, "PIEDRA");
        boton(n, 380f, 170f, 1, "AGUA");

        puertaBloqueo(n, 590f, 1, "TODAS", 6f);

        gema(n, 160f, 160f);
        gema(n, 410f, 200f);
        gema(n, 690f, 80f);

        // Puzzle opcional: una palanca de fuego crea un puente y una gema secreta.
        plataforma(n, 30f, 250f, 110f, 18f, "MADERA");
        palanca(n, 60f, 268f, 2, "FUEGO");
        puente(n, 430f, 200f, 130f, 16f, 2);
        gemaSecreta(n, 500f, 245f);
        return n;
    }

    /** 4. Rutas opuestas: lava y agua obligan a caminos diferentes. */
    private static Nivel nivel4() {
        Nivel n = base("Elementos opuestos", "HIELO",
            "El fuego cruza la lava; el agua usa las plataformas. Las placas son de un solo elemento.", 960f);
        n.origenFuegoX = 50f; n.origenFuegoY = 30f;
        n.origenAguaX = 95f; n.origenAguaY = 30f;
        n.salidaFuego.set(800f, 30f, 60f, 80f);
        n.salidaAgua.set(865f, 30f, 60f, 80f);

        peligroLava(n, 200f, 30f, 160f, 14f);
        peligroAgua(n, 420f, 30f, 160f, 14f);

        plataforma(n, 220f, 130f, 120f, 18f, "PIEDRA");
        boton(n, 255f, 148f, 1, "FUEGO");

        plataforma(n, 120f, 100f, 100f, 18f, "MADERA");
        plataforma(n, 260f, 180f, 100f, 18f, "MADERA");
        plataforma(n, 420f, 240f, 110f, 18f, "MADERA");
        plataforma(n, 560f, 180f, 110f, 18f, "MADERA");
        boton(n, 600f, 198f, 1, "AGUA");

        puertaBloqueo(n, 760f, 1, "TODAS", 6f);

        gema(n, 170f, 145f);
        gema(n, 360f, 225f);
        gema(n, 640f, 225f);

        // La caja permite alcanzar una gema escondida en una repisa alta.
        plataforma(n, 680f, 170f, 70f, 18f, "MADERA");
        caja(n, 620f, 30f, false);
        gemaSecreta(n, 700f, 200f);
        return n;
    }

    /** 5. Plataformas moviles sobre un abismo. */
    private static Nivel nivel5() {
        Nivel n = baseConHuecos("La camara de las plataformas moviles", "CIELO",
            "Monten las plataformas y lleguen a la palanca alta. Abajo no hay piso.", 960f,
            220f, 740f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(810f, 30f, 60f, 80f);
        n.salidaAgua.set(875f, 30f, 60f, 80f);

        movil(n, 240f, 110f, 110f, 18f, 400f, 110f, 95f, 0);
        movil(n, 480f, 120f, 100f, 18f, 480f, 300f, 80f, 0);
        movil(n, 620f, 200f, 110f, 18f, 720f, 200f, 110f, 0);

        plataforma(n, 420f, 320f, 130f, 18f, "MADERA");
        palanca(n, 460f, 338f, 1, "AMBOS");
        puertaBloqueo(n, 790f, 1, "ALGUNA", 0f);

        gema(n, 300f, 160f);
        gema(n, 500f, 250f);
        gema(n, 660f, 250f);
        gema(n, 760f, 80f);
        // Recompensa por subir hasta la palanca alta.
        gemaSecreta(n, 520f, 360f);
        return n;
    }

    /** 6. Secuencia de palancas: el orden importa. */
    private static Nivel nivel6() {
        Nivel n = base("El orden correcto", "RUINAS",
            "Activen las palancas en orden 1, 2 y 3. Si se equivocan, la secuencia se reinicia.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 105f; n.origenAguaY = 30f;
        n.salidaFuego.set(835f, 30f, 50f, 80f);
        n.salidaAgua.set(888f, 30f, 50f, 80f);

        plataforma(n, 100f, 90f, 120f, 18f, "PIEDRA");
        palancaSecuencia(n, 135f, 108f, 1, "AMBOS", 1, 1);

        plataforma(n, 260f, 110f, 90f, 18f, "MADERA");
        plataforma(n, 390f, 170f, 120f, 18f, "PIEDRA");
        palancaSecuencia(n, 430f, 188f, 1, "AGUA", 2, 1);
        peligroAgua(n, 215f, 30f, 65f, 14f);

        plataforma(n, 520f, 130f, 100f, 18f, "MADERA");
        plataforma(n, 650f, 200f, 90f, 18f, "MADERA");
        plataforma(n, 690f, 250f, 120f, 18f, "PIEDRA");
        palancaSecuencia(n, 730f, 268f, 1, "FUEGO", 3, 1);
        peligroLava(n, 560f, 30f, 80f, 14f);

        puertaBloqueo(n, 800f, 1, "TODAS", 0f);

        gema(n, 180f, 140f);
        gema(n, 460f, 220f);
        gema(n, 760f, 300f);
        gema(n, 660f, 80f);
        return n;
    }

    /** 7. Laboratorio: caja, placas temporizadas y una palanca elevada. */
    private static Nivel nivel7() {
        Nivel n = base("El laboratorio elemental", "LABORATORIO",
            "Usen la caja para mantener una placa. Despues, activen las dos placas a la vez.", 1320f);
        n.origenFuegoX = 50f; n.origenFuegoY = 30f;
        n.origenAguaX = 95f; n.origenAguaY = 30f;
        n.salidaFuego.set(1195f, 30f, 50f, 80f);
        n.salidaAgua.set(1250f, 30f, 50f, 80f);

        // Sala A: empujar la caja sobre la placa.
        caja(n, 200f, 30f, false);
        boton(n, 300f, 30f, 1, "AMBOS");
        plataforma(n, 370f, 30f, 20f, 50f, "METAL");
        puertaBloqueo(n, 400f, 1, "ALGUNA", 0f);

        // Sala B: lava, plataforma movil y dos placas elementales.
        peligroLava(n, 500f, 30f, 240f, 14f);
        plataforma(n, 440f, 100f, 100f, 18f, "METAL");
        boton(n, 470f, 118f, 2, "FUEGO");
        movil(n, 560f, 110f, 100f, 18f, 700f, 110f, 90f, 0);
        plataforma(n, 690f, 150f, 100f, 18f, "METAL");
        boton(n, 720f, 168f, 2, "AGUA");
        puertaBloqueo(n, 840f, 2, "TODAS", 5f);

        // Sala C: la caja es un escalon para la palanca.
        caja(n, 900f, 30f, false);
        plataforma(n, 940f, 190f, 110f, 18f, "METAL");
        palanca(n, 980f, 208f, 3, "AMBOS");
        puertaBloqueo(n, 1140f, 3, "ALGUNA", 0f);

        gema(n, 150f, 80f);
        gema(n, 520f, 170f);
        gema(n, 760f, 230f);
        gema(n, 1000f, 240f);
        gemaSecreta(n, 1090f, 60f);
        return n;
    }

    /** 8. Laberinto: una palanca crea el puente que necesita la otra. */
    private static Nivel nivel8() {
        Nivel n = base("El laberinto cambiante", "BOSQUE",
            "Fuego abre el puente; el agua alcanza la palanca que abre la salida.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(835f, 30f, 50f, 80f);
        n.salidaAgua.set(890f, 30f, 50f, 80f);

        // Ruta de fuego hasta la palanca que crea el puente.
        plataforma(n, 100f, 110f, 120f, 18f, "MADERA");
        plataforma(n, 200f, 220f, 100f, 18f, "MADERA");
        plataforma(n, 150f, 320f, 100f, 18f, "MADERA");
        plataforma(n, 140f, 390f, 120f, 18f, "PIEDRA");
        palanca(n, 170f, 408f, 2, "FUEGO");

        // El puente que permite cruzar al agua (sin el, el hueco es demasiado grande).
        plataforma(n, 300f, 180f, 160f, 18f, "MADERA");
        puente(n, 470f, 180f, 230f, 16f, 2);
        plataforma(n, 700f, 180f, 110f, 18f, "MADERA");
        plataforma(n, 720f, 260f, 100f, 18f, "MADERA");
        plataforma(n, 660f, 330f, 100f, 18f, "MADERA");
        plataforma(n, 620f, 390f, 120f, 18f, "PIEDRA");
        palanca(n, 660f, 408f, 1, "AGUA");

        // Muros del laberinto.
        plataforma(n, 420f, 30f, 24f, 120f, "MURO");
        plataforma(n, 520f, 300f, 24f, 100f, "MURO");
        plataforma(n, 280f, 470f, 220f, 20f, "MURO");
        plataforma(n, 420f, 470f, 20f, 54f, "MURO");

        peligroAgua(n, 280f, 30f, 80f, 14f);
        peligroLava(n, 600f, 30f, 80f, 14f);

        puertaBloqueo(n, 800f, 1, "ALGUNA", 0f);

        gema(n, 250f, 250f);
        gema(n, 350f, 210f);
        gema(n, 760f, 290f);
        gemaSecreta(n, 480f, 80f);
        return n;
    }

    /** 9. Prueba de los elementos: caja pesada, placas y secuencia. */
    private static Nivel nivel9() {
        Nivel n = base("La prueba de los elementos", "VOLCAN",
            "La caja pesada necesita los dos. Luego coordinen las placas y la secuencia.", 1460f);
        n.origenFuegoX = 45f; n.origenFuegoY = 30f;
        n.origenAguaX = 90f; n.origenAguaY = 30f;
        n.salidaFuego.set(1335f, 30f, 50f, 80f);
        n.salidaAgua.set(1390f, 30f, 50f, 80f);

        // Sala A: caja pesada que solo se mueve si empujan los dos.
        caja(n, 180f, 30f, true);
        boton(n, 300f, 30f, 1, "AMBOS");
        plataforma(n, 376f, 30f, 20f, 50f, "METAL");
        puertaBloqueo(n, 420f, 1, "ALGUNA", 0f);

        // Sala B: placas elementales + plataforma movil sobre lava.
        peligroLava(n, 500f, 30f, 260f, 14f);
        plataforma(n, 430f, 100f, 100f, 18f, "METAL");
        boton(n, 460f, 118f, 2, "FUEGO");
        movil(n, 560f, 110f, 100f, 18f, 720f, 110f, 100f, 0);
        plataforma(n, 770f, 130f, 100f, 18f, "METAL");
        boton(n, 800f, 148f, 2, "AGUA");
        puertaBloqueo(n, 900f, 2, "TODAS", 6f);

        // Sala C: secuencia de tres palancas.
        plataforma(n, 930f, 100f, 100f, 18f, "PIEDRA");
        palancaSecuencia(n, 960f, 118f, 3, "AMBOS", 1, 2);
        plataforma(n, 1050f, 190f, 100f, 18f, "PIEDRA");
        palancaSecuencia(n, 1080f, 208f, 3, "AGUA", 2, 2);
        plataforma(n, 1170f, 280f, 100f, 18f, "PIEDRA");
        palancaSecuencia(n, 1200f, 298f, 3, "FUEGO", 3, 2);
        puertaBloqueo(n, 1300f, 3, "TODAS", 0f);

        gema(n, 250f, 80f);
        gema(n, 500f, 160f);
        gema(n, 820f, 200f);
        gema(n, 1110f, 250f);
        gemaSecreta(n, 1250f, 360f);
        return n;
    }

    /** 10. Desafio final: todo combinado en cuatro salas. */
    private static Nivel nivel10() {
        Nivel n = baseConHuecos("El desafio definitivo", "FINAL",
            "Cuatro pruebas conectadas. Coordinen cada mecanismo y lleguen juntos al portal.", 2000f,
            500f, 800f, 900f, 1180f);
        n.origenFuegoX = 45f; n.origenFuegoY = 30f;
        n.origenAguaX = 90f; n.origenAguaY = 30f;
        n.salidaFuego.set(1840f, 30f, 60f, 80f);
        n.salidaAgua.set(1910f, 30f, 60f, 80f);

        // Sala 1: dos placas elementales simultaneas.
        plataforma(n, 110f, 100f, 100f, 18f, "PIEDRA");
        boton(n, 140f, 118f, 1, "FUEGO");
        plataforma(n, 290f, 140f, 100f, 18f, "PIEDRA");
        boton(n, 320f, 158f, 1, "AGUA");
        peligroAgua(n, 215f, 30f, 60f, 14f);
        puertaBloqueo(n, 430f, 1, "TODAS", 6f);

        // Sala 2: abismo con plataformas moviles y un puente activado por palanca.
        movil(n, 520f, 110f, 110f, 18f, 660f, 110f, 95f, 0);
        movil(n, 690f, 160f, 100f, 18f, 690f, 300f, 85f, 0);
        palanca(n, 830f, 30f, 2, "AMBOS");
        puente(n, 900f, 30f, 280f, 16f, 2);

        // Sala 3: secuencia de palancas.
        plataforma(n, 1180f, 100f, 90f, 18f, "METAL");
        palancaSecuencia(n, 1210f, 118f, 3, "AMBOS", 1, 3);
        plataforma(n, 1290f, 190f, 90f, 18f, "METAL");
        palancaSecuencia(n, 1320f, 208f, 3, "AGUA", 2, 3);
        plataforma(n, 1400f, 280f, 90f, 18f, "METAL");
        palancaSecuencia(n, 1430f, 298f, 3, "FUEGO", 3, 3);
        puertaBloqueo(n, 1620f, 3, "TODAS", 0f);

        // Sala 4: caja pesada final; los dos deben empujarla.
        caja(n, 1660f, 30f, true);
        boton(n, 1710f, 30f, 4, "AMBOS");
        plataforma(n, 1780f, 30f, 20f, 50f, "METAL");
        puertaBloqueo(n, 1800f, 4, "ALGUNA", 0f);

        gema(n, 160f, 150f);
        gema(n, 600f, 170f);
        gema(n, 740f, 260f);
        gema(n, 1240f, 160f);
        gema(n, 1350f, 250f);
        gemaSecreta(n, 1550f, 60f);
        return n;
    }

    // =========================================================
    //                 CONSTRUCCION Y ATAJOS
    // =========================================================

    private static Nivel base(String nombre, String tema, String pista, float ancho) {
        Nivel n = new Nivel();
        n.nombre = nombre;
        n.tema = tema;
        n.pista = pista;
        n.ancho = ancho;
        n.alto = Configuracion.ALTO_MUNDO;
        suelo(n, 0f, ancho);
        bordes(n);
        return n;
    }

    /** Crea un nivel con el piso cortado en varios huecos (para abismos). */
    private static Nivel baseConHuecos(String nombre, String tema, String pista, float ancho, float... cortes) {
        Nivel n = new Nivel();
        n.nombre = nombre;
        n.tema = tema;
        n.pista = pista;
        n.ancho = ancho;
        n.alto = Configuracion.ALTO_MUNDO;

        float inicio = 0f;
        for (int i = 0; i + 1 < cortes.length; i += 2) {
            float desde = cortes[i];
            float hasta = cortes[i + 1];
            if (desde > inicio) suelo(n, inicio, desde - inicio);
            inicio = hasta;
        }
        if (inicio < ancho) suelo(n, inicio, ancho - inicio);
        bordes(n);
        return n;
    }

    private static void suelo(Nivel n, float x, float ancho) {
        plataforma(n, x, 0f, ancho, 30f, "PIEDRA");
    }

    private static void bordes(Nivel n) {
        plataforma(n, 0f, 0f, 18f, n.alto, "MURO");
        plataforma(n, n.ancho - 18f, 0f, 18f, n.alto, "MURO");
        plataforma(n, 0f, n.alto - 16f, n.ancho, 16f, "MURO");
    }

    private static void plataforma(Nivel n, float x, float y, float ancho, float alto, String estilo) {
        n.agregar(new Nivel.Plataforma(x, y, ancho, alto, estilo));
    }

    private static void peligroLava(Nivel n, float x, float y, float ancho, float alto) {
        n.agregar(new Nivel.Peligro(x, y, ancho, alto, true));
    }

    private static void peligroAgua(Nivel n, float x, float y, float ancho, float alto) {
        n.agregar(new Nivel.Peligro(x, y, ancho, alto, false));
    }

    private static void gema(Nivel n, float x, float y) {
        n.agregar(new Nivel.Gema(x, y));
    }

    private static void gemaSecreta(Nivel n, float x, float y) {
        Nivel.Gema g = new Nivel.Gema(x, y);
        g.secreta = true;
        n.agregar(g);
    }

    private static void boton(Nivel n, float x, float y, int id, String elemento) {
        n.agregar(new Nivel.Boton(x, y, id, elemento));
    }

    private static void palanca(Nivel n, float x, float y, int id, String elemento) {
        n.agregar(new Nivel.Palanca(x, y, id, elemento));
    }

    private static void palancaSecuencia(Nivel n, float x, float y, int id, String elemento,
                                         int orden, int grupo) {
        Nivel.Palanca p = new Nivel.Palanca(x, y, id, elemento);
        p.orden = orden;
        p.grupo = grupo;
        n.agregar(p);
    }

    /** Puerta baja con un muro encima: no se puede saltar por arriba. */
    private static void puertaBloqueo(Nivel n, float x, int id, String modo, float duracion) {
        n.agregar(new Nivel.Puerta(x, 30f, 20f, 250f, id, modo, duracion, false));
        plataforma(n, x - 2f, 280f, 24f, n.alto - 296f, "MURO");
    }

    private static void puente(Nivel n, float x, float y, float ancho, float alto, int id) {
        n.agregar(new Nivel.Puerta(x, y, ancho, alto, id, "ALGUNA", 0f, true));
    }

    private static void movil(Nivel n, float x, float y, float ancho, float alto,
                              float finX, float finY, float velocidad, int id) {
        n.agregar(new Nivel.PlataformaMovil(x, y, ancho, alto, finX, finY, velocidad, id));
    }

    private static void caja(Nivel n, float x, float y, boolean pesada) {
        n.agregar(new Nivel.Caja(x, y, pesada));
    }
}
