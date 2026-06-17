package videojuego.observador;

import videojuego.modelo.EventoDeportivo;
import videojuego.modelo.TipoEvento;

public class Marcador implements IObservadorPartido {
    private String nombreLocal;
    private String nombreVisitante;
    private int golesLocal;
    private int golesVisitante;

    public Marcador(String nombreLocal, String nombreVisitante) {
        this.nombreLocal = nombreLocal;
        this.nombreVisitante = nombreVisitante;
        this.golesLocal = 0;
        this.golesVisitante = 0;
    }

    @Override
    public void actualizar(EventoDeportivo evento) {
        if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
            evento.getJugador().marcarGol();
            if (evento.getEquipo().getNombre().equals(nombreLocal)) {
                golesLocal++;
            } else {
                golesVisitante++;
            }
        }
    }

    public String getResultado() {
        return nombreLocal + " " + golesLocal + " - " + golesVisitante + " " + nombreVisitante;
    }

    public int getGolesLocal() { return golesLocal; }
    public int getGolesVisitante() { return golesVisitante; }
}
