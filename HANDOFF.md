# Handoff — Simulador de Partidos de Fútbol (TPO Patrones de Diseño)

Documento para retomar el proyecto en otra sesión. Describe el **estado real del código** (no el deseado), los patrones aplicados, y el **gap entre lo implementado y el alcance definido**. Java 21. **Persistencia: SQLite vía JDBC** (única dependencia externa: el driver `sqlite-jdbc` en `lib/`).

> **Última actualización:** se migró la **capa de persistencia de listas en memoria a SQLite/JDBC** (ver sección 10). `ConexionDB` ahora abre/posee una `Connection` a `simulador.db` y crea el esquema; los 4 DAOs reescribieron sus cuerpos con `PreparedStatement`. **La regla de oro se cumplió: nada fuera del paquete `persistencia` cambió** (modelo, fachada, observadores, `Main` intactos). Verificado end-to-end con **dos corridas separadas** (jugar+guardar; reiniciar JVM y leer todo desde la BD). Antes de esto, las 6 tareas del plan y el menú interactivo ya estaban completos.

---

## 1. Cómo se ejecuta hoy

- **`src/Main.java` es un menú de consola interactivo** (`Scanner`), única vía de entrada. Opera todo a través de la fachada `ControladorPartido` (nunca instancia DAOs). Menú principal: 1) Gestión de equipos/jugadores, 2) Configurar y jugar partido, 3) Ver historial, 4) Salir.
- **Requiere el driver SQLite** en el classpath: `lib/sqlite-jdbc-3.42.0.0.jar` (versionado en el repo). El separador de classpath es `;` en Windows y `:` en Linux/Mac.
- Compilar/ejecutar en **Windows (PowerShell)**:
  ```powershell
  Remove-Item -Recurse -Force bin; New-Item -ItemType Directory bin | Out-Null
  Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName > sources.txt
  javac -cp "lib\sqlite-jdbc-3.42.0.0.jar" -d bin "@sources.txt"
  java -cp "bin;lib\sqlite-jdbc-3.42.0.0.jar" Main
  ```
- Compilar/ejecutar en **Linux/Mac (bash)**:
  ```bash
  rm -rf bin && mkdir bin
  find src -name "*.java" > sources.txt
  javac -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
  java -cp "bin:lib/sqlite-jdbc-3.42.0.0.jar" Main
  ```
- Al primer arranque se crea `simulador.db` en la raíz (gitignored, se regenera). La compilación no necesita el driver (no se importa `org.sqlite.*`), pero la ejecución sí (el driver se registra por `ServiceLoader` en runtime).
- **Flujo del menú:** el usuario registra equipos (cada alta crea además su estadio), agrega jugadores (número de camiseta autoasignado por orden), configura un partido eligiendo local/visitante/estadio/tácticas/modo (con el estado de la selección visible en pantalla), y lo juega por tramos con **entretiempo interactivo** (`=== ENTRETIEMPO ===` → cambiar táctica local/visitante o continuar). Al finalizar el partido se persiste solo y se puede consultar el historial desde el menú.
- Internamente, `configurarPartido(...)` también persiste ambos equipos y todos sus jugadores en **SQLite** vía los DAOs (todo encapsulado tras `ControladorPartido`).
- **Recorrido del partido (máquina de estados):** un partido completo son **3** llamadas a `simularTramo()` — 1er tiempo (simula) → entretiempo (avanza sin simular) → 2do tiempo (simula y finaliza/guarda). El menú las encadena: una para el 1er tiempo, y tras el entretiempo dos seguidas para dejar atrás el entretiempo y jugar el 2do tiempo.

---

## 1.b Interfaz gráfica (JavaFX) — en construcción

