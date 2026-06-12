package videojuego.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import videojuego.fachada.ControladorPartido;

// Pantalla del menu principal: titulo + botones que navegan a cada seccion.
public class ControladorMenuPrincipal {

    private final Navegador navegador;
    private final ControladorPartido fachada; // Reservado para etapas futuras (logica via fachada)

    public ControladorMenuPrincipal(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista del menu principal.
    public Parent getVista() {
        Label titulo = new Label("Simulador de Partidos de Futbol");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button botonGestion = new Button("Gestion de Equipos");
        Button botonConfigurar = new Button("Configurar Partido");
        Button botonSimulacion = new Button("Simulacion");
        Button botonHistorial = new Button("Historial");
        Button botonSalir = new Button("Salir");

        // Ancho uniforme para una grilla de botones prolija.
        botonGestion.setMaxWidth(220);
        botonConfigurar.setMaxWidth(220);
        botonSimulacion.setMaxWidth(220);
        botonHistorial.setMaxWidth(220);
        botonSalir.setMaxWidth(220);

        // Cada boton dispara la navegacion a su pantalla.
        botonGestion.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.GESTION));
        botonConfigurar.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.CONFIGURAR));
        botonSimulacion.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.SIMULACION));
        botonHistorial.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.HISTORIAL));
        botonSalir.setOnAction(e -> Platform.exit());

        VBox raiz = new VBox(15, titulo, botonGestion, botonConfigurar,
                botonSimulacion, botonHistorial, botonSalir);
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new Insets(40));
        return raiz;
    }
}
