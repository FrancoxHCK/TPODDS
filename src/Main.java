// Punto de entrada del proyecto, en la raiz (sin paquete).
// NO extiende Application: delega el arranque de la interfaz grafica a
// videojuego.ui.MainApp. Esto permite ejecutar la app desde el classpath
// sin configurar el module-path, evitando el error
// "JavaFX runtime components are missing" que aparece al correr MainApp directo.
public class Main {

    public static void main(String[] args) {
        videojuego.ui.MainApp.main(args);
    }
}

