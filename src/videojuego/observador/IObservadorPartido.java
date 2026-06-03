package videojuego.observador;

import videojuego.modelo.EventoDeportivo;

public interface IObservadorPartido {
    void actualizar(EventoDeportivo evento);
}
