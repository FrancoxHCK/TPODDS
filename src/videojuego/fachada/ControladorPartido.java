package videojuego.fachada;

import videojuego.builder.PartidoBuilder;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.Partido;
import videojuego.persistencia.PartidoDATA;
import videojuego.simulacion.MotorSimulacion;

public class ControladorPartido {

    private Partido partido;
    private MotorSimulacion motor;

    public ControladorPartido() {
        this.motor = new MotorSimulacion();
    }

    public void configurarPartido(Equipo local, Equipo visitante, Estadio estadio, String modoJuego) {
        this.partido = new PartidoBuilder()
                .setEquipoLocal(local)
                .setEquipoVisitante(visitante)
                .setEstadio(estadio)
                .setModoJuego(modoJuego)
                .conMarcador()
                .conEstadisticas()
                .conRelato()
                .build();
    }

    public void iniciarPartido() {
        partido.avanzarEstado();
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
        // TODO: retornar resultado desde Marcador cuando Motor esté implementado
        return "Partido en estado: " + partido.getEstadoActual().getNombre();
    }

    public Partido getPartido() {
        return partido;
    }
}