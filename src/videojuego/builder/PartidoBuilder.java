package videojuego.builder;

import videojuego.modelo.Equipo;
import videojuego.modelo.Estadio;
import videojuego.modelo.Partido;
import videojuego.observador.Estadisticas;
import videojuego.observador.Marcador;
import videojuego.observador.RelatoDeportivo;

public class PartidoBuilder implements IBuilder<Partido> {
    private Equipo equipoLocal;
    private Equipo equipoVisitante;
    private Estadio estadio;
    private String modoJuego = "Amistoso";
    private boolean conMarcador = false;
    private boolean conEstadisticas = false;
    private boolean conRelato = false;

    public PartidoBuilder setEquipoLocal(Equipo equipo) {
        this.equipoLocal = equipo;
        return this;
    }

    public PartidoBuilder setEquipoVisitante(Equipo equipo) {
        this.equipoVisitante = equipo;
        return this;
    }

    public PartidoBuilder setEstadio(Estadio estadio) {
        this.estadio = estadio;
        return this;
    }

    public PartidoBuilder setModoJuego(String modoJuego) {
        this.modoJuego = modoJuego;
        return this;
    }

    public PartidoBuilder conMarcador() {
        this.conMarcador = true;
        return this;
    }

    public PartidoBuilder conEstadisticas() {
        this.conEstadisticas = true;
        return this;
    }

    public PartidoBuilder conRelato() {
        this.conRelato = true;
        return this;
    }

    @Override
    public Partido build() {
        if (equipoLocal == null || equipoVisitante == null || estadio == null) {
            throw new IllegalStateException(
                    "Faltan datos obligatorios: equipoLocal, equipoVisitante y estadio."
            );
        }

        Partido partido = new Partido(equipoLocal, equipoVisitante, estadio, modoJuego);

        if (conMarcador)
            partido.agregarObservador(new Marcador(equipoLocal.getNombre(), equipoVisitante.getNombre()));
        if (conEstadisticas)
            partido.agregarObservador(new Estadisticas());
        if (conRelato)
            partido.agregarObservador(new RelatoDeportivo());

        return partido;
    }
}
