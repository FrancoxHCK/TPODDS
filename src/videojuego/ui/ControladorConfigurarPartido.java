package videojuego.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import videojuego.fachada.ControladorPartido;
import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;

// Pantalla de configuracion del partido.
// Replica el submenu "Configurar y jugar" de Main: elegir local/visitante/estadio,
// tacticas (opcionales) y modo, y arrancar el partido. Al iniciar, configura el
// partido via la fachada y navega a la pantalla de Simulacion.
public class ControladorConfigurarPartido {

    private final Navegador navegador;
    private final ControladorPartido fachada;

    private Label lblMensaje;

    public ControladorConfigurarPartido(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista de la pantalla.
    public Parent getVista() {
        Label titulo = new Label("Configurar Partido");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Combos de equipos y estadio (se llenan desde la fachada).
        ComboBox<Equipo> cbLocal = new ComboBox<>();
        ComboBox<Equipo> cbVisitante = new ComboBox<>();
        configurarComboEquipo(cbLocal);
        configurarComboEquipo(cbVisitante);
        cbLocal.getItems().setAll(fachada.obtenerEquipos());
        cbVisitante.getItems().setAll(fachada.obtenerEquipos());

        ComboBox<Estadio> cbEstadio = new ComboBox<>();
        configurarComboEstadio(cbEstadio);
        cbEstadio.getItems().setAll(fachada.obtenerEstadios());

        // Al seleccionar el equipo local, el estadio se auto-asigna al estadio del equipo.
        cbLocal.valueProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                Estadio estadioLocal = fachada.obtenerEstadioDeEquipo(seleccionado);
                if (estadioLocal != null) {
                    for (Estadio e : cbEstadio.getItems()) {
                        if (e.getNombre().equals(estadioLocal.getNombre())) {
                            cbEstadio.setValue(e);
                            break;
                        }
                    }
                }
            }
        });

        // Combos de tacticas (opcionales) y modo. Son etiquetas de texto, no modelo.
        ComboBox<String> cbTacticaLocal = new ComboBox<>();
        ComboBox<String> cbTacticaVisitante = new ComboBox<>();
        cbTacticaLocal.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
        cbTacticaVisitante.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
        cbTacticaLocal.setPromptText("(actual del equipo)");
        cbTacticaVisitante.setPromptText("(actual del equipo)");

        ComboBox<String> cbModo = new ComboBox<>();
        cbModo.getItems().setAll("Amistoso", "Torneo");
        cbModo.setValue("Amistoso");

        // Formulario en grilla (etiqueta + control).
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Equipo local:"), 0, 0);        grid.add(cbLocal, 1, 0);
        grid.add(new Label("Equipo visitante:"), 0, 1);    grid.add(cbVisitante, 1, 1);
        grid.add(new Label("Estadio:"), 0, 2);             grid.add(cbEstadio, 1, 2);
        grid.add(new Label("Tactica local:"), 0, 3);       grid.add(cbTacticaLocal, 1, 3);
        grid.add(new Label("Tactica visitante:"), 0, 4);   grid.add(cbTacticaVisitante, 1, 4);
        grid.add(new Label("Modo:"), 0, 5);                grid.add(cbModo, 1, 5);

        lblMensaje = new Label();
        Button btnIniciar = new Button("Iniciar partido");
        Button botonVolver = new Button("Volver al menu");
        botonVolver.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.MENU));

        btnIniciar.setOnAction(e -> {
            Equipo local = cbLocal.getValue();
            Equipo visitante = cbVisitante.getValue();
            Estadio estadio = cbEstadio.getValue();
            // Validaciones equivalentes a las de Main.
            if (local == null || visitante == null || estadio == null) {
                mostrarMensaje("Debe elegir equipo local, visitante y estadio.");
                return;
            }
            if (local.getNombre().equalsIgnoreCase(visitante.getNombre())) {
                mostrarMensaje("El equipo local y el visitante no pueden ser el mismo.");
                return;
            }
            // Tacticas opcionales: si se eligieron, se aplican antes de configurar.
            String tacticaLocal = cbTacticaLocal.getValue();
            String tacticaVisitante = cbTacticaVisitante.getValue();
            if (tacticaLocal != null) {
                fachada.configurarTacticaInicial(local, tacticaLocal);
            }
            if (tacticaVisitante != null) {
                fachada.configurarTacticaInicial(visitante, tacticaVisitante);
            }
            String modo = cbModo.getValue();
            if (modo == null) {
                modo = "Amistoso";
            }
            // Configura el partido (queda guardado en la fachada compartida).
            fachada.configurarPartido(local, visitante, estadio, modo);
            mostrarMensaje("Partido configurado. Iniciando simulacion...");
            navegador.navegarA(Navegador.Pantalla.SIMULACION);
        });

        VBox raiz = new VBox(15, titulo, grid, btnIniciar, lblMensaje, botonVolver);
        raiz.setPadding(new Insets(25));
        return raiz;
    }

    // Configura un ComboBox de equipos para mostrar solo el nombre del equipo.
    private void configurarComboEquipo(ComboBox<Equipo> combo) {
        combo.setConverter(new StringConverter<Equipo>() {
            @Override
            public String toString(Equipo equipo) {
                return equipo == null ? "" : equipo.getNombre();
            }
            @Override
            public Equipo fromString(String texto) {
                return null; // no se usa: el combo no es editable
            }
        });
    }

    // Configura un ComboBox de estadios para mostrar solo el nombre del estadio.
    private void configurarComboEstadio(ComboBox<Estadio> combo) {
        combo.setConverter(new StringConverter<Estadio>() {
            @Override
            public String toString(Estadio estadio) {
                return estadio == null ? "" : estadio.getNombre();
            }
            @Override
            public Estadio fromString(String texto) {
                return null; // no se usa: el combo no es editable
            }
        });
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
    }
}
