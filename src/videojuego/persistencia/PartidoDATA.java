// DAO de Partido (historial). Migrado a SQLite/JDBC: la firma publica de cada metodo
// no cambia, solo su implementacion interna. Usa la Connection del Singleton ConexionDB.

package videojuego.persistencia;

import videojuego.estado.EstadoFinalizado;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.EventoDeportivo;
import videojuego.modelo.Jugador;
import videojuego.modelo.Partido;
import videojuego.modelo.TipoEvento;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PartidoDATA {
    private final Connection conexion;

    public PartidoDATA() {
        this.conexion = ConexionDB.getInstancia().getConexion();
    }

    // Inserta el partido y todos sus eventos. Usa RETURN_GENERATED_KEYS para obtener el
    // id recien generado y asociar cada evento al partido.
    public void guardar(Partido partido) {
        String nombreLocal = partido.getEquipoLocal().getNombre();
        List<EventoDeportivo> eventos = partido.getEventos();

        // Los goles se cuentan desde los eventos (misma regla que Marcador: GOL/PENAL).
        int golesLocal = 0;
        int golesVisitante = 0;
        for (int i = 0; i < eventos.size(); i++) {
            EventoDeportivo ev = eventos.get(i);
            if (ev.getTipo() == TipoEvento.GOL || ev.getTipo() == TipoEvento.PENAL) {
                if (ev.getEquipo().getNombre().equals(nombreLocal)) {
                    golesLocal++;
                } else {
                    golesVisitante++;
                }
            }
        }

        String sql = "INSERT INTO partidos (equipo_local, equipo_visitante, goles_local, goles_visitante, modo_juego, estado_final) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreLocal);
            ps.setString(2, partido.getEquipoVisitante().getNombre());
            ps.setInt(3, golesLocal);
            ps.setInt(4, golesVisitante);
            ps.setString(5, partido.getModoJuego());
            ps.setString(6, partido.getEstadoActual().getNombre());
            ps.executeUpdate();

            long partidoId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new RuntimeException("No se pudo obtener el id del partido guardado.");
                }
                partidoId = keys.getLong(1);
            }
            guardarEventos(partidoId, eventos);
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el partido: " + e.getMessage(), e);
        }
    }

    private void guardarEventos(long partidoId, List<EventoDeportivo> eventos) throws SQLException {
        String sql = "INSERT INTO eventos (partido_id, tipo, minuto, jugador_nombre, equipo_nombre) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < eventos.size(); i++) {
                EventoDeportivo ev = eventos.get(i);
                ps.setLong(1, partidoId);
                ps.setString(2, ev.getTipo().name());
                ps.setInt(3, ev.getMinuto());
                ps.setString(4, ev.getJugador() != null ? ev.getJugador().getNombre() : null);
                ps.setString(5, ev.getEquipo() != null ? ev.getEquipo().getNombre() : null);
                ps.executeUpdate();
            }
        }
    }

    public List<Partido> obtenerTodos() {
        String sql = "SELECT id, equipo_local, equipo_visitante, modo_juego FROM partidos";
        return consultarPartidos(sql, null);
    }

    public List<Partido> buscarPorEquipo(String nombreEquipo) {
        String sql = "SELECT id, equipo_local, equipo_visitante, modo_juego FROM partidos "
                + "WHERE equipo_local = ? OR equipo_visitante = ?";
        return consultarPartidos(sql, nombreEquipo);
    }

    public void limpiarHistorial() {
        try (Statement st = conexion.createStatement()) {
            st.executeUpdate("DELETE FROM eventos");
            st.executeUpdate("DELETE FROM partidos");
        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar el historial: " + e.getMessage(), e);
        }
    }

    // Ejecuta la consulta de partidos y reconstruye cada uno (con sus eventos) para que
    // getResumenTexto() y getEstadoActual().getNombre() funcionen como con la version en memoria.
    private List<Partido> consultarPartidos(String sql, String filtroEquipo) {
        // Materializamos primero las filas de partidos y CERRAMOS el ResultSet antes de
        // reconstruir cada uno, porque reconstruir dispara otra query (eventos) sobre la
        // misma Connection y SQLite puede invalidar un ResultSet externo abierto.
        List<Object[]> filas = new ArrayList<>(); // {id(Long), local, visitante, modo}
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (filtroEquipo != null) {
                ps.setString(1, filtroEquipo);
                ps.setString(2, filtroEquipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getLong("id"),
                            rs.getString("equipo_local"),
                            rs.getString("equipo_visitante"),
                            rs.getString("modo_juego")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los partidos: " + e.getMessage(), e);
        }
        List<Partido> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            Object[] f = filas.get(i);
            resultado.add(reconstruirPartido((Long) f[0], (String) f[1], (String) f[2], (String) f[3]));
        }
        return resultado;
    }

    // Reconstruye un Partido "basico" desde la BD. El constructor de Partido y los estados
    // imprimen mensajes de ciclo de vida; como aca solo rearmamos datos para el historial,
    // silenciamos System.out durante la reconstruccion para no ensuciar la salida. El swap
    // de System.out queda contenido en la capa de persistencia (no se toca el modelo).
    private Partido reconstruirPartido(long id, String local, String visitante, String modo) {
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            Equipo equipoLocal = new Equipo(local);
            Equipo equipoVisitante = new Equipo(visitante);
            Estadio estadio = new Estadio("Sin especificar", "Sin especificar", 0);
            Partido partido = new Partido(equipoLocal, equipoVisitante, estadio, modo);
            cargarEventos(id, partido, equipoLocal, equipoVisitante);
            // El historial solo guarda partidos finalizados; reflejamos ese estado final.
            partido.setEstado(new EstadoFinalizado());
            return partido;
        } finally {
            System.setOut(original);
        }
    }

    // Re-agrega los eventos persistidos al partido para que getResumenTexto() cuente bien
    // los goles (la regla compara el nombre del equipo del evento contra el equipo local).
    private void cargarEventos(long partidoId, Partido partido, Equipo local, Equipo visitante) {
        String sql = "SELECT tipo, minuto, jugador_nombre, equipo_nombre FROM eventos WHERE partido_id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setLong(1, partidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TipoEvento tipo = TipoEvento.valueOf(rs.getString("tipo"));
                    int minuto = rs.getInt("minuto");
                    String jugadorNombre = rs.getString("jugador_nombre");
                    String equipoNombre = rs.getString("equipo_nombre");
                    Equipo equipoEvento = visitante.getNombre().equals(equipoNombre) ? visitante : local;
                    Jugador jugador = new Jugador(jugadorNombre != null ? jugadorNombre : "?", "?", 0);
                    partido.registrarEvento(new EventoDeportivo(tipo, jugador, equipoEvento, minuto));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar los eventos del partido: " + e.getMessage(), e);
        }
    }
}
