package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoPrimerTiempo implements IEstadoPartido {

    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Comienza el Primer Tiempo ===");
    }

    @Override
    public void avanzar(Partido partido) {
        partido.setEstado(new EstadoEntretiempo());
    }

    @Override
    public String getNombre() {
        return "Primer Tiempo";
    }

    @Override
    public boolean permiteSimular() {
        return true;
    }

}
