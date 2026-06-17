# ⚽ Juego de Simulación de Futbol

Sistema de gestión y simulación de partidos de fútbol implementado en Java,
desarrollado como Trabajo Práctico Obligatorio para la materia de Patrones de Diseño.

---

## 👥 Integrantes

| Nombre | GitHub |
|--------|--------|
| Agustin Perez Leal    | https://github.com/aguusz5    |
| Pedro Smith    | https://github.com/pedrosmith17    |
| Facundo Martinez Zorzi   | https://github.com/Facumartinezz    |
| Franco Brunello Mioni Bonucelli    | https://github.com/FrancoxHCK    |

---

## 📋 Descripción

Sistema que permite registrar equipos y jugadores, configurar partidos,
simular eventos deportivos (goles, faltas, lesiones, penales con resultado
convertido/fallado) y consultar estadísticas y relato en tiempo real.
La simulación es representativa: un motor genera eventos con lógica
probabilística ponderada por la posición del jugador (Delantero,
Mediocampista, Defensor, Arquero) y la táctica del equipo, propagando cada
evento a los módulos del sistema automáticamente. En modo Torneo, los
empates se definen por tanda de penales (5 remates por equipo + muerte
súbita).

## Alcance del sistema

Incluye:

-Registro y administración de equipos, jugadores y estadios
-Jugadores con posición fija (Delantero, Mediocampista, Defensor, Arquero) que influye en la simulación
-Configuración de partidos con selección de equipos, táctica inicial y modo de juego (amistoso / torneo); el estadio se auto-asigna al elegir el equipo local
-Simulación representativa de un partido completo con generación de eventos por tramos (primer tiempo, entretiempo, segundo tiempo)
-Selección ponderada de jugadores por posición: Delanteros lideran goles, Defensores lideran faltas, Arqueros con mínima participación en ambos
-Cambio de táctica durante el partido desde la interfaz; las tácticas se configuran en el partido (no en la gestión de equipos)
-Visualización del resultado de cada penal (convertido o fallado) en el relato en tiempo real
-Pausa manual del reloj durante la simulación
-En modo Torneo: tanda de penales en caso de empate (5 remates por equipo, luego muerte súbita)
-Historial de partidos con indicador de modo de juego ([Amistoso] / [Torneo]) y resultado de penales cuando aplica
-Visualización en tiempo real de marcador, relato evento por evento y estadísticas del partido
-Persistencia de equipos, jugadores y resultados de partidos en base de datos
-Consulta de historial de partidos jugados

No incluye:

-Renderizado gráfico del campo de juego ni animaciones
-Inteligencia artificial autónoma por jugador
-Modo carrera, mercado de pases ni gestión económica de clubes
-Integración con APIs externas de datos futbolísticos reales
-Autenticación de usuarios ni roles de acceso

---

## ▶️ Instrucciones para ejecutar

**macOS (Apple Silicon):**
```bash
# Clonar el repositorio
git clone https://github.com/...
cd TPODesSoft

# Compilar (JavaFX + SQLite incluidos en el repo)
find src -name "*.java" > sources.txt
javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls \
      -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt

# Ejecutar la GUI
java --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls \
     -cp "bin:lib/sqlite-jdbc-3.42.0.0.jar" videojuego.ui.MainApp
```

**Windows (PowerShell):**
```powershell
Remove-Item -Recurse -Force bin -ErrorAction SilentlyContinue; New-Item -ItemType Directory bin | Out-Null
Get-ChildItem -Recurse src -Filter *.java | Resolve-Path -Relative | Out-File -Encoding ascii sources.txt
javac --module-path "lib\javafx-sdk-21-win\lib" --add-modules javafx.controls -cp "lib\sqlite-jdbc-3.42.0.0.jar" -d bin "@sources.txt"
java --module-path "lib\javafx-sdk-21-win\lib" --add-modules javafx.controls -cp "bin;lib\sqlite-jdbc-3.42.0.0.jar" videojuego.ui.MainApp
```

> El proyecto es self-contained: no hace falta descargar JavaFX ni SQLite.
> La primera ejecución crea `simulador.db` en la raíz (historial persistente entre sesiones).

---

## 🧩 Patrones de diseño aplicados

### Creacionales
- **Builder** — `PartidoBuilder` construye un `Partido` paso a paso
  (equipos, estadio, duración, modo de juego), evitando un constructor
  con demasiados parámetros.

### Estructurales
- **Facade** — `ControladorPartido` actúa como interfaz unificada que
  orquesta el motor de simulación, el marcador y las estadísticas.
  Toda la capa de interfaz gráfica (UI) interactúa únicamente con esta fachada.

### Comportamiento
- **Strategy** — `ITactica` define un algoritmo intercambiable en tiempo
  de ejecución. Implementaciones: `TacticaOfensiva`, `TacticaDefensiva`,
  `TacticaEquilibrada`. Cada una define probabilidades distintas de eventos.
- **Observer** — El `MotorSimulacion` publica cada evento generado.
  `Marcador`, `Estadisticas` y `RelatoDeportivo` están suscritos y
  reaccionan automáticamente sin que el motor los conozca directamente.
- **State** — El `Partido` transita por estados definidos:
  `PrimerTiempo` → `Entretiempo` → `SegundoTiempo` → `Finalizado`.
  En modo Torneo con empate: `SegundoTiempo` → `Penales` → `Finalizado`.
  Cada estado determina qué acciones son válidas en ese momento.

---

## 🔷 Principios SOLID aplicados

- **S — Responsabilidad única:** `Marcador`, `Estadisticas` y
  `RelatoDeportivo` son clases separadas, cada una con una única
  razón para cambiar.
- **O — Abierto/cerrado:** Agregar una nueva táctica solo requiere
  crear una clase que implemente `ITactica`, sin modificar código
  existente.
- **D — Inversión de dependencias:** El `MotorSimulacion` depende de
  la interfaz `IObservadorPartido`, no de las implementaciones concretas
  `Marcador` o `RelatoDeportivo`.

---

## 🔶 Patrones GRASP aplicados

- **Creator:** `PartidoBuilder` crea instancias de `Partido`
  porque contiene toda la información necesaria para construirlo.
- **Information Expert:** `Marcador` es el experto en el resultado del
  partido; es quien sabe cuántos goles hay y quién va ganando.
- **Low Coupling:** Observer desacopla el motor de los módulos de salida;
  el motor no conoce quiénes están suscritos ni cuántos son.

---


