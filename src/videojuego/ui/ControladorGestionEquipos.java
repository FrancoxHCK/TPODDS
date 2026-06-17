package videojuego.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import videojuego.fachada.ControladorPartido;
import videojuego.modelo.Equipo;
import videojuego.modelo.Jugador;

// Pantalla de gestion de equipos y jugadores.
// Replica el submenu de gestion de Main (registrar equipo+estadio, agregar jugador,
// ver equipos y ver jugadores) pero de forma grafica. Toda la logica pasa por la
// fachada ControladorPartido; la UI nunca instancia el modelo ni los DAOs.
public class ControladorGestionEquipos {

    private final Navegador navegador;
    private final ControladorPartido fachada;

    // Componentes que se consultan/actualizan desde varios handlers.
    private ListView<Equipo> listaEquipos;
    private ListView<Jugador> listaJugadores;
    private Label lblMensaje;

    public ControladorGestionEquipos(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista de la pantalla.
    public Parent getVista() {
        Label titulo = new Label("Gestion de Equipos");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // --- Zona 1: registrar equipo nuevo (con su estadio) ---
        TextField tfNombreEquipo = new TextField();
        tfNombreEquipo.setPromptText("Nombre del equipo");
        TextField tfNombreEstadio = new TextField();
        tfNombreEstadio.setPromptText("Nombre del estadio");
        Button btnRegistrarEquipo = new Button("Registrar equipo");
        HBox formEquipo = new HBox(10, new Label("Equipo:"), tfNombreEquipo,
                new Label("Estadio:"), tfNombreEstadio, btnRegistrarEquipo);

        // --- Zona 2: listas de equipos y de jugadores del equipo seleccionado ---
        listaEquipos = new ListView<>();
        listaEquipos.setPrefSize(360, 220);
        // Cada celda muestra nombre + tactica + cantidad de jugadores (solo lectura del modelo).
        listaEquipos.setCellFactory(lv -> new ListCell<Equipo>() {
            @Override
            protected void updateItem(Equipo equipo, boolean empty) {
                super.updateItem(equipo, empty);
                if (empty || equipo == null) {
                    setText(null);
                } else {
                    setText(equipo.getNombre() + "  [Tactica: " + equipo.getTactica().getNombre()
                            + " | Jugadores: " + equipo.getJugadores().size() + "]");
                }
            }
        });

        listaJugadores = new ListView<>();
        listaJugadores.setPrefSize(360, 220);
        // Jugador.toString() ya da una linea legible (numero - nombre (posicion) - estado).

        // Al seleccionar un equipo, se muestra su plantilla.
        listaEquipos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> mostrarJugadores(seleccionado));

        VBox cajaEquipos = new VBox(5, new Label("Equipos registrados:"), listaEquipos);
        VBox cajaJugadores = new VBox(5, new Label("Jugadores del equipo seleccionado:"), listaJugadores);
        HBox listas = new HBox(20, cajaEquipos, cajaJugadores);

        // --- Zona 2b: cambiar la tactica de base del equipo seleccionado (se persiste) ---
        ComboBox<String> cbTactica = new ComboBox<>();
        cbTactica.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
        cbTactica.setPromptText("Tactica");
        Button btnCambiarTactica = new Button("Cambiar tactica del equipo seleccionado");
        HBox formTactica = new HBox(10, new Label("Tactica:"), cbTactica, btnCambiarTactica);

        // --- Zona 3: agregar jugador al equipo seleccionado ---
        TextField tfNombreJugador = new TextField();
        tfNombreJugador.setPromptText("Nombre del jugador");
        TextField tfPosicion = new TextField();
        tfPosicion.setPromptText("Posicion (ej: Delantero)");
        Button btnAgregarJugador = new Button("Agregar jugador al equipo seleccionado");
        HBox formJugador = new HBox(10, new Label("Jugador:"), tfNombreJugador,
                new Label("Posicion:"), tfPosicion, btnAgregarJugador);

        // --- Mensajes y navegacion ---
        lblMensaje = new Label();
        Button botonVolver = new Button("Volver al menu");
        botonVolver.setOnAction(e -> navegador.navegarA(Navegador.Pantalla.MENU));

        // --- Handlers ---
        btnRegistrarEquipo.setOnAction(e -> {
            String nombreEquipo = tfNombreEquipo.getText().trim();
            String nombreEstadio = tfNombreEstadio.getText().trim();
            if (nombreEquipo.isEmpty()) {
                mostrarMensaje("El nombre del equipo no puede estar vacio.");
                return;
            }
            if (nombreEstadio.isEmpty()) {
                mostrarMensaje("El nombre del estadio no puede estar vacio.");
                return;
            }
            fachada.registrarEquipoConEstadio(nombreEquipo, nombreEstadio);
            tfNombreEquipo.clear();
            tfNombreEstadio.clear();
            refrescarEquipos();
            mostrarMensaje("Equipo '" + nombreEquipo + "' y estadio '" + nombreEstadio + "' registrados.");
        });

        // Al seleccionar un equipo, el combo muestra su tactica actual.
        listaEquipos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        cbTactica.setValue(seleccionado.getTactica().getNombre());
                    }
                });

        btnCambiarTactica.setOnAction(e -> {
            Equipo equipo = listaEquipos.getSelectionModel().getSelectedItem();
            if (equipo == null) {
                mostrarMensaje("Primero selecciona un equipo de la lista.");
                return;
            }
            String tactica = cbTactica.getValue();
            if (tactica == null) {
                mostrarMensaje("Elegi una tactica del combo.");
                return;
            }
            // Pasa por la fachada: mapea el nombre, aplica y persiste la tactica del equipo.
            fachada.cambiarTacticaEquipo(equipo, tactica);
            listaEquipos.refresh(); // la celda muestra la tactica: la refrescamos
            mostrarMensaje("Tactica de " + equipo.getNombre() + " cambiada a " + tactica + ".");
        });

        btnAgregarJugador.setOnAction(e -> {
            Equipo equipo = listaEquipos.getSelectionModel().getSelectedItem();
            if (equipo == null) {
                mostrarMensaje("Primero selecciona un equipo de la lista.");
                return;
            }
            String nombre = tfNombreJugador.getText().trim();
            if (nombre.isEmpty()) {
                mostrarMensaje("El nombre del jugador no puede estar vacio.");
                return;
            }
            String posicion = tfPosicion.getText().trim();
            fachada.agregarJugadorAEquipo(equipo, nombre, posicion);
            tfNombreJugador.clear();
            tfPosicion.clear();
            // Refrescamos jugadores (plantilla nueva) y equipos (cambio el contador).
            mostrarJugadores(equipo);
            listaEquipos.refresh();
            mostrarMensaje("Jugador '" + nombre + "' agregado a " + equipo.getNombre() + ".");
        });

        // Carga inicial de la lista de equipos.
        refrescarEquipos();

        VBox raiz = new VBox(15, titulo,
                new Label("Registrar equipo nuevo"), formEquipo,
                listas,
                new Label("Cambiar tactica del equipo"), formTactica,
                new Label("Agregar jugador"), formJugador,
                lblMensaje, botonVolver);
        raiz.setPadding(new Insets(25));
        return raiz;
    }

    // Vuelve a pedir los equipos a la fachada y repuebla la lista.
    private void refrescarEquipos() {
        listaEquipos.getItems().setAll(fachada.obtenerEquipos());
    }

    // Muestra la plantilla del equipo dado (o vacia si no hay seleccion).
    private void mostrarJugadores(Equipo equipo) {
        if (equipo == null) {
            listaJugadores.getItems().clear();
        } else {
            listaJugadores.getItems().setAll(equipo.getJugadores());
        }
    }

    // Centraliza el feedback al usuario (reemplaza los System.out.println de Main).
    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
    }
}
