package videojuego.modelo;

import videojuego.tactica.ITactica;
import videojuego.tactica.TacticaEquilibrada;
import java.util.ArrayList;
import java.util.List;


public class Equipo {
    private String nombre;
    private List<Jugador> jugadores;
    private ITactica tactica;

    public Equipo(String nombre) {
        this.nombre = nombre;
        this.tactica = new TacticaEquilibrada();
        this.jugadores = new ArrayList<>();
    }

    public void agregarJugador(Jugador jugador) {
        jugadores.add(jugador);
    }
    public void cambiarTactica(ITactica nuevaTactica) {
        this.tactica = nuevaTactica;
    }

    public String getNombre() { return nombre; }
    public List<Jugador> getJugadores() { return jugadores; }
    public ITactica getTactica() { return tactica; }

    @Override
    public String toString() {
        return nombre + " [" + tactica.getNombre() + "]";
    }
}
