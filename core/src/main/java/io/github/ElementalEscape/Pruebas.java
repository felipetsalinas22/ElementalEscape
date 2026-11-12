package io.github.ElementalEscape;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Pruebas ejecutables sin abrir una ventana ni crear una aplicacion LibGDX.
 * Se lanzan con: gradlew.bat :core:pruebas
 *
 * Cubren validacion de niveles, fisica basica, mecanismos, puntuacion,
 * persistencia JSON y el protocolo de red en localhost.
 */
public final class Pruebas {
    private static int pruebas = 0;
    private static int fallos = 0;

    private Pruebas() { }

    public static void main(String[] args) {
        System.out.println("== Pruebas de Elemental Escape ==");
        probarNivelesIntegrados();
        probarFisica();
        probarReinicioDePartida();
        probarMecanismos();
        probarMecanismosIntegrados();
        probarPuntuacionYProgreso();
        probarMedidasFijasDelEditor();
        probarPersistenciaNiveles();
        probarPersistenciaProgreso();
        probarRedLocalhost();

        System.out.println();
        System.out.println("Pruebas: " + pruebas + "   Fallos: " + fallos);
        if (fallos > 0) {
            System.out.println("RESULTADO: HAY FALLOS");
            System.exit(1);
        }
        System.out.println("RESULTADO: TODO OK");
    }

    // =========================================================
    //                       NIVELES
    // =========================================================

    private static void probarNivelesIntegrados() {
        seccion("Niveles integrados");
        Nivel[] niveles = NivelesIntegrados.todos();
        comprobar(niveles.length == Configuracion.TOTAL_NIVELES,
            "hay " + Configuracion.TOTAL_NIVELES + " niveles");

        for (int i = 0; i < niveles.length; i++) {
            Nivel n = niveles[i];
            List<String> errores = ValidadorNivel.validar(n);
            comprobar(errores.isEmpty(), "nivel " + (i + 1) + " valido" + detalle(errores));
            comprobar(n.gemas.length > 0, "nivel " + (i + 1) + " tiene gemas");
            comprobar(n.salidaFuego.x < n.salidaAgua.x || n.salidaFuego.y != n.salidaAgua.y,
                "nivel " + (i + 1) + " tiene salidas distintas");
            comprobar(tieneControlesEnlazados(n), "nivel " + (i + 1) + " enlaza controles y puertas");
            List<String> ruta = ValidadorNivel.validarConectividad(n);
            comprobar(ruta.isEmpty(), "nivel " + (i + 1) + " tiene recorrido viable" + detalle(ruta));
        }
    }

    private static boolean tieneControlesEnlazados(Nivel n) {
        for (Nivel.Puerta p : n.puertas) {
            boolean encontrado = false;
            for (Nivel.Boton b : n.botones) if (b.id == p.id) encontrado = true;
            for (Nivel.Palanca l : n.palancas) if (l.id == p.id) encontrado = true;
            if (!encontrado) return false;
        }
        return true;
    }

    private static String detalle(List<String> errores) {
        if (errores == null || errores.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" -> ");
        for (int i = 0; i < errores.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(errores.get(i));
        }
        return sb.toString();
    }

    // =========================================================
    //                        FISICA
    // =========================================================

