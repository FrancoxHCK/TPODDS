package videojuego.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import videojuego.fachada.ControladorPartido;
import videojuego.modelo.Equipo;
import videojuego.modelo.Partido;

// Pantalla de simulacion del partido. Replica graficamente el flujo de Main.jugarPartido()
// + menuEntretiempo(): se juega por tramos (1er tiempo -> entretiempo interactivo con cambio
// de tactica -> 2do tiempo) mostrando marcador, relato y estadisticas en vivo. Toda la logica
// pasa por la fachada ControladorPartido (el partido ya viene configurado desde la pantalla b).
public class ControladorSimulacion {

    private final Navegador navegador;
    private final ControladorPartido fachada;

    // Nodos que se refrescan tras cada tramo (se guardan como campos para actualizarlos).
    private Label lblFase;
    private Label lblMarcador;
    private Label lblEstadisticas;
    private ListView<String> listaRelato;
    private VBox panelAcciones; // contenedor de los controles que cambian segun la fase
    private Label lblMensaje;   // avisos puntuales (ej: tactica cambiada)

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

        // Guarda: la pantalla es alcanzable directo desde el menu. Si no hay partido
        // configurado, no hay nada que simular: avisamos y ofrecemos volver.
        Partido partido = fachada.getPartido();
        if (partido == null) {
            Label aviso = new Label("No hay partido configurado. Configure uno primero desde 'Configurar Partido'.");
            VBox raizVacia = new VBox(20, titulo, aviso, botonVolver);
            raizVacia.setAlignment(Pos.CENTER);
            raizVacia.setPadding(new Insets(40));
            return raizVacia;
        }

        // Cabecera fija con los datos del partido.
        Label cabecera = new Label(partido.getEquipoLocal().getNombre()
                + " vs " + partido.getEquipoVisitante().getNombre()
                + "  |  Estadio: " + partido.getEstadio().getNombre()
                + "  |  Modo: " + partido.getModoJuego());

        lblFase = new Label();
        lblFase.setStyle("-fx-font-weight: bold;");
        lblMarcador = new Label();
        lblMarcador.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        lblEstadisticas = new Label();

        Label tituloRelato = new Label("Relato:");
        listaRelato = new ListView<>();
        listaRelato.setPrefHeight(220);

        panelAcciones = new VBox(10);
        lblMensaje = new Label();

        // Pintamos el estado inicial (el partido ya arranca en Primer Tiempo).
        refrescar();

        VBox raiz = new VBox(12, titulo, cabecera, lblFase, lblMarcador, lblEstadisticas,
                tituloRelato, listaRelato, panelAcciones, lblMensaje, botonVolver);
        raiz.setPadding(new Insets(25));
        return raiz;
    }

    // Refresca marcador, relato, estadisticas y la botonera segun la fase actual del partido.
    private void refrescar() {
        Partido partido = fachada.getPartido();
        String fase = partido.getEstadoActual().getNombre();
        lblFase.setText("Fase actual: " + fase);

        if (fachada.getMarcador() != null) {
            lblMarcador.setText(fachada.getMarcador().getResultado());
        }
        if (fachada.getEstadisticas() != null) {
            lblEstadisticas.setText(fachada.getEstadisticas().getResumen());
        }
        listaRelato.getItems().setAll(fachada.obtenerRelato());

        // Reconstruimos los controles de accion segun la fase.
        panelAcciones.getChildren().clear();
        lblMensaje.setText("");
        if (fase.equals("Primer Tiempo")) {
            armarPanelPrimerTiempo();
        } else if (fase.equals("Entretiempo")) {
            armarPanelEntretiempo(partido);
        } else if (fase.equals("Segundo Tiempo")) {
            armarPanelSegundoTiempo();
        } else { // Finalizado
            armarPanelFinalizado();
        }
    }

    // Fase 1er tiempo: un boton que simula el tramo y avanza al entretiempo.
    private void armarPanelPrimerTiempo() {
        Button btnSimular = new Button("Simular primer tiempo");
        btnSimular.setOnAction(e -> {
            fachada.simularTramo(); // simula el 1er tiempo y deja el partido en Entretiempo
            refrescar();
        });
        panelAcciones.getChildren().add(btnSimular);
    }

    // Fase entretiempo: cambio de tactica de cada equipo (opcional) y boton para continuar.
    private void armarPanelEntretiempo(Partido partido) {
        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();

        Label sub = new Label("=== ENTRETIEMPO === (puede cambiar tacticas antes del segundo tiempo)");
        sub.setStyle("-fx-font-weight: bold;");

        HBox filaLocal = armarFilaTactica("Tactica " + local.getNombre() + ":", local);
        HBox filaVisitante = armarFilaTactica("Tactica " + visitante.getNombre() + ":", visitante);

        Button btnContinuar = new Button("Continuar al segundo tiempo");
        btnContinuar.setOnAction(e -> {
            // La maquina de estados exige pasar por el entretiempo: la primera llamada lo
            // deja atras (sin simular), la segunda simula el 2do tiempo, finaliza y guarda solo.
            fachada.simularTramo(); // entretiempo -> segundo tiempo
            fachada.simularTramo(); // segundo tiempo -> finalizado (se guarda en el historial)
            refrescar();
        });

        panelAcciones.getChildren().addAll(sub, filaLocal, filaVisitante, btnContinuar);
    }

    // Arma una fila: etiqueta + combo de tacticas + boton "Aplicar" para un equipo.
    private HBox armarFilaTactica(String etiqueta, Equipo equipo) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
        combo.setValue(equipo.getTactica().getNombre()); // muestra la tactica actual

        Button btnAplicar = new Button("Aplicar");
        btnAplicar.setOnAction(e -> {
            String elegida = combo.getValue();
            if (elegida != null) {
                // Pasa por la fachada: mapea el nombre y aplica con la guarda de finalizado.
                fachada.cambiarTacticaEnVivo(equipo, elegida);
                lblMensaje.setText("Tactica de " + equipo.getNombre() + " cambiada a " + elegida + ".");
            }
        });

        HBox fila = new HBox(10, new Label(etiqueta), combo, btnAplicar);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    // Fase 2do tiempo (camino alternativo): boton para simularlo y finalizar. Normalmente no
    // se ve, porque "Continuar al segundo tiempo" encadena los dos tramos en un solo paso.
    private void armarPanelSegundoTiempo() {
        Button btnSimular = new Button("Simular segundo tiempo");
        btnSimular.setOnAction(e -> {
            fachada.simularTramo(); // segundo tiempo -> finalizado (se guarda en el historial)
            refrescar();
        });
        panelAcciones.getChildren().add(btnSimular);
    }

    // Fase finalizado: sin acciones; el partido ya quedo guardado en el historial.
    private void armarPanelFinalizado() {
        Label fin = new Label("Partido finalizado. Resultado guardado en el historial.");
        fin.setStyle("-fx-font-weight: bold;");
        panelAcciones.getChildren().add(fin);
    }
}
