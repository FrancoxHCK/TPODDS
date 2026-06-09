package videojuego.fachada;

import videojuego.builder.PartidoBuilder;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.Jugador;
import videojuego.modelo.Partido;
import videojuego.observador.Estadisticas;
import videojuego.observador.Marcador;
import videojuego.persistencia.EquipoDATA;
import videojuego.persistencia.JugadorDATA;
import videojuego.persistencia.PartidoDATA;
import videojuego.simulacion.MotorSimulacion;

import java.util.List;

public class ControladorPartido {

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

    public Partido getPartido() {
        return partido;
    }
}