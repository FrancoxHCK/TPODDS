# Handoff — Simulador de Partidos de Fútbol (TPO Patrones de Diseño)

Documento para retomar el proyecto en otra sesión. Describe el **estado real del código** (no el deseado), los patrones aplicados, y el **gap entre lo implementado y el alcance definido**. Java puro, sin dependencias externas, persistencia en memoria.

---

## 1. Cómo se ejecuta hoy

- **No hay interfaz de usuario.** El único punto de entrada funcional es `src/Main.java`, una demo de consola con datos **hardcodeados**.
- Compilar/ejecutar: `javac -d bin src/**/*.java` y `java -cp bin Main`.
- `Main` hace: crea 6 jugadores y 2 equipos (Argentina ofensiva / Francia defensiva), crea un estadio, configura el partido vía `ControladorPartido`, y llama `simularTramo()` 3 veces (1er tiempo → entretiempo → 2do tiempo). Al finalizar persiste el partido y muestra el tamaño del historial.

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
| `persistencia` | `ConexionDB` (**Singleton**), `PartidoDATA`, `EquipoDATA`, `JugadorDATA` (**DAO**) | Persistencia en memoria |
| `ui` | `ControladorMenuPrincipal`, `ControladorConfigurarPartido`, `ControladorSimulacion`, `ControladorGestionEquipos`, `ControladorHistorial`, `MainApp` | **TODO STUBS VACÍOS** |
| `model` (suelto) | `Clase` | Resto sin uso, borrar |

---

## 3. Flujo de simulación (lo que SÍ funciona)

1. **`ControladorPartido.configurarPartido(local, visitante, estadio, modoJuego)`** usa `PartidoBuilder` fluido (`setEquipoLocal/Visitante/Estadio/ModoJuego`, `conMarcador/conEstadisticas/conRelato`). El `build()` crea el `Partido` y le suscribe los 3 observadores.
2. El `Partido` nace en `EstadoPrimerTiempo`.
3. **`simularTramo()`**: si el estado `permiteSimular()`, llama `MotorSimulacion.simularTramo(partido)` y luego `avanzarEstado()`.
   - Motor genera 3–8 eventos por tramo. Por cada uno: elige atacante/defensor al azar, `determinarEvento()` tira `random.nextDouble()` contra las probabilidades de las tácticas y devuelve un `TipoEvento` (GOL, PENAL, FALTA, TARJETA_AMARILLA, TARJETA_ROJA, LESION).
   - Faltas/tarjetas se asignan a un jugador **DISPONIBLE** del defensor; el resto al atacante. Si no hay disponibles, saltea el evento.
   - `partido.registrarEvento(...)` agrega al historial y notifica observadores.
4. **Entretiempo**: `permiteSimular()==false` → solo `avanzarEstado()` a 2do tiempo (sin simular).
5. **2do tiempo**: simula con `minutoBase` ya +45, `avanzarEstado()` → `EstadoFinalizado`.
6. Al detectar estado "Finalizado", `ControladorPartido` llama `new PartidoDATA().guardar(partido)`.

**Observadores:**
- `Marcador`: cuenta goles local/visitante (GOL y PENAL suman gol) y marca `jugador.marcarGol()`. Tiene `getResultado()`.
- `Estadisticas`: cuenta faltas/amarillas/rojas/penales/lesiones y aplica efectos al jugador (`recibirTarjetaAmarilla` → 2 amarillas = SUSPENDIDO; `recibirTarjetaRoja` → SUSPENDIDO; `lesionar` → LESIONADO). Tiene `getResumen()`.
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
| 1 | Registro y administración de equipos, jugadores y estadios | 🟡 | `EquipoDATA`/`JugadorDATA` existen con CRUD, pero **nunca se invocan** en ningún flujo. Equipos/jugadores se crean hardcodeados en `Main`. **`Estadio` no tiene DAO.** No hay alta/baja/edición real. |
| 2 | Configuración de partido: elegir equipos, táctica inicial y modo (amistoso/torneo) | 🟡 | El builder acepta los datos, pero todo viene hardcodeado de `Main`. El usuario no elige nada. `modoJuego` es un `String` sin efecto en la lógica. |
| 3 | Simulación representativa por tramos | ✅ | Motor + State funcionan. |
| 4 | Cambio de táctica DURANTE el partido desde la interfaz | ❌ | `Equipo.cambiarTactica()` existe pero **nadie la llama en medio del partido**. No hay método en `ControladorPartido` ni punto de interacción entre tramos. |
| 5 | Visualización en tiempo real de marcador, relato y estadísticas | 🟡 | Solo el **relato** se ve (imprime a consola). `Marcador` y `Estadisticas` calculan internamente pero **no se muestran**: el builder crea los observadores y **descarta las referencias**, así que nadie puede consultar `getResultado()`/`getResumen()`. Ver bug B1. |
| 6 | Persistencia de equipos, jugadores y resultados | 🟡 | Solo se persisten **partidos**. Equipos/jugadores nunca se guardan (DAOs sin uso). Es en memoria (Singleton), se pierde al cerrar — aceptado por consigna. |
| 7 | Consulta de historial | 🟡 | `PartidoDATA.obtenerTodos()` / `buscarPorEquipo()` funcionan, pero `Main` solo imprime el `.size()`. No hay vista de detalle. |

