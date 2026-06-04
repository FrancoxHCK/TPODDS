package videojuego.fachada;

import videojuego.builder.PartidoBuilder;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.Partido;
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
        } else {
            System.out.println("No se puede simular en el estado: "
                    + partido.getEstadoActual().getNombre());
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