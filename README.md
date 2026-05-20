# ⚽ Football Simulation Game

Sistema de gestión y simulación de partidos de fútbol implementado en Java,
desarrollado como Trabajo Práctico Obligatorio para la materia de Patrones de Diseño.

---

## 👥 Integrantes

| Nombre | GitHub |
|--------|--------|
| Agustin Perez Leal    | ...    |
| Pedro Smith    | ...    |
| Facundo Martinez Zorzi   | ...    |
| Franco Brunello Mioni Bonucelli    | ...    |

---

## 📋 Descripción

Sistema que permite registrar equipos y jugadores, configurar partidos,
simular eventos deportivos (goles, faltas, lesiones, penales) y consultar
estadísticas y relato en tiempo real. La simulación es representativa:
un motor genera eventos con lógica probabilística y los propaga a los
módulos del sistema automáticamente.

## Alcance del sistema

Incluye:

-Registro y administración de equipos, jugadores y estadios
-Configuración de partidos con selección de equipos, táctica inicial y modo de juego (amistoso / torneo)
-Simulación representativa de un partido completo con generación de eventos por tramos (primer tiempo, entretiempo, segundo tiempo)
-Cambio de táctica durante el partido desde la interfaz
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

```bash
# Clonar el repositorio
git clone https://github.com/...

# Compilar
javac -d bin src/**/*.java

# Ejecutar
java -cp bin Main
```

---

## 🧩 Patrones de diseño aplicados

### Creacionales
- **Builder** — `ConstructorPartido` construye un `Partido` paso a paso
  (equipos, estadio, duración, modo de juego), evitando un constructor
  con demasiados parámetros.

### Estructurales
- **Facade** — `ControladorPartido` actúa como interfaz unificada que
  orquesta el motor de simulación, el marcador y las estadísticas.
  El `Main` solo interactúa con esta fachada.

### Comportamiento
- **Strategy** — `ITactica` define un algoritmo intercambiable en tiempo
  de ejecución. Implementaciones: `TacticaOfensiva`, `TacticaDefensiva`,
  `TacticaEquilibrada`. Cada una define probabilidades distintas de eventos.
- **Observer** — El `MotorSimulacion` publica cada evento generado.
  `Marcador`, `Estadisticas` y `RelatoDeportivo` están suscritos y
  reaccionan automáticamente sin que el motor los conozca directamente.
- **State** — El `Partido` transita por estados definidos:
  `PrimerTiempo` → `Entretiempo` → `SegundoTiempo` → `Finalizado`.
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
  la interfaz `IObservador`, no de las implementaciones concretas
  `Marcador` o `RelatoDeportivo`.

---

## 🔶 Patrones GRASP aplicados

- **Creator:** `ConstructorPartido` crea instancias de `Partido`
  porque contiene toda la información necesaria para construirlo.
- **Information Expert:** `Marcador` es el experto en el resultado del
  partido; es quien sabe cuántos goles hay y quién va ganando.
- **Low Coupling:** Observer desacopla el motor de los módulos de salida;
  el motor no conoce quiénes están suscritos ni cuántos son.

---

## 📁 Distribución de tareas

| Integrante | Responsabilidad |
|------------|-----------------|
| ...        | ...             |
| ...        | ...             |
| ...        | ...             |
| ...        | ...             |
