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

    public Equipo getEquipoLocal() { return equipoLocal; }
    public Equipo getEquipoVisitante() { return equipoVisitante; }
    public Estadio getEstadio() { return estadio; }
    public String getModoJuego() { return modoJuego; }
    public IEstadoPartido getEstadoActual() { return estadoActual; }
    public List<EventoDeportivo> getEventos() { return eventos; }
}