- **Punto de entrada gráfico:** `src/videojuego/ui/MainApp.java` (`extends Application`). Convive con `Main.java` (consola), que **se mantiene intacto**. Son dos vías de entrada independientes: para la consola se ejecuta `Main`, para la GUI se ejecuta `videojuego.ui.MainApp`.
- **Estado actual (esqueleto):** solo navegación. El menú principal abre y desde él se puede entrar a las 4 pantallas (Gestión de Equipos, Configurar Partido, Simulación, Historial) y **volver al menú**. Las 4 pantallas son aún vistas mínimas (título + botón "Volver al menú"), sin lógica de negocio.
- **Navegación:** un único `Stage` con una única `Scene`; navegar = `escena.setRoot(...)`. El contrato está en `videojuego.ui.Navegador` (interfaz + enum `Pantalla` anidado); `MainApp` lo implementa y se lo pasa a cada controlador. La lógica (cuando llegue) seguirá pasando por la fachada `ControladorPartido` (única instancia compartida que `MainApp` crea y reparte a los controladores). **JavaFX puro, sin FXML:** cada controlador arma su vista por código en `getVista()`.
- **Dependencia: JavaFX 21 SDK — versionado en el repo (Windows + Mac).** El proyecto es **self-contained**: clonás y compila/corre sin instalar el SDK. Como el SDK no es Java puro (trae **librerías nativas** por SO), hay **dos builds** versionados, y cada uno apunta a la carpeta de su sistema operativo:
  - `lib/javafx-sdk-21-win/` → **Windows x64** (las `.dll` nativas viven en su `bin/`).
  - `lib/javafx-sdk-21-mac/` → **macOS Apple Silicon (aarch64)** (las `.dylib` viven en su `lib/`).

  El módulo que se usa es `javafx.controls` (arrastra `javafx.base` y `javafx.graphics`); no se usa `javafx.fxml`.

### Setup del SDK

**No hay que descargar nada**: ambos builds vienen versionados en el repo. Solo necesitás un **JDK 21+** instalado y usar, en los comandos de abajo, la carpeta de tu SO.

