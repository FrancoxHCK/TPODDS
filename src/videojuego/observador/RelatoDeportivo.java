package videojuego.observador;

import videojuego.modelo.EventoDeportivo;
import videojuego.modelo.TipoEvento;
import java.util.ArrayList;
import java.util.List;

public class RelatoDeportivo implements IObservadorPartido {
    private List<String> relato;

    public RelatoDeportivo() {
        this.relato = new ArrayList<>();
    }

    @Override
    public void actualizar(EventoDeportivo evento) {
        String linea = construirRelato(evento);
        relato.add(linea);
        System.out.println(linea);
    }

    private String construirRelato(EventoDeportivo evento) {
        String jugador = evento.getJugador().getNombre();
        String equipo = evento.getEquipo().getNombre();
        int min = evento.getMinuto();

        return switch (evento.getTipo()) {
            case GOL -> "Min " + min + " - GOL de " + jugador + " (" + equipo + ")";
            case FALTA -> "Min " + min + " - Falta cometida por " + jugador;
            case TARJETA_AMARILLA -> "Min " + min + " - Tarjeta AMARILLA para " + jugador;
            case TARJETA_ROJA -> "Min " + min + " - Tarjeta ROJA para " + jugador + ". Se va expulsado.";
            case LESION -> "Min " + min + " - " + jugador + " sale lesionado del campo.";
            case PENAL -> "Min " + min + " - PENAL a favor de " + equipo + ". Va a ejecutar " + jugador;
            case PENAL_CONVERTIDO -> "Min " + min + " - GOOOL de PENAL! Lo convierte " + jugador + " (" + equipo + ")";
            case PENAL_FALLADO -> "Min " + min + " - Penal FALLADO por " + jugador + ". El arquero lo ataja.";
        };
    }

    public List<String> getRelato() { return relato; }

    public String getUltimoEvento() {
        if (relato.isEmpty()) return "Sin eventos aún.";
        return relato.get(relato.size() - 1);
    }
}
