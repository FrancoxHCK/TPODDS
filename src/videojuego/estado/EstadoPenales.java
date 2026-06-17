package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoPenales implements IEstadoPartido {

    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Definicion por Penales ===");
    }

    @Override
    public void avanzar(Partido partido) {
        partido.setEstado(new EstadoFinalizado());
    }

    @Override
    public String getNombre() {
        return "Penales";
    }

    @Override
    public boolean permiteSimular() {
        return false;
    }
}
