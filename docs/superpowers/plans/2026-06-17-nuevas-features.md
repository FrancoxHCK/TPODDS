# Nuevas Features TPO — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar 4 features: (1) resultado de penal en el relato, (2) botón de pausa, (3) eliminar táctica de Gestión de Equipos, (4) tanda de penales en modo Torneo con empate.

**Architecture:** Las features son mayormente independientes. La única dependencia es que Task 4 (penales de torneo) reutiliza los tipos `PENAL_CONVERTIDO`/`PENAL_FALLADO` introducidos en Task 1. El orden de ejecución refleja esa dependencia. Toda la lógica de negocio pasa por `ControladorPartido` (Facade). Los estados del partido siguen el patrón State existente. La persistencia usa el Singleton `ConexionDB` + DAOs.

**Tech Stack:** Java 21, JavaFX 21 (sin FXML, vistas por código), SQLite/JDBC (`sqlite-jdbc-3.42.0.0.jar`), sin frameworks de testing (verificación manual corriendo la app).

---

## Mapa de archivos

| Archivo | Acción | Tarea |
|---|---|---|
| `src/videojuego/modelo/TipoEvento.java` | Modificar — agregar `PENAL_CONVERTIDO`, `PENAL_FALLADO` | 1 |
| `src/videojuego/observador/Marcador.java` | Modificar — contar `PENAL_CONVERTIDO` como gol en vez de `PENAL` | 1 |
| `src/videojuego/observador/RelatoDeportivo.java` | Modificar — agregar casos en switch exhaustivo | 1 |
| `src/videojuego/simulacion/MotorSimulacion.java` | Modificar — generar evento follow-up tras PENAL | 1 |
| `src/videojuego/fachada/ControladorPartido.java` | Modificar — `formatearRelato()` + `avanzarTramo()` + `simularTandaPenales()` + helpers | 1, 4 |
| `src/videojuego/modelo/Partido.java` | Modificar — `resultadoPenales` + `getResumenTexto()` | 4 |
| `src/videojuego/estado/EstadoPenales.java` | Crear — nuevo estado para la tanda de penales | 4 |
| `src/videojuego/persistencia/ConexionDB.java` | Modificar — migración ALTER TABLE para columna `penales` | 4 |
| `src/videojuego/persistencia/PartidoDATA.java` | Modificar — guardar/reconstruir `penales` | 4 |
| `src/videojuego/ui/ControladorSimulacion.java` | Modificar — pausa + UI de penales (estado "Penales") | 2, 4 |
| `src/videojuego/ui/ControladorGestionEquipos.java` | Modificar — eliminar zona de táctica | 3 |
| `src/videojuego/ui/ControladorHistorial.java` | Modificar — mostrar modoJuego en cada entrada | 4 |
| `README.md` | Modificar — documentar nuevas features | 5 |
| `HANDOFF.md` | Modificar — actualizar estado y alcance | 5 |

---

## Task 1: Evento follow-up de PENAL (PENAL_CONVERTIDO / PENAL_FALLADO)

**Problema:** Actualmente `PENAL` se registra como un único evento y se cuenta directamente como gol. Queremos mostrar en el relato si el penal se convirtió o se falló. La solución: dividir en dos eventos — `PENAL` (falta cobrada) y `PENAL_CONVERTIDO` / `PENAL_FALLADO` (resultado del remate).

**Files:**
- Modify: `src/videojuego/modelo/TipoEvento.java`
- Modify: `src/videojuego/observador/Marcador.java`
- Modify: `src/videojuego/observador/RelatoDeportivo.java`
- Modify: `src/videojuego/simulacion/MotorSimulacion.java`
- Modify: `src/videojuego/fachada/ControladorPartido.java`

### Paso 1.1 — Agregar valores al enum TipoEvento

- [ ] Reemplazar el contenido de `TipoEvento.java` con:

```java
package videojuego.modelo;

public enum TipoEvento {
    GOL,
    FALTA,
    TARJETA_AMARILLA,
    TARJETA_ROJA,
    LESION,
    PENAL,           // penal cobrado (falta dentro del área)
    PENAL_CONVERTIDO, // el remate entró — cuenta como gol
    PENAL_FALLADO     // el remate fue rechazado o fuera
}
```

> **⚠️ Compilar ahora FALLARÁ:** `RelatoDeportivo.java` usa un switch expression exhaustivo que ahora no cubre los nuevos valores. El próximo paso lo arregla.

### Paso 1.2 — Actualizar Marcador para usar PENAL_CONVERTIDO

- [ ] En `Marcador.java`, línea 21, cambiar la condición de:

```java
if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL) {
```

a:

```java
if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
```

### Paso 1.3 — Actualizar RelatoDeportivo (switch exhaustivo)

- [ ] En `RelatoDeportivo.java`, reemplazar el método `construirRelato` completo con:

