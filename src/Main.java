//Demo para poder probar el codigo sin tener UI

import videojuego.fachada.ControladorPartido;
import videojuego.modelo.*;
import videojuego.persistencia.PartidoDATA;
import videojuego.tactica.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        // Crear jugadores
        Jugador j1 = new Jugador("Messi", "Delantero", 10);
        Jugador j2 = new Jugador("Di Maria", "Mediocampista", 11);
        Jugador j3 = new Jugador("Otamendi", "Defensor", 19);

        Jugador j4 = new Jugador("Mbappé", "Delantero", 10);
        Jugador j5 = new Jugador("Griezmann", "Mediocampista", 7);
        Jugador j6 = new Jugador("Varane", "Defensor", 4);

        // Crear equipos
        Equipo argentina = new Equipo("Argentina");
        argentina.cambiarTactica(new TacticaOfensiva());
        argentina.agregarJugador(j1);
        argentina.agregarJugador(j2);
        argentina.agregarJugador(j3);

        Equipo francia = new Equipo("Francia");
        francia.cambiarTactica(new TacticaDefensiva());
        francia.agregarJugador(j4);
        francia.agregarJugador(j5);
        francia.agregarJugador(j6);

        // Crear estadio y configurar partido
        Estadio estadio = new Estadio("Monumental", "Buenos Aires", 84000);
        ControladorPartido controlador = new ControladorPartido();
        controlador.configurarPartido(argentina, francia, estadio, "Final");

        System.out.println("\n=== INICIO DEL PARTIDO ===\n");

        // Simular primer tiempo
        controlador.simularTramo();

        System.out.println("\n--- Pasando el entretiempo ---\n");

        // Simular segundo tiempo (primero avanza el entretiempo, después simula)
        controlador.simularTramo();
        controlador.simularTramo();

        System.out.println("\n=== PARTIDO FINALIZADO ===");

        // Ver historial guardado
        List<Partido> historial = new PartidoDATA().obtenerTodos();
        System.out.println("Partidos guardados en historial: " + historial.size());
    }
}

