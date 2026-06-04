//DAO de partido (Historial), se encarga de la persistencia de los datos relacionados con los partidos en el videojuego.
//Este archivo puede contener métodos para guardar, cargar, actualizar y eliminar información de los partidos en

package videojuego.persistencia;

import videojuego.modelo.Partido;
import java.util.List;
import java.util.stream.Collectors;

public class PartidoDATA {
        private final ConexionDB db;

        public PartidoDATA() {
            this.db = ConexionDB.getInstancia();
        }

        public void guardar(Partido partido) {
            db.getPartidos().add(partido);
        }

        public List<Partido> obtenerTodos() {
            return db.getPartidos();
        }

        public List<Partido> buscarPorEquipo(String nombreEquipo) {
            return db.getPartidos().stream()
                .filter(p -> p.getEquipoLocal().getNombre().equalsIgnoreCase(nombreEquipo) ||
                             p.getEquipoVisitante().getNombre().equalsIgnoreCase(nombreEquipo))
                .collect(Collectors.toList());
        }

        public void limpiarHistorial() {
            db.getPartidos().clear();
        }
}
