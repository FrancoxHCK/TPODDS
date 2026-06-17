// DAO de Equipo. Migrado a SQLite/JDBC: la firma publica de cada metodo no cambia,
// solo su implementacion interna. Usa la Connection del Singleton ConexionDB.

package videojuego.persistencia;

import videojuego.modelo.Equipo;
import videojuego.modelo.Jugador;
import videojuego.tactica.ITactica;
import videojuego.tactica.TacticaDefensiva;
import videojuego.tactica.TacticaEquilibrada;
import videojuego.tactica.TacticaOfensiva;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipoDATA {

    // Cache de identidad (first-level cache) a nivel del proceso. Necesaria para
    // preservar el comportamiento previo: con listas en memoria, obtenerTodos()
    // devolvia SIEMPRE la misma instancia de Equipo, de modo que los jugadores
    // agregados desde el menu (equipo.agregarJugador(...)) quedaban "pegados" al
    // equipo entre pantallas. SQLite reconstruiria objetos nuevos en cada lectura y,
    // como Jugador no conoce a su equipo ni registrarJugador() recibe el equipo, esa
    // asociacion se perderia hasta que el equipo se vuelva a guardar. La cache mantiene
    // la identidad dentro de la sesion; SQLite es el almacen durable (la asociacion
    // jugador->equipo se persiste en guardar(), p.ej. al configurar un partido).
    private static final Map<String, Equipo> cache = new HashMap<>();

    private final Connection conexion;

    public EquipoDATA() {
        this.conexion = ConexionDB.getInstancia().getConexion();
    }

    // Inserta o actualiza el equipo y persiste su plantilla actual (cada jugador con su
    // equipo_nombre). Deja la instancia en la cache de identidad.
    public void guardar(Equipo equipo) {
        String tactica = equipo.getTactica().getClass().getSimpleName();
        if (existe(equipo.getNombre())) {
            String sql = "UPDATE equipos SET tactica = ? WHERE nombre = ?";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, tactica);
                ps.setString(2, equipo.getNombre());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error al actualizar el equipo: " + e.getMessage(), e);
            }
        } else {
            String sql = "INSERT INTO equipos (nombre, tactica, estadio_nombre) VALUES (?, ?, NULL)";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, equipo.getNombre());
                ps.setString(2, tactica);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error al guardar el equipo: " + e.getMessage(), e);
            }
        }
        persistirJugadores(equipo);
        cache.put(equipo.getNombre(), equipo);
    }

    // Upsert de cada jugador del equipo fijando su equipo_nombre (la asociacion durable).
    private void persistirJugadores(Equipo equipo) {
        List<Jugador> jugadores = equipo.getJugadores();
        for (int i = 0; i < jugadores.size(); i++) {
            Jugador j = jugadores.get(i);
            String sql;
            if (jugadorExiste(j.getNombre())) {
                sql = "UPDATE jugadores SET posicion = ?, estado = ?, equipo_nombre = ? WHERE nombre = ?";
            } else {
                sql = "INSERT INTO jugadores (posicion, estado, equipo_nombre, nombre) VALUES (?, ?, ?, ?)";
            }
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, j.getPosicion());
                ps.setString(2, j.getEstado().name());
                ps.setString(3, equipo.getNombre());
                ps.setString(4, j.getNombre());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error al persistir la plantilla del equipo: " + e.getMessage(), e);
            }
        }
    }

    public List<Equipo> obtenerTodos() {
        // Materializamos primero (nombre, tactica) y CERRAMOS el ResultSet antes de
        // reconstruir cada equipo, porque reconstruir dispara otra query (jugadores)
        // sobre la misma Connection y SQLite puede invalidar un ResultSet externo abierto.
        List<String[]> filas = new ArrayList<>();
        String sql = "SELECT nombre, tactica FROM equipos";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new String[]{rs.getString("nombre"), rs.getString("tactica")});
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los equipos: " + e.getMessage(), e);
        }
        List<Equipo> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            resultado.add(obtenerOReconstruir(filas.get(i)[0], filas.get(i)[1]));
        }
        return resultado;
    }

    public Equipo buscarPorNombre(String nombre) {
        if (cache.containsKey(nombre)) {
            return cache.get(nombre);
        }
        // Leemos la tactica y cerramos el ResultSet antes de reconstruir (evita query anidada).
        String tactica = null;
        String sql = "SELECT tactica FROM equipos WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tactica = rs.getString("tactica");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el equipo: " + e.getMessage(), e);
        }
        if (tactica == null) {
            return null;
        }
        return obtenerOReconstruir(nombre, tactica);
    }

    // Actualiza el estadio_nombre del equipo en la BD (linkea equipo con su estadio).
    public void vincularEstadio(String nombreEquipo, String nombreEstadio) {
        String sql = "UPDATE equipos SET estadio_nombre = ? WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombreEstadio);
            ps.setString(2, nombreEquipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al vincular estadio al equipo: " + e.getMessage(), e);
        }
    }

    // Devuelve el nombre del estadio vinculado al equipo, o null si no tiene.
    public String obtenerNombreEstadio(String nombreEquipo) {
        String sql = "SELECT estadio_nombre FROM equipos WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombreEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estadio_nombre");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener estadio del equipo: " + e.getMessage(), e);
        }
        return null;
    }

    public void eliminar(String nombre) {
        cache.remove(nombre);
        String sql = "DELETE FROM equipos WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el equipo: " + e.getMessage(), e);
        }
    }

    // Devuelve la instancia cacheada si existe (preserva identidad y plantilla en
    // memoria); si no, reconstruye el equipo desde la BD (tactica + plantilla
    // persistida por equipo_nombre) y lo cachea.
    private Equipo obtenerOReconstruir(String nombre, String tacticaStr) {
        if (cache.containsKey(nombre)) {
            return cache.get(nombre);
        }
        Equipo equipo = new Equipo(nombre);
        equipo.cambiarTactica(mapearTactica(tacticaStr));
        List<Jugador> jugadores = cargarJugadores(nombre);
        for (int i = 0; i < jugadores.size(); i++) {
            equipo.agregarJugador(jugadores.get(i));
        }
        cache.put(nombre, equipo);
        return equipo;
    }

    // Carga la plantilla persistida de un equipo (jugadores con ese equipo_nombre).
    private List<Jugador> cargarJugadores(String equipoNombre) {
        List<Jugador> jugadores = new ArrayList<>();
        String sql = "SELECT nombre, posicion, estado FROM jugadores WHERE equipo_nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, equipoNombre);
            try (ResultSet rs = ps.executeQuery()) {
                int numero = 1;
                while (rs.next()) {
                    jugadores.add(JugadorDATA.reconstruir(rs.getString("nombre"),
                            rs.getString("posicion"), rs.getString("estado"), numero));
                    numero++;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar la plantilla del equipo: " + e.getMessage(), e);
        }
        return jugadores;
    }

    // Mapea el nombre simple de la clase de tactica (guardado en la columna) a una instancia.
    private ITactica mapearTactica(String tactica) {
        if ("TacticaOfensiva".equals(tactica)) {
            return new TacticaOfensiva();
        } else if ("TacticaDefensiva".equals(tactica)) {
            return new TacticaDefensiva();
        } else if ("TacticaEquilibrada".equals(tactica)) {
            return new TacticaEquilibrada();
        }
        return new TacticaEquilibrada(); // por defecto
    }

    private boolean existe(String nombre) {
        return existeEn("equipos", nombre);
    }

    private boolean jugadorExiste(String nombre) {
        return existeEn("jugadores", nombre);
    }

    // El nombre de tabla es un literal interno (no entrada de usuario), sin riesgo de inyeccion.
    private boolean existeEn(String tabla, String nombre) {
        String sql = "SELECT 1 FROM " + tabla + " WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia en " + tabla + ": " + e.getMessage(), e);
        }
    }
}