```java
private String construirRelato(EventoDeportivo evento) {
    String jugador = evento.getJugador().getNombre();
    String equipo = evento.getEquipo().getNombre();
    int min = evento.getMinuto();

    return switch (evento.getTipo()) {
        case GOL -> "Min " + min + " - GOL de " + jugador + " (" + equipo + ")";
        case FALTA -> "Min " + min + " - Falta cometida por " + jugador;
        case TARJETA_AMARILLA -> "Min " + min + " - Tarjeta AMARILLA para " + jugador;
        case TARJETA_ROJA -> "Min " + min + " - Tarjeta ROJA para " + jugador + ". Se va expulsado.";
        case LESION -> "Min " + min + " - " + jugador + " sale lesionado del campo.";
        case PENAL -> "Min " + min + " - PENAL a favor de " + equipo + ". Va a ejecutar " + jugador;
        case PENAL_CONVERTIDO -> "Min " + min + " - ¡GOOOL de PENAL! Lo convierte " + jugador + " (" + equipo + ")";
        case PENAL_FALLADO -> "Min " + min + " - Penal FALLADO por " + jugador + ". El arquero lo ataja.";
    };
}
```

### Paso 1.4 — Actualizar MotorSimulacion para generar el evento follow-up

- [ ] En `MotorSimulacion.java`, dentro de `generarEvento()`, reemplazar el bloque que crea el evento (desde `EventoDeportivo evento = new EventoDeportivo(...)` hasta el final del método) con:

```java
// Para PENAL: primero registrar el cobro, luego el resultado del remate
if (tipo == TipoEvento.PENAL) {
    partido.registrarEvento(new EventoDeportivo(TipoEvento.PENAL, jugador, equipoDelJugador, minuto));
    TipoEvento resultado = random.nextDouble() < 0.75
            ? TipoEvento.PENAL_CONVERTIDO
            : TipoEvento.PENAL_FALLADO;
    EventoDeportivo eventoResultado = new EventoDeportivo(resultado, jugador, equipoDelJugador, minuto);
    partido.registrarEvento(eventoResultado);
    return eventoResultado;
}

EventoDeportivo evento = new EventoDeportivo(tipo, jugador, equipoDelJugador, minuto);
partido.registrarEvento(evento);
return evento;
```

> El código completo del método `generarEvento` queda así (para referencia):
```java
private EventoDeportivo generarEvento(Partido partido, int minuto) {
    Equipo atacante, defensor;
    if (random.nextBoolean()) {
        atacante = partido.getEquipoLocal();
        defensor = partido.getEquipoVisitante();
    } else {
        atacante = partido.getEquipoVisitante();
        defensor = partido.getEquipoLocal();
    }

    TipoEvento tipo = determinarEvento(atacante, defensor);
    if (tipo == null) return null;

    Equipo equipoDelJugador = (tipo == TipoEvento.FALTA ||
                               tipo == TipoEvento.TARJETA_AMARILLA ||
                               tipo == TipoEvento.TARJETA_ROJA)
                                ? defensor : atacante;
    Jugador jugador = obtenerJugadorDisponible(equipoDelJugador, tipo);
    if (jugador == null) return null;

    if (tipo == TipoEvento.PENAL) {
        partido.registrarEvento(new EventoDeportivo(TipoEvento.PENAL, jugador, equipoDelJugador, minuto));
        TipoEvento resultado = random.nextDouble() < 0.75
                ? TipoEvento.PENAL_CONVERTIDO
                : TipoEvento.PENAL_FALLADO;
        EventoDeportivo eventoResultado = new EventoDeportivo(resultado, jugador, equipoDelJugador, minuto);
        partido.registrarEvento(eventoResultado);
        return eventoResultado;
    }

    EventoDeportivo evento = new EventoDeportivo(tipo, jugador, equipoDelJugador, minuto);
    partido.registrarEvento(evento);
    return evento;
}
```

### Paso 1.5 — Actualizar ControladorPartido.formatearRelato

- [ ] En `ControladorPartido.java`, método `formatearRelato()`, reemplazar el bloque completo con:

```java
private String formatearRelato(EventoDeportivo evento) {
    String jugador = evento.getJugador().getNombre();
    String equipo = evento.getEquipo().getNombre();
    String min = evento.getMinuto() + "´  ";
    TipoEvento tipo = evento.getTipo();
    if (tipo == TipoEvento.GOL) {
        return min + "GOL de " + jugador + " (" + equipo + ")";
    } else if (tipo == TipoEvento.FALTA) {
        return min + "Falta cometida por " + jugador;
    } else if (tipo == TipoEvento.TARJETA_AMARILLA) {
        return min + "Tarjeta AMARILLA para " + jugador;
    } else if (tipo == TipoEvento.TARJETA_ROJA) {
        return min + "Tarjeta ROJA para " + jugador + ". Se va expulsado.";
    } else if (tipo == TipoEvento.LESION) {
        return min + jugador + " sale lesionado del campo.";
    } else if (tipo == TipoEvento.PENAL) {
        return min + "PENAL a favor de " + equipo + ". Va a ejecutar " + jugador;
    } else if (tipo == TipoEvento.PENAL_CONVERTIDO) {
        return min + "¡GOOOL de PENAL! Lo convierte " + jugador + " (" + equipo + ")";
    } else { // PENAL_FALLADO
        return min + "Penal FALLADO por " + jugador + ". El arquero lo ataja.";
    }
}
```

