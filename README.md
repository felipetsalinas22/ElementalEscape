# Elemental Escape

Videojuego 2D de plataformas y puzzles cooperativo para dos jugadores, hecho con Java y LibGDX. Fuego y Agua tienen restricciones opuestas: la lava solo la cruza Fuego, el agua solo la cruza Agua. Los diez niveles combinan palancas, placas de presion, puertas temporizadas, secuencias, cajas pesadas, plataformas moviles, puentes de energia y rutas alternativas.

## Integrantes

- Luana Galván
- Juan Hracek
- Felipe Salinas
- Matías Siñani
- Marco Antonio Choquehuanca

## Caracteristicas

- **10 niveles de campana** con dificultad progresiva y validacion automatica de conectividad.
- **Juego local para dos jugadores** en la misma computadora.
- **Multijugador online directo** por LAN, reenvio de puertos o VPN (ZeroTier/Tailscale).
- **Editor de niveles** con snapshots, deshacer/rehacer, duplicar, borrar y probar.
- **Puntuacion y ranking** por nivel, gemas, mecanismos, muertes, bono de tiempo y logros.
- **Escenarios tematicos** con fondos multicapa, parallax, decoracion, iluminacion y particulas.
- **Personajes humanoides animados** con reposo, caminata, salto, caida, interaccion, victoria y derrota.
- **Guardado persistente** en la carpeta del usuario, fuera de la carpeta del juego.
- **Distribucion Windows autocontenida** con runtime Java incluido y lanzador `.exe`.

## Tecnologias

- Java 8 como nivel de lenguaje fuente (`sourceCompatibility`/`targetCompatibility`).
- LibGDX 1.14.2, backend LWJGL3 y OpenGL 3.2.
- Gradle Wrapper incluido.
- Solo graficos procedurales con `Pixmap`/`SpriteBatch`; no se usa `ShapeRenderer` ni assets externos.
- Para compilar se recomienda JDK 17 o superior. El juego distribuido incluye su propio runtime.

## Compilar y ejecutar

Desde la carpeta raiz del proyecto:

```powershell
.\gradlew.bat :lwjgl3:run --console=plain
```

Compilar el JAR ejecutable:

```powershell
.\gradlew.bat :lwjgl3:jar --console=plain
java -jar "lwjgl3\build\libs\Elemental Escape-1.2.0.jar"
```

## Pruebas

Pruebas headless de niveles, fisica, mecanismos, puntuacion, persistencia y red en localhost:

```powershell
.\gradlew.bat :core:pruebas --console=plain
```

Prueba de humo con OpenGL real. Abre y cierra menu, niveles, online, ranking, ajustes, editor y juego local, y guarda capturas en `lwjgl3/build/capturas`:

```powershell
.\gradlew.bat :lwjgl3:pruebasUI --console=plain
```

Las pruebas incluyen una comprobacion conservadora de recorrido: cada nivel debe tener superficies pisables bajo los orígenes, salidas y mecanismos, y esas superficies deben estar conectadas por saltos posibles. No sustituye una partida completa, pero detecta plataformas inalcanzables o mecanismos aislados.

## Distribucion para Windows

El repositorio incluye el paquete listo para usar en otra computadora:

**Descarga directa:** [Elemental-Escape-Windows.zip](https://github.com/felipetsalinas22/ElementalEscape/raw/main/dist/Elemental-Escape-Windows.zip)

```text
dist\Elemental-Escape-Windows.zip
```

Para jugar sin instalar nada:

1. Descargar `dist\Elemental-Escape-Windows.zip`.
2. Extraerlo.
3. Abrir `Elemental Escape\Elemental Escape.exe`.

El ZIP contiene el runtime de Java y todas las dependencias. Para volver a generar la distribucion desde el codigo:

Generar la carpeta portable con runtime incluido:

```powershell
.\gradlew.bat :lwjgl3:distWindows --console=plain
```

Tambien se puede usar el alias del modulo:

```powershell
.\gradlew.bat :lwjgl3:dist --console=plain
```

Resultado:

- `dist\Elemental Escape\Elemental Escape.exe`: aplicacion con runtime Java incluido.
- `dist\Elemental-Escape-Windows.zip`: paquete portable listo para copiar.
- `dist\LEEME-DISTRIBUCION.txt`: instrucciones para el usuario final.

La distribucion usa `jpackage --type app-image`. No hace falta instalar Java en el equipo de destino. La tarea busca `jpackage` en `JAVA_HOME` y en las instalaciones de JDK del sistema. El ejecutable no esta firmado digitalmente, por lo que Windows puede mostrar un aviso de SmartScreen la primera vez.

## Datos persistentes

El juego guarda todo en:

```text
%USERPROFILE%\.elemental-escape\
```

- `progreso.json`: mejores resultados, logros y campana.
- `ajustes.json`: sonido, volumen, particulas, sacudida y pantalla completa.
- `niveles\`: niveles personalizados creados en el editor.

Estos datos nunca se guardan dentro de `dist` ni de la carpeta de instalacion.

## Controles

### Juego local

| Accion | Fuego | Agua |
|---|---|---|
| Moverse | `A` / `D` | `Flecha izquierda` / `Flecha derecha` |
| Saltar | `W` | `Flecha arriba` |
| Pausa | `Esc` | `Esc` |
| Reintentar / continuar | `R` / `Enter` | `R` / `Enter` |

Las placas y palancas se activan al tocarlas o pisarlas. Las puertas se abren segun su modo (`ALGUNA`, `TODAS` o secuencia).

### Online

1. El anfitrion elige **Crear sala**. Juega como Fuego y comparte su IP y el codigo de sala.
2. El invitado elige **Unirse a una sala**, escribe la IP y el codigo. Juega como Agua.
3. En la misma red se usa la IP local. Para Internet hay que abrir el puerto TCP `54555` o usar una VPN como ZeroTier/Tailscale.
4. El codigo de sala evita conexiones accidentales, pero no descubre ni oculta la IP.

## Editor

- `Nuevo`, `Cargar`, `Guardar`, `Duplicar`, `Borrar`, `Probar`.
- Herramientas para plataformas, muros, lava, agua, gemas, placas, palancas, puertas, puentes, plataformas moviles, cajas, origenes y salidas.
- `Ctrl+Z` deshace; `Ctrl+Y` o `Ctrl+R` rehace; `F` encuadra; `G` alterna la cuadricula; `+`/`-` cambia el zoom.
- Las flechas mueven la seleccion o la camara; `Shift` acelera el desplazamiento.
- Los niveles se guardan como JSON en `%USERPROFILE%\.elemental-escape\niveles`.

## Estructura del proyecto

```text
core/     logica, fisica, niveles, UI procedural y pruebas headless
lwjgl3/   launcher de escritorio, smoke test OpenGL y empaquetado
assets/   icono y recursos minimos
scripts/  generacion de la distribucion Windows
dist/     salida generada; no se necesita para compilar
```

## Notas de diseno

- La fisica usa paso fijo de `1/120 s`, coyote time, jump buffer y salto de altura variable.
- El cuerpo de colision de los personajes no cambia cuando se animan las piezas visuales.
- La decoracion, las particulas y los fondos no participan en colisiones.
- En online el anfitrion es autoritativo: simula la partida y envia instantaneas; el cliente predice Agua y envia su entrada.
- El modo online fue probado automaticamente en localhost. Para dos computadoras reales se recomienda probar primero en la misma LAN.

## Enlaces

- Repositorio: <https://github.com/JuanMartinHracek/Escape-Elemental>
- Wiki con la propuesta: <https://github.com/JuanMartinHracek/Escape-Elemental/wiki>
