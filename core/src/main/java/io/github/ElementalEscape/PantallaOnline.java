package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Lobby de multijugador directo.
 * El anfitrion abre el puerto y simula la partida (Fuego). El invitado se
 * conecta por IP (Agua). El codigo de sala es una clave, no un servidor central.
 */
public class PantallaOnline extends PantallaBase {
    private String ip = "127.0.0.1";
    private String codigo = CodigoSala.generar();
    private GestorRed red;
    private String miIp = "127.0.0.1";
    private String errorLocal = "";
    private float tiempo = 0f;
    private float tiempoConectado = 0f;
    private boolean transferido = false;

    public PantallaOnline(Juego juego) {
        super(juego);
    }

    @Override
    protected void alMostrar() {
        miIp = new GestorRed().miIp();
    }

    @Override
    protected void actualizar(float delta) {
        tiempo += delta;
        if (red != null) {
            if (red.conectado) tiempoConectado += delta;
            else tiempoConectado = 0f;
            if (red.conectado && tiempoConectado >= 0.8f && !transferido) {
                transferido = true;
                aplazar(new Runnable() {
                    @Override public void run() { entrarAlJuego(); }
                });
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (red != null) cancelarConexion();
            else irAlMenu();
        }
    }

    @Override
    protected void dibujar(float delta) {
        aplicarVistaUI();
        ScreenUtils.clear(0.025f, 0.035f, 0.075f, 1f);
        lote.begin();
        fondoEstrellado(tiempo, new Color(0.025f, 0.035f, 0.075f, 1f),
            new Color(0.07f, 0.06f, 0.20f, 1f));
        tituloPantalla("MULTIJUGADOR EN LINEA", "Dos computadoras, una sala y el mismo nivel", Configuracion.AGUA_CLARA);

        if (red == null) {
            dibujarEleccion();
        } else {
            dibujarConexion();
        }

        dibujarAyuda();
        if (ui.boton("VOLVER AL MENU", 24f, 20f, 190f, 34f)) {
            if (red != null) cancelarConexion();
            irAlMenu();
        }
        lote.end();
    }

    private void dibujarEleccion() {
        Dibujo.panel(lote, 30f, 146f, 430f, 284f, new Color(0.06f, 0.07f, 0.14f, 0.97f),
            Configuracion.FUEGO);
        Dibujo.textoCentrado(lote, fuente, "CREAR SALA (FUEGO)", 245f, 400f,
            Configuracion.FUEGO_CLARO, 1.0f);
        Dibujo.textoCentrado(lote, fuente, "Tu simulacion manda. El invitado juega como Agua.",
            245f, 372f, Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.texto(lote, fuente, "CODIGO DE SALA", 58f, 338f, Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.panel(lote, 58f, 298f, 230f, 34f, new Color(0.04f, 0.06f, 0.10f, 1f),
            new Color(0.35f, 0.48f, 0.70f, 1f));
        Dibujo.texto(lote, fuente, codigo, 72f, 320f, Configuracion.DORADO, 1.0f);
        if (ui.boton("CAMBIAR", 298f, 298f, 140f, 34f)) pedirCodigo();
        if (ui.botonColor("CREAR SALA", 58f, 248f, 380f, 40f, Configuracion.FUEGO)) iniciarAnfitrion();

        Dibujo.textoCentrado(lote, fuente, "Tu IP local aproximada:", 245f, 222f,
            Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.textoCentrado(lote, fuente, recortarIp(miIp), 245f, 200f,
            Configuracion.TEXTO, 0.68f);

        Dibujo.panel(lote, 500f, 146f, 430f, 284f, new Color(0.05f, 0.08f, 0.14f, 0.97f),
            Configuracion.AGUA);
        Dibujo.textoCentrado(lote, fuente, "UNIRSE A UNA SALA (AGUA)", 715f, 400f,
            Configuracion.AGUA_CLARA, 1.0f);
        Dibujo.textoCentrado(lote, fuente, "El anfitrion te asigna el personaje Agua.",
            715f, 372f, Configuracion.TEXTO_SUAVE, 0.62f);

        Dibujo.texto(lote, fuente, "IP DEL ANFITRION", 528f, 338f, Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.panel(lote, 528f, 298f, 374f, 34f, new Color(0.04f, 0.06f, 0.10f, 1f),
            new Color(0.35f, 0.48f, 0.70f, 1f));
        Dibujo.texto(lote, fuente, ip, 542f, 320f, Configuracion.TEXTO, 0.76f);
        if (zonaClic(528f, 298f, 374f, 34f)) pedirIp();

        Dibujo.texto(lote, fuente, "CODIGO DE SALA", 528f, 280f, Configuracion.TEXTO_SUAVE, 0.62f);
        Dibujo.panel(lote, 528f, 240f, 170f, 34f, new Color(0.04f, 0.06f, 0.10f, 1f),
            new Color(0.35f, 0.48f, 0.70f, 1f));
        Dibujo.texto(lote, fuente, codigo, 542f, 262f, Configuracion.DORADO, 0.90f);
        if (zonaClic(528f, 240f, 170f, 34f)) pedirCodigoCliente();
        if (ui.botonColor("UNIRSE", 710f, 240f, 192f, 34f, Configuracion.AGUA)) iniciarInvitado();

        if (!errorLocal.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, errorLocal, 715f, 210f, Configuracion.ROJO, 0.64f);
        }
    }

    private void dibujarConexion() {
        boolean anfitrion = red.esAnfitrion();
        Dibujo.panel(lote, 160f, 170f, 640f, 250f,
            new Color(0.05f, 0.07f, 0.13f, 0.98f),
            anfitrion ? Configuracion.FUEGO : Configuracion.AGUA);
        Dibujo.textoCentrado(lote, fuente, anfitrion ? "SALA CREADA" : "CONECTANDO",
            480f, 388f, anfitrion ? Configuracion.FUEGO_CLARO : Configuracion.AGUA_CLARA, 1.25f);

        if (anfitrion) {
            Dibujo.textoCentrado(lote, fuente, "Codigo: " + red.codigo, 480f, 350f,
                Configuracion.DORADO, 1.05f);
            Dibujo.textoCentrado(lote, fuente, "IP para el invitado: " + recortarIp(miIp)
                + "   Puerto: " + GestorRed.PUERTO, 480f, 320f, Configuracion.TEXTO, 0.72f);
        } else {
            Dibujo.textoCentrado(lote, fuente, "Anfitrion: " + ip + "   Codigo: " + codigo,
                480f, 320f, Configuracion.TEXTO, 0.72f);
        }

        String estado = red.getEstado();
        Dibujo.textoCentrado(lote, fuente, estado, 480f, 278f,
            red.conectado ? Configuracion.VERDE : Configuracion.TEXTO_SUAVE, 0.72f);
        if (red.ultimoError != null && !red.ultimoError.isEmpty()) {
            Dibujo.textoCentrado(lote, fuente, red.ultimoError, 480f, 252f,
                Configuracion.ROJO, 0.62f);
        }
        if (red.conectado) {
            Dibujo.textoCentrado(lote, fuente, "Conexion establecida. Entrando a la partida...",
                480f, 224f, Configuracion.VERDE, 0.72f);
        } else {
            Dibujo.textoCentrado(lote, fuente,
                anfitrion ? "Esperando a que el invitado escriba la IP y el codigo..."
                    : "Se reintenta automaticamente. Revisa IP, puerto y codigo.",
                480f, 224f, Configuracion.TEXTO_SUAVE, 0.64f);
        }

        if (ui.boton("CANCELAR CONEXION", 350f, 188f, 260f, 30f)) cancelarConexion();
    }

    private void dibujarAyuda() {
        Dibujo.panel(lote, 30f, 82f, 900f, 54f, new Color(0.03f, 0.05f, 0.09f, 0.88f),
            new Color(0.22f, 0.30f, 0.44f, 0.90f));
        Dibujo.texto(lote, fuente,
            "Misma red: usen la IP local del anfitrion. Internet: abran el puerto "
                + GestorRed.PUERTO + " (TCP) en el router o usen ZeroTier/Tailscale.",
            44f, 114f, Configuracion.TEXTO_SUAVE, 0.60f);
        Dibujo.texto(lote, fuente,
            "El codigo de sala evita entradas accidentales, pero NO descubre la IP. "
                + "No hace falta cuenta ni servidor externo.",
            44f, 94f, Configuracion.TEXTO_SUAVE, 0.58f);
    }

    private void pedirIp() {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                if (texto != null && !texto.trim().isEmpty()) ip = texto.trim();
                errorLocal = "";
            }
            @Override public void canceled() { }
        }, "IP del anfitrion", ip, "192.168.1.10");
    }

    private void pedirCodigo() {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                String limpio = CodigoSala.limpiar(texto);
                if (CodigoSala.valido(limpio)) {
                    codigo = limpio;
                    errorLocal = "";
                } else {
                    errorLocal = "El codigo debe tener 6 letras o numeros.";
                }
            }
            @Override public void canceled() { }
        }, "Codigo de sala para el anfitrion", codigo, "ABC123");
    }

    private void pedirCodigoCliente() {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String texto) {
                String limpio = CodigoSala.limpiar(texto);
                if (CodigoSala.valido(limpio)) {
                    codigo = limpio;
                    errorLocal = "";
                } else {
                    errorLocal = "El codigo debe tener 6 letras o numeros.";
                }
            }
            @Override public void canceled() { }
        }, "Codigo de sala", codigo, "ABC123");
    }

    private void iniciarAnfitrion() {
        errorLocal = "";
        if (red != null) red.cerrar();
        red = new GestorRed();
        red.iniciarServidor(codigo);
        tiempoConectado = 0f;
    }

    private void iniciarInvitado() {
        errorLocal = "";
        if (ip.trim().isEmpty()) {
            errorLocal = "Escribe la IP del anfitrion.";
            return;
        }
        if (!CodigoSala.valido(codigo)) {
            errorLocal = "El codigo debe tener 6 letras o numeros.";
            return;
        }
        if (red != null) red.cerrar();
        red = new GestorRed();
        red.iniciarCliente(ip.trim(), codigo);
        tiempoConectado = 0f;
    }

    private void cancelarConexion() {
        if (red != null) {
            red.cerrar();
            red = null;
        }
        tiempoConectado = 0f;
        transferido = false;
    }

    private void entrarAlJuego() {
        if (red == null || !red.conectado) {
            transferido = false;
            return;
        }
        if (red.esAnfitrion()) {
            int indice = juego.getProgreso().siguienteNivel();
            cambiarPantalla(PantallaJuego.online(juego, red,
                NivelesIntegrados.crear(indice), indice, true));
        } else {
            cambiarPantalla(PantallaJuego.online(juego, red, null, 0, true));
        }
    }

    private static String recortarIp(String texto) {
        if (texto == null) return "127.0.0.1";
        if (texto.length() <= 44) return texto;
        return texto.substring(0, 41) + "...";
    }

    private boolean zonaClic(float x, float y, float ancho, float alto) {
        if (ui.clic && ui.dentro(x, y, ancho, alto)) {
            ui.clic = false;
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        if (red != null && !transferido) {
            red.cerrar();
            red = null;
        }
        super.dispose();
    }
}