    private static void probarFisica() {
        seccion("Fisica");
        Nivel n = nivelDePrueba();
        Partida p = new Partida(n, 0, false);

        Partida.Entrada quieto = new Partida.Entrada();
        for (int i = 0; i < 90; i++) p.actualizar(1f / 60f, quieto, quieto);
        comprobar(Math.abs(p.fuego.y - 30f) < 1.5f, "el jugador se queda sobre el piso");
        comprobar(p.fuego.enSuelo, "el jugador detecta el suelo");

        Partida p2 = new Partida(nivelDePrueba(), 0, false);
        float alturaMaxima = 0f;
        for (int i = 0; i < 120; i++) {
            Partida.Entrada salto = new Partida.Entrada();
            salto.saltoPresionado = i == 0;
            salto.saltoMantenido = i < 28;
            p2.actualizar(1f / 60f, salto, salto);
            alturaMaxima = Math.max(alturaMaxima, p2.fuego.y);
        }
        comprobar(alturaMaxima > 100f, "el salto sube mas de 100 px (max " + Math.round(alturaMaxima) + ")");
        comprobar(alturaMaxima < 260f, "el salto no es exagerado (max " + Math.round(alturaMaxima) + ")");

        Nivel conMuro = nivelDePrueba();
        conMuro.agregar(new Nivel.Plataforma(210f, 0f, 30f, 220f, "MURO"));
        Partida p3 = new Partida(conMuro, 0, false);
        Partida.Entrada derecha = new Partida.Entrada();
        derecha.derecha = true;
        for (int i = 0; i < 180; i++) p3.actualizar(1f / 60f, derecha, quieto);
        comprobar(p3.fuego.x + p3.fuego.ancho <= 211f,
            "la pared detiene al jugador (x=" + Math.round(p3.fuego.x) + ")");

        Nivel conMovil = nivelDePrueba();
        conMovil.agregar(new Nivel.PlataformaMovil(120f, 100f, 100f, 18f, 320f, 100f, 100f, 0));
        Partida p4 = new Partida(conMovil, 0, false);
        float xInicial = p4.moviles[0].rect.x;
        for (int i = 0; i < 60; i++) p4.actualizar(1f / 60f, quieto, quieto);
        comprobar(p4.moviles[0].rect.x > xInicial + 30f, "la plataforma movil se desplaza");

        Partida p5 = new Partida(nivelDePrueba(), 0, false);
        int vidasAntes = p5.vidasFuego;
        p5.puntuacion.sumarGema(Jugador.Tipo.FUEGO);
        int puntosAntes = p5.puntuacion.puntosFuego;
        p5.fuego.y = -200f;
        p5.actualizar(1f / 60f, quieto, quieto);
        comprobar(!p5.fuego.vivo && p5.vidasFuego == vidasAntes - 1,
            "caer al vacio consume una vida");
        comprobar(p5.puntuacion.puntosFuego
                == Math.max(0, puntosAntes - Configuracion.PENALIZACION_MUERTE),
            "caer al vacio aplica la penalizacion de muerte");
    }

    private static void probarReinicioDePartida() {
        seccion("Reinicio de partida");
        Nivel n = nivelDePrueba();
        Partida p = new Partida(n, 0, true);
        Partida.Entrada quieto = new Partida.Entrada();
        p.colocarJugador(Jugador.Tipo.FUEGO, 810f, 30f);
        p.colocarJugador(Jugador.Tipo.AGUA, 880f, 30f);
        for (int i = 0; i < 100; i++) p.actualizar(1f / 60f, quieto, quieto);
        comprobar(p.completado, "los dos jugadores en las salidas completan el nivel");
        comprobar(p.resultado != null && p.resultado.puntosFuego > 0, "se genera el resultado");

        p.reiniciar();
        comprobar(!p.completado && p.resultado == null, "reiniciar limpia el estado de victoria");
        comprobar(p.puntuacion.puntosFuego == 0 && p.puntuacion.puntosAgua == 0,
            "reiniciar limpia la puntuacion");

        p.colocarJugador(Jugador.Tipo.FUEGO, 810f, 30f);
        p.colocarJugador(Jugador.Tipo.AGUA, 880f, 30f);
        for (int i = 0; i < 100; i++) p.actualizar(1f / 60f, quieto, quieto);
        comprobar(p.completado && p.resultado != null && p.resultado.puntosFuego > 0,
            "se puede volver a completar el mismo nivel tras reiniciar");
    }

    // =========================================================
    //                     MECANISMOS
    // =========================================================