### Paso 1.6 — Actualizar Partido.getResumenTexto para contar PENAL_CONVERTIDO

- [ ] En `Partido.java`, método `getResumenTexto()`, cambiar la condición del for-each:

```java
// Cambiar de:
if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL) {
// A:
if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
```

### Paso 1.7 — Actualizar PartidoDATA para contar PENAL_CONVERTIDO como gol

- [ ] En `PartidoDATA.java`, método `guardar()`, cambiar la condición del for:

```java
// Cambiar de:
if (ev.getTipo() == TipoEvento.GOL || ev.getTipo() == TipoEvento.PENAL) {
// A:
if (ev.getTipo() == TipoEvento.GOL || ev.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
```

### Paso 1.8 — Compilar y verificar

- [ ] Compilar (Mac):
```bash
cd "/Users/franco/Documents/UADE/3er año/Proceso de Desarrollo de Software/TP Integrador/Java/TPODesSoft"
find src -name "*.java" > sources.txt
javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
```
Resultado esperado: **0 errores**.

- [ ] Correr la app y verificar:
```bash
java --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp "bin:lib/sqlite-jdbc-3.42.0.0.jar" videojuego.ui.MainApp
```
Crear un partido, jugarlo, y confirmar que en el relato aparecen dos líneas por penal: `"PENAL a favor de X. Va a ejecutar Y"` seguida de `"GOOOL de PENAL"` o `"Penal FALLADO"`.

- [ ] Commit:
```bash
git add src/videojuego/modelo/TipoEvento.java \
        src/videojuego/observador/Marcador.java \
        src/videojuego/observador/RelatoDeportivo.java \
        src/videojuego/simulacion/MotorSimulacion.java \
        src/videojuego/fachada/ControladorPartido.java \
        src/videojuego/modelo/Partido.java \
        src/videojuego/persistencia/PartidoDATA.java
git commit -m "feat: resultado de penal en relato (PENAL_CONVERTIDO / PENAL_FALLADO)"
```

---

## Task 2: Botón de pausa del partido

**Problema:** No hay forma de pausar el reloj en tiempo real para pensar antes de cambiar táctica. Se añade un botón Pausar/Reanudar visible mientras el reloj corre.

**Files:**
- Modify: `src/videojuego/ui/ControladorSimulacion.java`

### Paso 2.1 — Agregar campo `pausado`

- [ ] En `ControladorSimulacion.java`, agregar el campo privado junto a los otros campos del reloj:

```java
private boolean pausado = false;   // true cuando el usuario pausó manualmente
```

### Paso 2.2 — Agregar método togglePausa

- [ ] Agregar el método privado después de `detenerReloj()`:

```java
private void togglePausa() {
    if (pausado) {
        reloj.play();
    } else {
        reloj.pause();
    }
    pausado = !pausado;
    refrescar();
}
```

### Paso 2.3 — Resetear pausado al continuar segundo tiempo

- [ ] En `continuarSegundoTiempo()`, agregar `pausado = false;` antes de `reloj.play()`:

```java
private void continuarSegundoTiempo() {
    fachada.avanzarTramo(); // Entretiempo -> Segundo Tiempo
    pausado = false;        // resetear por si el usuario pausó durante el primer tiempo
    if (reloj != null) {
        reloj.play();
    }
    refrescar();
}
```

### Paso 2.4 — Resetear pausado al detener el reloj

- [ ] En `detenerReloj()`, agregar `pausado = false;`:

```java
private void detenerReloj() {
    if (reloj != null) {
        reloj.stop();
        reloj = null;
    }
    pausado = false;
}
```

### Paso 2.5 — Mostrar el botón Pausar/Reanudar en refrescar()

- [ ] En `refrescar()`, reemplazar el bloque `else` final (el que muestra "Partido en curso...") con:

```java
} else { // Primer/Segundo Tiempo con el reloj corriendo
    Label enCurso = new Label("Partido en curso...");
    Button btnPausar = new Button(pausado ? "Reanudar" : "Pausar");
    btnPausar.setOnAction(e -> togglePausa());
    panelControl.getChildren().addAll(enCurso, btnPausar);
}
```

### Paso 2.6 — Compilar y verificar

