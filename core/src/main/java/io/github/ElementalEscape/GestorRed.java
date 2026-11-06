package io.github.ElementalEscape;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Red TCP directa para dos computadoras.
 *
 * Arquitectura:
 *   Anfitrion (SERVIDOR): simula Partida y manda Instantaneas 20 veces por segundo.
 *   Invitado (CLIENTE): manda su entrada 30 veces por segundo y predice su personaje.
 *
 * Es una conexion DIRECTA: no hay servidor central. Para jugar por internet hay
 * que abrir el puerto 54555 en el router del anfitrion (port forwarding) o usar
 * una VPN tipo ZeroTier/Tailscale/Radmin. El codigo de sala es una clave de
 * acceso, no un servicio de descubrimiento.
 */
public class GestorRed {
    public enum Modo { LOCAL, SERVIDOR, CLIENTE }

    public static final int PUERTO = Configuracion.PUERTO_PREDETERMINADO;

    public volatile Modo modo = Modo.LOCAL;
    public volatile boolean conectado = false;
    public volatile String estado = "local";
    public volatile String codigo = "";
    public volatile int generacion = 0;
    public volatile String ultimoError = "";
    public volatile String nombreRemoto = "";
    public volatile String personaje = "FUEGO";

    private volatile boolean activo = false;
    private volatile boolean reintentar = true;
    private volatile boolean errorAutenticacion = false;

    private ServerSocket servidor;
    private Socket conexion;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private Thread hilo;

    private final Object bloqueoEscritura = new Object();
    private final Object bloqueoEntrada = new Object();
    private final Object bloqueoJson = new Object();
    private final Json json;

    private Partida.Entrada entradaRemota = new Partida.Entrada();
    private volatile Instantanea ultimaInstantanea;
    private volatile long versionInstantanea = 0L;
    private volatile Nivel nivelRemoto;
    private volatile int indiceNivelRemoto = 0;
    private volatile boolean campanaRemota = true;
    private volatile long versionNivel = 0L;
    private final List<String> comandos = Collections.synchronizedList(new ArrayList<String>());

