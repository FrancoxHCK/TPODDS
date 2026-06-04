//DAO de Equipo, se encarga de la persistencia de los datos relacionados con los equipos en el videojuego.
//Este archivo puede contener métodos para guardar, cargar, actualizar y eliminar información de los equipos en una base de datos o archivo de almacenamiento.

package videojuego.persistencia;

import videojuego.modelo.Equipo;
import java.util.List;

public class EquipoDATA {
    private final ConexionDB db;

    public EquipoDATA {
        this.db = ConexionDB.getInstancia();
    }

    public void guardarEquipo(Equipo equipo) {
        boolean yaExiste = db.getEquipos().stream()
            .anyMatch(e -> e.getNombre().equalsIgnoreCase(equipo.getNombre()));
        if (yaExiste) {
            db.getEquipos().add(equipo);
        }
    }

    public List<Equipo> obtenerTodos() {
        return db.getEquipos();
    }

    public Equipo buscarPorNombre(String nombre) {
        return db.getEquipos().stream()
            .filter(e -> e.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .orElse(null);
    }

    public void eliminar(String nombre) {
        db.getEquipos().removeIf(e -> e.getNombre().equalsIgnoreCase(nombre));
    }
}