- [ ] Compilar:
```bash
cd "/Users/franco/Documents/UADE/3er año/Proceso de Desarrollo de Software/TP Integrador/Java/TPODesSoft"
find src -name "*.java" > sources.txt
javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
```
Resultado esperado: **0 errores**.

- [ ] Correr y verificar:
  - Iniciar un partido.
  - Hacer clic en "Pausar" — el reloj deja de avanzar, el botón dice "Reanudar".
  - Cambiar una táctica mientras está pausado.
  - Hacer clic en "Reanudar" — el reloj vuelve a correr desde donde estaba.

- [ ] Commit:
```bash
git add src/videojuego/ui/ControladorSimulacion.java
git commit -m "feat: boton pausar/reanudar partido en tiempo real"
```

---

## Task 3: Eliminar táctica de Gestión de Equipos

**Problema:** La táctica es una decisión del partido, no de la gestión permanente del equipo. Se elimina el panel de cambio de táctica de `ControladorGestionEquipos`. La táctica seguirá siendo visible (read-only) en la lista de equipos.

**Files:**
- Modify: `src/videojuego/ui/ControladorGestionEquipos.java`

### Paso 3.1 — Eliminar la Zona 2b completa del método getVista()

- [ ] En `ControladorGestionEquipos.java`, localizar y eliminar (o comentar) todo el bloque:

```java
// --- Zona 2b: cambiar la tactica de base del equipo seleccionado (se persiste) ---
ComboBox<String> cbTactica = new ComboBox<>();
cbTactica.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
cbTactica.setPromptText("Tactica");
Button btnCambiarTactica = new Button("Cambiar tactica del equipo seleccionado");
HBox formTactica = new HBox(10, new Label("Tactica:"), cbTactica, btnCambiarTactica);
```

Y el listener que sincroniza el combo:
```java
// Al seleccionar un equipo, el combo muestra su tactica actual.
listaEquipos.getSelectionModel().selectedItemProperty().addListener(
        (obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                cbTactica.setValue(seleccionado.getTactica().getNombre());
            }
        });
```

Y el handler del botón:
```java
btnCambiarTactica.setOnAction(e -> {
    Equipo equipo = listaEquipos.getSelectionModel().getSelectedItem();
    if (equipo == null) {
        mostrarMensaje("Primero selecciona un equipo de la lista.");
        return;
    }
    String tactica = cbTactica.getValue();
    if (tactica == null) {
        mostrarMensaje("Elegi una tactica del combo.");
        return;
    }
    fachada.cambiarTacticaEquipo(equipo, tactica);
    listaEquipos.refresh();
    mostrarMensaje("Tactica de " + equipo.getNombre() + " cambiada a " + tactica + ".");
});
```

### Paso 3.2 — Eliminar formTactica del VBox raíz

- [ ] En el `VBox raiz = new VBox(...)` al final de `getVista()`, eliminar las líneas:
```java
new Label("Cambiar tactica del equipo"), formTactica,
```
El VBox queda sin esas dos entradas.

### Paso 3.3 — Compilar y verificar

- [ ] Compilar:
```bash
find src -name "*.java" > sources.txt
javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
```
Resultado esperado: **0 errores**.

- [ ] Correr y verificar:
  - Abrir "Gestión de Equipos".
  - Confirmar que no aparece el panel "Cambiar táctica del equipo".
  - Confirmar que la lista de equipos sigue mostrando la táctica en modo lectura (la celda customizada ya la mostraba).

- [ ] Commit:
```bash
git add src/videojuego/ui/ControladorGestionEquipos.java
git commit -m "feat: eliminar cambio de tactica de gestion de equipos (es opcion del partido)"
```

---

## Task 4: Tanda de penales en modo Torneo

**Problema:** Si el modo es "Torneo" y el partido termina empatado, debe haber una tanda de 5 penales por equipo. Si sigue empatado, muerte súbita. El historial debe mostrar si el partido fue amistoso o torneo, y si hubo penales, su resultado.

**Approach:**
- Nuevo estado `EstadoPenales` en el patrón State.
- `ControladorPartido.avanzarTramo()` intercepta la transición a "Finalizado": si es torneo con empate, va a "Penales" en vez de guardar.
- La lógica de la tanda vive en `MotorSimulacion.simularTandaPenales()` (coherente con que la simulación está en el motor).
- `Partido` almacena el resultado de la tanda como string.
- `ConexionDB` hace una migración `ALTER TABLE` para agregar columna `penales TEXT`.
- `PartidoDATA` persiste y reconstruye esa columna.
- `ControladorSimulacion` muestra la fase "Penales" con un botón para ejecutarla.
- `ControladorHistorial` muestra el `modoJuego` en cada entrada del historial.

