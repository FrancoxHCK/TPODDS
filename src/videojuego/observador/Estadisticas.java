package videojuego.observador;

import videojuego.modelo.EventoDeportivo;
import videojuego.modelo.TipoEvento;


public class Estadisticas implements IObservadorPartido {
    private int faltas;
    private int tarjetasAmarillas;
    private int tarjetasRojas;
    private int penales;
    private int lesiones;

    @Override
    public void actualizar(EventoDeportivo evento) {
        switch (evento.getTipo()) {
            case FALTA -> faltas++;
            case TARJETA_AMARILLA -> {
                tarjetasAmarillas++;
                evento.getJugador().recibirTarjetaAmarilla();
            }
            case TARJETA_ROJA -> {
                tarjetasRojas++;
                evento.getJugador().recibirTarjetaRoja();
            }
            case PENAL -> penales++;
            case LESION -> {
                lesiones++;
                evento.getJugador().lesionar();
            }
            default -> {}
        }
    }

    public String getResumen() {
        return "Faltas: " + faltas +
                " | Amarillas: " + tarjetasAmarillas +
                " | Rojas: " + tarjetasRojas +
                " | Penales: " + penales +
                " | Lesiones: " + lesiones;
    }
}