    private static void probarMecanismos() {
        seccion("Mecanismos");
        Nivel n = nivelDePrueba();
        n.agregar(new Nivel.Boton(300f, 30f, 1, "AMBOS"));
        n.agregar(new Nivel.Puerta(500f, 30f, 20f, 250f, 1, "ALGUNA", 0f, false));
        Partida p = new Partida(n, 0, false);
        Partida.Entrada quieto = new Partida.Entrada();
        p.colocarJugador(Jugador.Tipo.FUEGO, 310f, 30f);
        p.actualizar(1f / 60f, quieto, quieto);
        comprobar(p.botonesActivos[0], "la placa detecta al jugador");
        comprobar(p.puertasAbiertas[0], "la puerta se abre con la placa");
        comprobar(p.puntuacion.mecanismosFuego == 1, "la placa suma un mecanismo");

        Nivel secuencia = nivelDePrueba();
        Nivel.Palanca p1 = new Nivel.Palanca(300f, 30f, 7, "AMBOS");
        p1.orden = 1; p1.grupo = 4;
        Nivel.Palanca p2 = new Nivel.Palanca(400f, 30f, 7, "AMBOS");
        p2.orden = 2; p2.grupo = 4;
        secuencia.agregar(p1);
        secuencia.agregar(p2);
        secuencia.agregar(new Nivel.Puerta(600f, 30f, 20f, 250f, 7, "TODAS", 0f, false));
        Partida ps = new Partida(secuencia, 0, false);
        ps.colocarJugador(Jugador.Tipo.FUEGO, 410f, 30f);
        ps.actualizar(1f / 60f, quieto, quieto);
        comprobar(!ps.palancasActivas[1], "la secuencia rechaza una palanca fuera de orden");
        ps.colocarJugador(Jugador.Tipo.FUEGO, 300f, 30f);
        ps.actualizar(1f / 60f, quieto, quieto);
        comprobar(ps.palancasActivas[0], "la primera palanca de la secuencia se activa");
        ps.colocarJugador(Jugador.Tipo.FUEGO, 410f, 30f);
        ps.actualizar(1f / 60f, quieto, quieto);
        comprobar(ps.palancasActivas[1], "la segunda palanca se activa en orden");
        comprobar(ps.puertasAbiertas[0], "la puerta TODAS se abre al completar la secuencia");

        // Una caja pesada solo debe moverse si empujan los dos personajes.
        Nivel conCaja = nivelDePrueba();
        conCaja.agregar(new Nivel.Caja(300f, 30f, true));
        Partida pc = new Partida(conCaja, 0, false);
        Partida.Entrada derecha = new Partida.Entrada();
        derecha.derecha = true;
        pc.colocarJugador(Jugador.Tipo.FUEGO, 270f, 30f);
        for (int i = 0; i < 60; i++) pc.actualizar(1f / 60f, derecha, quieto);
        float xConUno = pc.cajas[0].rect.x;
        comprobar(xConUno <= 301f, "la caja pesada no se mueve con un solo personaje");
        pc.colocarJugador(Jugador.Tipo.AGUA, 270f, 30f);
        for (int i = 0; i < 60; i++) pc.actualizar(1f / 60f, derecha, derecha);
        comprobar(pc.cajas[0].rect.x > xConUno + 10f,
            "la caja pesada avanza cuando empujan los dos");

        Nivel conCajaLigera = nivelDePrueba();
        conCajaLigera.agregar(new Nivel.Caja(300f, 30f, false));
        Partida pl = new Partida(conCajaLigera, 0, false);
        pl.colocarJugador(Jugador.Tipo.FUEGO, 270f, 30f);
        for (int i = 0; i < 60; i++) pl.actualizar(1f / 60f, derecha, quieto);
        comprobar(pl.cajas[0].rect.x > 310f, "la caja ligera se mueve con un personaje");
    }

