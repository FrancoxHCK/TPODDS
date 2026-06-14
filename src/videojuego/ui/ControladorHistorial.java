package videojuego.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import videojuego.fachada.ControladorPartido;
import videojuego.modelo.Partido;

import java.util.List;

// Pantalla de consulta de historial. Replica graficamente mostrarHistorial() de Main:
// lista cada partido jugado con su resumen (equipos + resultado + eventos) y el estado
// final. Consume fachada.obtenerHistorial() para no depender de System.out.
public class ControladorHistorial {

    private final Navegador navegador;
    private final ControladorPartido fachada;

    public ControladorHistorial(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista de la pantalla.
    public Parent getVista() {
        Label titulo = new Label("Historial");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button botonVolver = new Button("Volver al menu");
        botonVolver.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.MENU));

        // Se consulta el historial cada vez que se abre la pantalla (asi refleja los
        // partidos jugados durante esta sesion).
        List<Partido> historial = fachada.obtenerHistorial();

        if (historial.isEmpty()) {
            Label aviso = new Label("No hay partidos en el historial.");
            VBox raizVacia = new VBox(20, titulo, aviso, botonVolver);
            raizVacia.setPadding(new Insets(25));
            return raizVacia;
        }

        // Cada item replica el texto de mostrarHistorial(): resumen + estado final.
        ListView<String> lista = new ListView<>();
        for (int i = 0; i < historial.size(); i++) {
            Partido p = historial.get(i);
            lista.getItems().add((i + 1) + ". " + p.getResumenTexto()
                    + " | Estado: " + p.getEstadoActual().getNombre());
        }
        lista.setPrefHeight(320);

        VBox raiz = new VBox(15, titulo, lista, botonVolver);
        raiz.setPadding(new Insets(25));
        return raiz;
    }
}
