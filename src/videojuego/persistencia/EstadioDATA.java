// DAO de Estadio. Migrado a SQLite/JDBC: la firma publica de cada metodo no cambia,
// solo su implementacion interna. Usa la Connection del Singleton ConexionDB.

package videojuego.persistencia;

import videojuego.modelo.Estadio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EstadioDATA {
    private final Connection conexion;

    public EstadioDATA() {
        this.conexion = ConexionDB.getInstancia().getConexion();
    }

    // Inserta el estadio si no existe otro con el mismo nombre (evita duplicados).
    public void guardar(Estadio estadio) {
        if (buscarPorNombre(estadio.getNombre()) != null) {
            return;
        }
        String sql = "INSERT INTO estadios (nombre) VALUES (?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estadio.getNombre());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el estadio: " + e.getMessage(), e);
        }
    }

    public List<Estadio> obtenerTodos() {
        List<Estadio> resultado = new ArrayList<>();
        String sql = "SELECT nombre FROM estadios";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(reconstruir(rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los estadios: " + e.getMessage(), e);
        }
        return resultado;
    }

    public Estadio buscarPorNombre(String nombre) {
        String sql = "SELECT nombre FROM estadios WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return reconstruir(rs.getString("nombre"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el estadio: " + e.getMessage(), e);
        }
        return null;
    }

    public void eliminar(String nombre) {
        String sql = "DELETE FROM estadios WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el estadio: " + e.getMessage(), e);
        }
    }

    // La tabla solo guarda el nombre; ciudad/capacidad se completan con valores por
    // defecto al reconstruir (coherente con como Main crea los estadios).
    private Estadio reconstruir(String nombre) {
        return new Estadio(nombre, "Sin especificar", 0);
    }
}
