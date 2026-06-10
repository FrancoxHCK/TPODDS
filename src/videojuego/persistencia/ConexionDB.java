// Singleton que gestiona la conexion JDBC a SQLite (archivo simulador.db en la raiz del proyecto).
// Antes esta clase guardaba listas en memoria; ahora abre/posee la conexion y crea el esquema.
// Regla de oro de la migracion: nada fuera del paquete persistencia cambia. Los DAOs obtienen
// la Connection con getConexion(); nadie mas usa esta clase.

package videojuego.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {
    private static ConexionDB instancia;
    private static final String URL = "jdbc:sqlite:simulador.db";

    private final Connection conexion;

    private ConexionDB() {
        try {
            this.conexion = DriverManager.getConnection(URL);
            crearTablas();
        } catch (SQLException e) {
            // Si la conexion falla, que el error sea visible (no seguir con estado invalido).
            throw new RuntimeException("No se pudo abrir la conexion con SQLite (" + URL + "): " + e.getMessage(), e);
        }
    }

    public static ConexionDB getInstancia() {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    public Connection getConexion() {
        return conexion;
    }

    // Crea las tablas si no existen. Se ejecuta una sola vez, al instanciar el Singleton.
    private void crearTablas() {
        String[] tablas = {
                """
                CREATE TABLE IF NOT EXISTS estadios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS equipos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    tactica TEXT NOT NULL,
                    estadio_nombre TEXT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS jugadores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    posicion TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    equipo_nombre TEXT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS partidos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipo_local TEXT NOT NULL,
                    equipo_visitante TEXT NOT NULL,
                    goles_local INTEGER NOT NULL,
                    goles_visitante INTEGER NOT NULL,
                    modo_juego TEXT,
                    estado_final TEXT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS eventos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    partido_id INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    minuto INTEGER NOT NULL,
                    jugador_nombre TEXT,
                    equipo_nombre TEXT,
                    FOREIGN KEY (partido_id) REFERENCES partidos(id)
                )
                """
        };
        try (Statement st = conexion.createStatement()) {
            for (String ddl : tablas) {
                st.execute(ddl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron crear las tablas: " + e.getMessage(), e);
        }
    }
}
// Por que: una sola instancia garantiza que todos los DAOs usen la misma Connection a simulador.db.