**Files:**
- Create: `src/videojuego/estado/EstadoPenales.java`
- Modify: `src/videojuego/modelo/Partido.java`
- Modify: `src/videojuego/simulacion/MotorSimulacion.java`
- Modify: `src/videojuego/fachada/ControladorPartido.java`
- Modify: `src/videojuego/persistencia/ConexionDB.java`
- Modify: `src/videojuego/persistencia/PartidoDATA.java`
- Modify: `src/videojuego/ui/ControladorSimulacion.java`
- Modify: `src/videojuego/ui/ControladorHistorial.java`

### Paso 4.1 — Crear EstadoPenales

- [ ] Crear el archivo `src/videojuego/estado/EstadoPenales.java`:

```java
package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoPenales implements IEstadoPartido {

    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Definicion por Penales ===");
    }

    @Override
    public void avanzar(Partido partido) {
        partido.setEstado(new EstadoFinalizado());
    }

    @Override
    public String getNombre() {
        return "Penales";
    }

    @Override
    public boolean permiteSimular() {
        return false;
    }
}
```

### Paso 4.2 — Agregar resultadoPenales a Partido

- [ ] En `Partido.java`, agregar el campo privado junto a los otros campos:

```java
private String resultadoPenales = null; // null si no hubo tanda; texto del resultado si sí
```

- [ ] Agregar los métodos getter y setter al final de la clase (antes del último `}`):

```java
public String getResultadoPenales() { return resultadoPenales; }
public void setResultadoPenales(String resultado) { this.resultadoPenales = resultado; }
```

- [ ] En `getResumenTexto()`, agregar al final del String construido la información de penales:

```java
public String getResumenTexto() {
    int golesLocal = 0;
    int golesVisitante = 0;
    for (EventoDeportivo evento : eventos) {
        if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
            if (evento.getEquipo().getNombre().equals(equipoLocal.getNombre())) {
                golesLocal++;
            } else {
                golesVisitante++;
            }
        }
    }
    String resumen = equipoLocal.getNombre() + " " + golesLocal + " - " + golesVisitante + " "
            + equipoVisitante.getNombre()
            + " | Eventos: " + eventos.size();
    if (resultadoPenales != null) {
        resumen += " | " + resultadoPenales;
    }
    return resumen;
}
```

> Nota: este método ya fue parcialmente modificado en Task 1 (paso 1.6) para usar `PENAL_CONVERTIDO`. Si ya está actualizado, solo agregar la parte de `resultadoPenales`.

### Paso 4.3 — Agregar simularTandaPenales a MotorSimulacion

- [ ] En `MotorSimulacion.java`, agregar el método al final de la clase (antes del último `}`):

```java
// Simula una tanda de penales: 5 patadas por equipo, luego muerte subita si hay empate.
// Probabilidad de conversion por patada: 75%. Cap de 20 rondas extra para evitar loop infinito.
// Devuelve el texto del resultado para almacenar en Partido.setResultadoPenales().
public String simularTandaPenales(String nombreLocal, String nombreVisitante) {
    int golesLocal = 0;
    int golesVisitante = 0;

    for (int i = 0; i < 5; i++) {
        if (random.nextDouble() < 0.75) golesLocal++;
        if (random.nextDouble() < 0.75) golesVisitante++;
    }

    boolean muerteSudbita = false;
    int maxExtra = 20;
    while (golesLocal == golesVisitante && maxExtra-- > 0) {
        muerteSudbita = true;
        boolean localConvierte = random.nextDouble() < 0.75;
        boolean visitanteConvierte = random.nextDouble() < 0.75;
        if (localConvierte) golesLocal++;
        if (visitanteConvierte) golesVisitante++;
    }

    String ganador = golesLocal > golesVisitante ? nombreLocal : nombreVisitante;
    return "Penales: " + nombreLocal + " " + golesLocal + " - " + golesVisitante
            + " " + nombreVisitante
            + (muerteSudbita ? " [Muerte subita]" : "")
            + " — Gana " + ganador;
}
```

### Paso 4.4 — Actualizar ControladorPartido

- [ ] Agregar el import necesario al inicio de `ControladorPartido.java`:

```java
import videojuego.estado.EstadoPenales;
```

- [ ] Agregar helper privado para detectar torneo empatado (agregar junto a los otros helpers al final):

```java
// Devuelve true si el partido es de torneo y el marcador esta empatado al finalizar.
private boolean esTorneoConEmpate() {
    if (!"Torneo".equalsIgnoreCase(partido.getModoJuego())) return false;
    Marcador m = getMarcador();
    return m != null && m.getGolesLocal() == m.getGolesVisitante();
}
```

- [ ] Reemplazar el método `avanzarTramo()` completo con:

```java
public void avanzarTramo() {
    if (partido == null) return;
    partido.avanzarEstado();
    String estado = partido.getEstadoActual().getNombre();
    if (estado.equals("Finalizado")) {
        if (esTorneoConEmpate()) {
            // Reemplazamos el estado Finalizado por Penales; aún no guardamos.
            partido.setEstado(new EstadoPenales());
        } else {
            new PartidoDATA().guardar(partido);
        }
    }
}
```

