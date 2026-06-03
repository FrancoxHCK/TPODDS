package videojuego.estado;

import videojuego.modelo.Partido;

public interface IEstadoPartido {
    void iniciar(Partido partido);
    void avanzar(Partido partido);
    String getNombre();
    boolean permiteSimular();
}