    /** Recorre los mecanismos reales de los ocho niveles y completa cada uno. */
    private static void probarMecanismosIntegrados() {
        seccion("Mecanismos de los niveles");
        Partida.Entrada quieto = new Partida.Entrada();
        for (int indice = 0; indice < Configuracion.TOTAL_NIVELES; indice++) {
            Nivel n = NivelesIntegrados.crear(indice);
            Partida p = new Partida(n, indice, true);
            boolean ok = true;

            boolean[] botonesVistos = new boolean[n.botones.length];
            for (int i = 0; i < n.botones.length; i++) {
                if (botonesVistos[i]) continue;
                int id = n.botones[i].id;
                reposicionarEnOrigenes(p, n);
                int asignados = 0;
                for (int j = i; j < n.botones.length; j++) {
                    Nivel.Boton b = n.botones[j];
                    if (botonesVistos[j] || b.id != id) continue;
                    botonesVistos[j] = true;
                    p.colocarJugador(tipoParaControl(b.elemento, asignados), b.rect.x, b.rect.y);
                    asignados++;
                }
                avanzar(p, quieto, 3);
                for (int j = i; j < n.botones.length; j++) {
                    if (n.botones[j].id == id && !p.botonesActivos[j]) ok = false;
                }
                if (!puertasDeIdAbiertas(p, id)) ok = false;
            }

            int[] ordenPalancas = new int[n.palancas.length];
            for (int i = 0; i < ordenPalancas.length; i++) ordenPalancas[i] = i;
            for (int i = 0; i < ordenPalancas.length; i++) {
                for (int j = i + 1; j < ordenPalancas.length; j++) {
                    if (n.palancas[ordenPalancas[j]].orden < n.palancas[ordenPalancas[i]].orden) {
                        int tmp = ordenPalancas[i];
                        ordenPalancas[i] = ordenPalancas[j];
                        ordenPalancas[j] = tmp;
                    }
                }
            }
            for (int k = 0; k < ordenPalancas.length; k++) {
                int i = ordenPalancas[k];
                reposicionarEnOrigenes(p, n);
                avanzar(p, quieto, 1);
                Nivel.Palanca palanca = n.palancas[i];
                p.colocarJugador(tipoParaControl(palanca.elemento, 0),
                    palanca.rect.x, palanca.rect.y);
                avanzar(p, quieto, 3);
                if (!p.palancasActivas[i]) ok = false;
            }
            for (Nivel.Puerta puerta : n.puertas) {
                if (palancasDeId(n, puerta.id) > 0 && !puertasDeIdAbiertas(p, puerta.id)) {
                    ok = false;
                }
            }

            p.colocarJugador(Jugador.Tipo.FUEGO, n.salidaFuego.x, n.salidaFuego.y);
            p.colocarJugador(Jugador.Tipo.AGUA, n.salidaAgua.x, n.salidaAgua.y);
            avanzar(p, quieto, 120);
            if (!p.completado) ok = false;

            comprobar(ok, "nivel " + (indice + 1)
                + " activa sus puertas y completa con ambos personajes");
        }
    }

    private static void reposicionarEnOrigenes(Partida p, Nivel n) {
        p.colocarJugador(Jugador.Tipo.FUEGO, n.origenFuegoX, n.origenFuegoY);
        p.colocarJugador(Jugador.Tipo.AGUA, n.origenAguaX, n.origenAguaY);
    }

    private static void avanzar(Partida p, Partida.Entrada quieto, int fotogramas) {
        for (int i = 0; i < fotogramas; i++) p.actualizar(1f / 60f, quieto, quieto);
    }

    private static Jugador.Tipo tipoParaControl(String elemento, int asignados) {
        if ("AGUA".equalsIgnoreCase(elemento)) return Jugador.Tipo.AGUA;
        if ("FUEGO".equalsIgnoreCase(elemento)) return Jugador.Tipo.FUEGO;
        return asignados == 0 ? Jugador.Tipo.FUEGO : Jugador.Tipo.AGUA;
    }

    private static boolean puertasDeIdAbiertas(Partida p, int id) {
        boolean hay = false;
        for (int i = 0; i < p.nivel.puertas.length; i++) {
            if (p.nivel.puertas[i].id != id) continue;
            hay = true;
            if (!p.puertasAbiertas[i]) return false;
        }
        return hay;
    }

    private static int palancasDeId(Nivel n, int id) {
        int total = 0;
        for (Nivel.Palanca p : n.palancas) if (p.id == id) total++;
        return total;
    }

    // =========================================================
    //                  PUNTUACION Y PROGRESO
    // =========================================================