- [ ] Agregar el método público `simularTandaPenales()` y el helper `estaEnPenales()` (junto a los demás métodos públicos):

```java
// Simula la tanda de penales, persiste el resultado y avanza a Finalizado.
// Solo tiene efecto si el estado actual es "Penales".
// Devuelve el texto del resultado para mostrarlo en la UI.
public String simularTandaPenales() {
    if (partido == null || !partido.getEstadoActual().getNombre().equals("Penales")) return "";
    String resultado = motor.simularTandaPenales(
            partido.getEquipoLocal().getNombre(),
            partido.getEquipoVisitante().getNombre());
    partido.setResultadoPenales(resultado);
    partido.avanzarEstado(); // Penales -> Finalizado
    new PartidoDATA().guardar(partido);
    return resultado;
}

public boolean estaEnPenales() {
    return partido != null && partido.getEstadoActual().getNombre().equals("Penales");
}
```

### Paso 4.5 — Schema migration en ConexionDB

- [ ] En `ConexionDB.java`, dentro del método `crearTablas()`, agregar después del bloque `try (Statement st = ...)` que crea las tablas, un segundo bloque para la migración:

```java
// Migracion: agrega la columna 'penales' si no existe (seguro en BD nueva y existente).
try (Statement st = conexion.createStatement()) {
    st.execute("ALTER TABLE partidos ADD COLUMN penales TEXT");
} catch (SQLException ignorado) {
    // La columna ya existe en la BD — es el caso normal en ejecuciones posteriores.
}
```

El método `crearTablas()` completo queda:
```java
private void crearTablas() {
    String[] tablas = { /* ... el array existente sin cambios ... */ };
    try (Statement st = conexion.createStatement()) {
        for (String ddl : tablas) {
            st.execute(ddl);
        }
    } catch (SQLException e) {
        throw new RuntimeException("No se pudieron crear las tablas: " + e.getMessage(), e);
    }
    // Migracion: agrega la columna 'penales' si no existe.
    try (Statement st = conexion.createStatement()) {
        st.execute("ALTER TABLE partidos ADD COLUMN penales TEXT");
    } catch (SQLException ignorado) {
        // La columna ya existe — ignorar.
    }
}
```

### Paso 4.6 — Actualizar PartidoDATA

- [ ] En `PartidoDATA.java`, método `guardar()`, actualizar el SQL INSERT para incluir la columna `penales`:

```java
String sql = "INSERT INTO partidos (equipo_local, equipo_visitante, goles_local, goles_visitante, modo_juego, estado_final, penales) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
```

- [ ] Agregar el `ps.setString(7, partido.getResultadoPenales());` antes de `ps.executeUpdate()`:

```java
ps.setString(1, nombreLocal);
ps.setString(2, partido.getEquipoVisitante().getNombre());
ps.setInt(3, golesLocal);
ps.setInt(4, golesVisitante);
ps.setString(5, partido.getModoJuego());
ps.setString(6, partido.getEstadoActual().getNombre());
ps.setString(7, partido.getResultadoPenales()); // null si no hubo penales
ps.executeUpdate();
```

- [ ] En `consultarPartidos()`, actualizar el SQL SELECT para incluir `penales`:

```java
String sql = "SELECT id, equipo_local, equipo_visitante, modo_juego, penales FROM partidos";
```

- [ ] En el mismo método, agregar `rs.getString("penales")` al array de filas:

```java
filas.add(new Object[]{
        rs.getLong("id"),
        rs.getString("equipo_local"),
        rs.getString("equipo_visitante"),
        rs.getString("modo_juego"),
        rs.getString("penales")});      // nuevo campo
```

- [ ] Actualizar el bucle de reconstrucción para pasar el nuevo campo:

```java
resultado.add(reconstruirPartido(
        (Long) f[0], (String) f[1], (String) f[2], (String) f[3], (String) f[4]));
```

- [ ] Actualizar la firma y cuerpo de `reconstruirPartido()`:

```java
private Partido reconstruirPartido(long id, String local, String visitante, String modo, String penales) {
    PrintStream original = System.out;
    try {
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        Equipo equipoLocal = new Equipo(local);
        Equipo equipoVisitante = new Equipo(visitante);
        Estadio estadio = new Estadio("Sin especificar", "Sin especificar", 0);
        Partido partido = new Partido(equipoLocal, equipoVisitante, estadio, modo);
        cargarEventos(id, partido, equipoLocal, equipoVisitante);
        partido.setEstado(new EstadoFinalizado());
        if (penales != null) {
            partido.setResultadoPenales(penales);
        }
        return partido;
    } finally {
        System.setOut(original);
    }
}
```

- [ ] Actualizar `buscarPorEquipo()` para incluir también `penales` en su SELECT:

