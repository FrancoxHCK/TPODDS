# Handoff — Simulador de Partidos de Fútbol (TPO Patrones de Diseño)

Documento para retomar el proyecto en otra sesión. Describe el **estado real del código** (no el deseado), los patrones aplicados, y el **gap entre lo implementado y el alcance definido**. Java puro, sin dependencias externas, persistencia en memoria.

> **Última actualización:** se completaron las **Tareas 1, 2 y 3** del plan (ver sección 9). Resumen: cerrados los bugs B1, B2, B3 y B8; activados los DAOs de Equipo y Jugador; creado el DAO de Estadio. Pendientes: Tareas 4, 5 y 6.

---

## 1. Cómo se ejecuta hoy

- **Todavía no hay menú interactivo.** El único punto de entrada funcional sigue siendo `src/Main.java`, una demo de consola con datos **hardcodeados** (el menú con `Scanner` es la Tarea 6, pendiente).
- Compilar/ejecutar (zsh, el glob recursivo `**` no siempre funciona, usar `find`):
  ```bash
  rm -rf bin && mkdir bin
  find src -name "*.java" > sources.txt
  javac -d bin @sources.txt
  java -cp bin Main
  ```
- `Main` hace: crea 6 jugadores y 2 equipos (Argentina ofensiva / Francia defensiva), crea un estadio, configura el partido vía `ControladorPartido`, y llama `simularTramo()` 3 veces (1er tiempo → entretiempo → 2do tiempo). Al finalizar persiste el partido y muestra el tamaño del historial.
- **Novedad (Tarea 2):** al llamar `configurarPartido(...)`, ahora ambos equipos y todos sus jugadores quedan **persistidos** automáticamente en `ConexionDB` vía sus DAOs.

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
| 1 | Registro y administración de equipos, jugadores y estadios | 🟡 | DAOs de `Equipo`/`Jugador`/`Estadio` existen y **ya se invocan** desde la fachada (`registrar/obtener...`, persistencia en `configurarPartido`). Falta la **UI de alta/baja** (Tarea 6); hoy los datos siguen viniendo hardcodeados de `Main`. |
| 2 | Configuración de partido: elegir equipos, táctica inicial y modo (amistoso/torneo) | 🟡 | El builder y la fachada aceptan los datos, pero el usuario aún no elige nada (falta menú). `modoJuego` es un `String` sin efecto en la lógica (B4). |
| 3 | Simulación representativa por tramos | ✅ | Motor + State funcionan. |
| 4 | Cambio de táctica DURANTE el partido desde la interfaz | 🟡 | El motor ya lee la táctica en tiempo real, pero **falta `cambiarTactica(equipo, tactica)` en `ControladorPartido`** y el punto de interacción entre tramos (Tarea 4 + Tarea 6). |
| 5 | Visualización en tiempo real de marcador, relato y estadísticas | 🟡 | El **relato** se imprime. `Marcador` y `Estadisticas` ahora **son accesibles** (`getMarcador()`/`getEstadisticas()`, fix B1), pero falta el menú que los muestre entre tramos (Tarea 5/6). |
| 6 | Persistencia de equipos, jugadores y resultados | ✅ | Se persisten partidos, equipos, jugadores y estadios (en memoria, Singleton). Se pierde al cerrar — aceptado por consigna. |
| 7 | Consulta de historial | 🟡 | `PartidoDATA.obtenerTodos()`/`buscarPorEquipo()` funcionan, pero `Main` solo imprime el `.size()`. Falta `mostrarHistorial()`/`obtenerHistorial()`/`getResumenTexto()` (Tarea 5). |

### Requisitos extra pedidos explícitamente
- 🟡 **Elegir los equipos** — la fachada ya lo soporta; falta el menú (Tarea 6).
- 🟡 **Elegir las tácticas en tiempo real** — falta `cambiarTactica` en fachada (Tarea 4) + menú.
- ❌ **Elegir tipo de partido: amistoso o torneo** — se podrá elegir en el menú, pero `modoJuego` sigue sin comportamiento (B4).

---

## 5. Bugs e inconsistencias

### Resueltos
- ✅ **B1 — Marcador/Estadísticas inaccesibles.** `PartidoBuilder` ahora retiene referencias privadas a `marcador`/`estadisticas` y las expone con `getMarcador()`/`getEstadisticas()`. `ControladorPartido` retiene el builder y expone los mismos getters; `getResultado()` devuelve `marcador.getResultado()` real (ya no es un TODO).
- ✅ **B2 — `iniciar()` del primer tiempo nunca corría.** El constructor de `Partido` ahora llama `estadoActual.iniciar(this)` tras asignar el estado.
- ✅ **B3 — `iniciarPartido()` código muerto/peligroso.** Convertido en no-op con comentario explicativo (ya no llama `avanzarEstado()`).
- ✅ **B8 — `model/Clase.java`.** Eliminado (junto con el paquete `model` vacío).

### Pendientes
- **B4 — `modoJuego` (amistoso/torneo) no tiene comportamiento.** Es un String que se guarda y no afecta nada. Un "torneo" real implicaría fixture/tabla/llaves; hoy no existe.
- **B5 — Sin punto de interacción entre tramos.** `Main` encadena los `simularTramo()` sin pausa. La arquitectura lo permite; lo resolverá el menú (Tarea 6) con su entretiempo interactivo.
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

1. **Tarea 4 — Cambio de táctica en tiempo real**: agregar `cambiarTactica(Equipo, ITactica)` en `ControladorPartido`, lanzando error si el partido está `EstadoFinalizado`. (El motor ya lee la táctica en vivo.)
2. **Tarea 5 — Historial con detalle**: `mostrarHistorial()`, `obtenerHistorial()` en la fachada y `getResumenTexto()` en `Partido` (equipos, resultado contado desde los eventos GOL/PENAL, cantidad de eventos, estado final).
3. **Tarea 6 — Menú de consola interactivo**: reemplazar `Main` por un menú con `Scanner` (gestión de equipos/jugadores, configurar y jugar partido con entretiempo interactivo, ver historial). Única fachada = `ControladorPartido`.
4. **(Opcional / fuera de las 6 tareas)** Dar significado a amistoso vs torneo (B4); sincronizar README (B6); decidir si se borran los stubs `ui` (B7); revisar default `FALTA` (B9).

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
| 4 | Cambio de táctica en tiempo real (`cambiarTactica` en fachada con guarda de finalizado) | ⬜ Pendiente |
| 5 | Mejorar historial (`mostrarHistorial`/`obtenerHistorial`/`getResumenTexto`) | ⬜ Pendiente |
| 6 | Menú de consola interactivo (reemplazar `Main`) | ⬜ Pendiente |
