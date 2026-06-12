package videojuego.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import videojuego.fachada.ControladorPartido;

// Punto de entrada de la interfaz grafica (JavaFX puro, sin FXML).
// Posee el Stage y una unica Scene; navegar = cambiar el root de esa Scene.
// Implementa Navegador para que los controladores puedan pedir cambios de pantalla.
public class MainApp extends Application implements Navegador {

    private Scene escena;
    private ControladorPartido fachada; // Unica instancia, compartida por todas las pantallas

    // Controladores de cada pantalla: se crean una sola vez y se reutilizan.
    private ControladorMenuPrincipal menuPrincipal;
    private ControladorGestionEquipos gestionEquipos;
    private ControladorConfigurarPartido configurarPartido;
    private ControladorSimulacion simulacion;
    private ControladorHistorial historial;

    @Override
    public void start(Stage stage) {
        // Toda la logica de negocio pasa por esta unica fachada.
        this.fachada = new ControladorPartido();

        // Se instancian los controladores pasandoles el navegador (this) y la fachada.
        this.menuPrincipal = new ControladorMenuPrincipal(this, fachada);
        this.gestionEquipos = new ControladorGestionEquipos(this, fachada);
        this.configurarPartido = new ControladorConfigurarPartido(this, fachada);
        this.simulacion = new ControladorSimulacion(this, fachada);
        this.historial = new ControladorHistorial(this, fachada);

        // La Scene se crea una vez con la vista del menu principal.
        this.escena = new Scene(menuPrincipal.getVista(), 800, 600);

        stage.setTitle("Simulador de Partidos de Futbol");
        stage.setScene(escena);
        stage.show();
    }

    // Cambia la pantalla visible reemplazando el root de la unica Scene.
    @Override
    public void navegarA(Pantalla pantalla) {
        Parent vista;
        switch (pantalla) {
            case GESTION:
                vista = gestionEquipos.getVista();
                break;
            case CONFIGURAR:
                vista = configurarPartido.getVista();
                break;
            case SIMULACION:
                vista = simulacion.getVista();
                break;
            case HISTORIAL:
                vista = historial.getVista();
                break;
            case MENU:
            default:
                vista = menuPrincipal.getVista();
                break;
        }
        escena.setRoot(vista);
    }

    // Entrada de la aplicacion JavaFX (necesaria para 'java videojuego.ui.MainApp').
    public static void main(String[] args) {
        launch(args);
    }
}
