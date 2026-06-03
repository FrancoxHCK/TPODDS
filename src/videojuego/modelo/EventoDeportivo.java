package videojuego.modelo;

public class EventoDeportivo {
    private TipoEvento tipo;
    private Jugador jugador;
    private Equipo equipo;
    private int minuto;

    public EventoDeportivo(TipoEvento tipo, Jugador jugador, Equipo equipo, int minuto) {
        this.tipo = tipo;
        this.jugador = jugador;
        this.equipo = equipo;
        this.minuto = minuto;
    }

    public TipoEvento getTipo() { return tipo; }
    public Jugador getJugador() { return jugador; }
    public Equipo getEquipo() { return equipo; }
    public int getMinuto() { return minuto; }

    @Override
    public String toString() {
        return "Min " + minuto + " - " + tipo + " de " + jugador.getNombre() + " (" + equipo.getNombre() + ")";
    }
}
