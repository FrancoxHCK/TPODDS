# ⚽ Football Simulation Game

Sistema de gestión y simulación de partidos de fútbol implementado en Java,
desarrollado como Trabajo Práctico Obligatorio para la materia de Patrones de Diseño.

---

## 👥 Integrantes

| Nombre | GitHub |
|--------|--------|
| Agustin Perez Leal    | ...    |
| ...    | ...    |
| ...    | ...    |
| ...    | ...    |

---

## 📋 Descripción

Sistema que permite registrar equipos y jugadores, configurar partidos,
simular eventos deportivos (goles, faltas, lesiones, penales) y consultar
estadísticas y relato en tiempo real. La simulación es representativa:
un motor genera eventos con lógica probabilística y los propaga a los
módulos del sistema automáticamente.

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