```java
String sql = "SELECT id, equipo_local, equipo_visitante, modo_juego, penales FROM partidos "
        + "WHERE equipo_local = ? OR equipo_visitante = ?";
```

### Paso 4.7 — Actualizar ControladorSimulacion para la fase Penales

- [ ] En `refrescar()`, en el bloque `panelControl.getChildren().clear()`, agregar un nuevo `else if` para el estado "Penales" **entre** el caso de "Entretiempo" y el de "Finalizado":

```java
} else if (fase.equals("Penales")) {
    Label sub = new Label("DEFINICION POR PENALES — Torneo empatado al 90'");
    sub.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    Button btnPenales = new Button("Simular tanda de penales");
    btnPenales.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20;");
    btnPenales.setOnAction(e -> {
        String resultado = fachada.simularTandaPenales();
        lblMensaje.setText(resultado);
        refrescar();
    });
    panelControl.getChildren().addAll(sub, btnPenales);
```

El bloque completo queda:
```java
panelControl.getChildren().clear();
if (fase.equals("Primer Tiempo") && reloj == null) {
    Button btnEmpezar = new Button("Empezar partido");
    btnEmpezar.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12 30 12 30;");
    btnEmpezar.setOnAction(e -> iniciarReloj());
    panelControl.getChildren().add(btnEmpezar);
} else if (fase.equals("Entretiempo")) {
    Label sub = new Label("=== ENTRETIEMPO === (puede ajustar tacticas antes de seguir)");
    sub.setStyle("-fx-font-weight: bold;");
    Button btnContinuar = new Button("Continuar al segundo tiempo");
    btnContinuar.setOnAction(e -> continuarSegundoTiempo());
    panelControl.getChildren().addAll(sub, btnContinuar);
} else if (fase.equals("Penales")) {
    Label sub = new Label("DEFINICION POR PENALES — Torneo empatado al 90'");
    sub.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    Button btnPenales = new Button("Simular tanda de penales");
    btnPenales.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20;");
    btnPenales.setOnAction(e -> {
        String resultado = fachada.simularTandaPenales();
        lblMensaje.setText(resultado);
        refrescar();
    });
    panelControl.getChildren().addAll(sub, btnPenales);
} else if (fase.equals("Finalizado")) {
    Label fin = new Label("Partido finalizado. Resultado guardado en el historial.");
    fin.setStyle("-fx-font-weight: bold;");
    panelControl.getChildren().add(fin);
} else { // Primer/Segundo Tiempo con el reloj corriendo
    Label enCurso = new Label("Partido en curso...");
    Button btnPausar = new Button(pausado ? "Reanudar" : "Pausar");
    btnPausar.setOnAction(e -> togglePausa());
    panelControl.getChildren().addAll(enCurso, btnPausar);
}
```

### Paso 4.8 — Actualizar ControladorHistorial para mostrar modoJuego

- [ ] En `ControladorHistorial.java`, dentro del `for` que llena la lista, cambiar el `lista.getItems().add(...)` de:

```java
lista.getItems().add((i + 1) + ". " + p.getResumenTexto()
        + " | Estado: " + p.getEstadoActual().getNombre());
```

a:

```java
lista.getItems().add((i + 1) + ". [" + p.getModoJuego() + "] "
        + p.getResumenTexto()
        + " | Estado: " + p.getEstadoActual().getNombre());
```

> El `getResumenTexto()` ya incluye el resultado de penales (si hubo), por lo que el historial mostrará algo como:
> `1. [Torneo] Argentina 1 - 1 Francia | Penales: Argentina 4-3 Francia — Gana Argentina | Eventos: 14 | Estado: Finalizado`

### Paso 4.9 — Compilar y verificar

- [ ] Compilar:
```bash
cd "/Users/franco/Documents/UADE/3er año/Proceso de Desarrollo de Software/TP Integrador/Java/TPODesSoft"
find src -name "*.java" > sources.txt
javac --module-path lib/javafx-sdk-21-mac/lib --add-modules javafx.controls -cp lib/sqlite-jdbc-3.42.0.0.jar -d bin @sources.txt
```
Resultado esperado: **0 errores**.

- [ ] **Test de Amistoso:** Correr la app, crear un partido en modo Amistoso y jugarlo hasta el final. Verificar que no aparece nada de penales — el partido finaliza directo.

- [ ] **Test de Torneo con ganador:** Crear un partido en modo Torneo y jugarlo hasta que no haya empate. Verificar que el historial muestra `[Torneo]` y finaliza sin penales.

- [ ] **Test de Torneo empatado:** Dado que el resultado es aleatorio, puede que no caiga empate en el primer intento. Para forzar el test: si el partido termina empatado (marcador 0-0 o 1-1, etc.), verificar que:
  1. Aparece el mensaje "DEFINICION POR PENALES" y un botón "Simular tanda de penales".
  2. Al hacer clic, se muestra el resultado de la tanda.
  3. El historial muestra el resultado de penales.
  4. Cerrar y reabrir la app → el historial sigue mostrando los penales (persistencia verificada).

