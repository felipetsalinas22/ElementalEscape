package io.github.ElementalEscape;

/**
 * Los ocho niveles del modo campana, rediseñados desde cero con dificultad
 * progresiva, cooperación obligatoria y elementos elemento-estrictos.
 * Cada nivel es un rompecabezas de plataformas que requiere que Fuego y
 * Agua trabajen juntos para llegar a sus respectivas salidas.
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
            default: return nivel1();
        }
    }

    public static String nombre(int indice) {
        return crear(indice).nombre;
    }

    // =========================================================
    //                        NIVELES
    // =========================================================

    /** Nivel 1 - Introducción: movimiento básico, primeras palancas y puerta simple. */
    private static Nivel nivel1() {
        Nivel n = base("El despertar elemental", "TEMPLO",
            "Activá la palanca roja y después la azul. Luego crucen la puerta juntos.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(810f, 30f, 60f, 80f);
        n.salidaAgua.set(875f, 30f, 60f, 80f);

        // Piso principal con tres plataformas horizontales a diferentes alturas.
        plataforma(n, 90f, 90f, 140f, 20f, "PIEDRA");
        plataforma(n, 300f, 90f, 180f, 20f, "PIEDRA");
        plataforma(n, 540f, 90f, 140f, 20f, "MADERA");

        // Peligras elementales separadas, al alcance del suelo.
        peligroAgua(n, 470f, 30f, 80f, 14f);
        peligroLava(n, 250f, 30f, 80f, 14f);

        // Gemas accesibles en el camino.
        gema(n, 180f, 135f);
        gema(n, 350f, 205f);
        gema(n, 590f, 150f);

        // Palanca roja para Fuego (solo Fuego puede activarla).
        palanca(n, 125f, 110f, 1, "FUEGO");
        // Palanca azul para Agua (solo Agua puede activarla).
        palanca(n, 220f, 110f, 2, "AGUA");

        // Puerta final que se abre después de activar ambas palancas (modo ALGUNA, cualquier sirve).
        puertaBloqueo(n, 700f, 1, "ALGUNA", 0f);

        return n;
    }

    /** Nivel 2 - Primeros rompecabezas cooperativos: dos caminos separados que requieren ayuda mutua. */
    private static Nivel nivel2() {
        Nivel n = base("Caminos separados", "CAVERNA",
            "Cada palanca abre la puerta del otro camino. Ambos personajes son necesarios.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 855f; n.origenAguaY = 30f;
        n.salidaFuego.set(390f, 30f, 60f, 80f);
        n.salidaAgua.set(510f, 30f, 60f, 80f);

        // Muro central que divide el nivel, con dos puertas laterales.
        plataforma(n, 460f, 30f, 40f, 494f, "MURO");
        puertaBloqueo(n, 330f, 1, "ALGUNA", 0f);
        puertaBloqueo(n, 590f, 1, "ALGUNA", 0f);

        // Camino de la izquierda (Fuego).
        plataforma(n, 90f, 90f, 140f, 20f, "PIEDRA");
        plataforma(n, 60f, 190f, 130f, 18f, "MADERA");
        plataforma(n, 230f, 270f, 80f, 18f, "MADERA");
        plataforma(n, 60f, 350f, 120f, 18f, "MADERA");
        palanca(n, 120f, 110f, 1, "FUEGO");
        peligroAgua(n, 240f, 30f, 70f, 14f);

        // Camino de la derecha (Agua).
        plataforma(n, 800f, 90f, 130f, 20f, "PIEDRA");
        plataforma(n, 760f, 190f, 130f, 18f, "MADERA");
        plataforma(n, 620f, 270f, 110f, 18f, "MADERA");
        plataforma(n, 800f, 350f, 120f, 18f, "MADERA");
        palanca(n, 840f, 110f, 2, "AGUA");
        peligroLava(n, 650f, 30f, 70f, 14f);

        // Gemas en cada camino.
        gema(n, 150f, 140f);
        gema(n, 100f, 240f);
        gema(n, 280f, 320f);
        gemaSecreta(n, 90f, 400f);
        gema(n, 810f, 140f);
        gemaSecreta(n, 860f, 400f);

        // Mecanismo clave: una palanca en el lado izquierdo abre la puerta derecha, y viceversa.
        // El diseño obliga a que un personaje active su palanca para que el otro pueda pasar.

        return n;
    }

    /** Nivel 3 - Caminos entrelazados con secuencias de palancas y plataformas móviles. */
    private static Nivel nivel3() {
        Nivel n = base("El templo de los interruptores", "RUINAS",
            "Dos placas simultáneas deben pisarse a la vez. La puerta queda abierta 7 segundos.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 105f; n.origenAguaY = 30f;
        n.salidaFuego.set(810f, 30f, 60f, 80f);
        n.salidaAgua.set(875f, 30f, 60f, 80f);

        // Dos botones de presión elementales.
        boton(n, 250f, 30f, 1, "FUEGO");
        boton(n, 390f, 30f, 1, "AGUA");
        puertaBloqueo(n, 620f, 1, "TODAS", 7f);

        // Plataformas verticales de apoyo en columnas separadas.
        plataforma(n, 120f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 300f, 180f, 120f, 18f, "MADERA");
        plataforma(n, 480f, 110f, 110f, 18f, "MADERA");
        plataforma(n, 660f, 190f, 120f, 18f, "MADERA");

        // Ruta alta opcional con gemas secreto s.
        plataforma(n, 60f, 300f, 100f, 18f, "MADERA");
        plataforma(n, 200f, 370f, 100f, 18f, "MADERA");
        plataforma(n, 340f, 440f, 100f, 18f, "MADERA");

        // Peligras elementales.
        peligroAgua(n, 500f, 30f, 80f, 14f);
        peligroLava(n, 700f, 30f, 70f, 14f);

        // Gemas principales y secreta.
        gema(n, 160f, 150f);
        gema(n, 350f, 230f);
        gema(n, 520f, 160f);
        gemaSecreta(n, 375f, 480f);

        // Secuencia implícita: primero pisar la placa Fuego, luego la placa Agua, dentro de los 7 segundos,
        // para que la puerta TODAS se abra. Si uno falla, la puerta se cierra.

        return n;
    }

    /** Nivel 4 - Ascenso vertical con plataforma móvil y palanca final. */
    private static Nivel nivel4() {
        Nivel n = base("La torre de los elementos", "CIELO",
            "Suban por la torre hasta la palanca. Luego bajen y crucen la puerta.", 960f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(820f, 30f, 60f, 80f);
        n.salidaAgua.set(880f, 30f, 60f, 80f);

        // Ascenso en zigzag con plataformas fijas.
        plataforma(n, 70f, 90f, 140f, 18f, "PIEDRA");
        plataforma(n, 260f, 170f, 130f, 18f, "MADERA");
        plataforma(n, 90f, 250f, 130f, 18f, "MADERA");
        plataforma(n, 280f, 330f, 130f, 18f, "MADERA");
        plataforma(n, 100f, 410f, 130f, 18f, "MADERA");
        plataforma(n, 330f, 450f, 140f, 18f, "PIEDRA");
        palanca(n, 370f, 468f, 1, "AMBOS");

        // Plataforma móvil que desciende por la derecha, velocidad 85 unidades/seg.
        movil(n, 500f, 340f, 100f, 18f, 640f, 340f, 85f, 0);
        plataforma(n, 620f, 300f, 120f, 18f, "MADERA");
        plataforma(n, 650f, 210f, 100f, 18f, "MADERA");
        plataforma(n, 620f, 130f, 90f, 18f, "MADERA");

        // Puerta al final del descenso.
        puertaBloqueo(n, 770f, 1, "ALGUNA", 0f);
        peligroAgua(n, 430f, 30f, 70f, 14f);
        peligroLava(n, 520f, 30f, 70f, 14f);

        // Gemas distribuidas por el recorrido vertical.
        gema(n, 120f, 140f);
        gema(n, 320f, 220f);
        gema(n, 140f, 300f);
        gema(n, 340f, 380f);
        gema(n, 160f, 460f);
        gema(n, 380f, 495f);
        gemaSecreta(n, 680f, 255f);

        // El salto final desde la plataforma móvil hasta el requiere sincronización:
        // un personaje debe activar la palanca AMBOS mientras el otro está en posición.

        return n;
    }

    /** Nivel 5 - Tres salas conectadas con ruta baja y ruta alta. */
    private static Nivel nivel5() {
        Nivel n = base("Salas conectadas", "RUINAS",
            "Pueden avanzar por el suelo o por las repisas. La palanca de la sala central abre la salida.", 1200f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(1060f, 30f, 60f, 80f);
        n.salidaAgua.set(1120f, 30f, 60f, 80f);

        // Murales que dejan libre la planta baja entre salas.
        plataforma(n, 400f, 240f, 20f, 284f, "MURO");
        plataforma(n, 800f, 240f, 20f, 284f, "MURO");

        // Sala A: dos repisas y gemas. Riesgo de peligros elementales.
        plataforma(n, 90f, 100f, 110f, 18f, "MADERA");
        plataforma(n, 230f, 180f, 110f, 18f, "MADERA");
        plataforma(n, 350f, 120f, 100f, 18f, "MADERA");
        peligroAgua(n, 200f, 30f, 60f, 14f);
        peligroLava(n, 300f, 30f, 60f, 14f);

        // Sala B: palanca sobre repisa a la que se llega por dos caminos.
        // Requiere que ambos personajes alcancen la repisa simultáneamente.
        plataforma(n, 430f, 110f, 110f, 18f, "MADERA");
        plataforma(n, 550f, 190f, 120f, 18f, "MADERA");
        plataforma(n, 680f, 130f, 110f, 18f, "MADERA");
        palanca(n, 590f, 208f, 1, "AMBOS");
        puertaBloqueo(n, 1000f, 1, "ALGUNA", 0f);

        // Sala C: salida protegida y última gema.
        plataforma(n, 900f, 120f, 90f, 18f, "MADERA");
        peligroLava(n, 890f, 30f, 50f, 14f);

        // Gemas finales y secreta.
        gema(n, 130f, 140f);
        gema(n, 270f, 220f);
        gemaSecreta(n, 390f, 160f);
        gema(n, 470f, 150f);
        gema(n, 630f, 240f);
        gema(n, 930f, 160f);

        // La sala central obliga a que ambos actúen juntos sobre la palanca AMBOS antes de poder salir.

        return n;
    }

    /** Nivel 6 - Cooperación fuerte: caja pesada y dos placas a la vez. */
    private static Nivel nivel6() {
        Nivel n = base("El corazón compartido", "VOLCAN",
            "Empujen la caja pesada entre los dos. Despues pisen las dos placas a la vez.", 1260f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(1120f, 30f, 60f, 80f);
        n.salidaAgua.set(1180f, 30f, 60f, 80f);

        // Sala A: la caja pesada solo avanza si empujan los dos.
        // Mecanismo de empuje: cada personaje debe estar en un lado de la caja.
        caja(n, 200f, 30f, true);
        boton(n, 320f, 30f, 1, "AMBOS");
        puertaBloqueo(n, 430f, 1, "ALGUNA", 0f);

        // Sala B: dos placas simultáneas con puerta temporizada TODAS (8 segundos).
        // Requiere que ambos personajes pisen sus respectivas placas al mismo tiempo.
        boton(n, 620f, 30f, 2, "FUEGO");
        boton(n, 720f, 30f, 2, "AGUA");
        puertaBloqueo(n, 840f, 2, "TODAS", 8f);
        plataforma(n, 480f, 120f, 110f, 18f, "MADERA");
        plataforma(n, 620f, 200f, 100f, 18f, "MADERA");
        plataforma(n, 760f, 120f, 100f, 18f, "MADERA");
        peligroAgua(n, 500f, 30f, 60f, 14f);

        // Sala C: palanca final que mantiene la puerta abierta.
        plataforma(n, 920f, 100f, 100f, 18f, "PIEDRA");
        palanca(n, 950f, 118f, 3, "AMBOS");
        puertaBloqueo(n, 1060f, 3, "ALGUNA", 0f);
        peligroLava(n, 890f, 30f, 50f, 14f);

        // Gemas dispersas en las tres salas.
        gema(n, 250f, 140f);
        gema(n, 510f, 160f);
        gema(n, 660f, 240f);
        gema(n, 800f, 160f);
        gemaSecreta(n, 990f, 160f);

        // Nivel completo: primero mover la caja pesada, luego presionar las dos placas a la vez,
        // y finalmente activar la palanca final. Cualquier paso sin la cooperación requerida falla.

        return n;
    }

    /** Nivel 7 - Mecánicas mezcladas: plataforma móvil, puente, secuencia y caja. */
    private static Nivel nivel7() {
        Nivel n = baseConHuecos("El mecanismo de las tres llaves", "LABORATORIO",
            "Crucen el abismo, abran el puente y activen las tres palancas en orden.", 1500f,
            220f, 400f, 520f, 700f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(1360f, 30f, 60f, 80f);
        n.salidaAgua.set(1420f, 30f, 60f, 80f);

        // Sala A: plataforma móvil sobre el primer abismo.
        // La plataforma se mueve de x=240 a x=370, ancho 110, velocidad 90.
        movil(n, 240f, 110f, 110f, 18f, 370f, 110f, 90f, 0);
        palanca(n, 430f, 30f, 1, "AMBOS");
        puente(n, 520f, 30f, 180f, 16f, 1);

        // Sala B: tres palancas en secuencia obligatoria.
        // Orden: primero AMBOS, luego Fuego, luego Agua. Si se rompe el orden, la secuencia reinicia.
        plataforma(n, 760f, 90f, 110f, 18f, "METAL");
        palancaSecuencia(n, 790f, 108f, 2, "AMBOS", 1, 1);
        plataforma(n, 880f, 170f, 110f, 18f, "METAL");
        palancaSecuencia(n, 910f, 188f, 2, "FUEGO", 2, 1);
        plataforma(n, 1000f, 250f, 110f, 18f, "METAL");
        palancaSecuencia(n, 1030f, 268f, 2, "AGUA", 3, 1);
        puertaBloqueo(n, 1120f, 2, "TODAS", 0f);

        // Sala C: una caja (ligera) mantiene la última placa y abre la salida.
        caja(n, 1160f, 30f, false);
        boton(n, 1260f, 30f, 3, "AMBOS");
        puertaBloqueo(n, 1320f, 3, "ALGUNA", 0f);

        // Peligras elementales distribuidas en las tres salas.
        peligroAgua(n, 160f, 30f, 50f, 14f);
        peligroLava(n, 470f, 30f, 40f, 14f);
        peligroAgua(n, 720f, 30f, 40f, 14f);

        // Gemas en alturas variadas, algunas en zonas de difícil acceso.
        gema(n, 180f, 140f);
        gema(n, 300f, 170f);
        gema(n, 820f, 140f);
        gema(n, 940f, 220f);
        gema(n, 1060f, 300f);
        gemaSecreta(n, 1060f, 330f);
        gema(n, 1200f, 70f);

        // Secuencia estricta: las tres palancas deben activarse en el orden correcto (1→2→3).
        // Si un personaje activa una palanca fuera de orden, la secuencia se reinicia.
        // Requiere coordinación: un personaje puede necesitar sostener una posición mientras el otro avanza.

        return n;
    }

    /** Nivel 8 - Desafío final: combinación de todas las mecánicas con cooperación avanzada. */
    private static Nivel nivel8() {
        Nivel n = baseConHuecos("El santuario final", "FINAL",
            "Cuatro salas conectadas. Coordinen cada mecanismo y lleguen juntos al santuario.", 1700f,
            540f, 760f);
        n.origenFuegoX = 55f; n.origenFuegoY = 30f;
        n.origenAguaX = 100f; n.origenAguaY = 30f;
        n.salidaFuego.set(1560f, 30f, 60f, 80f);
        n.salidaAgua.set(1620f, 30f, 60f, 80f);

        // Sala 1: dos placas elementales simultáneas al inicio.
        // Deben pisarse juntas para que la puerta TODAS (7s) se abra.
        boton(n, 200f, 30f, 1, "FUEGO");
        boton(n, 340f, 30f, 1, "AGUA");
        puertaBloqueo(n, 460f, 1, "TODAS", 7f);
        peligroLava(n, 500f, 30f, 30f, 14f);

        // Sala 2: abismo crossed por una plataforma móvil de lado a lado.
        // La plataforma se mueve de x=560 a x=680, ancho 110, velocidad 85.
        movil(n, 560f, 110f, 110f, 18f, 680f, 110f, 85f, 0);
        peligroAgua(n, 780f, 30f, 40f, 14f);

        // Sala 3: secuencia de tres palancas con orden obligatorio.
        // Igual que en el nivel 7: AMBOS (1), Fuego (2), Agua (3).
        // Si el orden se rompe, la secuencia se reinicia y las puertas se cierran.
        plataforma(n, 800f, 90f, 110f, 18f, "PIEDRA");
        palancaSecuencia(n, 830f, 108f, 2, "AMBOS", 1, 1);
        plataforma(n, 920f, 170f, 110f, 18f, "PIEDRA");
        palancaSecuencia(n, 950f, 188f, 2, "FUEGO", 2, 1);
        plataforma(n, 1040f, 250f, 110f, 18f, "PIEDRA");
        palancaSecuencia(n, 1070f, 268f, 2, "AGUA", 3, 1);
        plataforma(n, 1090f, 330f, 90f, 18f, "MADERA");
        puertaBloqueo(n, 1200f, 2, "TODAS", 0f);

        // Sala 4: caja pesada y placa final.
        // La caja pesada debe empujarse hasta la placa final, luego activar la palanca AMBOS.
        caja(n, 1320f, 30f, true);
        boton(n, 1430f, 30f, 3, "AMBOS");
        puertaBloqueo(n, 1500f, 3, "ALGUNA", 0f);

        // Gemas finales, algunas en zonas de difícil acceso tras los mecanismos.
        gema(n, 250f, 130f);
        gema(n, 610f, 170f);
        gema(n, 860f, 140f);
        gema(n, 980f, 220f);
        gema(n, 1100f, 300f);
        gemaSecreta(n, 1120f, 360f);
        gema(n, 1360f, 80f);

        // El nivel 8 es el gran desafío: combina todo lo aprendido en los niveles anteriores.
        // Requiere:
        // - Activación simultánea de placas en la Sala 1.
        // - Timing preciso con la plataforma móvil en la Sala 2.
        // - Secuencia perfecta de tres palancas en la Sala 3.
        // - Empuje de caja pesada y placa final en la Sala 4.
        // - Coordinación constante entre ambos personajes para no fallar en ningun paso.
        // - No es posible saltear ningun paso: cada mecanismo es necesario para llegar a la salida.

        return n;
    }

    // =========================================================
    //                 CONSTRUCCIÓN Y ATAJOS
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