package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Toda la logica de una partida: fisica, mecanismos, puntaje y final del nivel.
 * No depende de la pantalla ni de la red, asi que se puede probar sin abrir una ventana.
 *
 * El anfitrion del modo online simula esta clase y envia Instantaneas.
 * El invitado solo predice su propio movimiento y recibe el resto del anfitrion.
 */
public class Partida {
    public final Nivel nivel;
    public final int indiceNivel;
    public final boolean campana;

    public final Jugador fuego;
    public final Jugador agua;
    public final Puntuacion puntuacion;
    public final Particulas particulas = new Particulas();

    public int vidasFuego = Configuracion.VIDAS_INICIALES;
    public int vidasAgua = Configuracion.VIDAS_INICIALES;

    public boolean[] gemasTomadas;
    public boolean[] botonesActivos;
    public boolean[] palancasActivas;
    public boolean[] puertasAbiertas;
    public float[] puertasTiempo;

    public final CajaEstado[] cajas;
    public final MovilEstado[] moviles;

    private final boolean[] botonPuntuado;
    private final boolean[] palancaPuntuada;
    private final boolean[] palancaOcupada;

    public float tiempo = 0f;
    public float tiempoSalida = 0f;
    public boolean completado = false;
    public boolean derrota = false;
    public boolean pausado = false;
    public float sacudida = 0f;
    public String mensaje = "";
    public float mensajeTiempo = 0f;
    public Puntuacion.ResultadoNivel resultado;
    public boolean resultadoEnviado = false;

    private final List<Rectangle> solidosJugador = new ArrayList<>();
    private final List<Rectangle> solidosCaja = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private float acumulador = 0f;
    private boolean saltoFuegoPendiente = false;
    private boolean saltoAguaPendiente = false;

    /** Entrada de un jugador. Las pantallas la llenan desde el teclado o la red. */
    public static class Entrada {
        public boolean izquierda;
        public boolean derecha;
        public boolean saltoMantenido;
        public boolean saltoPresionado;

        public Entrada() { }

        public void limpiar() {
            izquierda = false;
            derecha = false;
            saltoMantenido = false;
            saltoPresionado = false;
        }
    }

    public static class CajaEstado {
        public final Nivel.Caja datos;
        public final Rectangle rect = new Rectangle();
        public float velX = 0f, velY = 0f;
        public boolean enSuelo = false;
        public final float origenX, origenY;

        public CajaEstado(Nivel.Caja datos) {
            this.datos = datos;
            this.rect.set(datos.rect);
            this.origenX = datos.rect.x;
            this.origenY = datos.rect.y;
        }

        public void reiniciar() {
            rect.set(origenX, origenY, datos.rect.width, datos.rect.height);
            velX = 0f;
            velY = 0f;
            enSuelo = false;
        }
    }

    public static class MovilEstado {
        public final Nivel.PlataformaMovil datos;
        public final Rectangle rect = new Rectangle();
        public float recorrido = 0f;
        public int direccion = 1;
        public final float longitud;

