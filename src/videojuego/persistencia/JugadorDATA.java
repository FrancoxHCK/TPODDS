// DAO de Jugador. Migrado a SQLite/JDBC: la firma publica de cada metodo no cambia,
// solo su implementacion interna. Usa la Connection del Singleton ConexionDB.

package videojuego.persistencia;

import videojuego.modelo.EstadoJugador;
import videojuego.modelo.Jugador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JugadorDATA {
    private final Connection conexion;

    public JugadorDATA() {
        this.conexion = ConexionDB.getInstancia().getConexion();
    }

    // Inserta el jugador, o si ya existe (por nombre) actualiza posicion y estado.
    // Nota: el modelo Jugador NO conoce a que equipo pertenece, asi que aca no se
    // setea equipo_nombre. Esa asociacion la persiste EquipoDATA.guardar(), que si
    // dispone del equipo y de su plantilla. Por eso el UPDATE no toca equipo_nombre.
    public void guardar(Jugador jugador) {
        if (existe(jugador.getNombre())) {
            String sql = "UPDATE jugadores SET posicion = ?, estado = ? WHERE nombre = ?";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, jugador.getPosicion());
                ps.setString(2, jugador.getEstado().name());
                ps.setString(3, jugador.getNombre());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error al actualizar el jugador: " + e.getMessage(), e);
            }
        } else {
            String sql = "INSERT INTO jugadores (nombre, posicion, estado, equipo_nombre) VALUES (?, ?, ?, NULL)";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, jugador.getNombre());
                ps.setString(2, jugador.getPosicion());
                ps.setString(3, jugador.getEstado().name());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error al guardar el jugador: " + e.getMessage(), e);
            }
        }
    }

    public List<Jugador> obtenerTodos() {
        List<Jugador> resultado = new ArrayList<>();
        String sql = "SELECT nombre, posicion, estado FROM jugadores";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int numero = 1;
            while (rs.next()) {
                resultado.add(reconstruir(rs.getString("nombre"), rs.getString("posicion"),
                        rs.getString("estado"), numero));
                numero++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los jugadores: " + e.getMessage(), e);
        }
        return resultado;
    }

    public Jugador buscarPorNombre(String nombre) {
        String sql = "SELECT nombre, posicion, estado FROM jugadores WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return reconstruir(rs.getString("nombre"), rs.getString("posicion"),
                            rs.getString("estado"), 1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el jugador: " + e.getMessage(), e);
        }
        return null;
    }

    public void eliminar(String nombre) {
        String sql = "DELETE FROM jugadores WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el jugador: " + e.getMessage(), e);
        }
    }

    private boolean existe(String nombre) {
        String sql = "SELECT 1 FROM jugadores WHERE nombre = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar el jugador: " + e.getMessage(), e);
        }
    }

    // La tabla no guarda el numero de camiseta; se asigna uno secuencial al reconstruir.
    // El estado persistido se restaura best-effort (Jugador no expone un setter directo).
    // Package-private para que EquipoDATA reutilice esta reconstruccion al cargar plantillas.
    static Jugador reconstruir(String nombre, String posicion, String estado, int numero) {
        Jugador j = new Jugador(nombre, posicion, numero);
        if (EstadoJugador.LESIONADO.name().equals(estado)) {
            j.lesionar();
        } else if (EstadoJugador.SUSPENDIDO.name().equals(estado)) {
            j.recibirTarjetaRoja();
        }
        return j;
    }
}