    private static void probarPuntuacionYProgreso() {
        seccion("Puntuacion y progreso");
        Puntuacion puntos = new Puntuacion(2);
        puntos.sumarGema(Jugador.Tipo.FUEGO);
        puntos.sumarMecanismo(Jugador.Tipo.AGUA);
        puntos.sumarMuerte(Jugador.Tipo.FUEGO);
        Puntuacion.ResultadoNivel r1 = puntos.terminar(20f);
        int fuego = r1.puntosFuego;
        int agua = r1.puntosAgua;
        Puntuacion.ResultadoNivel r2 = puntos.terminar(20f);
        comprobar(r1.puntosFuego == fuego && r1.puntosAgua == agua,
            "terminar dos veces no duplica puntos");
        comprobar(fuego >= 300 + Configuracion.BONO_TIEMPO_MAXIMO,
            "el nivel completado suma bono de tiempo");
        comprobar(fuego > agua, "la gema y la muerte quedan en Fuego");
        comprobar(agua >= 300 + Configuracion.BONO_TIEMPO_MAXIMO + Configuracion.PUNTOS_MECANISMO,
            "el mecanismo queda en Agua");

        puntos.reiniciar(2);
        puntos.sumarGema(Jugador.Tipo.FUEGO);
        Puntuacion.ResultadoNivel r3 = puntos.terminar(20f);
        comprobar(r3.puntosFuego == Configuracion.PUNTOS_COLECCIONABLE
                + Configuracion.PUNTOS_COMPLETAR_NIVEL + Configuracion.BONO_TIEMPO_MAXIMO,
            "reiniciar la puntuacion permite volver a completar el nivel");

        Progreso progreso = new Progreso();
        Puntuacion.ResultadoNivel bajo = new Puntuacion.ResultadoNivel();
        bajo.completado = true; bajo.puntosFuego = 100; bajo.puntosAgua = 100;
        Puntuacion.ResultadoNivel alto = new Puntuacion.ResultadoNivel();
        alto.completado = true; alto.puntosFuego = 500; alto.puntosAgua = 400;
        progreso.registrar(0, bajo);
        progreso.registrar(0, alto);
        progreso.registrar(0, bajo);
        comprobar(progreso.niveles[0] == alto, "el progreso conserva el mejor intento");
        comprobar(progreso.puntosTotales(Jugador.Tipo.FUEGO) == 500,
            "repetir un nivel no suma puntos otra vez");
        comprobar(progreso.ganadorFinal() == 1, "el ganador final se calcula por totales");
        comprobar(progreso.estaCompletado(0), "el nivel queda completado");
        comprobar(progreso.siguienteNivel() == 1, "el siguiente nivel es el 2");
    }

    // =========================================================
    //                  MEDIDAS FIJAS DEL EDITOR
    // =========================================================

    private static void probarMedidasFijasDelEditor() {
        seccion("Medidas fijas del editor");
        Nivel n = new Nivel();
        n.nombre = "Medidas";
        n.ancho = 960f;
        n.alto = 540f;

        Nivel.Plataforma borde = new Nivel.Plataforma(0f, 0f, 960f, 30f, "PIEDRA");
        borde.limite = true;
        n.agregar(borde);
        Nivel.Plataforma plataforma = new Nivel.Plataforma(100f, 100f, 300f, 90f, "PIEDRA");
        n.agregar(plataforma);
        Nivel.Plataforma muro = new Nivel.Plataforma(200f, 100f, 200f, 200f, "MURO");
        n.agregar(muro);
        Nivel.Peligro peligro = new Nivel.Peligro(300f, 200f, 260f, 40f, true);
        n.agregar(peligro);
        Nivel.Gema gema = new Nivel.Gema(400f, 300f);
        gema.rect.setSize(50f, 50f);
        n.agregar(gema);
        Nivel.Boton boton = new Nivel.Boton(400f, 30f, 1, "AMBOS");
        boton.rect.setSize(90f, 30f);
        n.agregar(boton);
        Nivel.Palanca palanca = new Nivel.Palanca(500f, 30f, 2, "AMBOS");
        palanca.rect.setSize(60f, 90f);
        n.agregar(palanca);
        Nivel.Puerta puerta = new Nivel.Puerta(600f, 30f, 80f, 400f, 3, "ALGUNA", 0f, false);
        n.agregar(puerta);
        Nivel.Puerta puente = new Nivel.Puerta(700f, 100f, 90f, 50f, 4, "ALGUNA", 0f, true);
        n.agregar(puente);
        Nivel.PlataformaMovil movil = new Nivel.PlataformaMovil(100f, 400f, 200f, 50f,
            300f, 400f, 80f, 0);
        n.agregar(movil);
        Nivel.Caja caja = new Nivel.Caja(500f, 30f, true);
        caja.rect.setSize(80f, 80f);
        n.agregar(caja);

        n.normalizarEditor();

        comprobar(misma(plataforma.rect.width, Configuracion.ANCHO_PLATAFORMA_EDITOR)
                && misma(plataforma.rect.height, Configuracion.ALTO_PLATAFORMA_EDITOR),
            "la plataforma usa la medida fija");
        comprobar(misma(muro.rect.width, Configuracion.ANCHO_MURO_EDITOR)
                && misma(muro.rect.height, Configuracion.ALTO_MURO_EDITOR),
            "el muro usa la medida fija");
        comprobar(misma(peligro.rect.width, Configuracion.ANCHO_PELIGRO_EDITOR),
            "el peligro usa la medida fija");
        comprobar(misma(gema.rect.width, Configuracion.ANCHO_GEMA_EDITOR),
            "la gema usa la medida fija");
        comprobar(misma(boton.rect.width, Configuracion.ANCHO_BOTON_EDITOR),
            "la placa usa la medida fija");
        comprobar(misma(palanca.rect.height, Configuracion.ALTO_PALANCA_EDITOR),
            "la palanca usa la medida fija");
        comprobar(misma(puerta.rect.width, Configuracion.ANCHO_PUERTA_EDITOR)
                && misma(puerta.rect.height, Configuracion.ALTO_PUERTA_EDITOR),
            "la puerta usa la medida fija");
        comprobar(misma(puente.rect.width, Configuracion.ANCHO_PUENTE_EDITOR)
                && misma(puente.rect.height, Configuracion.ALTO_PUENTE_EDITOR),
            "el puente usa la medida fija");
        comprobar(misma(movil.rect.width, Configuracion.ANCHO_MOVIL_EDITOR),
            "la plataforma movil usa la medida fija");
        comprobar(misma(caja.rect.width, Configuracion.ANCHO_CAJA_EDITOR),
            "la caja usa la medida fija");
        comprobar(misma(borde.rect.width, 960f) && misma(borde.rect.height, 30f),
            "el borde del escenario conserva su medida");
    }

