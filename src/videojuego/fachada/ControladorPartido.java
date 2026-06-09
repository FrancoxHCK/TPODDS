package videojuego.fachada;

import videojuego.builder.PartidoBuilder;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.Partido;
import videojuego.observador.Estadisticas;
import videojuego.observador.Marcador;
import videojuego.persistencia.PartidoDATA;
import videojuego.simulacion.MotorSimulacion;

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

    public Partido getPartido() {
        return partido;
    }
}