// Menu de consola interactivo del Simulador de Futbol (Tarea 6 / Fase 3).
// Reemplaza la antigua demo hardcodeada. Toda operacion pasa por la unica
// fachada ControladorPartido; Main nunca instancia DAOs directamente.

import videojuego.fachada.ControladorPartido;
import videojuego.modelo.*;
import videojuego.observador.Estadisticas;
import videojuego.observador.Marcador;
import videojuego.tactica.*;

import java.util.List;
import java.util.Scanner;

public class Main {

    // Unica fachada para todas las operaciones (gestion, configuracion, simulacion, historial).
    private static final ControladorPartido controlador = new ControladorPartido();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        boolean salir = false;
        while (!salir) {
            System.out.println();
            System.out.println("=== SIMULADOR DE FUTBOL ===");
            System.out.println("1. Gestion de equipos y jugadores");
            System.out.println("2. Configurar y jugar partido");
            System.out.println("3. Ver historial de partidos");
            System.out.println("4. Salir");
            int opcion = leerOpcion();
            switch (opcion) {
                case 1 -> menuGestion();
                case 2 -> menuConfigurarYJugar();
                case 3 -> controlador.mostrarHistorial();
                case 4 -> {
                    salir = true;
                    System.out.println("Saliendo del simulador. Hasta la proxima!");
                }
                default -> System.out.println("Opcion invalida. Intente de nuevo.");
            }
        }
    }

    // ===================== SUBMENU 1: GESTION =====================

    private static void menuGestion() {
        boolean volver = false;
        while (!volver) {
            System.out.println();
            System.out.println("=== GESTION DE EQUIPOS Y JUGADORES ===");
            System.out.println("1. Registrar equipo nuevo");
            System.out.println("2. Agregar jugador a un equipo");
            System.out.println("3. Ver equipos registrados");
            System.out.println("4. Ver jugadores de un equipo");
            System.out.println("5. Volver");
            int opcion = leerOpcion();
            switch (opcion) {
                case 1 -> registrarEquipoNuevo();
                case 2 -> agregarJugadorAEquipo();
                case 3 -> verEquipos();
                case 4 -> verJugadoresDeEquipo();
                case 5 -> volver = true;
                default -> System.out.println("Opcion invalida.");
            }
        }
    }

    private static void registrarEquipoNuevo() {
        String nombreEquipo = leerLinea("Nombre del equipo: ");
        if (nombreEquipo.isEmpty()) {
            System.out.println("El nombre del equipo no puede estar vacio.");
            return;
        }
        String nombreEstadio = leerLinea("Nombre del estadio: ");
        if (nombreEstadio.isEmpty()) {
            System.out.println("El nombre del estadio no puede estar vacio.");
            return;
        }

        Equipo equipo = new Equipo(nombreEquipo);
        // El constructor de Estadio pide ciudad y capacidad; el enunciado solo pide
        // el nombre, asi que completamos esos campos con valores por defecto.
        Estadio estadio = new Estadio(nombreEstadio, "Sin especificar", 0);

        controlador.registrarEquipo(equipo);   // persiste el equipo (y sus jugadores)
        controlador.registrarEstadio(estadio); // persiste el estadio para poder elegirlo luego

        System.out.println("Equipo '" + nombreEquipo + "' y estadio '" + nombreEstadio + "' registrados.");
    }

    private static void agregarJugadorAEquipo() {
        Equipo equipo = elegirEquipo("Elija el equipo al que desea agregar el jugador:");
        if (equipo == null) {
            return;
        }
        String nombre = leerLinea("Nombre del jugador: ");
        if (nombre.isEmpty()) {
            System.out.println("El nombre del jugador no puede estar vacio.");
            return;
        }
        String posicion = leerLinea("Posicion (ej: Delantero, Mediocampista, Defensor): ");
        // El enunciado pide solo nombre y posicion; el numero de camiseta lo asignamos
        // automaticamente segun la cantidad actual de jugadores del equipo.
        int numero = equipo.getJugadores().size() + 1;

        Jugador jugador = new Jugador(nombre, posicion, numero);
        equipo.agregarJugador(jugador);        // lo asocia al equipo elegido
        controlador.registrarJugador(jugador); // lo persiste en el DAO de jugadores

        System.out.println("Jugador '" + nombre + "' (#" + numero + ") agregado a " + equipo.getNombre() + ".");
    }

    private static void verEquipos() {
        List<Equipo> equipos = controlador.obtenerEquipos();
        if (equipos.isEmpty()) {
            System.out.println("No hay equipos registrados.");
            return;
        }
        System.out.println("=== Equipos registrados ===");
        for (int i = 0; i < equipos.size(); i++) {
            Equipo e = equipos.get(i);
            System.out.println((i + 1) + ". " + e.getNombre()
                    + " [Tactica: " + e.getTactica().getNombre()
                    + " | Jugadores: " + e.getJugadores().size() + "]");
        }
    }

    private static void verJugadoresDeEquipo() {
        Equipo equipo = elegirEquipo("Elija el equipo cuya plantilla desea ver:");
        if (equipo == null) {
            return;
        }
        List<Jugador> jugadores = equipo.getJugadores();
        if (jugadores.isEmpty()) {
            System.out.println("El equipo " + equipo.getNombre() + " no tiene jugadores cargados.");
            return;
        }
        System.out.println("=== Plantilla de " + equipo.getNombre() + " ===");
        for (int i = 0; i < jugadores.size(); i++) {
            System.out.println("  " + jugadores.get(i)); // usa Jugador.toString()
        }
    }

    // ===================== SUBMENU 2: CONFIGURAR Y JUGAR =====================

    private static void menuConfigurarYJugar() {
        // Seleccion en curso. Se va completando opcion por opcion antes de iniciar.
        Equipo local = null;
        Equipo visitante = null;
        Estadio estadio = null;
        ITactica tacticaLocal = null;
        ITactica tacticaVisitante = null;
        String modoJuego = null;

        boolean salir = false;
        while (!salir) {
            System.out.println();
            System.out.println("=== CONFIGURAR Y JUGAR PARTIDO ===");
            System.out.println("Local: " + nombreOSinElegir(local));
            System.out.println("Visitante: " + nombreOSinElegir(visitante));
            System.out.println("Estadio: " + (estadio == null ? "(sin elegir)" : estadio.getNombre()));
            System.out.println("Tactica local: " + (tacticaLocal == null ? "(actual del equipo)" : tacticaLocal.getNombre()));
            System.out.println("Tactica visitante: " + (tacticaVisitante == null ? "(actual del equipo)" : tacticaVisitante.getNombre()));
            System.out.println("Modo: " + (modoJuego == null ? "(sin elegir)" : modoJuego));
            System.out.println("-------------------------------------");
            System.out.println("1. Elegir equipo local");
            System.out.println("2. Elegir equipo visitante");
            System.out.println("3. Elegir estadio");
            System.out.println("4. Elegir tactica del equipo local");
            System.out.println("5. Elegir tactica del equipo visitante");
            System.out.println("6. Elegir modo de juego");
            System.out.println("7. Iniciar partido");
            System.out.println("8. Volver");
            int opcion = leerOpcion();
            switch (opcion) {
                case 1 -> local = elegirEquipo("Elija el equipo LOCAL:");
                case 2 -> visitante = elegirEquipo("Elija el equipo VISITANTE:");
                case 3 -> estadio = elegirEstadio();
                case 4 -> tacticaLocal = elegirTactica("Tactica del equipo LOCAL");
                case 5 -> tacticaVisitante = elegirTactica("Tactica del equipo VISITANTE");
                case 6 -> modoJuego = elegirModo();
                case 7 -> {
                    if (configuracionValida(local, visitante, estadio)) {
                        // Aplicamos las tacticas elegidas; si no se eligieron, queda la del equipo.
                        if (tacticaLocal != null) {
                            local.cambiarTactica(tacticaLocal);
                        }
                        if (tacticaVisitante != null) {
                            visitante.cambiarTactica(tacticaVisitante);
                        }
                        if (modoJuego == null) {
                            modoJuego = "Amistoso"; // valor por defecto si no se eligio
                        }
                        jugarPartido(local, visitante, estadio, modoJuego);
                        salir = true; // tras jugar el partido, volvemos al menu principal
                    }
                }
                case 8 -> salir = true;
                default -> System.out.println("Opcion invalida.");
            }
        }
    }

    // Valida que esten elegidos los datos minimos para iniciar el partido.
    private static boolean configuracionValida(Equipo local, Equipo visitante, Estadio estadio) {
        if (local == null || visitante == null || estadio == null) {
            System.out.println("Debe elegir equipo local, visitante y estadio antes de iniciar.");
            return false;
        }
        if (local.getNombre().equalsIgnoreCase(visitante.getNombre())) {
            System.out.println("El equipo local y el visitante no pueden ser el mismo.");
            return false;
        }
        return true;
    }

    private static Estadio elegirEstadio() {
        List<Estadio> estadios = controlador.obtenerEstadios();
        if (estadios.isEmpty()) {
            System.out.println("No hay estadios registrados. Registre un equipo (que crea su estadio) primero.");
            return null;
        }
        System.out.println("Elija el estadio:");
        for (int i = 0; i < estadios.size(); i++) {
            System.out.println((i + 1) + ". " + estadios.get(i).getNombre());
        }
        int idx = leerOpcion();
        if (idx < 1 || idx > estadios.size()) {
            System.out.println("Seleccion invalida.");
            return null;
        }
        return estadios.get(idx - 1);
    }

    private static ITactica elegirTactica(String titulo) {
        System.out.println(titulo + " (1=Ofensiva, 2=Defensiva, 3=Equilibrada):");
        int op = leerOpcion();
        switch (op) {
            case 1 -> { return new TacticaOfensiva(); }
            case 2 -> { return new TacticaDefensiva(); }
            case 3 -> { return new TacticaEquilibrada(); }
            default -> {
                System.out.println("Opcion invalida, se mantiene la tactica actual.");
                return null;
            }
        }
    }

    private static String elegirModo() {
        System.out.println("Modo de juego (1=Amistoso, 2=Torneo):");
        int op = leerOpcion();
        switch (op) {
            case 1 -> { return "Amistoso"; }
            case 2 -> { return "Torneo"; }
            default -> {
                System.out.println("Opcion invalida.");
                return null;
            }
        }
    }

    // ===================== FLUJO DEL PARTIDO =====================

    private static void jugarPartido(Equipo local, Equipo visitante, Estadio estadio, String modoJuego) {
        // Configura el partido (su constructor ya imprime "=== Comienza el Primer Tiempo ===").
        controlador.configurarPartido(local, visitante, estadio, modoJuego);

        System.out.println();
        System.out.println("########## INICIA EL PARTIDO ##########");
        System.out.println(local.getNombre() + " vs " + visitante.getNombre()
                + " | Estadio: " + estadio.getNombre() + " | Modo: " + modoJuego);

        // 1) Primer tiempo: genera eventos (el relato se imprime solo) y avanza al entretiempo.
        System.out.println();
        System.out.println("--- PRIMER TIEMPO ---");
        controlador.simularTramo();

        // 2) Mostrar marcador y estadisticas al cierre del primer tiempo.
        mostrarMarcadorYEstadisticas();

        // 3) Entretiempo interactivo: permite cambiar tacticas antes del segundo tiempo.
        menuEntretiempo(local, visitante);

        // 4) Segundo tiempo. La maquina de estados exige pasar por el entretiempo:
        //    la primera llamada lo deja atras (sin simular), la segunda simula el
        //    segundo tiempo, finaliza el partido y lo guarda automaticamente.
        System.out.println();
        System.out.println("--- SEGUNDO TIEMPO ---");
        controlador.simularTramo(); // entretiempo -> segundo tiempo
        controlador.simularTramo(); // segundo tiempo -> finalizado (se guarda solo)

        // 5) Resultado final.
        System.out.println();
        System.out.println("########## RESULTADO FINAL ##########");
        mostrarMarcadorYEstadisticas();
        System.out.println("Resumen: " + controlador.getPartido().getResumenTexto());
        System.out.println("El partido fue guardado en el historial.");
    }

    private static void menuEntretiempo(Equipo local, Equipo visitante) {
        boolean continuar = false;
        while (!continuar) {
            System.out.println();
            System.out.println("=== ENTRETIEMPO ===");
            System.out.println("1. Cambiar tactica equipo local (" + local.getNombre() + ")");
            System.out.println("2. Cambiar tactica equipo visitante (" + visitante.getNombre() + ")");
            System.out.println("3. Continuar al segundo tiempo");
            int op = leerOpcion();
            switch (op) {
                case 1 -> cambiarTacticaEnVivo(local);
                case 2 -> cambiarTacticaEnVivo(visitante);
                case 3 -> continuar = true;
                default -> System.out.println("Opcion invalida.");
            }
        }
    }

    private static void cambiarTacticaEnVivo(Equipo equipo) {
        ITactica nueva = elegirTactica("Nueva tactica para " + equipo.getNombre());
        if (nueva == null) {
            return;
        }
        // Pasa por la fachada: valida que el partido no este finalizado y aplica el cambio.
        // El motor lee la tactica en tiempo real, asi que impacta en el segundo tiempo.
        controlador.cambiarTactica(equipo, nueva);
        System.out.println("Tactica de " + equipo.getNombre() + " cambiada a " + nueva.getNombre() + ".");
    }

    private static void mostrarMarcadorYEstadisticas() {
        Marcador marcador = controlador.getMarcador();
        Estadisticas estadisticas = controlador.getEstadisticas();
        if (marcador != null) {
            System.out.println("Marcador: " + marcador.getResultado());
        }
        if (estadisticas != null) {
            System.out.println("Estadisticas: " + estadisticas.getResumen());
        }
    }

    // ===================== HELPERS DE SELECCION E INPUT =====================

    // Lista los equipos registrados y devuelve el elegido (o null si no hay/seleccion invalida).
    private static Equipo elegirEquipo(String titulo) {
        List<Equipo> equipos = controlador.obtenerEquipos();
        if (equipos.isEmpty()) {
            System.out.println("No hay equipos registrados. Registre uno desde 'Gestion' primero.");
            return null;
        }
        System.out.println(titulo);
        for (int i = 0; i < equipos.size(); i++) {
            System.out.println((i + 1) + ". " + equipos.get(i).getNombre());
        }
        int idx = leerOpcion();
        if (idx < 1 || idx > equipos.size()) {
            System.out.println("Seleccion invalida.");
            return null;
        }
        return equipos.get(idx - 1);
    }

    private static String nombreOSinElegir(Equipo equipo) {
        return equipo == null ? "(sin elegir)" : equipo.getNombre();
    }

    private static String leerLinea(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    // Lee una opcion numerica. Devuelve -1 si la entrada no es un numero valido.
    private static int leerOpcion() {
        System.out.print("Opcion: ");
        String linea = sc.nextLine().trim();
        try {
            return Integer.parseInt(linea);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}