    private static boolean misma(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    // =========================================================
    //                       PERSISTENCIA
    // =========================================================

    private static void probarPersistenciaNiveles() {
        seccion("Persistencia de niveles");
        FileHandle temp = null;
        try {
            temp = new FileHandle(Files.createTempDirectory("elemental-escape-niveles").toFile());
            GestorNiveles gestor = new GestorNiveles(temp);
            Nivel original = NivelesIntegrados.crear(0);
            original.nombre = "Prueba JSON";
            gestor.guardar(original, false);
            comprobar(gestor.existeNombre("Prueba JSON"), "el nivel aparece en la lista");

            Nivel cargado = gestor.cargar(gestor.archivoDeNivel(original));
            comprobar("Prueba JSON".equals(cargado.nombre), "el nombre sobrevive al JSON");
            comprobar(cargado.plataformas.length == original.plataformas.length,
                "las plataformas sobreviven al JSON");
            comprobar(Math.abs(cargado.ancho - original.ancho) < 0.01f, "el ancho sobrevive al JSON");

            original.pista = "Pista actualizada";
            gestor.guardar(original, false);
            comprobar("Pista actualizada".equals(gestor.cargar(gestor.archivoDeNivel(original)).pista),
                "guardar de nuevo el mismo nivel actualiza el archivo");
            gestor.guardar(original, true);
            comprobar("Prueba JSON".equals(gestor.cargar(gestor.archivoDeNivel(original)).nombre),
                "sobrescribir explicito actualiza el archivo");

            String archivoOriginal = gestor.archivoDeNivel(original);
            original.nombre = "Prueba JSON renombrada";
            String mismoArchivo = gestor.guardarEn(original, archivoOriginal);
            comprobar(archivoOriginal.equals(mismoArchivo),
                "renombrar conserva el archivo actual");
            comprobar(archivoOriginal.equals(gestor.archivoConNombre("Prueba JSON renombrada")),
                "el nombre nuevo se lee desde el mismo archivo");

            Nivel copia = gestor.duplicar(gestor.archivoDeNivel(original), "Prueba copia");
            comprobar(gestor.existeNombre("Prueba copia"), "duplicar crea otro archivo");
            comprobar(!copia.nombre.equals(original.nombre), "la copia tiene otro nombre");

            boolean rechazoNombre = false;
            try {
                gestor.duplicarNivel(original, "Prueba copia");
            } catch (IOException e) {
                rechazoNombre = true;
            }
            comprobar(rechazoNombre, "duplicar rechaza un nombre que ya existe");

            Nivel borrador = NivelesIntegrados.crear(1);
            borrador.nombre = "Borrador sin guardar";
            Nivel copiaBorrador = gestor.duplicarNivel(borrador, "Copia de borrador");
            comprobar(gestor.existeNombre("Copia de borrador"),
                "duplicar tambien funciona antes de guardar");

            gestor.eliminar(gestor.archivoDeNivel(copia));
            comprobar(!gestor.existeNombre("Prueba copia"), "eliminar borra el archivo");
            gestor.eliminar(gestor.archivoDeNivel(copiaBorrador));
        } catch (Exception e) {
            fallo("persistencia de niveles: " + e);
        } finally {
            if (temp != null && temp.exists()) temp.deleteDirectory();
        }
    }

    private static void probarPersistenciaProgreso() {
        seccion("Persistencia de progreso");
        FileHandle temp = null;
        try {
            temp = new FileHandle(Files.createTempDirectory("elemental-escape-progreso").toFile());
            GestorProgreso gestor = new GestorProgreso(temp);
            Progreso progreso = new Progreso();
            Puntuacion.ResultadoNivel r = new Puntuacion.ResultadoNivel();
            r.completado = true; r.puntosFuego = 321; r.puntosAgua = 123;
            r.tiempo = 42f; r.todasGemas = true;
            progreso.registrar(3, r);
            Ajustes ajustes = new Ajustes();
            ajustes.volumen = 0.4f;
            ajustes.sonido = false;
            gestor.guardarProgreso(progreso);
            gestor.guardarAjustes(ajustes);

            Progreso leido = gestor.cargarProgreso();
            comprobar(leido.estaCompletado(3), "el progreso completado se guarda");
            comprobar(leido.niveles[3].puntosFuego == 321, "los puntos de Fuego se guardan");
            comprobar(leido.niveles[3].puntosAgua == 123, "los puntos de Agua se guardan");
            Ajustes ajustesLeidos = gestor.cargarAjustes();
            comprobar(!ajustesLeidos.sonido, "el ajuste de sonido se guarda");
            comprobar(Math.abs(ajustesLeidos.volumen - 0.4f) < 0.001f, "el volumen se guarda");

            Progreso viejo = new Progreso();
            viejo.niveles = new Puntuacion.ResultadoNivel[10];
            for (int i = 0; i < viejo.niveles.length; i++) {
                viejo.niveles[i] = new Puntuacion.ResultadoNivel();
                viejo.niveles[i].completado = true;
                viejo.niveles[i].puntosFuego = 100 + i;
            }
            viejo.normalizar();
            comprobar(viejo.niveles.length == Configuracion.TOTAL_NIVELES,
                "el progreso de diez niveles migra a ocho");
            comprobar(viejo.niveles[7].completado && viejo.niveles[7].puntosFuego == 107,
                "la migracion conserva los ocho primeros resultados");

            gestor.borrarProgreso();
            comprobar(!gestor.cargarProgreso().estaCompletado(3), "borrar progreso lo deja vacio");
        } catch (Exception e) {
            fallo("persistencia de progreso: " + e);
        } finally {
            if (temp != null && temp.exists()) temp.deleteDirectory();
        }
    }

    // =========================================================
    //                         RED
    // =========================================================

    private static void probarRedLocalhost() {
        seccion("Red en localhost");
        GestorRed host = new GestorRed();
        GestorRed cliente = new GestorRed();
        try {
            host.iniciarServidor("ABC234");
            cliente.iniciarCliente("127.0.0.1", "ABC234");
            comprobar(esperar(6000, new Condicion() {
                @Override public boolean seCumple() { return host.conectado && cliente.conectado; }
            }), "anfitrion e invitado conectan (codigo correcto)");

            Nivel n = nivelDePrueba();
            n.nombre = "Nivel de red";
            Partida partida = new Partida(n, 0, true);
            host.enviarNivel(n, 0, true);
            host.enviarEstado(partida, new Progreso(), new String[]{"prueba"});

            comprobar(esperar(4000, new Condicion() {
                @Override public boolean seCumple() { return cliente.getVersionNivel() > 0; }
            }), "el invitado recibe el nivel");
            comprobar(cliente.getNivelRemoto() != null
                && "Nivel de red".equals(cliente.getNivelRemoto().nombre), "el nivel recibido es correcto");

            comprobar(esperar(4000, new Condicion() {
                @Override public boolean seCumple() { return cliente.getUltimaInstantanea() != null; }
            }), "el invitado recibe la instantanea");
            comprobar(cliente.getUltimaInstantanea() != null
                && cliente.getUltimaInstantanea().nivel == 0, "la instantanea tiene el nivel correcto");

            Partida.Entrada entrada = new Partida.Entrada();
            entrada.derecha = true;
            entrada.saltoPresionado = true;
            cliente.enviarEntrada(entrada);
            comprobar(esperar(3000, new Condicion() {
                @Override public boolean seCumple() { return host.consumirEntradaRemota().derecha; }
            }), "el anfitrion recibe la entrada del invitado");

            cliente.enviarComando("pausa");
            comprobar(esperar(3000, new Condicion() {
                @Override public boolean seCumple() { return "pausa".equals(host.consumirComando()); }
            }), "el anfitrion recibe comandos del invitado");

            host.enviarComando("reanudar");
            comprobar(esperar(3000, new Condicion() {
                @Override public boolean seCumple() { return "reanudar".equals(cliente.consumirComando()); }
            }), "el invitado recibe comandos del anfitrion");

            // Reconexion: se cierra el primer cliente y entra otro con la misma sala.
            cliente.cerrar();
            comprobar(esperar(4000, new Condicion() {
                @Override public boolean seCumple() { return !host.conectado; }
            }), "el anfitrion detecta la desconexion");

            GestorRed cliente2 = new GestorRed();
            try {
                cliente2.iniciarCliente("127.0.0.1", "ABC234");
                comprobar(esperar(6000, new Condicion() {
                    @Override public boolean seCumple() { return host.conectado && cliente2.conectado; }
                }), "el anfitrion acepta una reconexion");
                host.enviarEstado(partida, new Progreso(), null);
                comprobar(esperar(3000, new Condicion() {
                    @Override public boolean seCumple() { return cliente2.getUltimaInstantanea() != null; }
                }), "el cliente reconectado recibe estado");
            } finally {
                cliente2.cerrar();
            }
        } catch (Exception e) {
            fallo("red localhost: " + e);
        } finally {
            host.cerrar();
            cliente.cerrar();
        }
    }

    // =========================================================
    //                       UTILIDADES
    // =========================================================

    private static Nivel nivelDePrueba() {
        Nivel n = new Nivel();
        n.nombre = "Nivel de prueba";
        n.ancho = 960f;
        n.alto = 540f;
        n.origenFuegoX = 60f;
        n.origenFuegoY = 30f;
        n.origenAguaX = 100f;
        n.origenAguaY = 30f;
        n.salidaFuego.set(800f, 30f, 60f, 80f);
        n.salidaAgua.set(870f, 30f, 60f, 80f);
        n.agregar(new Nivel.Plataforma(0f, 0f, 960f, 30f, "PIEDRA"));
        n.agregar(new Nivel.Plataforma(0f, 0f, 18f, 540f, "MURO"));
        n.agregar(new Nivel.Plataforma(942f, 0f, 18f, 540f, "MURO"));
        return n;
    }

    private static boolean esperar(int milisegundos, Condicion condicion) {
        long fin = System.currentTimeMillis() + milisegundos;
        while (System.currentTimeMillis() < fin) {
            if (condicion.seCumple()) return true;
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condicion.seCumple();
    }

    private interface Condicion { boolean seCumple(); }

    private static void seccion(String nombre) {
        System.out.println();
        System.out.println("-- " + nombre + " --");
    }

    private static void comprobar(boolean condicion, String descripcion) {
        pruebas++;
        if (condicion) {
            System.out.println("[OK] " + descripcion);
        } else {
            fallo(descripcion);
        }
    }

    private static void fallo(String descripcion) {
        fallos++;
        System.out.println("[FALLO] " + descripcion);
    }
}
