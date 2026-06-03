package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoFinalizado implements IEstadoPartido {
    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Partido Finalizado ===");
    }

    @Override
    public void avanzar(Partido partido) {
        System.out.println("El partido ya ha finalizado.");
    }

    @Override
    public String getNombre() {
        return "Finalizado";
    }

    @Override
    public boolean permiteSimular() {
        return false;
    }
}
