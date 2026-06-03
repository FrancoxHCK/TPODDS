package videojuego.modelo;

public class Estadio {
    private String nombre;
    private String ciudad;
    private int capacidad;

    public Estadio(String nombre, String ciudad, int capacidad) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.capacidad = capacidad;
    }

    public String getNombre() { return nombre; }
    public String getCiudad() { return ciudad; }
    public int getCapacidad() { return capacidad; }

    @Override
    public String toString() {
        return nombre + " - " + ciudad + " (Capacidad: " + capacidad + ")";
    }
}
