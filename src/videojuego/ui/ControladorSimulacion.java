package videojuego.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import videojuego.fachada.ControladorPartido;

// Pantalla de simulacion del partido. Vista minima por ahora.
public class ControladorSimulacion {

    private final Navegador navegador;
    private final ControladorPartido fachada; // Reservado para etapas futuras (logica via fachada)

    public ControladorSimulacion(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista de la pantalla.
    public Parent getVista() {
        Label titulo = new Label("Simulacion");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button botonVolver = new Button("Volver al menu");
        botonVolver.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.MENU));

        VBox raiz = new VBox(20, titulo, botonVolver);
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new Insets(40));
        return raiz;
    }
}
