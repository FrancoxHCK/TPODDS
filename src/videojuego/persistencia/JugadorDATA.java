//DAO de jugador, se encarga de la persistencia de los datos relacionados con los jugadores en el videojuego.
//Este archivo puede contener métodos para guardar, cargar, actualizar y eliminar información de los jugadores en una base de datos o archivo de almacenamiento.

package videojuego.persistencia;

import videojuego.modelo.Jugador;
import java.util.List;
import java.util.stream.Collectors;

public class JugadorDATA {
    private final ConexionDB db;

    public JugadorDATA() {
        this.db = ConexionDB.getInstancia();
    }

    public void guardar(Jugador jugador) {
        boolean yaExiste = db.getJugadores().stream()
            .anyMatch(j -> j.getNombre().equalsIgnoreCase(jugador.getNombre()));
        if (!yaExiste) {
            db.getJugadores().add(jugador);
        }
    }

    public List<Jugador> obtenerTodos() {
        return db.getJugadores();
    }

    public Jugador buscarPorNombre(String nombre) {
        return db.getJugadores().stream()
            .filter(j -> j.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .orElse(null);
    }

    public void eliminar(String nombre) {
        db.getJugadores().removeIf(j -> j.getNombre().equalsIgnoreCase(nombre));
    }

}
