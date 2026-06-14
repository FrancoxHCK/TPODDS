package videojuego.fachada;

import videojuego.builder.PartidoBuilder;
import videojuego.estado.EstadoFinalizado;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.EventoDeportivo;
import videojuego.modelo.Jugador;
import videojuego.modelo.Partido;
import videojuego.modelo.TipoEvento;
import videojuego.observador.Estadisticas;
import videojuego.observador.Marcador;
import videojuego.persistencia.EquipoDATA;
import videojuego.persistencia.EstadioDATA;
import videojuego.persistencia.JugadorDATA;
import videojuego.persistencia.PartidoDATA;
import videojuego.simulacion.MotorSimulacion;
import videojuego.tactica.ITactica;
import videojuego.tactica.TacticaOfensiva;
import videojuego.tactica.TacticaDefensiva;
import videojuego.tactica.TacticaEquilibrada;

import java.util.ArrayList;
import java.util.List;

public class
ControladorPartido {

    private Partido partido;
    private MotorSimulacion motor;
    private PartidoBuilder builder; // Se retiene para acceder a marcador y estadisticas

    public ControladorPartido() {
        this.motor = new MotorSimulacion();
    }

    public void configurarPartido(Equipo local, Equipo visitante, Estadio estadio, String modoJuego) {
        this.builder = new PartidoBuilder()
                .setEquipoLocal(local)
                .setEquipoVisitante(visitante)
                .setEstadio(estadio)
                .setModoJuego(modoJuego)
                .conMarcador()
                .conEstadisticas()
                .conRelato();
        this.partido = this.builder.build();

        // Al configurar, persistimos ambos equipos y sus jugadores en memoria.
        persistirEquipoConJugadores(local);
        persistirEquipoConJugadores(visitante);
    }

    // Helper privado: guarda un equipo y todos sus jugadores en sus respectivos DAOs.
    // Reutilizado por configurarPartido() y registrarEquipo().
    private void persistirEquipoConJugadores(Equipo equipo) {
        new EquipoDATA().guardar(equipo);
        List<Jugador> jugadores = equipo.getJugadores();
        for (int i = 0; i < jugadores.size(); i++) {
            new JugadorDATA().guardar(jugadores.get(i));
        }
    }

    // Antes este metodo llamaba avanzarEstado(), lo que salteaba el primer tiempo.
    // Se deja como no-op: el partido ya arranca en EstadoPrimerTiempo desde su constructor.
    // La simulacion real se dispara con simularTramo().
    public void iniciarPartido() {
        // Intencionalmente vacio (ver comentario arriba).
    }

    public void simularTramo() {
        if (partido.getEstadoActual().permiteSimular()) {
            motor.simularTramo(partido);
            partido.avanzarEstado();
            // Si después de avanzar quedó finalizado, lo guardamos
            if (!partido.getEstadoActual().permiteSimular() &&
                    partido.getEstadoActual().getNombre().equals("Finalizado")) {
                new PartidoDATA().guardar(partido);
            }
        } else {
            partido.avanzarEstado(); // avanza el entretiempo sin simular
        }
    }

    // Variante por nombre del cambio de tactica en tiempo real, pensada para la UI:
    // mapea el nombre a la tactica concreta (la UI no instancia tacticas, Regla 1) y
    // delega en cambiarTactica(Equipo, ITactica), que conserva la guarda de finalizado.
    // Nombres validos: "Ofensiva", "Defensiva", "Equilibrada"; si no coincide, no hace nada.
    public void cambiarTacticaEnVivo(Equipo equipo, String nombreTactica) {
        ITactica nueva = crearTactica(nombreTactica);
        if (nueva != null) {
            cambiarTactica(equipo, nueva);
        }
    }

    // Cambia la tactica de un equipo en tiempo real. El MotorSimulacion lee
    // equipo.getTactica() al generar cada evento, asi que el cambio impacta de
    // inmediato en los tramos que resten por simular.
    public void cambiarTactica(Equipo equipo, ITactica nuevaTactica) {
        if (partido != null && partido.getEstadoActual() instanceof EstadoFinalizado) {
            throw new IllegalStateException(
                    "No se puede cambiar la tactica en un partido finalizado.");
        }
        equipo.cambiarTactica(nuevaTactica);
    }

    // Imprime el historial de partidos persistido. Por cada partido muestra el
    // resumen (equipos + resultado + eventos) y el estado final.
    public void mostrarHistorial() {
        List<Partido> historial = new PartidoDATA().obtenerTodos();
        if (historial.isEmpty()) {
            System.out.println("No hay partidos en el historial.");
            return;
        }
        System.out.println("=== Historial de Partidos ===");
        for (int i = 0; i < historial.size(); i++) {
            Partido p = historial.get(i);
            System.out.println((i + 1) + ". " + p.getResumenTexto()
                    + " | Estado: " + p.getEstadoActual().getNombre());
        }
    }

    // Expone el historial crudo para que la UI lo consuma sin depender de System.out.
    public List<Partido> obtenerHistorial() {
        return new PartidoDATA().obtenerTodos();
    }

    // Devuelve el relato del partido en curso como lista de lineas, para que la GUI lo
    // muestre en vivo (la consola lo recibe por System.out via el observador RelatoDeportivo;
    // la UI no tiene acceso a ese stream). Se reconstruye desde los eventos ya registrados
    // del partido, sin tocar el builder ni el observador (Regla 2). Si no hay partido,
    // devuelve una lista vacia.
    public List<String> obtenerRelato() {
        List<String> lineas = new ArrayList<>();
        if (partido == null) {
            return lineas;
        }
        List<EventoDeportivo> eventos = partido.getEventos();
        for (int i = 0; i < eventos.size(); i++) {
            lineas.add(formatearRelato(eventos.get(i)));
        }
        return lineas;
    }

    // Helper privado: arma la linea de relato de un evento. Replica el formato textual
    // que usa RelatoDeportivo para la consola, adaptando el dato del modelo a la GUI.
    private String formatearRelato(EventoDeportivo evento) {
        String jugador = evento.getJugador().getNombre();
        String equipo = evento.getEquipo().getNombre();
        int min = evento.getMinuto();
        TipoEvento tipo = evento.getTipo();
        if (tipo == TipoEvento.GOL) {
            return "Min " + min + " - GOL de " + jugador + " (" + equipo + ")";
        } else if (tipo == TipoEvento.FALTA) {
            return "Min " + min + " - Falta cometida por " + jugador;
        } else if (tipo == TipoEvento.TARJETA_AMARILLA) {
            return "Min " + min + " - Tarjeta AMARILLA para " + jugador;
        } else if (tipo == TipoEvento.TARJETA_ROJA) {
            return "Min " + min + " - Tarjeta ROJA para " + jugador + ". Se va expulsado.";
        } else if (tipo == TipoEvento.LESION) {
            return "Min " + min + " - " + jugador + " sale lesionado del campo.";
        } else { // PENAL
            return "Min " + min + " - PENAL a favor de " + equipo + ". Ejecuta " + jugador;
        }
    }

    public String getResultado() {
        if (builder == null || builder.getMarcador() == null) {
            return "No hay marcador disponible.";
        }
        return builder.getMarcador().getResultado();
    }

    public Marcador getMarcador() {
        return builder != null ? builder.getMarcador() : null;
    }

    public Estadisticas getEstadisticas() {
        return builder != null ? builder.getEstadisticas() : null;
    }

    // === Gestion de equipos y jugadores (delega en los DAOs) ===

    public List<Equipo> obtenerEquipos() {
        return new EquipoDATA().obtenerTodos();
    }

    public List<Jugador> obtenerJugadores() {
        return new JugadorDATA().obtenerTodos();
    }

    // Registra un equipo y, ademas, persiste todos sus jugadores.
    public void registrarEquipo(Equipo equipo) {
        persistirEquipoConJugadores(equipo);
    }

    public void registrarJugador(Jugador jugador) {
        new JugadorDATA().guardar(jugador);
    }

    public void registrarEstadio(Estadio estadio) {
        new EstadioDATA().guardar(estadio);
    }

    public List<Estadio> obtenerEstadios() {
        return new EstadioDATA().obtenerTodos();
    }

    // === Altas para la UI (encapsulan la creacion del modelo) ===
    // La UI no instancia el modelo directamente: recibe Strings y aca se hace el
    // 'new'. Replican exactamente lo que hace el menu de consola (Main), pero
    // detras de la fachada. Son aditivos: no alteran registrarEquipo/Jugador/Estadio.

    // Crea un equipo nuevo junto a su estadio (mismos valores por defecto que Main:
    // ciudad "Sin especificar", capacidad 0) y persiste ambos.
    public void registrarEquipoConEstadio(String nombreEquipo, String nombreEstadio) {
        Equipo equipo = new Equipo(nombreEquipo);
        Estadio estadio = new Estadio(nombreEstadio, "Sin especificar", 0);
        registrarEquipo(equipo);
        registrarEstadio(estadio);
    }

    // Agrega un jugador al equipo indicado: autoasigna el numero de camiseta
    // (cantidad actual + 1), lo asocia al equipo y lo persiste.
    public void agregarJugadorAEquipo(Equipo equipo, String nombreJugador, String posicion) {
        int numero = equipo.getJugadores().size() + 1;
        Jugador jugador = new Jugador(nombreJugador, posicion, numero);
        equipo.agregarJugador(jugador);
        registrarJugador(jugador);
    }

    // Aplica una tactica inicial (antes del partido) a un equipo, mapeando el nombre
    // a la tactica concreta. La UI no instancia tacticas (Regla 1): el 'new' vive aca.
    // Se aplica directo sobre el equipo (sin el guard de finalizado), igual que Main al
    // configurar. Nombres validos: "Ofensiva", "Defensiva", "Equilibrada".
    public void configurarTacticaInicial(Equipo equipo, String nombreTactica) {
        ITactica tactica = crearTactica(nombreTactica);
        if (tactica != null) {
            equipo.cambiarTactica(tactica);
        }
    }

    // Helper privado: mapea un nombre a una instancia de tactica (o null si no coincide).
    // Reutilizable por la configuracion inicial y por el cambio en el entretiempo.
    private ITactica crearTactica(String nombreTactica) {
        if (nombreTactica == null) {
            return null;
        }
        if (nombreTactica.equalsIgnoreCase("Ofensiva")) {
            return new TacticaOfensiva();
        } else if (nombreTactica.equalsIgnoreCase("Defensiva")) {
            return new TacticaDefensiva();
        } else if (nombreTactica.equalsIgnoreCase("Equilibrada")) {
            return new TacticaEquilibrada();
        }
        return null;
    }

    public Partido getPartido() {
        return partido;
    }
}