package videojuego.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import videojuego.fachada.ControladorPartido;
import videojuego.modelo.Equipo;
import videojuego.modelo.Partido;

// Pantalla de simulacion del partido EN TIEMPO REAL. Un reloj (Timeline de JavaFX) avanza
// minuto a minuto: en cada minuto la fachada decide, con baja probabilidad, si ocurre un
// acontecimiento, y el relato/marcador se refrescan al instante. Las tacticas se pueden
// cambiar durante TODO el partido (no solo en el entretiempo): el motor lee la tactica
// vigente al generar cada evento, asi que el cambio impacta en los minutos siguientes.
//
// Tramos (maquina de estados del Partido): Primer Tiempo (min 1-45) -> Entretiempo (pausa,
// el usuario continua cuando quiere) -> Segundo Tiempo (min 46-90) -> Finalizado (se guarda
// en el historial). Toda la logica pasa por la fachada ControladorPartido.
public class ControladorSimulacion {

    // Milisegundos de reloj por cada minuto de partido. 200ms -> un tramo de 45' dura ~9s.
    private static final double MS_POR_MINUTO = 200;
    private static final int MINUTOS_PRIMER_TIEMPO = 45;
    private static final int MINUTOS_PARTIDO = 90;

    private final Navegador navegador;
    private final ControladorPartido fachada;

    private Timeline reloj;     // motor del tiempo real; null hasta que se empieza el partido
    private int minutoActual;   // minuto de juego en curso (0 = aun no arranco)

    // Nodos que se refrescan en cada minuto (se guardan como campos para actualizarlos).
    private Label lblReloj;
    private Label lblFase;
    private Label lblMarcador;
    private Label lblEstadisticas;
    private ListView<String> listaRelato;
    private ComboBox<String> comboLocal;
    private ComboBox<String> comboVisitante;
    private VBox panelControl; // boton que cambia segun la fase (empezar / continuar / fin)
    private Label lblMensaje;  // avisos puntuales (ej: tactica cambiada)

    public ControladorSimulacion(Navegador navegador, ControladorPartido fachada) {
        this.navegador = navegador;
        this.fachada = fachada;
    }

    // Construye y devuelve la vista de la pantalla.
    public Parent getVista() {
        Label titulo = new Label("Simulacion");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button botonVolver = new Button("Volver al menu");
        botonVolver.setOnAction(e -> {
            detenerReloj(); // si el partido seguia corriendo, frenamos el tiempo real
            navegador.navegarA(Navegador.Pantalla.MENU);
        });

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

        // Aviso: si un equipo no tiene jugadores, el motor no puede generar eventos para el,
        // y el partido puede terminar 0-0 sin relato. Lo avisamos para que no confunda.
        Label lblAvisoPlantilla = new Label();
        if (partido.getEquipoLocal().getJugadores().isEmpty()
                || partido.getEquipoVisitante().getJugadores().isEmpty()) {
            lblAvisoPlantilla.setText("Atencion: algun equipo no tiene jugadores cargados; "
                    + "el partido puede no generar eventos. Carga la plantilla en 'Gestion de Equipos'.");
            lblAvisoPlantilla.setStyle("-fx-text-fill: #b00020;");
        }

        lblReloj = new Label();
        lblReloj.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        lblFase = new Label();
        lblFase.setStyle("-fx-font-weight: bold;");
        lblMarcador = new Label();
        lblMarcador.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        lblEstadisticas = new Label();

        Label tituloRelato = new Label("Relato:");
        listaRelato = new ListView<>();
        listaRelato.setPrefHeight(220);

        // Panel de tacticas SIEMPRE visible: se pueden cambiar durante todo el partido.
        Label subTacticas = new Label("Tacticas (se pueden cambiar en cualquier momento):");
        subTacticas.setStyle("-fx-font-weight: bold;");
        comboLocal = new ComboBox<>();
        comboVisitante = new ComboBox<>();
        HBox filaLocal = armarFilaTactica("Local (" + partido.getEquipoLocal().getNombre() + "):",
                partido.getEquipoLocal(), comboLocal);
        HBox filaVisitante = armarFilaTactica("Visitante (" + partido.getEquipoVisitante().getNombre() + "):",
                partido.getEquipoVisitante(), comboVisitante);

        panelControl = new VBox(10);
        lblMensaje = new Label();

        // Minuto inicial segun el tramo (flujo normal: se entra recien configurado).
        this.minutoActual = partido.getEstadoActual().getNombre().equals("Primer Tiempo")
                ? 0 : MINUTOS_PRIMER_TIEMPO;

        refrescar();

        VBox raiz = new VBox(12, titulo, cabecera, lblAvisoPlantilla, lblReloj, lblFase,
                lblMarcador, lblEstadisticas, tituloRelato, listaRelato,
                subTacticas, filaLocal, filaVisitante, panelControl, lblMensaje, botonVolver);
        raiz.setPadding(new Insets(25));
        return raiz;
    }

    // ===================== RELOJ EN TIEMPO REAL =====================

