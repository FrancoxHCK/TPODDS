package videojuego.modelo;

public enum TipoEvento {
    GOL,
    FALTA,
    TARJETA_AMARILLA,
    TARJETA_ROJA,
    LESION,
    PENAL,            // penal cobrado (falta dentro del area)
    PENAL_CONVERTIDO, // el remate entro — cuenta como gol
    PENAL_FALLADO     // el remate fue rechazado o fuera
}
