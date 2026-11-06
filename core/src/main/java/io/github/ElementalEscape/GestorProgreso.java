package io.github.ElementalEscape;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Guarda progreso y ajustes en la carpeta del usuario.
 * Si un archivo no existe o esta roto, el juego empieza con datos nuevos
 * en lugar de cerrarse con un error.
 */
public class GestorProgreso {
    private final FileHandle carpeta;
    private final Json json;

    public GestorProgreso(FileHandle carpeta) {
        this.carpeta = carpeta;
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        this.json.setIgnoreUnknownFields(true);
        this.json.setUsePrototypes(false);
    }

    public static GestorProgreso predeterminado() {
        return new GestorProgreso(Gdx.files.external(".elemental-escape"));
    }

    public FileHandle getCarpeta() {
        if (!carpeta.exists()) carpeta.mkdirs();
        return carpeta;
    }

    public Progreso cargarProgreso() {
        try {
            FileHandle archivo = getCarpeta().child("progreso.json");
            if (!archivo.exists()) return new Progreso();
            String texto = archivo.readString("UTF-8");
            if (texto.trim().isEmpty()) return new Progreso();
            Progreso p = json.fromJson(Progreso.class, texto);
            if (p == null) return new Progreso();
            p.normalizar();
            return p;
        } catch (Exception e) {
            return new Progreso();
        }
    }

    public void guardarProgreso(Progreso progreso) {
        try {
            progreso.normalizar();
            getCarpeta().child("progreso.json").writeString(json.prettyPrint(progreso), false, "UTF-8");
        } catch (Exception e) {
            Gdx.app.error("Progreso", "No se pudo guardar el progreso: " + e.getMessage());
        }
    }

    public Ajustes cargarAjustes() {
        try {
            FileHandle archivo = getCarpeta().child("ajustes.json");
            if (!archivo.exists()) return new Ajustes();
            Ajustes a = json.fromJson(Ajustes.class, archivo.readString("UTF-8"));
            if (a == null) return new Ajustes();
            a.normalizar();
            return a;
        } catch (Exception e) {
            return new Ajustes();
        }
    }

    public void guardarAjustes(Ajustes ajustes) {
        try {
            ajustes.normalizar();
            getCarpeta().child("ajustes.json").writeString(json.prettyPrint(ajustes), false, "UTF-8");
        } catch (Exception e) {
            Gdx.app.error("Ajustes", "No se pudieron guardar los ajustes: " + e.getMessage());
        }
    }

    public void borrarProgreso() {
        try {
            FileHandle archivo = getCarpeta().child("progreso.json");
            if (archivo.exists()) archivo.delete();
        } catch (Exception ignored) { }
    }
}
