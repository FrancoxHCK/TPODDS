package videojuego.modelo;

public class Jugador {
    private String nombre;
    private String posicion;
    private int numero;
    private EstadoJugador estado;
    private int goles;
    private int amarillas;
    private int rojas;

    public Jugador(String nombre, String posicion, int numero)
    {
        this.nombre = nombre;
        this.posicion = posicion;
        this.numero = numero;
        this.estado = EstadoJugador.DISPONIBLE;
        this.goles = 0;
        this.amarillas = 0;
        this.rojas = 0;
    }

    public void recibirTarjetaAmarilla() {
        this.amarillas ++;
        if (this.amarillas >= 2) {
            this.estado = EstadoJugador.SUSPENDIDO;
        }
    }

    public void recibirTarjetaRoja() {
        this.rojas ++;
        this.estado = EstadoJugador.SUSPENDIDO;
    }

    public void lesionar() {
        this.estado = EstadoJugador.LESIONADO;
    }

    public void marcarGol() {
        this.goles++;
    }

    public String getNombre() { return nombre; }
    public String getPosicion() { return posicion; }
    public int getNumero() { return numero; }
    public EstadoJugador getEstado() { return estado; }
    public int getGoles() { return goles; }
    public int getAmarillas() { return amarillas; }
    public int getRojas() { return rojas; }

    @Override
    public String toString() {
        return numero + " - " + nombre + " (" + posicion + ") - " + estado;
    }
}
