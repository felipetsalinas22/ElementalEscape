# Changelog

Todos los cambios importantes del proyecto se documentan en este archivo.

El formato esta basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/).
El proyecto usa versionado semantico.

## [1.2.0] - 2026-11-06

### Added

- Fondos multicapa con parallax, decoracion, iluminacion ambiental y particulas para TEMPLO, VOLCAN, CAVERNA, BOSQUE, LABORATORIO, HIELO, CIELO, RUINAS y FINAL.
- Personajes humanoides por piezas con animaciones de reposo, caminata, salto, caida, interaccion, victoria y derrota.
- Nuevo menu principal con titulo protagonista, avatares de Fuego y Agua, botones en dos columnas, atajos de teclado y panel de progreso.
- Botones con hover, brillo, acento lateral y ajuste automatico de texto para evitar recortes.
- Validacion automatica de conectividad de los diez niveles: superficies pisables, origenes, salidas y mecanismos alcanzables por saltos posibles.
- Gemas secretas en las rutas altas opcionales de los niveles 2 y 5.
- Distribucion Windows autocontenida con `jpackage`, runtime incluido, lanzador `.exe`, ZIP portable y tarea Gradle `distWindows`/`dist`.
- Capturas automaticas de las pantallas desde la prueba de humo OpenGL.
- Icono propio `logo.png` en la ventana y en la distribucion.

### Changed

- HUD y pantallas de niveles, ranking, online, ajustes, resultados, derrota y final con mejor contraste, jerarquia y espaciado.
- El ranking muestra cinco logros destacados sin invadir la zona de records.
- La paleta del editor distribuye las 16 herramientas dentro del panel y por encima de la barra de estado.
- El porcentaje de volumen ya no se superpone con los botones `-` y `+`.
- La pantalla completa guardada se aplica al iniciar el juego.
- `points` y datos persistentes se mantienen en `%USERPROFILE%\.elemental-escape`.
- README actualizado a 10 niveles, Java 8 fuente, pruebas, editor, online y distribucion.
- Version del proyecto y del JAR actualizada a `1.2.0`.

### Fixed

- Texto de botones largos que podia quedar cortado en paneles estrechos.
- Boton inferior de la paleta del editor que quedaba tapado por la barra de estado.
- Separador de logros que podia cruzarse con el texto en el ranking.
- Porcentaje de volumen que se solapaba con los controles de ajuste.

### Verified

- `:core:pruebas`: 107 pruebas, 0 fallos.
- `:lwjgl3:pruebasUI`: menu, niveles, online, ranking, ajustes, editor y juego local abiertos y cerrados con OpenGL real.
- `dist\Elemental Escape\Elemental Escape.exe` inicia correctamente con runtime incluido.

## [0.3.0] - 2026-09-28

### Changed

- Codigo pasado a espanol: `Main` a `Juego`, `MenuScreen` a `MenuPrincipal`, `GameScreen` a `PantallaJuego`, `Player` a `Jugador`, `Level` a `Nivel`, `NetManager` a `GestorRed` y `Draw` a `Dibujo`.

## [0.2.0] - 2026-09-24

### Added

- Menu con host, cliente y juego local.
- Fisica AABB, salto variable, vidas y respawn.
- Tres niveles iniciales con plataformas, peligros, botones, puertas, gemas y salidas.
- Red TCP cliente-servidor en el puerto `54555`.
- Graficos procedurales sin `ShapeRenderer`.

### Fixed

- Alturas y distancias de salto ajustadas para que las plataformas fueran alcanzables.
- Puertas con tope para impedir saltarlas por arriba.
- Render migrado a `SpriteBatch` por un fallo del driver AMD `atio6axx.dll`.

## [0.1.0] - 2026-09-24

### Added

- Creacion del proyecto con `gdx-liftoff` (Core + Desktop LWJGL3).