### Requisitos extra pedidos explícitamente (en `src/Estado actual`)
- ❌ **Elegir los equipos** (hoy hardcode).
- ❌ **Elegir las tácticas en tiempo real** (que no se asignen automáticamente).
- ❌ **Elegir tipo de partido: amistoso o torneo.**

---

## 5. Bugs e inconsistencias detectados

- **B1 — Marcador/Estadísticas inaccesibles.** `PartidoBuilder.build()` hace `partido.agregarObservador(new Marcador(...))` sin guardar la referencia. `Partido` tampoco expone su lista de observadores. Resultado: imposible leer el resultado final o las estadísticas. `ControladorPartido.getResultado()` es un TODO que solo devuelve el nombre del estado. **Hay que exponer estos observadores** (que el builder/controlador los retenga, o que `Partido` permita buscarlos por tipo).
- **B2 — `iniciar()` del primer tiempo nunca corre.** El constructor de `Partido` hace `estadoActual = new EstadoPrimerTiempo()` directo, sin llamar `iniciar()`. Por eso el mensaje "=== Comienza el Primer Tiempo ===" no se imprime; el primer `iniciar()` que se ejecuta es el del entretiempo. Inconsistente con los demás estados.
- **B3 — `iniciarPartido()` es código muerto/peligroso.** `ControladorPartido.iniciarPartido()` hace `avanzarEstado()`; si se llamara, saltearía el primer tiempo. No se usa.
- **B4 — `modoJuego` (amistoso/torneo) no tiene comportamiento.** Es un String que se guarda y no afecta nada. Un "torneo" real implicaría fixture/tabla/llaves; hoy no existe.
- **B5 — Sin punto de interacción entre tramos.** `Main` encadena los `simularTramo()` sin pausa, por eso no se puede cambiar táctica ni mostrar estado intermedio. La arquitectura lo permite; falta el controlador/UI que lo orqueste.
- **B6 — Drift de documentación.** El README nombra `ConstructorPartido` e `IObservador`, pero el código usa `PartidoBuilder` e `IObservadorPartido`. Unificar nombres.
- **B7 — Capa `ui` y `MainApp` vacías.** Los 5 controladores `ui` son clases vacías y `MainApp` está todo comentado (JavaFX comentado). No hay decisión tomada sobre consola vs JavaFX vs Swing.
- **B8 — `model/Clase.java`** es un leftover sin uso (paquete `model`). Borrar.
- **B9 — Default silencioso en `determinarEvento()`.** Si no cae en ningún rango devuelve `FALTA`, lo que puede sesgar el conteo. Revisar si es intencional.

---

## 6. Patrones aplicados (para la defensa del TPO)
- **Facade**: `ControladorPartido`.
- **Builder**: `IBuilder<T>` + `PartidoBuilder`.
- **State**: `IEstadoPartido` + 4 estados.
- **Strategy**: `ITactica` + 3 tácticas.
- **Observer**: `IObservadorPartido` + `Marcador`/`Estadisticas`/`RelatoDeportivo`.
- **Singleton + DAO**: `ConexionDB` + `*DATA`.

---

## 7. Qué falta construir (resumen accionable)

1. **Decidir la UI** (consola interactiva con `Scanner` es lo más rápido y suficiente para la consigna; JavaFX si se quiere gráfico). Implementar los controladores `ui` o un menú de consola.
2. **Flujo de configuración interactivo**: elegir/crear equipos y jugadores, elegir táctica inicial por equipo, elegir modo amistoso/torneo. Usar `EquipoDATA`/`JugadorDATA` para persistirlos.
3. **Exponer Marcador y Estadísticas** (resolver B1) y mostrarlos en vivo entre tramos.
4. **Cambio de táctica en tiempo real**: agregar `cambiarTactica(equipo, tactica)` en `ControladorPartido` y un punto de interacción entre tramos (resolver B4/B5).
5. **Dar significado a amistoso vs torneo** (mínimo: distinguir, idealmente fixture/tabla simple para torneo).
6. **Vista de historial** con detalle (resultado, eventos) en vez de solo el tamaño.
7. **Limpieza**: borrar `model/Clase.java`, decidir sobre `MainApp`, corregir B2/B3, sincronizar README (B6).

---

## 8. Datos para el otro chat
- Java sin frameworks. Persistencia en memoria intencional (sin BD real).
- Mantener los patrones existentes; las nuevas features deben encajar en Facade/Builder/State/Strategy/Observer/DAO.
- El objetivo del TPO es demostrar patrones de diseño, no realismo del fútbol.