//DAO de Estadio, se encarga de la persistencia de los datos relacionados con los estadios en el videojuego.
//Este archivo puede contener metodos para guardar, cargar, actualizar y eliminar informacion de los estadios en una base de datos o archivo de almacenamiento.

package videojuego.persistencia;

import videojuego.modelo.Estadio;
import java.util.List;

public class EstadioDATA {
    private final ConexionDB db;

    public EstadioDATA() {
        this.db = ConexionDB.getInstancia();
    }

    public void guardar(Estadio estadio) {
        // Evita duplicados por nombre (mismo criterio que EquipoDATA/JugadorDATA).
        boolean yaExiste = false;
        List<Estadio> estadios = db.getEstadios();
        for (int i = 0; i < estadios.size(); i++) {
            if (estadios.get(i).getNombre().equalsIgnoreCase(estadio.getNombre())) {
                yaExiste = true;
                break;
            }
        }
        if (!yaExiste) {
            db.getEstadios().add(estadio);
        }
    }

    public List<Estadio> obtenerTodos() {
        return db.getEstadios();
    }

    public Estadio buscarPorNombre(String nombre) {
        List<Estadio> estadios = db.getEstadios();
        for (int i = 0; i < estadios.size(); i++) {
            if (estadios.get(i).getNombre().equalsIgnoreCase(nombre)) {
                return estadios.get(i);
            }
        }
        return null; // No existe un estadio con ese nombre.
    }

    public void eliminar(String nombre) {
        List<Estadio> estadios = db.getEstadios();
        for (int i = estadios.size() - 1; i >= 0; i--) {
            if (estadios.get(i).getNombre().equalsIgnoreCase(nombre)) {
                estadios.remove(i);
            }
        }
    }
}
