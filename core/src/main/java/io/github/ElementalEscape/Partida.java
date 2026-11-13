package io.github.ElementalEscape;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

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
        this.fuego.altoMundoActual = nivel.alto;
        this.agua.anchoMundoActual = nivel.ancho;
        this.agua.altoMundoActual = nivel.alto;
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

    public void reiniciar() {
        fuego.reaparecer();
        agua.reaparecer();
        fuego.vivo = true;
        agua.vivo = true;
        fuego.anchoMundoActual = nivel.ancho;
        fuego.altoMundoActual = nivel.alto;
        agua.anchoMundoActual = nivel.ancho;
        agua.altoMundoActual = nivel.alto;
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

            contarMuerte(p, "cayo al vacio");
        }
    }

    private void actualizarMoviles(float dt) {
        for (MovilEstado m : moviles) {
            if (m.longitud <= 0.01f) continue;
            if (m.datos.id > 0 && !controlActivo(m.datos.id)) {
                continue;
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

            Rectangle viejoRect = new Rectangle(viejoX, viejoY, m.rect.width, m.rect.height);

            for (Jugador p : new Jugador[]{fuego, agua}) {
                if (!p.vivo) continue;
                boolean montado = p.enSuelo
                    && p.y >= viejoY + m.rect.height - 4f
                    && p.y <= viejoY + m.rect.height + 8f
                    && p.x + p.ancho > viejoX + 0.5f
                    && p.x < viejoX + m.rect.width - 0.5f;
                if (!montado) continue;

                p.x += dx;
                p.y += dy;
                if (p.velY < 0f) p.velY = 0f;
                p.enSuelo = true;

                if (p.x + p.ancho <= m.rect.x + 1f || p.x >= m.rect.x + m.rect.width - 1f) {
                    p.enSuelo = false;
                } else {
                    p.y = m.rect.y + m.rect.height;
                }

                resolverJugadorContraEntorno(p, m, true, dx);
            }

            for (Jugador p : new Jugador[]{fuego, agua}) {
                if (!p.vivo) continue;
                if (!p.cuerpo().overlaps(m.rect)) continue;
                boolean sobre = p.y >= m.rect.y + m.rect.height - 3f
                    && p.x + p.ancho > m.rect.x + 0.5f && p.x < m.rect.x + m.rect.width - 0.5f;
                if (sobre) continue;

                separarPorMovimientoPlataforma(p, m.rect, viejoRect, dx, dy);
                resolverJugadorContraEntorno(p, m, false, dx);
            }
        }
    }

    private void resolverJugadorContraEntorno(Jugador p, MovilEstado movilActual, boolean montado, float dxPlataforma) {
        List<Rectangle> entorno = entornoSolidoParaJugador(movilActual);
        Rectangle plat = movilActual.rect;

        for (int iter = 0; iter < 5; iter++) {
            boolean ajustado = false;
            for (Rectangle s : entorno) {
                if (!p.cuerpo().overlaps(s)) continue;
                ajustado = true;
                float solapeIzq = (p.x + p.ancho) - s.x;
                float solapeDer = (s.x + s.width) - p.x;
                float solapeAbajo = (p.y + p.alto) - s.y;
                float solapeArriba = (s.y + s.height) - p.y;
                if (solapeIzq <= 0f || solapeDer <= 0f || solapeAbajo <= 0f || solapeArriba <= 0f) continue;

                if (montado) {

                    if (solapeAbajo < solapeArriba && solapeAbajo < Math.min(solapeIzq, solapeDer)) {
                        p.y = s.y - p.alto;
                        if (p.velY > 0f) p.velY = 0f;
                    } else {
                        if (solapeIzq <= solapeDer) p.x = s.x - p.ancho;
                        else p.x = s.x + s.width;
                        p.velX = 0f;

                        if (p.x + p.ancho > plat.x + 1f && p.x < plat.x + plat.width - 1f) {
                            p.y = plat.y + plat.height;
                        }
                    }
                } else {
                    float minX = Math.min(solapeIzq, solapeDer);
                    float minY = Math.min(solapeAbajo, solapeArriba);
                    if (minY < minX) {
                        if (solapeArriba <= solapeAbajo) {
                            p.y = s.y + s.height;
                            p.enSuelo = true;
                            if (p.velY < 0f) p.velY = 0f;
                        } else {
                            p.y = s.y - p.alto;
                            if (p.velY > 0f) p.velY = 0f;
                        }
                    } else {
                        if (solapeIzq <= solapeDer) p.x = s.x - p.ancho;
                        else p.x = s.x + s.width;
                        p.velX = 0f;
                    }
                }
            }
            if (!ajustado) break;
        }

        if (montado) {
            boolean apoyado = p.x + p.ancho > plat.x + 1f && p.x < plat.x + plat.width - 1f;
            boolean libreArriba = !solapaAlguno(p, entorno);
            if (apoyado && libreArriba) {
                p.y = plat.y + plat.height;
                p.enSuelo = true;
            } else if (!apoyado) {
                p.enSuelo = false;
            } else {

                p.enSuelo = false;
                float empuje = Math.abs(dxPlataforma) + 2f;
                if (dxPlataforma >= 0f) p.x += empuje; else p.x -= empuje;
                if (p.cuerpo().overlaps(plat)) {

                    p.y = plat.y - p.alto - 0.5f;
                }

                for (int i = 0; i < 2; i++) {
                    for (Rectangle s : entorno) {
                        if (!p.cuerpo().overlaps(s)) continue;
                        float solapeIzq = (p.x + p.ancho) - s.x;
                        float solapeDer = (s.x + s.width) - p.x;
                        if (solapeIzq <= solapeDer) p.x = s.x - p.ancho - 0.1f;
                        else p.x = s.x + s.width + 0.1f;
                    }
                }
            }
        }

        limitarJugadorAlMundo(p);
        if (p.y < -90f) matar(p, "cayo al vacio");
    }

    private void separarPorMovimientoPlataforma(Jugador p, Rectangle plat, Rectangle viejo, float dx, float dy) {
        if (!p.cuerpo().overlaps(plat)) return;
        float solapeIzq = (p.x + p.ancho) - plat.x;
        float solapeDer = (plat.x + plat.width) - p.x;
        float solapeAbajo = (p.y + p.alto) - plat.y;
        float solapeArriba = (plat.y + plat.height) - p.y;
        if (solapeIzq <= 0f || solapeDer <= 0f || solapeAbajo <= 0f || solapeArriba <= 0f) return;

        boolean veniaArriba = p.y >= viejo.y + viejo.height - 2f;
        boolean veniaAbajo = p.y + p.alto <= viejo.y + 2f;
        boolean veniaIzq = p.x + p.ancho <= viejo.x + 2f;
        boolean veniaDer = p.x >= viejo.x + viejo.width - 2f;

        if (veniaArriba) {
            p.y = plat.y + plat.height;
            p.enSuelo = true;
            if (p.velY < 0f) p.velY = 0f;
        } else if (veniaAbajo) {
            p.y = plat.y - p.alto;
            if (p.velY > 0f) p.velY = 0f;
        } else if (veniaIzq) {
            p.x = plat.x - p.ancho;
            p.velX = 0f;
        } else if (veniaDer) {
            p.x = plat.x + plat.width;
            p.velX = 0f;
        } else {
            float minX = Math.min(solapeIzq, solapeDer);
            float minY = Math.min(solapeAbajo, solapeArriba);
            if (minY < minX) {
                if (solapeArriba < solapeAbajo) {
                    p.y = plat.y + plat.height;
                    p.enSuelo = true;
                    if (p.velY < 0f) p.velY = 0f;
                } else {
                    p.y = plat.y - p.alto;
                    if (p.velY > 0f) p.velY = 0f;
                }
            } else {
                if (solapeIzq < solapeDer) p.x = plat.x - p.ancho;
                else p.x = plat.x + plat.width;
                p.velX = 0f;
            }
        }
    }

    private boolean solapaAlguno(Jugador p, List<Rectangle> entorno) {
        Rectangle c = p.cuerpo();
        for (Rectangle s : entorno) if (c.overlaps(s)) return true;
        return false;
    }

    private List<Rectangle> entornoSolidoParaJugador(MovilEstado movilActual) {
        List<Rectangle> entorno = new ArrayList<>();
        for (Nivel.Plataforma pl : nivel.plataformas) entorno.add(pl.rect);
        for (int i = 0; i < nivel.puertas.length; i++) {
            Nivel.Puerta puerta = nivel.puertas[i];
            boolean activa = puertasAbiertas[i];
            if (puerta.puente) {
                if (activa) entorno.add(puerta.rect);
            } else if (!activa) {
                entorno.add(puerta.rect);
            }
        }
        for (MovilEstado otro : moviles) {
            if (otro == movilActual) continue;
            entorno.add(otro.rect);
        }
        for (CajaEstado c : cajas) entorno.add(c.rect);
        return entorno;
    }

    private void limitarJugadorAlMundo(Jugador p) {
        if (p.x < 0f) p.x = 0f;
        if (p.x > nivel.ancho - p.ancho) p.x = Math.max(0f, nivel.ancho - p.ancho);
        if (p.y + p.alto > nivel.alto) {
            p.y = Math.max(0f, nivel.alto - p.alto);
            if (p.velY > 0f) p.velY = 0f;
        }
    }

    private void resolverChoqueJugadorConEntorno(Jugador p, MovilEstado movilActual) {
        resolverJugadorContraEntorno(p, movilActual, false, 0f);
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
        if (p.y >= caja.y + caja.height - 3f) return false;
        Rectangle cuerpo = p.cuerpo();
        return cuerpo.x + cuerpo.width >= caja.x - 5f
            && cuerpo.x <= caja.x + caja.width + 5f
            && cuerpo.y < caja.y + caja.height - 3f
            && cuerpo.y + cuerpo.height > caja.y + 3f;
    }

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

    private void actualizarMecanismos(float dt, boolean puntuar) {

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
            if (!activo && esDeAmbos(b.elemento)) {

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
        if (esDeAmbos(elemento)) return true;
        if ("FUEGO".equalsIgnoreCase(elemento)) return tipo == Jugador.Tipo.FUEGO;
        if ("AGUA".equalsIgnoreCase(elemento)) return tipo == Jugador.Tipo.AGUA;
        return false;
    }

    private static boolean esDeAmbos(String elemento) {
        return elemento == null || elemento.trim().isEmpty() || "AMBOS".equalsIgnoreCase(elemento);
    }

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

    private void matar(Jugador p, String motivo) {
        if (!p.vivo) return;
        p.morir();
        contarMuerte(p, motivo);
    }

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

    public void predecirJugador(Jugador.Tipo tipo, float dt, Entrada entrada) {
        if (completado || derrota || pausado) return;
        construirSolidos();
        Jugador p = tipo == Jugador.Tipo.FUEGO ? fuego : agua;
        if (!p.vivo) return;
        p.actualizar(dt, entrada.izquierda, entrada.derecha, entrada.saltoMantenido,
            entrada.saltoPresionado, solidosJugador);
        p.anchoMundoActual = nivel.ancho;
        p.altoMundoActual = nivel.alto;
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