- [ ] Commit:
```bash
git add src/videojuego/estado/EstadoPenales.java \
        src/videojuego/modelo/Partido.java \
        src/videojuego/simulacion/MotorSimulacion.java \
        src/videojuego/fachada/ControladorPartido.java \
        src/videojuego/persistencia/ConexionDB.java \
        src/videojuego/persistencia/PartidoDATA.java \
        src/videojuego/ui/ControladorSimulacion.java \
        src/videojuego/ui/ControladorHistorial.java
git commit -m "feat: tanda de penales en modo Torneo con muerte subita + modo en historial"
```

---

## Task 5: Actualizar README y HANDOFF

**Files:**
- Modify: `README.md`
- Modify: `HANDOFF.md`

### Paso 5.1 — Actualizar README

- [ ] En la sección **Descripción**, agregar en la lista "Incluye":
  - "Tanda de penales en modo Torneo si el partido termina empatado (5 penales + muerte súbita)"
  - "Resultado del remate de cada penal en el relato (convertido o fallado)"
  - "Pausa del partido en tiempo real"

- [ ] En **Patrones de diseño aplicados**, en la sección de State, actualizar para mencionar el nuevo estado:
```markdown
- **State** — El `Partido` transita por estados definidos:
  `PrimerTiempo` → `Entretiempo` → `SegundoTiempo` → `[Penales]` → `Finalizado`.
  El estado `Penales` solo aparece en modo Torneo con empate al 90'.
  Cada estado determina qué acciones son válidas en ese momento.
```

### Paso 5.2 — Actualizar HANDOFF

- [ ] En la nota de **Última actualización**, agregar las 4 features implementadas.

- [ ] En la **sección 4 (alcance real)**, actualizar:
  - Fila "Simulación por tramos": agregar nota sobre modo Torneo y penales
  - Fila "Cambio de táctica": aclarar que ya no está en Gestión, solo en el partido
  - Fila "Visualización tiempo real": mencionar pausa

- [ ] En la **sección 2 (arquitectura)**, actualizar la fila de `estado` para incluir `EstadoPenales`.

- [ ] En la **sección 5 (bugs)**, si B4 (modoJuego sin comportamiento) ahora está parcialmente resuelto (el Torneo con empate ya tiene comportamiento diferenciado), marcarlo como `🟡` o `✅` con el detalle.

- [ ] En el **registro de progreso (sección 9)**, agregar las 4 tareas nuevas.

- [ ] Commit:
```bash
git add README.md HANDOFF.md
git commit -m "docs: actualizar README y HANDOFF con nuevas features"
```

---

## Checklist de auto-review

- [x] **PENAL en TipoEvento:** `PENAL_CONVERTIDO` y `PENAL_FALLADO` agregados. `RelatoDeportivo` switch exhaustivo actualizado. `Marcador` cuenta `PENAL_CONVERTIDO` como gol. `Partido.getResumenTexto()` y `PartidoDATA.guardar()` usan `PENAL_CONVERTIDO`.
- [x] **PENAL follow-up en Motor:** `generarEvento()` registra ambos eventos (cobro + resultado). Reutiliza el mismo jugador.
- [x] **Pausa:** Campo `pausado` + método `togglePausa()`. Reset en `detenerReloj()` y `continuarSegundoTiempo()`. Botón en el bloque "en curso" de `refrescar()`.
- [x] **Táctica en gestión:** Zona 2b completamente eliminada (combo, listener y handler). VBox actualizado.
- [x] **EstadoPenales:** Nuevo estado con `permiteSimular() = false`, transiciona a `EstadoFinalizado` en `avanzar()`.
- [x] **Interceptación en avanzarTramo():** Detecta torneo con empate **después** de que `EstadoSegundoTiempo.avanzar()` ya llamó `setEstado(new EstadoFinalizado())`. Reemplaza ese estado con `EstadoPenales`. La persistencia NO ocurre hasta que se llama `simularTandaPenales()`.
- [x] **Schema migration:** `ALTER TABLE ... ADD COLUMN penales TEXT` en try-catch para ser idempotente.
- [x] **PartidoDATA:** SELECT, INSERT y reconstrucción actualizados. `buscarPorEquipo` también actualizado.
- [x] **Historial:** Prefijo `[Amistoso]` / `[Torneo]` en cada entrada. El resultado de penales viene de `getResumenTexto()`.
- [x] **Tipos consistentes:** `simularTandaPenales()` en Motor recibe `String, String` y devuelve `String`. La Fachada llama al Motor, almacena en `Partido`, avanza el estado y persiste. La UI solo llama a `fachada.simularTandaPenales()`.
