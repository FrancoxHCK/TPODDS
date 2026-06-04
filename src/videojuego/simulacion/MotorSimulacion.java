package videojuego.simulacion;

import videojuego.modelo.*;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MotorSimulacion {

    private final Random random = new Random();
    private int minutoBase = 0; // Minuto base para generar eventos

    public void simularTramo(Partido partido) {
        int cantidadEventos = 3 + random.nextInt(6); // Genera entre 3 y 8 eventos por tramo

        for (int i = 0; i < cantidadEventos; i++) {
            int minuto = minutoBase + 1 + random.nextInt(44); // Genera un minuto entre el minuto base y el siguiente tramo
            Equipo atacante, defensor;
            if (random.nextBoolean()) {
                atacante = partido.getEquipoLocal();
                defensor = partido.getEquipoVisitante();
            } else {
                atacante = partido.getEquipoVisitante();
                defensor = partido.getEquipoLocal();
            }

            TipoEvento tipo = determinarEvento(atacante, defensor);

            //Faltas y tarjetas las comete el defensor, el resto el atacante
            Equipo equipoDelJugador = (tipo == TipoEvento.FALTA ||
                                       tipo == TipoEvento.TARJETA_AMARILLA ||
                                       tipo == TipoEvento.TARJETA_ROJA)
                                        ? defensor : atacante;
            Jugador jugador = obtenerJugadorDisponible(equipoDelJugador);
            if (jugador == null) continue; // Si no hay jugadores disponibles, salta el evento

            partido.registrarEvento(new EventoDeportivo(tipo, jugador, equipoDelJugador, minuto));
        }
        minutoBase += 45; // Avanza el minuto base para el siguiente tramo
    }
    private TipoEvento determinarEvento(Equipo atacante, Equipo defensor) {
        double probGol = atacante.getTactica().getProbabilidadGol();
        double probFalta = defensor.getTactica().getProbabilidadFalta();
        double probLesion = atacante.getTactica().getProbabilidadLesion();

        double r = random.nextDouble();

        if (r < probGol ) return TipoEvento.GOL;
        if (r < probGol + 0.04) return TipoEvento.PENAL;
        if (r < probGol + 0.04 + probFalta * 0.08) return TipoEvento.TARJETA_ROJA;
        if (r < probGol + 0.04 + probFalta * 0.30) return TipoEvento.TARJETA_AMARILLA;
        if (r < probGol + 0.04 + probFalta) return TipoEvento.FALTA;
        if (r < probGol + 0.04 + probFalta + probLesion) return TipoEvento.LESION;

        return TipoEvento.FALTA; // Evento por defecto si no se cumple ninguna condición
    }

    private Jugador obtenerJugadorDisponible(Equipo equipo) {
        List<Jugador> disponibles = equipo.getJugadores().stream()
                .filter(j -> j.getEstado() == EstadoJugador.DISPONIBLE)
                .collect(Collectors.toList());

        if (disponibles.isEmpty()) return null;
        return disponibles.get(random.nextInt(disponibles.size()));
    }
}