    public GestorRed() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
    }

    public static GestorRed local() {
        return new GestorRed();
    }

    public boolean esLocal() { return modo == Modo.LOCAL; }
    public boolean esAnfitrion() { return modo == Modo.SERVIDOR; }
    public boolean esCliente() { return modo == Modo.CLIENTE; }

    /** Abre el puerto y espera a un invitado. Se puede llamar varias veces tras desconexiones. */
    public void iniciarServidor(String codigoSala) {
        cerrar();
        modo = Modo.SERVIDOR;
        personaje = "FUEGO";
        codigo = CodigoSala.limpiar(codigoSala);
        if (!CodigoSala.valido(codigo)) codigo = CodigoSala.generar();
        activo = true;
        conectado = false;
        reintentar = true;
        errorAutenticacion = false;
        estado = "Abriendo sala " + codigo + "...";
        hilo = new Thread(new Runnable() {
            @Override public void run() {
                bucleServidor();
            }
        }, "red-anfitrion");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Se conecta al anfitrion. Si se cae, reintenta solo mientras la pantalla siga abierta. */
    public void iniciarCliente(String ip, String codigoSala) {
        cerrar();
        modo = Modo.CLIENTE;
        personaje = "AGUA";
        codigo = CodigoSala.limpiar(codigoSala);
        activo = true;
        conectado = false;
        reintentar = true;
        errorAutenticacion = false;
        final String destino = ip == null ? "" : ip.trim();
        estado = "Conectando a " + destino + ":" + PUERTO + "...";
        hilo = new Thread(new Runnable() {
            @Override public void run() {
                bucleCliente(destino);
            }
        }, "red-invitado");
        hilo.setDaemon(true);
        hilo.start();
    }

    // =========================================================
    //                    HILOS DE RED
    // =========================================================

    private void bucleServidor() {
        try {
            servidor = new ServerSocket();
            servidor.setReuseAddress(true);
            servidor.bind(new InetSocketAddress(PUERTO));
            estado = "Sala " + codigo + " escuchando. IP: " + miIp() + ":" + PUERTO;
            while (activo) {
                Socket socket = servidor.accept();
                atenderInvitado(socket);
                if (activo) estado = "Invitado desconectado. Esperando en sala " + codigo + "...";
            }
        } catch (Exception e) {
            if (activo) {
                ultimoError = "No se pudo abrir el puerto " + PUERTO + ": " + e.getMessage();
                estado = ultimoError;
            }
        } finally {
            conectado = false;
        }
    }

    private void atenderInvitado(Socket socket) {
        DataInputStream in = null;
        DataOutputStream out = null;
        try {
            socket.setSoTimeout(12000);
            socket.setTcpNoDelay(true);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String saludo = leerMensaje(in);
            String[] p = saludo == null ? new String[0] : saludo.split("\\|", -1);
            if (p.length < 4 || !"HOLA".equals(p[0])
                || !String.valueOf(Configuracion.VERSION_PROTOCOLO).equals(p[1])
                || !codigo.equalsIgnoreCase(CodigoSala.limpiar(p[2]))) {
                enviarMensajeDirecto(out, "ERROR|Codigo de sala o version incorrectos.");
                return;
            }
            nombreRemoto = p[3];

            synchronized (bloqueoEscritura) {
                conexion = socket;
                entrada = in;
                salida = out;
            }
            synchronized (bloqueoEntrada) {
                entradaRemota = new Partida.Entrada();
            }
            conectado = true;
            generacion++;
            estado = "Invitado conectado: " + nombreRemoto + " (Agua)";
            enviarMensajeDirecto(out, "BIENVENIDO|" + Configuracion.VERSION_PROTOCOLO
                + "|" + codigo + "|AGUA");
            socket.setSoTimeout(20000);
            bucleLecturaHost(in);
        } catch (Exception e) {
            if (activo) ultimoError = "Invitado: " + e.getMessage();
        } finally {
            conectado = false;
            synchronized (bloqueoEntrada) {
                entradaRemota = new Partida.Entrada();
            }
            synchronized (bloqueoEscritura) {
                try { if (salida != null) salida.flush(); } catch (Exception ignored) { }
                if (conexion == socket) {
                    conexion = null;
                    entrada = null;
                    salida = null;
                }
            }
            try { socket.close(); } catch (Exception ignored) { }
        }
    }

    private void bucleCliente(String ip) {
        while (activo) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, PUERTO), 5000);
                socket.setSoTimeout(8000);
                socket.setTcpNoDelay(true);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                enviarMensajeDirecto(out, "HOLA|" + Configuracion.VERSION_PROTOCOLO + "|" + codigo + "|Jugador");
                String respuesta = leerMensaje(in);
                if (respuesta == null) throw new IOException("El anfitrion cerro la conexion.");
                if (respuesta.startsWith("ERROR|")) {
                    ultimoError = respuesta.substring(6);
                    estado = "No se pudo entrar: " + ultimoError;
                    errorAutenticacion = true;
                    try { socket.close(); } catch (Exception ignored) { }
                    break;
                }
                if (!respuesta.startsWith("BIENVENIDO|")) throw new IOException("Respuesta inesperada del anfitrion.");

                synchronized (bloqueoEscritura) {
                    conexion = socket;
                    entrada = in;
                    salida = out;
                }
                conectado = true;
                generacion++;
                estado = "Conectado al anfitrion " + ip + ". Sala " + codigo;
                socket.setSoTimeout(20000);
                bucleLecturaCliente(in);
            } catch (Exception e) {
                if (activo) {
                    conectado = false;
                    ultimoError = e.getMessage();
                    estado = "Desconectado: " + e.getMessage() + ". Reconectando...";
                }
            } finally {
                synchronized (bloqueoEscritura) {
                    if (conexion != null) {
                        try { conexion.close(); } catch (Exception ignored) { }
                    }
                    conexion = null;
                    entrada = null;
                    salida = null;
                }
                conectado = false;
            }
            if (!activo || !reintentar || errorAutenticacion) break;
            try { Thread.sleep(1500L); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void bucleLecturaHost(DataInputStream in) throws IOException {
        while (activo && conectado) {
            String mensaje = leerMensaje(in);
            if (mensaje == null) break;
            String[] p = mensaje.split("\\|", -1);
            if ("ENTRADA".equals(p[0]) && p.length >= 5) {
                synchronized (bloqueoEntrada) {
                    entradaRemota.izquierda = "1".equals(p[1]);
                    entradaRemota.derecha = "1".equals(p[2]);
                    entradaRemota.saltoMantenido = "1".equals(p[3]);
                    if ("1".equals(p[4])) entradaRemota.saltoPresionado = true;
                }
            } else if ("COMANDO".equals(p[0]) && p.length >= 2) {
                comandos.add(p[1]);
            }
        }
    }

    private void bucleLecturaCliente(DataInputStream in) throws IOException {
        while (activo && conectado) {
            String mensaje = leerMensaje(in);
            if (mensaje == null) break;
            int separador = mensaje.indexOf('|');
            String tipo = separador < 0 ? mensaje : mensaje.substring(0, separador);
            String resto = separador < 0 ? "" : mensaje.substring(separador + 1);
            if ("ESTADO".equals(tipo)) {
                try {
                    Instantanea s;
                    synchronized (bloqueoJson) {
                        s = json.fromJson(Instantanea.class, resto);
                    }
                    if (s != null) {
                        ultimaInstantanea = s;
                        versionInstantanea++;
                    }
                } catch (Exception ignored) { }
            } else if ("NIVEL".equals(tipo)) {
                try {
                    String[] p = resto.split("\\|", 3);
                    if (p.length >= 3) {
                        Nivel n;
                        synchronized (bloqueoJson) {
                            n = json.fromJson(Nivel.class, p[2]);
                        }
                        if (n != null) {
                            n.normalizar();
                            nivelRemoto = n;
                            indiceNivelRemoto = Integer.parseInt(p[0]);
                            campanaRemota = "1".equals(p[1]);
                            versionNivel++;
                        }
                    }
                } catch (Exception ignored) { }
            } else if ("COMANDO".equals(tipo)) {
                comandos.add(resto);
            }
        }
    }

    // =========================================================
    //                 ENVIO / RECEPCION
    // =========================================================

    /** El anfitrion envia su simulacion autoritativa. */
    public void enviarEstado(Partida partida, Progreso progreso) {
        enviarEstado(partida, progreso, null);
    }

    /** Variante que adjunta eventos acumulados entre dos envios (sonidos/particulas). */
    public void enviarEstado(Partida partida, Progreso progreso, String[] eventos) {
        if (!conectado || partida == null) return;
        try {
            Instantanea s = new Instantanea();
            partida.llenarInstantanea(s, progreso);
            if (eventos != null) s.eventos = eventos;
            String texto;
            synchronized (bloqueoJson) {
                texto = json.toJson(s);
            }
            enviarMensaje("ESTADO|" + texto);
        } catch (Exception e) {
            ultimoError = "No se pudo enviar estado: " + e.getMessage();
        }
    }

    /** El anfitrion envia el nivel completo (campana o personalizado). */
    public void enviarNivel(Nivel nivel, int indice, boolean campana) {
        if (!conectado || nivel == null) return;
        try {
            String texto;
            synchronized (bloqueoJson) {
                texto = json.toJson(nivel);
            }
            enviarMensaje("NIVEL|" + indice + "|" + (campana ? "1" : "0") + "|" + texto);
        } catch (Exception e) {
            ultimoError = "No se pudo enviar el nivel: " + e.getMessage();
        }
    }

    /** El invitado envia su input. */
    public void enviarEntrada(Partida.Entrada e) {
        if (modo != Modo.CLIENTE || !conectado || e == null) return;
        enviarMensaje("ENTRADA|" + (e.izquierda ? "1" : "0") + "|" + (e.derecha ? "1" : "0")
            + "|" + (e.saltoMantenido ? "1" : "0") + "|" + (e.saltoPresionado ? "1" : "0"));
    }

    public void enviarComando(String comando) {
        if (!conectado || comando == null) return;
        enviarMensaje("COMANDO|" + comando);
    }

    private synchronized void enviarMensaje(String mensaje) {
        DataOutputStream out;
        synchronized (bloqueoEscritura) {
            out = salida;
            if (out == null || !conectado) return;
            try {
                escribirMensaje(out, mensaje);
            } catch (Exception e) {
                ultimoError = "Conexion perdida al enviar.";
            }
        }
    }

    private static void enviarMensajeDirecto(DataOutputStream out, String mensaje) throws IOException {
        escribirMensaje(out, mensaje);
    }

    private static void escribirMensaje(DataOutputStream out, String mensaje) throws IOException {
        byte[] datos = mensaje.getBytes("UTF-8");
        out.writeInt(datos.length);
        out.write(datos);
        out.flush();
    }

    private static String leerMensaje(DataInputStream in) throws IOException {
        int largo = in.readInt();
        if (largo < 0 || largo > 4 * 1024 * 1024) throw new IOException("Mensaje de red invalido.");
        byte[] datos = new byte[largo];
        in.readFully(datos);
        return new String(datos, "UTF-8");
    }

    // =========================================================
    //                    CONSULTA PARA PANTALLAS
    // =========================================================

    public Partida.Entrada consumirEntradaRemota() {
        Partida.Entrada copia = new Partida.Entrada();
        synchronized (bloqueoEntrada) {
            copia.izquierda = entradaRemota.izquierda;
            copia.derecha = entradaRemota.derecha;
            copia.saltoMantenido = entradaRemota.saltoMantenido;
            copia.saltoPresionado = entradaRemota.saltoPresionado;
            entradaRemota.saltoPresionado = false;
        }
        return copia;
    }

    public Instantanea getUltimaInstantanea() { return ultimaInstantanea; }
    public long getVersionInstantanea() { return versionInstantanea; }

    public Nivel getNivelRemoto() { return nivelRemoto; }
    public int getIndiceNivelRemoto() { return indiceNivelRemoto; }
    public boolean isCampanaRemota() { return campanaRemota; }
    public long getVersionNivel() { return versionNivel; }

    public String consumirComando() {
        if (comandos.isEmpty()) return null;
        return comandos.remove(0);
    }

    public void limpiarComandos() { comandos.clear(); }

    public String getEstado() { return estado; }
    public String getUltimoError() { return ultimoError; }
    public String getNombreRemoto() { return nombreRemoto; }

    public void setReintentar(boolean valor) { reintentar = valor; }

    public String miIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            StringBuilder sb = new StringBuilder();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> direcciones = ni.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress d = direcciones.nextElement();
                    if (d instanceof Inet4Address && !d.isLoopbackAddress())
                        sb.append(d.getHostAddress()).append("  ");
                }
            }
            if (sb.length() == 0) return InetAddress.getLocalHost().getHostAddress();
            return sb.toString().trim();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public void cerrar() {
        activo = false;
        reintentar = false;
        conectado = false;
        synchronized (bloqueoEscritura) {
            try { if (salida != null) salida.flush(); } catch (Exception ignored) { }
            try { if (conexion != null) conexion.close(); } catch (Exception ignored) { }
            conexion = null;
            salida = null;
            entrada = null;
        }
        try { if (servidor != null) servidor.close(); } catch (Exception ignored) { }
        servidor = null;
        if (hilo != null) {
            hilo.interrupt();
            hilo = null;
        }
        comandos.clear();
        synchronized (bloqueoEntrada) {
            entradaRemota = new Partida.Entrada();
        }
    }
}
