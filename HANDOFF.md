# Handoff — Simulador de Partidos de Fútbol (TPO Patrones de Diseño)

Documento para retomar el proyecto en otra sesión. Describe el **estado real del código** (no el deseado), los patrones aplicados, y el **gap entre lo implementado y el alcance definido**. Java puro, sin dependencias externas, persistencia en memoria.

> **Última actualización:** se completó la **Tarea 6 = Fase 3 (última)**: `src/Main.java` es ahora un **menú de consola interactivo** (`Scanner`) que reemplaza la demo hardcodeada. Con esto **las 6 tareas del plan están terminadas** y los 7 requisitos del alcance quedan cubiertos (ver secciones 4 y 9). Verificado: **compila OK** + corrida end-to-end (registrar equipos/jugadores → configurar → jugar con cambio de táctica en el entretiempo → ver historial). Único punto de entrada = `Main` operando siempre sobre la fachada `ControladorPartido`. Pendientes únicamente los ítems **opcionales** (B4 amistoso/torneo, B6 README, B7 stubs `ui`, B9 default FALTA).

---

## 1. Cómo se ejecuta hoy

- **`src/Main.java` es un menú de consola interactivo** (`Scanner`), única vía de entrada. Opera todo a través de la fachada `ControladorPartido` (nunca instancia DAOs). Menú principal: 1) Gestión de equipos/jugadores, 2) Configurar y jugar partido, 3) Ver historial, 4) Salir.
- Compilar/ejecutar (zsh, el glob recursivo `**` no siempre funciona, usar `find`):
  ```bash
  rm -rf bin && mkdir bin
  find src -name "*.java" > sources.txt
  javac -d bin @sources.txt
  java -cp bin Main
  ```
- **Flujo del menú:** el usuario registra equipos (cada alta crea además su estadio), agrega jugadores (número de camiseta autoasignado por orden), configura un partido eligiendo local/visitante/estadio/tácticas/modo (con el estado de la selección visible en pantalla), y lo juega por tramos con **entretiempo interactivo** (`=== ENTRETIEMPO ===` → cambiar táctica local/visitante o continuar). Al finalizar el partido se persiste solo y se puede consultar el historial desde el menú.
- Internamente, `configurarPartido(...)` también persiste ambos equipos y todos sus jugadores en `ConexionDB` vía los DAOs (todo encapsulado tras `ControladorPartido`).
- **Recorrido del partido (máquina de estados):** un partido completo son **3** llamadas a `simularTramo()` — 1er tiempo (simula) → entretiempo (avanza sin simular) → 2do tiempo (simula y finaliza/guarda). El menú las encadena: una para el 1er tiempo, y tras el entretiempo dos seguidas para dejar atrás el entretiempo y jugar el 2do tiempo.

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
| `persistencia` | `ConexionDB` (**Singleton**), `PartidoDATA`, `EquipoDATA`, `JugadorDATA`, **`EstadioDATA` (nuevo, Tarea 3)** (**DAO**) | Persistencia en memoria |
| `ui` | `ControladorMenuPrincipal`, `ControladorConfigurarPartido`, `ControladorSimulacion`, `ControladorGestionEquipos`, `ControladorHistorial`, `MainApp` | **STUBS VACÍOS** (placeholders JavaFX comentados; el enfoque elegido es consola, ver Tarea 6) |

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
- Java sin frameworks. Persistencia **en memoria intencional** (sin BD real), vía el Singleton `ConexionDB`.
- **DAOs aislados**: si mañana se migra a SQLite/JDBC, solo se cambian los cuerpos de los métodos de cada `*DATA`, sin tocar el resto del sistema. Nadie fuera de `persistencia` debe saber cómo se almacena.
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
