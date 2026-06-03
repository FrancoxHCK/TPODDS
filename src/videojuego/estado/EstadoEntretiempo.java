package videojuego.estado;

import videojuego.modelo.Partido;

public class EstadoEntretiempo implements IEstadoPartido {
    @Override
    public void iniciar(Partido partido) {
        System.out.println("=== Entretiempo ===");
    }

    @Override
    public void avanzar(Partido partido) {
        partido.setEstado(new EstadoSegundoTiempo());
    }

    @Override
    public String getNombre() {
        return "Entretiempo";
    }

    @Override
    public boolean permiteSimular() {
        return false;
}
