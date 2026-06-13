package videojuego.ui;

// Contrato de navegacion entre pantallas de la interfaz JavaFX.
// MainApp lo implementa; cada controlador lo recibe para poder cambiar de pantalla
// sin acoplarse a la clase concreta MainApp.
public interface Navegador {

    // Identifica cada pantalla disponible. Se deja anidado para no sumar otro archivo.
    enum Pantalla {
        MENU,
        GESTION,
        CONFIGURAR,
        SIMULACION,
        HISTORIAL
    }

    // Cambia la pantalla visible por la indicada.
    void navegarA(Pantalla pantalla);
}
