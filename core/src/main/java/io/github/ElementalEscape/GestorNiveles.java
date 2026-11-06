package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Guarda y carga los niveles personalizados en archivos JSON.
 *
 * En el juego se usa una carpeta del usuario para que los niveles no se pierdan:
 *   C:\Users\TU_USUARIO\.elemental-escape\niveles
 *
 * La clase tambien se puede crear con otra carpeta (lo usan las pruebas).
 */
public class GestorNiveles {
    private final FileHandle carpeta;
    private final Json json;

    public GestorNiveles(FileHandle carpeta) {
        this.carpeta = carpeta;
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        this.json.setIgnoreUnknownFields(true);
        this.json.setUsePrototypes(false);
    }

    public static GestorNiveles predeterminado() {
        return new GestorNiveles(Gdx.files.external(".elemental-escape/niveles"));
    }

    public FileHandle getCarpeta() {
        asegurarCarpeta();
        return carpeta;
    }

    private void asegurarCarpeta() {
        if (!carpeta.exists()) carpeta.mkdirs();
    }

    /** Entrada de la lista. Si un archivo esta roto, error explica el problema. */
    public static class Entrada {
        public String archivo;
        public Nivel nivel;
        public String error;
    }

    public List<Entrada> listar() {
        asegurarCarpeta();
        List<Entrada> lista = new ArrayList<>();
        FileHandle[] archivos = carpeta.list(".json");
        for (FileHandle archivo : archivos) {
            Entrada entrada = new Entrada();
            entrada.archivo = archivo.name();
            try {
                entrada.nivel = leer(archivo);
            } catch (Exception e) {
                entrada.error = "No se pudo leer " + archivo.name() + ": " + e.getMessage();
            }
            lista.add(entrada);
        }
        Collections.sort(lista, new Comparator<Entrada>() {
            @Override
            public int compare(Entrada a, Entrada b) {
                String na = a.nivel != null ? a.nivel.nombre : a.archivo;
                String nb = b.nivel != null ? b.nivel.nombre : b.archivo;
                return na.compareToIgnoreCase(nb);
            }
        });
        return lista;
    }

    public Nivel cargar(String nombreArchivo) throws IOException {
        asegurarCarpeta();
        FileHandle archivo = carpeta.child(nombreArchivo);
        if (!archivo.exists() || archivo.isDirectory())
            throw new IOException("No existe el nivel: " + nombreArchivo);
        return leer(archivo);
    }

    private Nivel leer(FileHandle archivo) throws IOException {
        try {
            String texto = archivo.readString("UTF-8");
            if (texto == null || texto.trim().isEmpty())
                throw new IOException("El archivo esta vacio.");
            Nivel nivel = json.fromJson(Nivel.class, texto);
            if (nivel == null) throw new IOException("El JSON no contiene un nivel.");
            nivel.normalizar();
            return nivel;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Datos incorrectos en " + archivo.name() + ".", e);
        }
    }

    /**
     * Guarda un nivel. Si ya existe otro con el mismo nombre solo sobrescribe
     * cuando permitirSobrescribir es true.
     */
    public void guardar(Nivel nivel, boolean permitirSobrescribir) throws IOException {
        if (nivel == null) throw new IOException("No hay nivel para guardar.");
        nivel.normalizar();
        if (nivel.nombre.trim().isEmpty()) throw new IOException("El nivel necesita un nombre.");
        asegurarCarpeta();

        FileHandle destino = archivoPara(nivel.nombre);
        boolean mismoArchivo = false;
        if (destino.exists()) {
            String nombreExistente = "";
            try {
                nombreExistente = leer(destino).nombre;
            } catch (Exception ignored) { }
            mismoArchivo = nombresIguales(nombreExistente, nivel.nombre);
            if (!mismoArchivo && !permitirSobrescribir)
                throw new IOException("Ya existe un archivo llamado \"" + destino.name() + "\".");
        }
        // Guardar de nuevo el mismo nivel es una actualizacion normal: no hace
        // falta pedir confirmacion. La confirmacion queda para colisiones reales
        // con otro archivo o para la API que la solicite explicitamente.
        if (existeNombre(nivel.nombre) && !permitirSobrescribir && !mismoArchivo)
            throw new IOException("Ya existe un nivel llamado \"" + nivel.nombre + "\". Elige otro nombre o confirma sobrescribir.");

        String texto = json.prettyPrint(nivel);
        destino.writeString(texto, false, "UTF-8");
    }

    public void eliminar(String nombreArchivo) throws IOException {
        asegurarCarpeta();
        FileHandle archivo = carpeta.child(nombreArchivo);
        if (!archivo.exists()) throw new IOException("No existe el nivel: " + nombreArchivo);
        if (!archivo.delete()) throw new IOException("No se pudo borrar " + nombreArchivo + ".");
    }

    public Nivel duplicar(String nombreArchivo, String nuevoNombre) throws IOException {
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty())
            throw new IOException("El nombre de la copia no puede estar vacio.");
        Nivel original = cargar(nombreArchivo);
        Nivel copia = json.fromJson(Nivel.class, json.toJson(original));
        if (copia == null) throw new IOException("No se pudo copiar el nivel.");
        copia.nombre = nuevoNombre.trim();
        copia.normalizar();
        guardar(copia, false);
        return copia;
    }

    public boolean existeNombre(String nombre) {
        if (nombre == null) return false;
        for (Entrada e : listar()) {
            if (e.nivel != null && nombresIguales(e.nivel.nombre, nombre)) return true;
        }
        return false;
    }

    private static boolean nombresIguales(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    /** Convierte el nombre en un archivo seguro para Windows/Linux. */
    public FileHandle archivoPara(String nombre) {
        String base = nombre == null ? "nivel" : nombre.trim();
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        base = base.replaceAll("\\s+", " ");
        if (base.isEmpty()) base = "nivel";
        if (base.length() > 80) base = base.substring(0, 80);
        FileHandle archivo = carpeta.child(base + ".json");

        int numero = 2;
        while (archivo.exists()) {
            // Si el archivo ya contiene este mismo nivel, es el destino correcto.
            try {
                Nivel existente = leer(archivo);
                if (nombresIguales(existente.nombre, nombre)) return archivo;
            } catch (Exception ignored) { }
            archivo = carpeta.child(base + " (" + numero + ").json");
            numero++;
        }
        return archivo;
    }

    /** Devuelve el nombre de archivo que corresponde a un nivel ya guardado. */
    public String archivoDeNivel(Nivel nivel) {
        if (nivel == null) return "";
        for (Entrada e : listar()) {
            if (e.nivel != null && nombresIguales(e.nivel.nombre, nivel.nombre)) return e.archivo;
        }
        return archivoPara(nivel.nombre).name();
    }
}
