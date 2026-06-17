package videojuego.modelo;
import videojuego.estado.EstadoPrimerTiempo;
import videojuego.estado.IEstadoPartido;
import videojuego.observador.IObservadorPartido;
import java.util.ArrayList;
import java.util.List;

public class Partido {
    private Equipo equipoLocal;
    private Equipo equipoVisitante;
    private Estadio estadio;
    private String modoJuego;
    private IEstadoPartido estadoActual;
    private List<IObservadorPartido> observadores;
    private List<EventoDeportivo> eventos;
    private String resultadoPenales; // null si el partido no fue a penales

    public Partido(Equipo local, Equipo visitante, Estadio estadio, String modoJuego) {
        this.equipoLocal = local;
        this.equipoVisitante = visitante;
        this.estadio = estadio;
        this.modoJuego = modoJuego;
        this.observadores = new ArrayList<>();
        this.eventos = new ArrayList<>();
        this.estadoActual = new EstadoPrimerTiempo();
        // Se invoca iniciar() para que el primer tiempo sea consistente con los demas
        // estados (que llaman iniciar() al transicionar via setEstado()).
        this.estadoActual.iniciar(this);
    }

    public void agregarObservador(IObservadorPartido observador) {
        observadores.add(observador);
    }

    public void notificarObservadores(EventoDeportivo evento) {
        for (IObservadorPartido obs : observadores) {
            obs.actualizar(evento);
        }
    }

    public void registrarEvento(EventoDeportivo evento) {
        eventos.add(evento);
        notificarObservadores(evento);
    }

    public void avanzarEstado() {
        estadoActual.avanzar(this);
    }

    public void setEstado(IEstadoPartido nuevoEstado) {
        this.estadoActual = nuevoEstado;
        nuevoEstado.iniciar(this);
    }

    // Resumen de una linea: equipos, resultado (goles por GOL/PENAL) y total de eventos.
    // El conteo de goles replica la regla del Marcador: GOL o PENAL suma al equipo del evento.
    public String getResumenTexto() {
        int golesLocal = 0;
        int golesVisitante = 0;
        for (EventoDeportivo evento : eventos) {
            if (evento.getTipo() == TipoEvento.GOL || evento.getTipo() == TipoEvento.PENAL_CONVERTIDO) {
                if (evento.getEquipo().getNombre().equals(equipoLocal.getNombre())) {
                    golesLocal++;
                } else {
                    golesVisitante++;
                }
            }
        }
        String resumen = equipoLocal.getNombre() + " " + golesLocal + " - " + golesVisitante + " "
                + equipoVisitante.getNombre()
                + " | Eventos: " + eventos.size();
        if (resultadoPenales != null && !resultadoPenales.isEmpty()) {
            resumen += " | Penales: " + resultadoPenales;
        }
        return resumen;
    }

    public Equipo getEquipoLocal() { return equipoLocal; }
    public Equipo getEquipoVisitante() { return equipoVisitante; }
    public Estadio getEstadio() { return estadio; }
    public String getModoJuego() { return modoJuego; }
    public IEstadoPartido getEstadoActual() { return estadoActual; }
    public List<EventoDeportivo> getEventos() { return eventos; }
    public String getResultadoPenales() { return resultadoPenales; }
    public void setResultadoPenales(String resultadoPenales) { this.resultadoPenales = resultadoPenales; }
}
