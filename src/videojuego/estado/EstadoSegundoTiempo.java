package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoSegundoTiempo implements IEstadoPartido {

    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Comienza el Segundo Tiempo ===");
    }

    @Override
    public void avanzar(Partido partido) {
        partido.setEstado(new EstadoFinalizado());
    }

    @Override
    public String getNombre() {
        return "Segundo Tiempo";
    }

    @Override
    public boolean permiteSimular() {
        return true;
    }
}
