package io.github.ElementalEscape;

/**
 * Campana de 8 niveles rediseñada: rompecabezas cooperativos con dificultad
 * progresiva, rutas con sentido, activaciones elemento-estrictas y cooperación
 * obligatoria. Ningun elemento jugable importante se coloca en la franja del HUD.
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
    // NIVEL 1 - Introduccion cooperativa (mas elaborado)
    // Recorrido: Fuego toma ruta izquierda alta para palanca roja que abre
    // puerta azul; Agua toma ruta derecha para palanca azul que abre puerta roja.
    // Ambos deben activar sus mecanismos para que el otro avance y reunirse al final.
    // =========================================================
    private static Nivel nivel1() {
        Nivel n = base("El despertar elemental", "TEMPLO",
            "Fuego y Agua deben abrirse paso mutuamente: cada uno activa la palanca de su elemento.", 960f);

        n.origenFuegoX = 40f;  n.origenFuegoY = 30f;
        n.origenAguaX  = 90f;  n.origenAguaY  = 30f;
        n.salidaFuego.set(820f, 30f, 60f, 80f);
        n.salidaAgua.set(885f, 30f, 60f, 80f);

        // Etapa 1: rutas separadas iniciales con pequeños desniveles
        plataforma(n, 30f, 90f, 120f, 18f, "PIEDRA");
        plataforma(n, 180f, 150f, 110f, 18f, "MADERA");
        plataforma(n, 30f, 210f, 110f, 18f, "MADERA");
        // Palanca roja en zona alta izquierda: solo Fuego puede alcanzarla/activarla
        palanca(n, 55f, 228f, 1, "FUEGO");
        gema(n, 210f, 185f);

        plataforma(n, 320f, 90f, 120f, 18f, "PIEDRA");
        plataforma(n, 470f, 150f, 110f, 18f, "MADERA");
        plataforma(n, 610f, 210f, 110f, 18f, "MADERA");
        // Palanca azul en zona alta derecha: solo Agua
        palanca(n, 650f, 228f, 2, "AGUA");
        gema(n, 500f, 185f);

        // Peligros elementales obligan a usar rutas correctas
        peligroLava(n, 240f, 30f, 70f, 14f);   // bloquea paso bajo para Agua
        peligroAgua(n, 560f, 30f, 70f, 14f);   // bloquea paso bajo para Fuego

        // Etapa 2: puertas cruzadas en pasillo central
        // Puerta 1 (abierta por palanca roja de Fuego) deja pasar a Agua
        puertaBloqueo(n, 360f, 1, "ALGUNA", 0f);
        // Puerta 2 (abierta por palanca azul de Agua) deja pasar a Fuego
        puertaBloqueo(n, 430f, 2, "ALGUNA", 0f);

        // Plataformas de apoyo para cruzar tras abrir
        plataforma(n, 330f, 120f, 70f, 18f, "MADERA");
        plataforma(n, 440f, 120f, 70f, 18f, "MADERA");
        gema(n, 395f, 155f);

        // Etapa 3: placa conjunta final abre ultima puerta a salidas
        plataforma(n, 700f, 100f, 90f, 18f, "PIEDRA");
        boton(n, 735f, 118f, 3, "AMBOS");
        puertaBloqueo(n, 760f, 3, "ALGUNA", 0f);
        gema(n, 720f, 155f);

        return n;
    }

    // =========================================================
    // NIVEL 2 - Palancas, puertas y plataforma movil
    // Dos salas: en la primera deben coordinar activaciones para habilitar
    // la plataforma movil; en la segunda usarla para alcanzar palancas finales.
    // =========================================================
    private static Nivel nivel2() {
        Nivel n = base("Ruedas y cerrojos", "CAVERNA",
            "Activen los mecanismos en orden: primero habiliten la plataforma movil, luego abran la salida.", 1100f);

        n.origenFuegoX = 40f;  n.origenFuegoY = 30f;
        n.origenAguaX  = 90f;  n.origenAguaY  = 30f;
        n.salidaFuego.set(960f, 30f, 60f, 80f);
        n.salidaAgua.set(1020f, 30f, 60f, 80f);

        // Sala 1
        plataforma(n, 170f, 90f, 120f, 18f, "PIEDRA");
        plataforma(n, 320f, 150f, 120f, 18f, "MADERA");
        palanca(n, 360f, 168f, 1, "FUEGO"); // habilita movil id 1
        gema(n, 200f, 125f);

        plataforma(n, 170f, 210f, 120f, 18f, "MADERA");
        palanca(n, 210f, 228f, 2, "AGUA"); // abre puerta intermedia id 2
        gema(n, 350f, 185f);

        // Puerta intermedia bloquea acceso a zona de movil
        puertaBloqueo(n, 470f, 2, "ALGUNA", 0f);

        // Plataforma movil controlada por palanca de fuego (id 1)
        movil(n, 500f, 120f, 110f, 18f, 660f, 220f, 85f, 1);
        plataforma(n, 700f, 220f, 120f, 18f, "PIEDRA");
        // Desde plataforma movil en punto alto se alcanza palanca azul para puerta final
        palanca(n, 740f, 238f, 3, "AGUA");

        peligroLava(n, 520f, 30f, 80f, 14f);
        peligroAgua(n, 640f, 30f, 80f, 14f);

        // Sala 2 / salida
        plataforma(n, 780f, 140f, 100f, 18f, "MADERA");
        boton(n, 810f, 158f, 4, "FUEGO"); // Fuego mantiene paso para Agua? no, placa abre puerta 4
        puertaBloqueo(n, 900f, 4, "ALGUNA", 0f);
        // Palanca de agua alternativa: ambas necesarias? puerta 4 es ALGUNA asi que cualquiera - mejor TODAS no.
        // Requiere que Fuego active placa mientras Agua pasa? placa es presion mantiene mientras pisa.
        gema(n, 820f, 175f);
        gema(n, 980f, 70f);

        return n;
    }

    // =========================================================
    // NIVEL 3 - Caminos separados que requieren coordinacion
    // Lado izquierdo para Fuego, derecho para Agua; deben activar mecanismos
    // cruzados y reunirse en zona central elevada para abrir salida.
    // =========================================================
    private static Nivel nivel3() {
        Nivel n = base("Torres enfrentadas", "RUINAS",
            "Cada uno recorre su torre y activa mecanismos para el otro. Reunanse arriba para salir.", 1100f);

        n.origenFuegoX = 40f;  n.origenFuegoY = 30f;
        n.origenAguaX  = 1040f; n.origenAguaY = 30f;
        n.salidaFuego.set(480f, 350f, 60f, 80f);
        n.salidaAgua.set(560f, 350f, 60f, 80f);

        // Divisor central bajo (no bloquea zona alta de reunion)
        plataforma(n, 530f, 30f, 40f, 180f, "MURO");

        // Torre Fuego (izquierda)
        plataforma(n, 30f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 180f, 170f, 120f, 18f, "MADERA");
        plataforma(n, 30f, 240f, 120f, 18f, "MADERA");
        palanca(n, 60f, 258f, 1, "FUEGO"); // abre puerta derecha baja id1
        gema(n, 210f, 205f);
        peligroAgua(n, 160f, 30f, 70f, 14f);

        // Torre Agua (derecha)
        plataforma(n, 950f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 800f, 170f, 120f, 18f, "MADERA");
        plataforma(n, 950f, 240f, 120f, 18f, "MADERA");
        palanca(n, 990f, 258f, 2, "AGUA"); // abre puerta izquierda baja id2
        gema(n, 830f, 205f);
        peligroLava(n, 870f, 30f, 70f, 14f);

        // Puertas cruzadas en base
        puertaBloqueo(n, 330f, 2, "ALGUNA", 0f); // Agua abre para Fuego
        puertaBloqueo(n, 750f, 1, "ALGUNA", 0f); // Fuego abre para Agua

        // Ruta hacia centro alto tras pasar puertas
        plataforma(n, 360f, 120f, 140f, 18f, "MADERA");
        plataforma(n, 600f, 120f, 140f, 18f, "MADERA");
        plataforma(n, 420f, 200f, 120f, 18f, "MADERA");
        plataforma(n, 560f, 200f, 120f, 18f, "MADERA");
        plataforma(n, 480f, 280f, 140f, 18f, "PIEDRA"); // plataforma comun de reunion

        // Dos placas simultaneas en reunion abren puertas de salidas
        boton(n, 495f, 298f, 3, "FUEGO");
        boton(n, 575f, 298f, 3, "AGUA");
        puertaBloqueo(n, 450f, 3, "TODAS", 0f);
        puertaBloqueo(n, 630f, 3, "TODAS", 0f);

        gema(n, 500f, 235f);
        gema(n, 580f, 235f);
        gemaSecreta(n, 550f, 325f);

        return n;
    }

    // =========================================================
    // NIVEL 4 - Laberinto vertical con mecanismos interconectados
    // Ascenso en etapas; palancas en distintos niveles abren rutas alternas
    // y una plataforma movil conecta secciones.
    // =========================================================
    private static Nivel nivel4() {
        Nivel n = base("Ascenso entrelazado", "CIELO",
            "Suban coordinando palancas en distintos niveles. La plataforma movil depende de sus acciones.", 960f);

        n.origenFuegoX = 40f; n.origenFuegoY = 30f;
        n.origenAguaX  = 90f; n.origenAguaY  = 30f;
        n.salidaFuego.set(800f, 380f, 60f, 80f);
        n.salidaAgua.set(870f, 380f, 60f, 80f);

        // Nivel 1
        plataforma(n, 30f, 90f, 140f, 18f, "PIEDRA");
        plataforma(n, 220f, 90f, 140f, 18f, "PIEDRA");
        palanca(n, 260f, 108f, 1, "AGUA"); // abre acceso izq nivel 2
        peligroLava(n, 170f, 30f, 50f, 14f);
        gema(n, 70f, 125f);

        // Nivel 2
        puertaBloqueo(n, 150f, 1, "ALGUNA", 0f);
        plataforma(n, 30f, 170f, 120f, 18f, "MADERA");
        plataforma(n, 200f, 170f, 120f, 18f, "MADERA");
        palanca(n, 60f, 188f, 2, "FUEGO"); // activa movil id 2
        gema(n, 240f, 205f);

        // Conexión móvil entre nivel 2 y 3
        movil(n, 360f, 170f, 110f, 18f, 360f, 260f, 80f, 2);
        plataforma(n, 500f, 260f, 140f, 18f, "PIEDRA");
        palanca(n, 560f, 278f, 3, "AMBOS"); // abre puerta a nivel 4

        // Nivel 3 derecha
        plataforma(n, 680f, 170f, 120f, 18f, "MADERA");
        plataforma(n, 820f, 220f, 110f, 18f, "MADERA");
        puertaBloqueo(n, 640f, 3, "ALGUNA", 0f);
        peligroAgua(n, 420f, 30f, 80f, 14f);
        gema(n, 720f, 205f);

        // Nivel 4 superior - requiere ambos para placa final
        plataforma(n, 680f, 300f, 120f, 18f, "MADERA");
        plataforma(n, 820f, 300f, 110f, 18f, "PIEDRA");
        boton(n, 700f, 318f, 4, "FUEGO");
        boton(n, 850f, 318f, 4, "AGUA");
        // puertas frente a salidas
        puertaBloqueo(n, 770f, 4, "TODAS", 0f);
        gema(n, 860f, 335f);
        gemaSecreta(n, 540f, 295f);

        return n;
    }

    // =========================================================
    // NIVEL 5 - Varias habitaciones y mecanismos en orden determinado
    // Tres habitaciones; hay que resolver A->B->C, con secuencia de palancas
    // y una placa que debe mantenerse mientras el otro avanza.
    // =========================================================
    private static Nivel nivel5() {
        Nivel n = base("Salas del sello", "HIELO",
            "Resuelvan las salas en orden. Una secuencia y una placa mantenida son la clave.", 1200f);

        n.origenFuegoX = 40f; n.origenFuegoY = 30f;
        n.origenAguaX  = 85f; n.origenAguaY  = 30f;
        n.salidaFuego.set(1060f, 30f, 60f, 80f);
        n.salidaAgua.set(1125f, 30f, 60f, 80f);

        // Muros divisorios con puertas
        plataforma(n, 380f, 30f, 20f, 434f, "MURO");
        plataforma(n, 780f, 30f, 20f, 434f, "MURO");

        // Sala A
        plataforma(n, 120f, 100f, 110f, 18f, "MADERA");
        plataforma(n, 260f, 170f, 100f, 18f, "MADERA");
        palancaSecuencia(n, 150f, 118f, 1, "FUEGO", 1, 1);
        palancaSecuencia(n, 290f, 188f, 1, "AGUA", 2, 1);
        puertaBloqueo(n, 360f, 1, "TODAS", 0f); // paso A->B requiere secuencia
        gema(n, 300f, 205f);
        peligroLava(n, 220f, 30f, 50f, 14f);

        // Sala B
        plataforma(n, 420f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 560f, 180f, 120f, 18f, "MADERA");
        plataforma(n, 670f, 100f, 90f, 18f, "MADERA");
        boton(n, 450f, 118f, 2, "AGUA"); // Agua mantiene, Fuego pasa por puerta temporal
        puertaBloqueo(n, 760f, 2, "ALGUNA", 4.5f);
        gema(n, 590f, 215f);
        gema(n, 700f, 135f);
        peligroAgua(n, 520f, 30f, 60f, 14f);

        // Sala C
        plataforma(n, 820f, 100f, 120f, 18f, "MADERA");
        plataforma(n, 960f, 170f, 100f, 18f, "MADERA");
        boton(n, 850f, 118f, 3, "FUEGO");
        boton(n, 990f, 188f, 3, "AGUA");
        puertaBloqueo(n, 1010f, 3, "TODAS", 0f);
        gema(n, 990f, 205f);
        gemaSecreta(n, 880f, 215f);

        return n;
    }

    // =========================================================
    // NIVEL 6 - Recorridos extensos, rutas diferentes y obstaculos combinados
    // Caja pesada, plataforma movil, rutas bifurcadas y reunion final.
    // =========================================================
    private static Nivel nivel6() {
        Nivel n = base("Galerias cruzadas", "LABORATORIO",
            "Rutas distintas, caja pesada y plataforma movil. Coordinen cada tramo para avanzar.", 1400f);

        n.origenFuegoX = 40f; n.origenFuegoY = 30f;
        n.origenAguaX  = 85f; n.origenAguaY  = 30f;
        n.salidaFuego.set(1260f, 30f, 60f, 80f);
        n.salidaAgua.set(1325f, 30f, 60f, 80f);

        // Tramo 1: caja pesada sobre placa abre puerta
        caja(n, 180f, 30f, true);
        plataforma(n, 300f, 100f, 100f, 18f, "PIEDRA"); // acceso para empujar desde arriba? no necesario
        boton(n, 340f, 30f, 1, "AMBOS");
        puertaBloqueo(n, 420f, 1, "ALGUNA", 0f);
        peligroLava(n, 260f, 30f, 40f, 14f);
        gema(n, 320f, 135f);

        // Tramo 2: rutas separadas con palancas cruzadas
        plataforma(n, 460f, 100f, 120f, 18f, "MADERA");
        plataforma(n, 460f, 200f, 120f, 18f, "MADERA");
        palanca(n, 500f, 218f, 2, "FUEGO"); // abre paso bajo derecho para Agua
        plataforma(n, 620f, 100f, 120f, 18f, "MADERA");
        plataforma(n, 620f, 200f, 120f, 18f, "MADERA");
        palanca(n, 660f, 218f, 3, "AGUA"); // abre paso alto para Fuego
        puertaBloqueo(n, 580f, 3, "ALGUNA", 0f);
        puertaBloqueo(n, 760f, 2, "ALGUNA", 0f);
        peligroAgua(n, 520f, 30f, 60f, 14f);
        gema(n, 500f, 135f);
        gema(n, 660f, 135f);

        // Tramo 3: plataforma movil para alcanzar zona elevada
        movil(n, 800f, 90f, 110f, 18f, 800f, 220f, 90f, 0);
        plataforma(n, 940f, 220f, 120f, 18f, "PIEDRA");
        palanca(n, 980f, 238f, 4, "AMBOS");
        puertaBloqueo(n, 1080f, 4, "ALGUNA", 0f);
        peligroLava(n, 860f, 30f, 70f, 14f);
        gema(n, 980f, 255f);

        // Tramo 4: dos placas simultaneas antes de salida
        plataforma(n, 1120f, 100f, 100f, 18f, "MADERA");
        boton(n, 1140f, 118f, 5, "FUEGO");
        boton(n, 1180f, 30f, 5, "AGUA");
        puertaBloqueo(n, 1220f, 5, "TODAS", 0f);
        gemaSecreta(n, 1150f, 155f);

        return n;
    }

    // =========================================================
    // NIVEL 7 - Rompecabezas complejos, colaboracion constante
    // Secuencia estricta, puente temporal, movil y sostener placas.
    // =========================================================
    private static Nivel nivel7() {
        Nivel n = base("Nucleo sincronizado", "VOLCAN",
            "Secuencia estricta, un puente activado y placas simultaneas: sin coordinacion no hay salida.", 1500f);

        n.origenFuegoX = 40f; n.origenFuegoY = 30f;
        n.origenAguaX  = 85f; n.origenAguaY  = 30f;
        n.salidaFuego.set(1360f, 30f, 60f, 80f);
        n.salidaAgua.set(1425f, 30f, 60f, 80f);

        // Etapa 1: secuencia de 3 palancas en orden
        plataforma(n, 160f, 100f, 110f, 18f, "MADERA");
        plataforma(n, 300f, 170f, 110f, 18f, "MADERA");
        plataforma(n, 440f, 240f, 110f, 18f, "MADERA");
        palancaSecuencia(n, 190f, 118f, 1, "FUEGO", 1, 1);
        palancaSecuencia(n, 330f, 188f, 1, "AGUA", 2, 1);
        palancaSecuencia(n, 470f, 258f, 1, "AMBOS", 3, 1);
        puertaBloqueo(n, 560f, 1, "TODAS", 0f);
        gema(n, 330f, 205f);
        gema(n, 470f, 275f);

        // Etapa 2: placa de Fuego activa puente para cruzar pozo
        plataforma(n, 600f, 100f, 100f, 18f, "PIEDRA");
        boton(n, 630f, 118f, 2, "FUEGO");
        peligroAgua(n, 720f, 30f, 120f, 14f);
        puente(n, 720f, 30f, 120f, 16f, 2);
        plataforma(n, 860f, 100f, 100f, 18f, "PIEDRA");
        palanca(n, 890f, 118f, 3, "AGUA"); // abre siguiente puerta
        gema(n, 780f, 70f);

        // Etapa 3: movil y placas simultaneas
        puertaBloqueo(n, 980f, 3, "ALGUNA", 0f);
        movil(n, 1000f, 90f, 110f, 18f, 1140f, 90f, 95f, 0);
        plataforma(n, 1160f, 160f, 110f, 18f, "MADERA");
        plataforma(n, 1160f, 240f, 110f, 18f, "MADERA");
        boton(n, 1185f, 178f, 4, "FUEGO");
        boton(n, 1185f, 258f, 4, "AGUA");
        puertaBloqueo(n, 1300f, 4, "TODAS", 0f);
        peligroLava(n, 1040f, 30f, 90f, 14f);
        gema(n, 1185f, 295f);
        gemaSecreta(n, 1080f, 125f);

        return n;
    }

    // =========================================================
    // NIVEL 8 - Final: combina todas las mecanicas, coordinacion avanzada
    // Varias etapas encadenadas: caja pesada, secuencia, movil, placas dobles.
    // =========================================================
    private static Nivel nivel8() {
        Nivel n = base("Santuario de los elementos", "FINAL",
            "Etapas encadenadas: empujen, activen en orden y coordinen cada paso hasta el final.", 1700f);

        n.origenFuegoX = 40f; n.origenFuegoY = 30f;
        n.origenAguaX  = 85f; n.origenAguaY  = 30f;
        n.salidaFuego.set(1560f, 30f, 60f, 80f);
        n.salidaAgua.set(1625f, 30f, 60f, 80f);

        // Etapa 1: caja pesada abre primer paso
        caja(n, 180f, 30f, true);
        boton(n, 320f, 30f, 1, "AMBOS");
        puertaBloqueo(n, 400f, 1, "ALGUNA", 0f);
        plataforma(n, 140f, 100f, 100f, 18f, "MADERA");
        gema(n, 160f, 135f);

        // Etapa 2: rutas separadas con palancas cruzadas
        plataforma(n, 440f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 440f, 200f, 120f, 18f, "MADERA");
        palanca(n, 480f, 218f, 2, "FUEGO");
        plataforma(n, 600f, 100f, 120f, 18f, "PIEDRA");
        plataforma(n, 600f, 200f, 120f, 18f, "MADERA");
        palanca(n, 640f, 218f, 3, "AGUA");
        puertaBloqueo(n, 560f, 3, "ALGUNA", 0f); // Fuego necesita que Agua abra
        puertaBloqueo(n, 740f, 2, "ALGUNA", 0f); // Agua necesita que Fuego abra
        peligroLava(n, 500f, 30f, 50f, 14f);
        peligroAgua(n, 660f, 30f, 50f, 14f);
        gema(n, 480f, 135f);
        gema(n, 640f, 135f);

        // Etapa 3: secuencia obligatoria
        plataforma(n, 780f, 100f, 110f, 18f, "MADERA");
        plataforma(n, 920f, 170f, 110f, 18f, "MADERA");
        plataforma(n, 1060f, 240f, 110f, 18f, "MADERA");
        palancaSecuencia(n, 810f, 118f, 4, "AMBOS", 1, 1);
        palancaSecuencia(n, 950f, 188f, 4, "FUEGO", 2, 1);
        palancaSecuencia(n, 1090f, 258f, 4, "AGUA", 3, 1);
        puertaBloqueo(n, 1180f, 4, "TODAS", 0f);
        gema(n, 950f, 205f);
        gema(n, 1090f, 275f);

        // Etapa 4: plataforma movil + placas simultaneas finales
        movil(n, 1200f, 120f, 110f, 18f, 1320f, 220f, 90f, 0);
        plataforma(n, 1340f, 220f, 120f, 18f, "PIEDRA");
        plataforma(n, 1340f, 120f, 120f, 18f, "PIEDRA");
        boton(n, 1360f, 138f, 5, "FUEGO");
        boton(n, 1380f, 238f, 5, "AGUA");
        puertaBloqueo(n, 1500f, 5, "TODAS", 0f);
        peligroLava(n, 1240f, 30f, 60f, 14f);
        peligroAgua(n, 1440f, 30f, 50f, 14f);
        gemaSecreta(n, 1380f, 275f);

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
