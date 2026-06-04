//Como no estamos utilizando bases de datos real ni dependencias externas, entonces la persistencia vamos a hacerla usando almacenamiento en memoria (Listas/mapas).

package videojuego.persistencia;

import videojuego.modelo.Equipo;
import videojuego.modelo.Jugador;
import videojuego.modelo.Partido;
import java.util.ArrayList;
import java.util.List;

public class ConexionDB {
    private static ConexionDB instancia;

    private final List<Equipo> equipos;
    private final List<Jugador> jugadores;
    private final List<Partido> partidos;

    private ConexionDB() {
        this.equipos = new ArrayList<>();
        this.jugadores = new ArrayList<>();
        this.partidos = new ArrayList<>();
    }

    public static ConexionDB getInstancia() {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    public List<Equipo> getEquipos() { return equipos; }
    public List<Jugador> getJugadores() { return jugadores; }
    public List<Partido> getPartidos() { return partidos; }

}
//  Por qué: Una sola instancia garantiza que todos los DAOs lean y escriban en las mismas listas. Simula la "sesión de base de datos".