    // Crea y arranca el reloj: dispara tick() una vez por cada minuto de partido.
    private void iniciarReloj() {
        reloj = new Timeline(new KeyFrame(Duration.millis(MS_POR_MINUTO), e -> tick()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        refrescar();
    }

    // Un minuto de juego: avanza el reloj, pide a la fachada simular ese minuto y, al llegar
    // al final de un tramo, avanza la maquina de estados (pausando o frenando el reloj).
    private void tick() {
        String fase = fachada.getPartido().getEstadoActual().getNombre();

        if (fase.equals("Primer Tiempo")) {
            minutoActual++;
            fachada.simularMinuto(minutoActual);
            if (minutoActual >= MINUTOS_PRIMER_TIEMPO) {
                reloj.pause();
                fachada.avanzarTramo(); // Primer Tiempo -> Entretiempo (el usuario continua)
            }
        } else if (fase.equals("Segundo Tiempo")) {
            minutoActual++;
            fachada.simularMinuto(minutoActual);
            if (minutoActual >= MINUTOS_PARTIDO) {
                reloj.stop();
                fachada.avanzarTramo(); // Segundo Tiempo -> Finalizado (se guarda solo)
            }
        }
        refrescar();
    }

    // Reanuda el reloj para el segundo tiempo tras el entretiempo.
    private void continuarSegundoTiempo() {
        fachada.avanzarTramo(); // Entretiempo -> Segundo Tiempo
        if (reloj != null) {
            reloj.play();
        }
        refrescar();
    }

    private void detenerReloj() {
        if (reloj != null) {
            reloj.stop();
            reloj = null;
        }
    }

    // ===================== REFRESCO DE LA VISTA =====================

    // Refresca reloj, fase, marcador, estadisticas, relato, combos y la botonera de control.
    private void refrescar() {
        Partido partido = fachada.getPartido();
        String fase = partido.getEstadoActual().getNombre();

        lblReloj.setText("Minuto " + Math.min(minutoActual, MINUTOS_PARTIDO) + "'");
        lblFase.setText("Fase actual: " + fase);

        if (fachada.getMarcador() != null) {
            lblMarcador.setText(fachada.getMarcador().getResultado());
        }
        if (fachada.getEstadisticas() != null) {
            lblEstadisticas.setText(fachada.getEstadisticas().getResumen());
        }
        listaRelato.getItems().setAll(fachada.obtenerRelato());
        if (!listaRelato.getItems().isEmpty()) {
            listaRelato.scrollTo(listaRelato.getItems().size() - 1); // autoscroll al ultimo
        }

        // Las tacticas se pueden cambiar mientras el partido no este finalizado.
        boolean finalizado = fase.equals("Finalizado");
        comboLocal.setDisable(finalizado);
        comboVisitante.setDisable(finalizado);

        // Botonera de control segun la fase / si el reloj ya arranco.
        panelControl.getChildren().clear();
        if (fase.equals("Primer Tiempo") && reloj == null) {
            Button btnEmpezar = new Button("Empezar partido");
            btnEmpezar.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12 30 12 30;");
            btnEmpezar.setOnAction(e -> iniciarReloj());
            panelControl.getChildren().add(btnEmpezar);
        } else if (fase.equals("Entretiempo")) {
            Label sub = new Label("=== ENTRETIEMPO === (puede ajustar tacticas antes de seguir)");
            sub.setStyle("-fx-font-weight: bold;");
            Button btnContinuar = new Button("Continuar al segundo tiempo");
            btnContinuar.setOnAction(e -> continuarSegundoTiempo());
            panelControl.getChildren().addAll(sub, btnContinuar);
        } else if (fase.equals("Finalizado")) {
            Label fin = new Label("Partido finalizado. Resultado guardado en el historial.");
            fin.setStyle("-fx-font-weight: bold;");
            panelControl.getChildren().add(fin);
        } else { // Primer/Segundo Tiempo con el reloj corriendo
            Label enCurso = new Label("Partido en curso...");
            panelControl.getChildren().add(enCurso);
        }
    }

    // Arma una fila: etiqueta + combo de tacticas + boton "Aplicar" para un equipo.
    // El combo recibido se guarda como campo para poder habilitarlo/deshabilitarlo.
    private HBox armarFilaTactica(String etiqueta, Equipo equipo, ComboBox<String> combo) {
        combo.getItems().setAll("Ofensiva", "Defensiva", "Equilibrada");
        combo.setValue(equipo.getTactica().getNombre()); // muestra la tactica actual

        Button btnAplicar = new Button("Aplicar");
        btnAplicar.setOnAction(e -> {
            String elegida = combo.getValue();
            if (elegida != null) {
                // Pasa por la fachada: mapea el nombre y aplica con la guarda de finalizado.
                fachada.cambiarTacticaEnVivo(equipo, elegida);
                lblMensaje.setText("Minuto " + Math.min(minutoActual, MINUTOS_PARTIDO)
                        + "': tactica de " + equipo.getNombre() + " cambiada a " + elegida + ".");
            }
        });

        HBox fila = new HBox(10, new Label(etiqueta), combo, btnAplicar);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}