        public MovilEstado(Nivel.PlataformaMovil datos) {
            this.datos = datos;
            this.rect.set(datos.rect);
            float dx = datos.finX - datos.rect.x;
            float dy = datos.finY - datos.rect.y;
            this.longitud = (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    public Partida(Nivel nivel, int indiceNivel, boolean campana) {
        this.nivel = nivel;
        this.nivel.normalizar();
        this.indiceNivel = indiceNivel;
        this.campana = campana;
        this.fuego = new Jugador(Jugador.Tipo.FUEGO, nivel.origenFuegoX, nivel.origenFuegoY);
        this.agua = new Jugador(Jugador.Tipo.AGUA, nivel.origenAguaX, nivel.origenAguaY);
        this.fuego.anchoMundoActual = nivel.ancho;
        this.agua.anchoMundoActual = nivel.ancho;
        this.puntuacion = new Puntuacion(nivel.gemas.length);

        gemasTomadas = new boolean[nivel.gemas.length];
        botonesActivos = new boolean[nivel.botones.length];
        palancasActivas = new boolean[nivel.palancas.length];
        puertasAbiertas = new boolean[nivel.puertas.length];
        puertasTiempo = new float[nivel.puertas.length];
        botonPuntuado = new boolean[nivel.botones.length];
        palancaPuntuada = new boolean[nivel.palancas.length];
        palancaOcupada = new boolean[nivel.palancas.length];

        cajas = new CajaEstado[nivel.cajas.length];
        for (int i = 0; i < cajas.length; i++) cajas[i] = new CajaEstado(nivel.cajas[i]);
        moviles = new MovilEstado[nivel.moviles.length];
        for (int i = 0; i < moviles.length; i++) moviles[i] = new MovilEstado(nivel.moviles[i]);

        reiniciar();
    }

    /** Vuelve todo al estado inicial del nivel (sin tocar el progreso guardado). */
    public void reiniciar() {
        fuego.reaparecer();
        agua.reaparecer();
        fuego.vivo = true;
        agua.vivo = true;
        fuego.anchoMundoActual = nivel.ancho;
        agua.anchoMundoActual = nivel.ancho;
        vidasFuego = Configuracion.VIDAS_INICIALES;
        vidasAgua = Configuracion.VIDAS_INICIALES;
        for (int i = 0; i < gemasTomadas.length; i++) gemasTomadas[i] = false;
        for (int i = 0; i < botonesActivos.length; i++) {
            botonesActivos[i] = false;
            botonPuntuado[i] = false;
        }
        for (int i = 0; i < palancasActivas.length; i++) {
            palancasActivas[i] = false;
            palancaPuntuada[i] = false;
            palancaOcupada[i] = false;
        }
        for (int i = 0; i < puertasAbiertas.length; i++) {
            puertasAbiertas[i] = false;
            puertasTiempo[i] = 0f;
        }
        for (CajaEstado c : cajas) c.reiniciar();
        for (MovilEstado m : moviles) {
            m.recorrido = 0f;
            m.direccion = 1;
            m.rect.set(m.datos.rect);
        }
        reiniciarPuntuacion();
        tiempo = 0f;
        tiempoSalida = 0f;
        completado = false;
        derrota = false;
        pausado = false;
        resultado = null;
        resultadoEnviado = false;
        mensaje = "";
        mensajeTiempo = 0f;
        sacudida = 0f;
        acumulador = 0f;
        saltoFuegoPendiente = false;
        saltoAguaPendiente = false;
        particulas.limpiar();
        actualizarMecanismos(0f, false);
    }

    private void reiniciarPuntuacion() {
        puntuacion.reiniciar(nivel.gemas.length);
    }

    // =========================================================
    //                       ACTUALIZACION
    // =========================================================

    public void actualizar(float dt, Entrada entradaFuego, Entrada entradaAgua) {
        eventos.clear();
        if (mensajeTiempo > 0f) {
            mensajeTiempo -= dt;
            if (mensajeTiempo <= 0f) mensaje = "";
        }
        if (sacudida > 0f) sacudida = Math.max(0f, sacudida - dt * 18f);

        if (pausado) return;
        if (completado || derrota) {
            particulas.actualizar(dt);
            return;
        }

        if (entradaFuego != null && entradaFuego.saltoPresionado) saltoFuegoPendiente = true;
        if (entradaAgua != null && entradaAgua.saltoPresionado) saltoAguaPendiente = true;

        acumulador += Math.min(dt, 0.10f);
        int pasos = 0;
        while (acumulador >= Configuracion.PASO_FISICA && pasos < Configuracion.MAX_PASOS_POR_FOTOGRAMA) {
            paso(Configuracion.PASO_FISICA, entradaFuego, entradaAgua,
                saltoFuegoPendiente, saltoAguaPendiente);
            saltoFuegoPendiente = false;
            saltoAguaPendiente = false;
            acumulador -= Configuracion.PASO_FISICA;
            pasos++;
        }
        if (pasos >= Configuracion.MAX_PASOS_POR_FOTOGRAMA) acumulador = 0f;
    }

    private void paso(float dt, Entrada entradaFuego, Entrada entradaAgua,
                      boolean saltoFuego, boolean saltoAgua) {
        tiempo += dt;

        actualizarMoviles(dt);
        actualizarCajas(dt);
        empujarCajas(dt, entradaFuego, entradaAgua);
        construirSolidos();

        actualizarJugador(fuego, entradaFuego, saltoFuego, dt);
        actualizarJugador(agua, entradaAgua, saltoAgua, dt);

        revisarPeligros(dt);
        actualizarMecanismos(dt, true);
        recogerGemas();
        revisarSalidas(dt);
        actualizarMuertes(dt);
        particulas.actualizar(dt);
    }

    private void actualizarJugador(Jugador p, Entrada e, boolean salto, float dt) {
        boolean vivoAntes = p.vivo;
        boolean izquierda = e != null && e.izquierda;
        boolean derecha = e != null && e.derecha;
        boolean mantenido = e != null && e.saltoMantenido;
        p.actualizar(dt, izquierda, derecha, mantenido, salto, solidosJugador);
        if (vivoAntes && p.vivo && p.estabaEnSuelo && !p.enSuelo && p.velY > 300f) {
            evento("salto");
        }
        if (vivoAntes && p.vivo && !p.estabaEnSuelo && p.enSuelo) {
            evento("aterrizar");
            particulas.polvo(p.x + p.ancho / 2f, p.y, 6);
        }
        if (vivoAntes && !p.vivo && !p.tiempoReaparicionYaContado()) {
            // El jugador murio por caer fuera del mapa.
            contarMuerte(p, "cayo al vacio");
        }
    }

    // =========================================================
    //                  PLATAFORMAS Y CAJAS
    // =========================================================

    private void actualizarMoviles(float dt) {
        for (MovilEstado m : moviles) {
            if (m.longitud <= 0.01f) continue;
            if (m.datos.id > 0 && !controlActivo(m.datos.id)) {
                continue; // pausada hasta que se active su mecanismo
            }
            float viejoX = m.rect.x;
            float viejoY = m.rect.y;

            m.recorrido += m.datos.velocidad * m.direccion * dt;
            if (m.recorrido > m.longitud) {
                m.recorrido = m.longitud;
                m.direccion = -1;
            } else if (m.recorrido < 0f) {
                m.recorrido = 0f;
                m.direccion = 1;
            }
            float dxTotal = m.datos.finX - m.datos.rect.x;
            float dyTotal = m.datos.finY - m.datos.rect.y;
            float proporcion = m.recorrido / m.longitud;
            m.rect.x = m.datos.rect.x + dxTotal * proporcion;
            m.rect.y = m.datos.rect.y + dyTotal * proporcion;

            float dx = m.rect.x - viejoX;
            float dy = m.rect.y - viejoY;
            if (dx == 0f && dy == 0f) continue;

            // Arrastrar a quien este parado sobre la plataforma.
            for (Jugador p : new Jugador[]{fuego, agua}) {
                if (!p.vivo || !p.enSuelo) continue;
                if (Math.abs(p.y - (viejoY + m.rect.height)) > 7f) continue;
                if (p.x + p.ancho < viejoX || p.x > viejoX + m.rect.width) continue;
                p.x += dx;
                p.y += dy;
            }
        }
    }

    private void actualizarCajas(float dt) {
        construirSolidosCaja();
        for (CajaEstado c : cajas) {
            c.velY -= Configuracion.GRAVEDAD * dt;
            if (c.velY < -Configuracion.VELOCIDAD_CAIDA_MAXIMA) c.velY = -Configuracion.VELOCIDAD_CAIDA_MAXIMA;

            c.rect.x += c.velX * dt;
            c.velX *= 0.82f;
            for (Rectangle s : solidosCaja) {
                if (!c.rect.overlaps(s)) continue;
                if (c.velX > 0f) c.rect.x = s.x - c.rect.width;
                else if (c.velX < 0f) c.rect.x = s.x + s.width;
                c.velX = 0f;
            }

            float yAntes = c.rect.y;
            c.rect.y += c.velY * dt;
            c.enSuelo = false;
            for (Rectangle s : solidosCaja) {
                if (!c.rect.overlaps(s)) continue;
                if (c.velY <= 0f && yAntes >= s.y + s.height - 1f) {
                    c.rect.y = s.y + s.height;
                    c.velY = 0f;
                    c.enSuelo = true;
                } else if (c.velY > 0f) {
                    c.rect.y = s.y - c.rect.height;
                    c.velY = 0f;
                }
            }

            if (c.rect.y < -100f) {
                c.reiniciar();
                mostrarMensaje("La caja volvio a su lugar.");
            }
        }
    }

    /** Mueve una caja cuando uno o los dos jugadores empujan en la misma direccion. */
    private void empujarCajas(float dt, Entrada entradaFuego, Entrada entradaAgua) {
        for (CajaEstado c : cajas) {
            int derecha = 0;
            int izquierda = 0;
            if (puedeEmpujar(fuego, c.rect, entradaFuego)) {
                if (fuego.x + fuego.ancho / 2f < c.rect.x + c.rect.width / 2f) derecha++;
                else izquierda++;
            }
            if (puedeEmpujar(agua, c.rect, entradaAgua)) {
                if (agua.x + agua.ancho / 2f < c.rect.x + c.rect.width / 2f) derecha++;
                else izquierda++;
            }
            int necesarios = c.datos.pesada ? 2 : 1;
            float paso = Configuracion.VELOCIDAD_CAJA * dt;
            float dx = 0f;
            if (derecha > 0 && izquierda == 0 && derecha >= necesarios) dx = paso;
            else if (izquierda > 0 && derecha == 0 && izquierda >= necesarios) dx = -paso;
            if (dx == 0f) continue;

            float xAntes = c.rect.x;
            c.rect.x += dx;
            for (Rectangle s : solidosCaja) {
                if (c.rect.overlaps(s)) {
                    c.rect.x = xAntes;
                    break;
                }
            }
        }
    }

    private boolean puedeEmpujar(Jugador p, Rectangle caja, Entrada e) {
        if (p == null || e == null || !p.vivo) return false;
        if (!e.izquierda && !e.derecha) return false;
        if (e.izquierda && e.derecha) return false;
        if (p.y >= caja.y + caja.height - 3f) return false; // esta encima, no al lado
        Rectangle cuerpo = p.cuerpo();
        return cuerpo.x + cuerpo.width >= caja.x - 5f
            && cuerpo.x <= caja.x + caja.width + 5f
            && cuerpo.y < caja.y + caja.height - 3f
            && cuerpo.y + cuerpo.height > caja.y + 3f;
    }

    // =========================================================
    //                      COLISIONES
    // =========================================================

    private void construirSolidos() {
        solidosJugador.clear();
        for (Nivel.Plataforma p : nivel.plataformas) solidosJugador.add(p.rect);
        for (int i = 0; i < nivel.puertas.length; i++) {
            Nivel.Puerta p = nivel.puertas[i];
            boolean activa = puertasAbiertas[i];
            if (p.puente) {
                if (activa) solidosJugador.add(p.rect);
            } else if (!activa) {
                solidosJugador.add(p.rect);
            }
        }
        for (MovilEstado m : moviles) solidosJugador.add(m.rect);
        for (CajaEstado c : cajas) solidosJugador.add(c.rect);
    }

    private void construirSolidosCaja() {
        solidosCaja.clear();
        for (Nivel.Plataforma p : nivel.plataformas) solidosCaja.add(p.rect);
        for (int i = 0; i < nivel.puertas.length; i++) {
            Nivel.Puerta p = nivel.puertas[i];
            boolean activa = puertasAbiertas[i];
            if (p.puente) {
                if (activa) solidosCaja.add(p.rect);
            } else if (!activa) {
                solidosCaja.add(p.rect);
            }
        }
        for (MovilEstado m : moviles) solidosCaja.add(m.rect);
    }

    // =========================================================
    //                     MECANISMOS
    // =========================================================

    private void actualizarMecanismos(float dt, boolean puntuar) {
        // 1. Placas de presion.
        for (int i = 0; i < nivel.botones.length; i++) {
            Nivel.Boton b = nivel.botones[i];
            boolean activo = false;
            Jugador quien = null;
            for (Jugador p : new Jugador[]{fuego, agua}) {
                if (!p.vivo) continue;
                if (!elementoPermitido(b.elemento, p.tipo)) continue;
                if (p.cuerpo().overlaps(b.rect)) {
                    activo = true;
                    quien = p;
                    break;
                }
            }
            if (!activo) {
                for (CajaEstado c : cajas) {
                    if (c.rect.overlaps(b.rect)) { activo = true; break; }
                }
            }
            if (activo && puntuar && !botonPuntuado[i] && quien != null) {
                puntuacion.sumarMecanismo(quien.tipo);
                botonPuntuado[i] = true;
                quien.tiempoInteraccion = 0.35f;
                evento("mecanismo");
            }
            botonesActivos[i] = activo;
        }

        // 2. Palancas de un solo uso y secuencias.
        for (int i = 0; i < nivel.palancas.length; i++) {
            Nivel.Palanca palanca = nivel.palancas[i];
            boolean ocupadaAhora = false;
            Jugador quien = null;
            for (Jugador p : new Jugador[]{fuego, agua}) {
                if (!p.vivo) continue;
                if (!elementoPermitido(palanca.elemento, p.tipo)) continue;
                if (p.cuerpo().overlaps(palanca.rect)) {
                    ocupadaAhora = true;
                    quien = p;
                    break;
                }
            }
            if (ocupadaAhora && !palancaOcupada[i] && !palancasActivas[i]) {
                boolean ordenCorrecto = true;
                if (palanca.orden > 0) {
                    for (int j = 0; j < nivel.palancas.length; j++) {
                        Nivel.Palanca otra = nivel.palancas[j];
                        if (otra == palanca) continue;
                        if (otra.grupo != palanca.grupo || otra.orden <= 0) continue;
                        if (otra.orden < palanca.orden && !palancasActivas[j]) ordenCorrecto = false;
                    }
                }
                if (!ordenCorrecto) {
                    reiniciarSecuencia(palanca.grupo);
                    mostrarMensaje("Secuencia reiniciada: habia que seguir el orden.");
                    evento("error");
                } else {
                    palancasActivas[i] = true;
                    if (quien != null) quien.tiempoInteraccion = 0.35f;
                    if (puntuar && quien != null && !palancaPuntuada[i]) {
                        puntuacion.sumarMecanismo(quien.tipo);
                        palancaPuntuada[i] = true;
                    }
                    evento("palanca");
                }
            }
            palancaOcupada[i] = ocupadaAhora;
        }

        // 3. Puertas y puentes.
        for (int i = 0; i < nivel.puertas.length; i++) {
            Nivel.Puerta puerta = nivel.puertas[i];
            boolean cumple = cumpleCondicion(puerta);
            if (puerta.duracion > 0f) {
                if (cumple) puertasTiempo[i] = puerta.duracion;
                else puertasTiempo[i] = Math.max(0f, puertasTiempo[i] - dt);
            } else {
                puertasTiempo[i] = 0f;
            }
            boolean abierta = cumple || puertasTiempo[i] > 0f;
            if (abierta != puertasAbiertas[i]) {
                puertasAbiertas[i] = abierta;
                evento("puerta");
            }
        }
    }

    private void reiniciarSecuencia(int grupo) {
        for (int i = 0; i < nivel.palancas.length; i++) {
            if (nivel.palancas[i].grupo == grupo) {
                palancasActivas[i] = false;
                // No se devuelve la puntuacion: reintentar la secuencia no debe
                // permitir sumar el mismo mecanismo varias veces.
            }
        }
    }

    private boolean cumpleCondicion(Nivel.Puerta puerta) {
        int total = 0;
        int activos = 0;
        for (int i = 0; i < nivel.botones.length; i++) {
            if (nivel.botones[i].id != puerta.id) continue;
            total++;
            if (botonesActivos[i]) activos++;
        }
        for (int i = 0; i < nivel.palancas.length; i++) {
            if (nivel.palancas[i].id != puerta.id) continue;
            total++;
            if (palancasActivas[i]) activos++;
        }
        if (total == 0) return false;
        if ("TODAS".equalsIgnoreCase(puerta.modo)) return activos == total;
        return activos > 0;
    }

    public boolean controlActivo(int id) {
        for (int i = 0; i < nivel.botones.length; i++) {
            if (nivel.botones[i].id == id && botonesActivos[i]) return true;
        }
        for (int i = 0; i < nivel.palancas.length; i++) {
            if (nivel.palancas[i].id == id && palancasActivas[i]) return true;
        }
        return false;
    }

    private boolean elementoPermitido(String elemento, Jugador.Tipo tipo) {
        if (elemento == null || elemento.equalsIgnoreCase("AMBOS")) return true;
        return "FUEGO".equalsIgnoreCase(elemento) && tipo == Jugador.Tipo.FUEGO
            || "AGUA".equalsIgnoreCase(elemento) && tipo == Jugador.Tipo.AGUA;
    }

    // =========================================================
    //                GEMAS, PELIGROS Y SALIDAS
    // =========================================================

    private void recogerGemas() {
        for (int i = 0; i < nivel.gemas.length; i++) {
            if (gemasTomadas[i]) continue;
            Nivel.Gema g = nivel.gemas[i];
            Jugador quien = null;
            if (fuego.vivo && fuego.cuerpo().overlaps(g.rect)) quien = fuego;
            else if (agua.vivo && agua.cuerpo().overlaps(g.rect)) quien = agua;
            if (quien != null) {
                gemasTomadas[i] = true;
                puntuacion.sumarGema(quien.tipo);
                evento("gema");
                particulas.chispas(g.rect.x + 11f, g.rect.y + 11f,
                    g.secreta ? Configuracion.DORADO : Configuracion.VERDE, 16);
            }
        }
    }

    private void revisarPeligros(float dt) {
        for (Jugador p : new Jugador[]{fuego, agua}) {
            if (!p.vivo) continue;
            for (Nivel.Peligro peligro : nivel.peligros) {
                boolean leHaceDano = peligro.lava ? p.tipo == Jugador.Tipo.AGUA : p.tipo == Jugador.Tipo.FUEGO;
                if (leHaceDano && p.cuerpo().overlaps(peligro.rect)) {
                    matar(p, peligro.lava ? "toco la lava" : "toco el agua");
                    break;
                }
            }
        }
    }

    private void revisarSalidas(float dt) {
        if (completado || derrota) return;
        boolean fuegoDentro = fuego.vivo && fuego.cuerpo().overlaps(nivel.salidaFuego);
        boolean aguaDentro = agua.vivo && agua.cuerpo().overlaps(nivel.salidaAgua);
        if (fuegoDentro && aguaDentro) {
            tiempoSalida += dt;
            if (tiempoSalida >= Configuracion.TIEMPO_SALIDA) {
                completarNivel();
            } else {
                mensaje = "Abriendo el portal... " + (int) Math.ceil(Configuracion.TIEMPO_SALIDA - tiempoSalida);
                mensajeTiempo = 0.3f;
            }
        } else {
            tiempoSalida = 0f;
        }
    }

    private void completarNivel() {
        if (completado || derrota) return;
        completado = true;
        resultado = puntuacion.terminar(tiempo);
        tiempoSalida = Configuracion.TIEMPO_SALIDA;
        mostrarMensaje("Nivel completado!");
        evento("victoria");
        sacudida = 4f;
        for (Jugador p : new Jugador[]{fuego, agua}) {
            p.celebrando = true;
            p.derrotado = false;
            particulas.chispas(p.x + p.ancho / 2f, p.y + p.alto / 2f,
                p.tipo == Jugador.Tipo.FUEGO ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA, 24);
        }
    }

    // =========================================================
    //                    MUERTE Y REINICIO
    // =========================================================

    private void matar(Jugador p, String motivo) {
        if (!p.vivo) return;
        p.morir();
        contarMuerte(p, motivo);
    }

    /** Cuenta una muerte que ya ocurrio (por ejemplo, al caer al vacio). */
    private void contarMuerte(Jugador p, String motivo) {
        p.marcarMuerteContada();
        puntuacion.sumarMuerte(p.tipo);
        if (p.tipo == Jugador.Tipo.FUEGO) vidasFuego--;
        else vidasAgua--;
        sacudida = 7f;
        evento("muerte");
        particulas.chispas(p.x + p.ancho / 2f, p.y + p.alto / 2f,
            p.tipo == Jugador.Tipo.FUEGO ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA, 24);
        mostrarMensaje((p.tipo == Jugador.Tipo.FUEGO ? "Fuego " : "Agua ") + motivo + ".");

        if (vidasFuego <= 0 || vidasAgua <= 0) {
            derrota = true;
            fuego.derrotado = true;
            agua.derrotado = true;
            if (vidasFuego <= 0 && vidasAgua <= 0) mostrarMensaje("Los dos se quedaron sin vidas.");
            else if (vidasFuego <= 0) mostrarMensaje("Fuego se quedo sin vidas.");
            else mostrarMensaje("Agua se quedo sin vidas.");
        }
    }

    private void actualizarMuertes(float dt) {
        if (derrota) return;
        if (!fuego.vivo && fuego.tiempoReaparicion <= 0f && vidasFuego > 0) {
            fuego.reaparecer();
            evento("reaparecer");
        }
        if (!agua.vivo && agua.tiempoReaparicion <= 0f && vidasAgua > 0) {
            agua.reaparecer();
            evento("reaparecer");
        }
    }

    // =========================================================
    //                     AYUDAS PUBLICAS
    // =========================================================

    public void mostrarMensaje(String texto) {
        mensaje = texto == null ? "" : texto;
        mensajeTiempo = 2.8f;
    }

    private void evento(String nombre) {
        if (eventos.size() < 40) eventos.add(nombre);
    }

    public List<String> consumirEventos() {
        if (eventos.isEmpty()) return java.util.Collections.emptyList();
        List<String> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    /** Copia sin borrar: la usa la red para no perder los sonidos del anfitrion. */
    public String[] eventosActuales() {
        return eventos.toArray(new String[0]);
    }

    public int estrellasGemas() {
        if (nivel.gemas.length == 0) return 3;
        int recogidas = 0;
        for (boolean tomada : gemasTomadas) if (tomada) recogidas++;
        float porcentaje = recogidas / (float) nivel.gemas.length;
        if (porcentaje >= 1f) return 3;
        if (porcentaje >= 0.5f) return 2;
        if (porcentaje > 0f) return 1;
        return 0;
    }

    public int gemasRecogidas() {
        int total = 0;
        for (boolean tomada : gemasTomadas) if (tomada) total++;
        return total;
    }

    public String textoTiempo() {
        int minutos = (int) (tiempo / 60f);
        int segundos = (int) (tiempo % 60f);
        return (minutos < 10 ? "0" : "") + minutos + ":" + (segundos < 10 ? "0" : "") + segundos;
    }

    // =========================================================
    //                 SINCronIZACION ONLINE
    // =========================================================

    public void llenarInstantanea(Instantanea s, Progreso progreso) {
        s.version = Configuracion.VERSION_PROTOCOLO;
        s.nivel = indiceNivel;
        s.tiempo = tiempo;
        s.tiempoSalida = tiempoSalida;
        s.completado = completado;
        s.derrota = derrota;
        s.pausado = pausado;
        s.vidasFuego = vidasFuego;
        s.vidasAgua = vidasAgua;

        llenarJugador(s, 0, fuego);
        llenarJugador(s, 1, agua);

        s.gemasTomadas = copiar(gemasTomadas);
        s.botonesActivos = copiar(botonesActivos);
        s.palancasActivas = copiar(palancasActivas);
        s.puertasAbiertas = copiar(puertasAbiertas);
        s.puertasTiempo = copiar(puertasTiempo);

        s.cajasX = new float[cajas.length];
        s.cajasY = new float[cajas.length];
        for (int i = 0; i < cajas.length; i++) {
            s.cajasX[i] = cajas[i].rect.x;
            s.cajasY[i] = cajas[i].rect.y;
        }
        s.movilesX = new float[moviles.length];
        s.movilesY = new float[moviles.length];
        for (int i = 0; i < moviles.length; i++) {
            s.movilesX[i] = moviles[i].rect.x;
            s.movilesY[i] = moviles[i].rect.y;
        }

        s.puntosFuego = puntuacion.puntosFuego;
        s.puntosAgua = puntuacion.puntosAgua;
        s.gemasFuego = puntuacion.gemasFuego;
        s.gemasAgua = puntuacion.gemasAgua;
        s.muertesFuego = puntuacion.muertesFuego;
        s.muertesAgua = puntuacion.muertesAgua;
        s.mecanismosFuego = puntuacion.mecanismosFuego;
        s.mecanismosAgua = puntuacion.mecanismosAgua;
        s.bonusFuego = puntuacion.bonusFuego;
        s.bonusAgua = puntuacion.bonusAgua;
        s.todasLasGemas = puntuacion.todasLasGemas;
        s.tiempoPuntuacion = puntuacion.tiempo;
        s.resultado = resultado != null ? resultado : new Puntuacion.ResultadoNivel();
        s.mensaje = mensaje;
        s.eventos = eventosActuales();

        if (progreso != null) {
            s.totalesFuego = progreso.puntosTotales(Jugador.Tipo.FUEGO);
            s.totalesAgua = progreso.puntosTotales(Jugador.Tipo.AGUA);
            s.nivelesCompletados = progreso.nivelesCompletados();
            s.nivelesGanadosFuego = progreso.nivelesGanados(Jugador.Tipo.FUEGO);
            s.nivelesGanadosAgua = progreso.nivelesGanados(Jugador.Tipo.AGUA);
        }
    }

    private void llenarJugador(Instantanea s, int i, Jugador p) {
        s.jugadorX[i] = p.x;
        s.jugadorY[i] = p.y;
        s.jugadorVelX[i] = p.velX;
        s.jugadorVelY[i] = p.velY;
        s.jugadorVivo[i] = p.vivo;
        s.jugadorDerecha[i] = p.mirandoDerecha;
        s.jugadorAnim[i] = p.tiempoAnim;
        s.jugadorAplastado[i] = p.aplastado;
    }

    public void aplicarInstantanea(Instantanea s, Jugador.Tipo jugadorLocal) {
        if (s == null || s.nivel != indiceNivel) return;
        tiempo = s.tiempo;
        tiempoSalida = s.tiempoSalida;
        completado = s.completado;
        derrota = s.derrota;
        pausado = s.pausado;
        vidasFuego = s.vidasFuego;
        vidasAgua = s.vidasAgua;

        aplicarJugador(fuego, 0, s, jugadorLocal);
        aplicarJugador(agua, 1, s, jugadorLocal);

        copiarEn(s.gemasTomadas, gemasTomadas);
        copiarEn(s.botonesActivos, botonesActivos);
        copiarEn(s.palancasActivas, palancasActivas);
        copiarEn(s.puertasAbiertas, puertasAbiertas);
        copiarEn(s.puertasTiempo, puertasTiempo);

        for (int i = 0; i < cajas.length && i < s.cajasX.length; i++) {
            cajas[i].rect.x = s.cajasX[i];
            cajas[i].rect.y = s.cajasY[i];
        }
        for (int i = 0; i < moviles.length && i < s.movilesX.length; i++) {
            moviles[i].rect.x = s.movilesX[i];
            moviles[i].rect.y = s.movilesY[i];
        }

        puntuacion.puntosFuego = s.puntosFuego;
        puntuacion.puntosAgua = s.puntosAgua;
        puntuacion.gemasFuego = s.gemasFuego;
        puntuacion.gemasAgua = s.gemasAgua;
        puntuacion.muertesFuego = s.muertesFuego;
        puntuacion.muertesAgua = s.muertesAgua;
        puntuacion.mecanismosFuego = s.mecanismosFuego;
        puntuacion.mecanismosAgua = s.mecanismosAgua;
        puntuacion.bonusFuego = s.bonusFuego;
        puntuacion.bonusAgua = s.bonusAgua;
        puntuacion.todasLasGemas = s.todasLasGemas;
        puntuacion.tiempo = s.tiempoPuntuacion;
        resultado = s.resultado;
        mensaje = s.mensaje;
        if (mensaje != null && !mensaje.isEmpty()) mensajeTiempo = 1.2f;
    }

    private void aplicarJugador(Jugador p, int i, Instantanea s, Jugador.Tipo local) {
        if (i >= s.jugadorX.length) return;
        float destinoX = s.jugadorX[i];
        float destinoY = s.jugadorY[i];
        boolean suavizar = local != null && p.tipo == local
            && Math.abs(p.x - destinoX) < 80f && Math.abs(p.y - destinoY) < 80f;
        if (suavizar) {
            p.x += (destinoX - p.x) * 0.35f;
            p.y += (destinoY - p.y) * 0.35f;
        } else {
            p.x = destinoX;
            p.y = destinoY;
        }
        p.velX = s.jugadorVelX[i];
        p.velY = s.jugadorVelY[i];
        p.vivo = s.jugadorVivo[i];
        p.mirandoDerecha = s.jugadorDerecha[i];
        p.tiempoAnim = s.jugadorAnim[i];
        p.aplastado = s.jugadorAplastado[i];
        if (p.vivo) p.tiempoReaparicion = 0f;
    }

    /** Prediccion local del invitado: solo mueve su personaje, sin tocar puntos ni mecanismos. */
    public void predecirJugador(Jugador.Tipo tipo, float dt, Entrada entrada) {
        if (completado || derrota || pausado) return;
        construirSolidos();
        Jugador p = tipo == Jugador.Tipo.FUEGO ? fuego : agua;
        if (!p.vivo) return;
        p.actualizar(dt, entrada.izquierda, entrada.derecha, entrada.saltoMantenido,
            entrada.saltoPresionado, solidosJugador);
        p.anchoMundoActual = nivel.ancho;
    }

    private static boolean[] copiar(boolean[] original) {
        boolean[] copia = new boolean[original.length];
        System.arraycopy(original, 0, copia, 0, original.length);
        return copia;
    }

    private static float[] copiar(float[] original) {
        float[] copia = new float[original.length];
        System.arraycopy(original, 0, copia, 0, original.length);
        return copia;
    }

    private static void copiarEn(boolean[] origen, boolean[] destino) {
        if (origen == null) return;
        int total = Math.min(origen.length, destino.length);
        System.arraycopy(origen, 0, destino, 0, total);
    }

    private static void copiarEn(float[] origen, float[] destino) {
        if (origen == null) return;
        int total = Math.min(origen.length, destino.length);
        System.arraycopy(origen, 0, destino, 0, total);
    }

    // Metodos de ayuda para las pruebas y para el editor.
    public void colocarJugador(Jugador.Tipo tipo, float x, float y) {
        Jugador p = tipo == Jugador.Tipo.FUEGO ? fuego : agua;
        p.x = x; p.y = y; p.velX = 0f; p.velY = 0f; p.vivo = true;
    }

    public void colocarCaja(int indice, float x, float y) {
        if (indice < 0 || indice >= cajas.length) return;
        cajas[indice].rect.x = x;
        cajas[indice].rect.y = y;
        cajas[indice].velX = 0f;
        cajas[indice].velY = 0f;
    }
}