> **Builds no versionados (excepciones):** el repo trae **solo** Windows x64 y macOS aarch64. Si tu Mac es **Intel (x64)** o usás **Linux**, ese build no está: bajalo de Gluon (https://gluonhq.com/products/javafx/, versión **21.0.6**), dejalo en una carpeta propia (ej. `lib/javafx-sdk-21-mac-x64/` o `lib/javafx-sdk-21-linux/`, **sin pisar** las versionadas) y apuntá el `--module-path` ahí. La **consola** (`Main`) no usa JavaFX y corre sin nada de esto.

### Compilar / ejecutar la GUI

Usá la carpeta del SDK de tu SO (`lib/javafx-sdk-21-win/lib` en Windows, `lib/javafx-sdk-21-mac/lib` en Mac). Los comandos son equivalentes (cambia la shell y el separador de classpath: `;` en Windows, `:` en Mac/Linux):

- **Windows (PowerShell)** — comandos **verificados** en esta máquina:
  ```powershell
  Remove-Item -Recurse -Force bin -ErrorAction SilentlyContinue; New-Item -ItemType Directory bin | Out-Null
  Get-ChildItem -Recurse src -Filter *.java | Resolve-Path -Relative | Out-File -Encoding ascii sources.txt
  javac --module-path "lib\javafx-sdk-21-win\lib" --add-modules javafx.controls -cp "lib\sqlite-jdbc-3.42.0.0.jar" -d bin "@sources.txt"
  java --module-path "lib\javafx-sdk-21-win\lib" --add-modules javafx.controls -cp "bin;lib\sqlite-jdbc-3.42.0.0.jar" videojuego.ui.MainApp
  ```
- **macOS (bash/zsh)** — Apple Silicon (en Linux o Mac Intel, cambiá la ruta del `--module-path` a tu carpeta):
  ```bash
  rm -rf bin && mkdir bin
  find src -name "*.java" > sources.txt
  javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
  java --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp "bin:lib/sqlite-jdbc-3.42.0.0.jar" videojuego.ui.MainApp
  ```
- **⚠️ Gotcha del `sources.txt` en PowerShell** (descubierto al verificar — aplica también al comando de consola de la sección 1): `javac @sources.txt` falla si el archivo de listado no está bien generado. Dos causas y su fix (ya aplicado en el comando de arriba):
  1. **Codificación:** `> sources.txt` en **Windows PowerShell 5.1** escribe **UTF-16 LE con BOM** y `javac` corta con `MalformedInputException`. Solución: `Out-File -Encoding ascii` (o UTF-8 sin BOM).
  2. **Espacios en la ruta:** la ruta de este repo contiene `Desarrollo SW` (con espacio); con rutas **absolutas** (`ForEach-Object FullName`) `javac` parte el path por el espacio (`invalid flag: ...\Desarrollo`). Solución: usar rutas **relativas** con `Resolve-Path -Relative` (ejecutando desde la raíz del repo, el espacio queda en la carpeta padre, fuera del listado). En bash con `find` no aparece este problema.
- Al ejecutar con **JDK 25** aparecen WARNINGs benignos (`restricted method System::load` por la carga de nativas de JavaFX, y `sun.misc.Unsafe::allocateMemory` del render interno Marlin de JavaFX 21); **no son errores**, la app abre igual y son ignorables. Para silenciarlos ambos, agregar al comando `java` (o a las VM options del IDE): `--enable-native-access=javafx.graphics --sun-misc-unsafe-memory-access=allow`.
- Notas: el `javac` único compila **toda** `src/` (consola + GUI) de una pasada; agregar `--add-modules` no afecta a las clases de consola. La **consola** se sigue corriendo con el comando de la sección 1 (sin JavaFX). En esta máquina el JDK instalado es **25** (JavaFX 21 es LTS y corre sobre JDK 25 sin problemas).

### Setup en IntelliJ IDEA (imports `javafx.*` en rojo)

Como el SDK se baja como jars sueltos (sin Maven/Gradle), **IntelliJ no lo detecta solo**: hay que configurarlo a mano una vez por máquina. Son dos pasos:

1. **Resolver los imports en rojo (agregar la librería):** `File → Project Structure` (Ctrl+Alt+Shift+S) → `Project Settings → Libraries` → `+` → **Java** → seleccionar la carpeta del SDK de tu SO (`lib/javafx-sdk-21-win/lib` en Windows, `lib/javafx-sdk-21-mac/lib` en Mac; toma todos los `.jar`) → OK → elegir el módulo del proyecto → Apply. Esto calla los `Cannot resolve symbol javafx`.
2. **Que arranque al correr (VM options):** `Run → Edit Configurations…` → la *Application* con Main class `videojuego.ui.MainApp` → campo **VM options** (si no aparece: `Modify options → Add VM options`) →
   ```
   --module-path "<RUTA_ABSOLUTA_AL_REPO>\lib\javafx-sdk-21-win\lib" --add-modules javafx.controls
   ```
   (en Mac usar `.../lib/javafx-sdk-21-mac/lib` con `/`.)
   (en Windows las comillas son necesarias si la ruta tiene espacios; en Mac/Linux usar `/` y `:`). Sin esto, al ejecutar desde el IDE aparece `Error: JavaFX runtime components are missing`.

> Verificá además que el **Project SDK** del IDE sea un JDK 21+ (`Project Structure → Project`). La config del IDE es local (`.idea/`), no se comparte por git.

---

## 2. Arquitectura y paquetes (`src/videojuego/`)

| Paquete | Clases | Rol |
|---|---|---|
| `modelo` | `Partido`, `Equipo`, `Jugador`, `Estadio`, `EventoDeportivo`, `TipoEvento` (enum), `EstadoJugador` (enum) | Dominio |
| `fachada` | `ControladorPartido` | **Facade** — orquesta builder + motor + persistencia |
| `builder` | `IBuilder<T>`, `PartidoBuilder` | **Builder** — arma `Partido` con observadores |
| `estado` | `IEstadoPartido`, `EstadoPrimerTiempo`, `EstadoEntretiempo`, `EstadoSegundoTiempo`, `EstadoFinalizado` | **State** |
| `tactica` | `ITactica`, `TacticaOfensiva`, `TacticaDefensiva`, `TacticaEquilibrada` | **Strategy** |
| `observador` | `IObservadorPartido`, `Marcador`, `Estadisticas`, `RelatoDeportivo` | **Observer** |
| `simulacion` | `MotorSimulacion` | Genera eventos por tramo (ruleta probabilística) |
| `persistencia` | `ConexionDB` (**Singleton**, posee la `Connection` JDBC), `PartidoDATA`, `EquipoDATA`, `JugadorDATA`, `EstadioDATA` (**DAO**) | **Persistencia en SQLite/JDBC** (ver sección 10) |
| `ui` | `MainApp`, `Navegador` (interfaz + enum `Pantalla`), `ControladorMenuPrincipal`, `ControladorConfigurarPartido`, `ControladorSimulacion`, `ControladorGestionEquipos`, `ControladorHistorial` | **GUI JavaFX en construcción** (ver sección 1.b). Esqueleto de navegación funcionando; vistas mínimas sin lógica. Toda la lógica pasará por `ControladorPartido`. |

> El paquete `model` con `Clase.java` (leftover) **ya fue eliminado** (B8).

---

## 3. Flujo de simulación (lo que SÍ funciona)

1. **`ControladorPartido.configurarPartido(local, visitante, estadio, modoJuego)`** usa `PartidoBuilder` fluido (`setEquipoLocal/Visitante/Estadio/ModoJuego`, `conMarcador/conEstadisticas/conRelato`). El `build()` crea el `Partido` y le suscribe los 3 observadores. El controlador **retiene el builder** para poder consultar marcador/estadísticas después (fix B1). Además persiste ambos equipos y sus jugadores (Tarea 2).
2. El `Partido` nace en `EstadoPrimerTiempo` y su constructor **ya llama `iniciar(this)`** (fix B2), por lo que se imprime "=== Comienza el Primer Tiempo ===".
3. **`simularTramo()`**: si el estado `permiteSimular()`, llama `MotorSimulacion.simularTramo(partido)` y luego `avanzarEstado()`.
   - Motor genera 3–8 eventos por tramo. Por cada uno: elige atacante/defensor al azar, `determinarEvento()` tira `random.nextDouble()` contra las probabilidades de las tácticas (las lee **en tiempo real**, evento por evento) y devuelve un `TipoEvento` (GOL, PENAL, FALTA, TARJETA_AMARILLA, TARJETA_ROJA, LESION).
   - Faltas/tarjetas se asignan a un jugador **DISPONIBLE** del defensor; el resto al atacante. Si no hay disponibles, saltea el evento.
   - `partido.registrarEvento(...)` agrega al historial y notifica observadores.
4. **Entretiempo**: `permiteSimular()==false` → solo `avanzarEstado()` a 2do tiempo (sin simular).
5. **2do tiempo**: simula con `minutoBase` ya +45, `avanzarEstado()` → `EstadoFinalizado`.
6. Al detectar estado "Finalizado", `ControladorPartido` llama `new PartidoDATA().guardar(partido)`.

**Observadores:**
- `Marcador`: cuenta goles local/visitante (GOL y PENAL suman gol) y marca `jugador.marcarGol()`. Tiene `getResultado()`. **Accesible** vía `ControladorPartido.getMarcador()` (fix B1).
- `Estadisticas`: cuenta faltas/amarillas/rojas/penales/lesiones y aplica efectos al jugador (`recibirTarjetaAmarilla` → 2 amarillas = SUSPENDIDO; `recibirTarjetaRoja` → SUSPENDIDO; `lesionar` → LESIONADO). Tiene `getResumen()`. **Accesible** vía `ControladorPartido.getEstadisticas()` (fix B1).
- `RelatoDeportivo`: arma una línea de texto por evento, la guarda en lista y la imprime con `System.out.println`.

**Tácticas (probabilidades):**
| Táctica | Gol | Falta | Lesión |
|---|---|---|---|
| Ofensiva | 0.35 | 0.30 | 0.20 |
| Defensiva | 0.15 | 0.40 | 0.10 |
| Equilibrada | 0.25 | 0.30 | 0.15 |

---

## 4. Alcance definido (README) vs estado real

Leyenda: ✅ hecho · 🟡 parcial · ❌ falta

| # | Requisito del alcance (README "Incluye") | Estado | Detalle |
|---|---|---|---|
| 1 | Registro y administración de equipos, jugadores y estadios | ✅ | El menú permite **alta y consulta** de equipos, jugadores y estadios vía la fachada (`registrar/obtener...`). Baja/edición existen en los DAOs (`eliminar`) pero no se expusieron en el menú (no pedido explícitamente). |
| 2 | Configuración de partido: elegir equipos, táctica inicial y modo (amistoso/torneo) | ✅ | El submenú "Configurar y jugar" deja elegir local/visitante/estadio, táctica inicial de cada equipo y modo. El `modoJuego` se elige y se guarda, pero **aún no altera la lógica** (B4, opcional). |
| 3 | Simulación representativa por tramos | ✅ | Motor + State funcionan. |
| 4 | Cambio de táctica DURANTE el partido desde la interfaz | ✅ | El menú de **entretiempo** invoca `cambiarTactica(Equipo, ITactica)` (con guarda de partido finalizado); el motor lee la táctica en tiempo real, así que impacta en el 2do tiempo. |
| 5 | Visualización en tiempo real de marcador, relato y estadísticas | ✅ | El **relato** se imprime evento por evento; el menú muestra **marcador** (`getMarcador()`) y **estadísticas** (`getEstadisticas()`) al cierre del 1er tiempo y al final. |
| 6 | Persistencia de equipos, jugadores y resultados | ✅ | Se persisten partidos, equipos, jugadores y estadios (en memoria, Singleton). Se pierde al cerrar — aceptado por consigna. |
| 7 | Consulta de historial | ✅ | El menú llama `mostrarHistorial()` (sobre `obtenerHistorial()`/`getResumenTexto()`): lista cada partido con resultado, total de eventos y estado final. |

### Requisitos extra pedidos explícitamente
- ✅ **Elegir los equipos** — el menú lista los registrados y permite elegir local y visitante.
- ✅ **Elegir las tácticas en tiempo real** — el menú de entretiempo dispara `cambiarTactica()` por equipo; el motor la aplica de inmediato.
- 🟡 **Elegir tipo de partido: amistoso o torneo** — ya se elige en el menú (1=Amistoso, 2=Torneo), pero `modoJuego` sigue sin comportamiento diferenciado (B4, opcional).

---

## 5. Bugs e inconsistencias

### Resueltos
- ✅ **B1 — Marcador/Estadísticas inaccesibles.** `PartidoBuilder` ahora retiene referencias privadas a `marcador`/`estadisticas` y las expone con `getMarcador()`/`getEstadisticas()`. `ControladorPartido` retiene el builder y expone los mismos getters; `getResultado()` devuelve `marcador.getResultado()` real (ya no es un TODO).
- ✅ **B2 — `iniciar()` del primer tiempo nunca corría.** El constructor de `Partido` ahora llama `estadoActual.iniciar(this)` tras asignar el estado.
- ✅ **B3 — `iniciarPartido()` código muerto/peligroso.** Convertido en no-op con comentario explicativo (ya no llama `avanzarEstado()`).
- ✅ **B8 — `model/Clase.java`.** Eliminado (junto con el paquete `model` vacío).
- ✅ **B5 — Sin punto de interacción entre tramos.** Resuelto por el menú de **entretiempo** del `Main` interactivo: tras el 1er tiempo se ofrece cambiar tácticas o continuar antes de simular el 2do tiempo.

### Pendientes
- **B4 — `modoJuego` (amistoso/torneo) no tiene comportamiento.** Es un String que se elige y se guarda pero no afecta nada. Un "torneo" real implicaría fixture/tabla/llaves; hoy no existe.
- **B6 — Drift de documentación.** El README nombra `ConstructorPartido` e `IObservador`, pero el código usa `PartidoBuilder` e `IObservadorPartido`. Unificar nombres.
- **B7 — Capa `ui` y `MainApp` vacías.** Los 5 controladores `ui` son clases vacías y `MainApp` está comentado (JavaFX). Decisión tomada: **el enfoque es consola** (menú en `Main`, Tarea 6); los stubs pueden borrarse más adelante.
- **B9 — Default silencioso en `determinarEvento()`.** Si no cae en ningún rango devuelve `FALTA`, lo que puede sesgar el conteo. Revisar si es intencional.

---

## 6. Patrones aplicados (para la defensa del TPO)
- **Facade**: `ControladorPartido`.
- **Builder**: `IBuilder<T>` + `PartidoBuilder`.
- **State**: `IEstadoPartido` + 4 estados.
- **Strategy**: `ITactica` + 3 tácticas.
- **Observer**: `IObservadorPartido` + `Marcador`/`Estadisticas`/`RelatoDeportivo`.
- **Singleton + DAO**: `ConexionDB` + `PartidoDATA`/`EquipoDATA`/`JugadorDATA`/`EstadioDATA`.

---

## 7. Qué falta construir (resumen accionable)

**Las 6 tareas del plan están completas** y verificadas. El simulador se usa de punta a punta desde el menú de consola (`Main` → `ControladorPartido`). Solo quedan ítems **opcionales**, ninguno exigido por la consigna:

1. **B4 — amistoso vs torneo con comportamiento.** Hoy `modoJuego` se elige y se guarda, pero no cambia la lógica. Un "torneo" real implicaría fixture/tabla/llaves.
2. **B6 — Sincronizar el README.** Nombra `ConstructorPartido`/`IObservador`; el código usa `PartidoBuilder`/`IObservadorPartido`.
3. **B7 — Stubs `ui`.** Los 5 controladores JavaFX y `MainApp` siguen vacíos; al haberse elegido el enfoque consola, pueden borrarse.
4. **B9 — Default `FALTA` en `determinarEvento()`.** Revisar si el fallback sesga el conteo de eventos.

---

## 8. Convenciones del proyecto (importante para no romper la estructura)
- Java 21. **Dependencias externas, ambas versionadas en `lib/`** (proyecto self-contained): (1) el driver `sqlite-jdbc` — Java puro, multiplataforma, un solo `.jar`; y (2) el **JavaFX 21 SDK** — con builds por SO en `lib/javafx-sdk-21-win/` (Windows x64) y `lib/javafx-sdk-21-mac/` (macOS aarch64), porque trae binarios nativos por sistema operativo (ver sección 1.b). Persistencia en **SQLite** (`simulador.db`), vía el Singleton `ConexionDB`.
- **DAOs aislados**: la migración a SQLite/JDBC solo reescribió los cuerpos de cada `*DATA` y de `ConexionDB`, sin tocar el resto del sistema. Nadie fuera de `persistencia` sabe cómo se almacena (modelo, fachada, observadores y `Main` quedaron intactos).
- **Código nuevo sin Streams** (usar `for`/`while`). Los Streams ya existentes en DAOs/motor se dejan como están salvo pedido explícito.
- Comentarios en español. Mantener nombres de paquetes/clases existentes.
- Las features nuevas deben encajar en los patrones existentes (Facade/Builder/State/Strategy/Observer/DAO).
- El objetivo del TPO es demostrar patrones de diseño, no realismo del fútbol.

---

## 9. Registro de progreso (plan de tareas)

| Tarea | Descripción | Estado |
|---|---|---|
| 1 | Corregir bugs críticos (B1, B2, B3) + limpieza (B8) | ✅ Hecho y verificado (compila + `Main` corre) |
| 2 | Activar DAOs de Equipo y Jugador (unificar `guardar()`, cablear en fachada, métodos `registrar/obtener`) | ✅ Hecho y verificado (test ad-hoc: 3 equipos / 5 jugadores) |
| 3 | Agregar `EstadioDATA` (+ lista en `ConexionDB`, métodos en fachada) | ✅ Hecho y verificado (test ad-hoc: dedup + `buscarPorNombre`) |
| 4 | Cambio de táctica en tiempo real (`cambiarTactica` en fachada con guarda de finalizado) | ✅ Hecho y verificado (compila; guarda lanza `IllegalStateException` si finalizado) |
| 5 | Mejorar historial (`mostrarHistorial`/`obtenerHistorial`/`getResumenTexto`) | ✅ Hecho y verificado (compila; resultado contado desde eventos GOL/PENAL) |
| 6 | Menú de consola interactivo (reemplazar `Main`) — **Fase 3 (última)** | ✅ Hecho y verificado (compila + corrida end-to-end por pipe: gestión → configurar → jugar con cambio de táctica en entretiempo → historial) |

> **Plan completo.** Las 6 tareas (Fases 1-3) están terminadas. Lo único restante son los 4 ítems opcionales de la sección 7.

---

## 10. Migración de persistencia a SQLite/JDBC

Se reemplazó la persistencia en memoria por **SQLite vía JDBC**, tocando **solo** el paquete `persistencia` (regla de oro cumplida).

### Driver (P1)
- `lib/sqlite-jdbc-3.42.0.0.jar`, versionado en el repo (sin Maven/Gradle).
- **Por qué 3.42.0.0 y no 3.45.x:** desde la 3.43.0.0 `sqlite-jdbc` declara `slf4j-api` como dependencia; con jars sueltos (sin Maven que la resuelva) la 3.45.x falla en runtime con `NoClassDefFoundError: org/slf4j/LoggerFactory`. La 3.42.0.0 es autocontenida (un solo jar, sin warnings).

### `ConexionDB` (P2)
- Singleton: abre `DriverManager.getConnection("jdbc:sqlite:simulador.db")` la primera vez y crea las 5 tablas (`CREATE TABLE IF NOT EXISTS`: `estadios`, `equipos`, `jugadores`, `partidos`, `eventos`).
- Expone `getConexion()` (la `Connection`). Si la conexión falla, lanza `RuntimeException` (error visible).

### DAOs (P3) — `PreparedStatement` en todas las queries
- **`EstadioDATA` / `JugadorDATA`**: upsert por nombre, `obtenerTodos`/`buscarPorNombre`/`eliminar` con JDBC.
- **`EquipoDATA`**: guarda táctica como `getClass().getSimpleName()` y la mapea de vuelta al reconstruir. `guardar()` **también persiste la plantilla** (cada jugador con su `equipo_nombre`).
- **`PartidoDATA`**: `guardar()` inserta el partido (`Statement.RETURN_GENERATED_KEYS`) y luego sus eventos. `obtenerTodos()` reconstruye cada `Partido` con sus eventos para que `getResumenTexto()` y el estado funcionen.

### Decisiones de diseño que NO eran obvias
1. **Caché de identidad en `EquipoDATA`** (`static Map<String,Equipo>`). Con listas en memoria, `obtenerEquipos()` devolvía siempre la misma instancia, así que los jugadores agregados desde el menú quedaban "pegados" al equipo. Como `Jugador` no conoce a su equipo y `registrarJugador(jugador)` no recibe el equipo, reconstruir objetos nuevos en cada lectura habría dejado las plantillas vacías (y los partidos 0-0). La caché preserva la identidad dentro de la sesión; SQLite es el almacén durable. **No se pudo evitar sin tocar `Main`/fachada.**
2. **Asociación jugador→equipo** la persiste `EquipoDATA.guardar()` (que tiene el equipo y su plantilla), no `JugadorDATA.guardar()` (que solo recibe el jugador y deja `equipo_nombre` en NULL / sin pisar).
3. **Silenciado de `System.out`** durante la reconstrucción de partidos en `PartidoDATA`: el constructor de `Partido` y `setEstado()` imprimen mensajes de ciclo de vida; se silencia temporalmente (try/finally, contenido en `persistencia`) para no ensuciar `mostrarHistorial()`.

### Limitaciones aceptadas
- El **número de camiseta** no se persiste (la tabla `jugadores` no lo tiene); al reconstruir se asigna secuencial.
- `equipos.estadio_nombre` queda **NULL**: el modelo `Equipo` no referencia un `Estadio`.
- Un jugador agregado por menú que **nunca** llega a jugar/configurar no graba su `equipo_nombre` hasta el próximo `guardar()` del equipo (en sesión sí se ve por la caché).
- La táctica persistida es la del momento de `guardar()`; cambios de táctica en el entretiempo no se regraban (no hay `guardar()` posterior).

### Verificación
- **Compila OK** con el driver en el classpath.
- **Run 1** (BD limpia): registrar equipos/jugadores → configurar → jugar con cambio de táctica en entretiempo → guardar → historial. OK (exit 0).
- **Run 2** (JVM nuevo, caché estática vacía, misma `simulador.db`): lee equipos con táctica y plantilla, plantilla de Argentina (Messi, DiMaria) e historial (`Argentina 0 - 1 Francia | Eventos: 7 | Estado: Finalizado`). **La persistencia sobrevive al reinicio.**
