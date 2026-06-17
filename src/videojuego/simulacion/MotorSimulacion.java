package videojuego.simulacion;

import videojuego.modelo.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MotorSimulacion {

    private final Random random = new Random();

    // Probabilidad de que en un minuto dado ocurra algun acontecimiento. Calibrada para
    // que un partido de 90 minutos genere ~10-12 eventos (similar al modo por tramos).
    // La usa simularMinuto() para el modo en tiempo real de la GUI.
    private static final double PROB_EVENTO_POR_MINUTO = 0.13;

    public void simularTramo(Partido partido) {
        // El minuto base depende del TRAMO que se juega; NO es estado del motor: el Primer
        // Tiempo arranca en 0 y el Segundo en 45. Se calcula desde el estado del partido en
        // vez de acumularlo en un campo, para que cada partido reinicie su numeracion de
        // minutos. El motor es una unica instancia compartida por todos los partidos de la
        // sesion: un campo acumulado arrastraba el minuto del partido anterior (y, al quedar
        // los minutos > 45, el relato etiquetaba el primer tiempo como "Segundo Tiempo").
        int minutoBase = partido.getEstadoActual().getNombre().equals("Segundo Tiempo") ? 45 : 0;
        int cantidadEventos = 3 + random.nextInt(6); // Genera entre 3 y 8 eventos por tramo

        for (int i = 0; i < cantidadEventos; i++) {
            int minuto = minutoBase + 1 + random.nextInt(44); // Genera un minuto entre el minuto base y el siguiente tramo
            generarEvento(partido, minuto);
        }
    }

    // Simula UN unico minuto del partido: con baja probabilidad genera (y registra) un solo
    // evento. Es la base del modo en tiempo real de la GUI, que llama a este metodo una vez
    // por cada minuto a medida que avanza el reloj. Devuelve el evento generado, o null si en
    // ese minuto no paso nada. Lee las tacticas vigentes de cada equipo, asi que un cambio de
    // tactica en vivo impacta en los minutos siguientes.
    public EventoDeportivo simularMinuto(Partido partido, int minuto) {
        if (random.nextDouble() >= PROB_EVENTO_POR_MINUTO) {
            return null; // minuto sin acontecimientos
        }
        return generarEvento(partido, minuto);
    }

    // Logica compartida por simularTramo() y simularMinuto(): elige atacante/defensor, decide
    // el tipo de evento segun las tacticas, selecciona el jugador y registra el evento en el
    // partido. Devuelve el evento creado, o null si no habia jugador disponible.
    private EventoDeportivo generarEvento(Partido partido, int minuto) {
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
        Jugador jugador = obtenerJugadorDisponible(equipoDelJugador, tipo);
        if (jugador == null) return null; // Si no hay jugadores disponibles, no hay evento

        EventoDeportivo evento = new EventoDeportivo(tipo, jugador, equipoDelJugador, minuto);
        partido.registrarEvento(evento);
        return evento;
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

    // Selecciona un jugador disponible con peso segun su posicion y el tipo de evento.
    // Delantero: mas chance de GOL/PENAL; Defensor: mas chance de FALTA/tarjetas;
    // Mediocampista: intermedio; Arquero: chances minimas en ambas categorias.
    private Jugador obtenerJugadorDisponible(Equipo equipo, TipoEvento tipo) {
        List<Jugador> disponibles = new ArrayList<>();
        for (Jugador j : equipo.getJugadores()) {
            if (j.getEstado() == EstadoJugador.DISPONIBLE) {
                disponibles.add(j);
            }
        }
        if (disponibles.isEmpty()) return null;

        double[] pesos = new double[disponibles.size()];
        double totalPeso = 0;
        for (int i = 0; i < disponibles.size(); i++) {
            pesos[i] = getPesoJugador(disponibles.get(i).getPosicion(), tipo);
            totalPeso += pesos[i];
        }

        double r = random.nextDouble() * totalPeso;
        double acumulado = 0;
        for (int i = 0; i < disponibles.size(); i++) {
            acumulado += pesos[i];
            if (r < acumulado) return disponibles.get(i);
        }
        return disponibles.get(disponibles.size() - 1);
    }

    private double getPesoJugador(String posicion, TipoEvento tipo) {
        if (tipo == TipoEvento.GOL || tipo == TipoEvento.PENAL) {
            if ("Delantero".equals(posicion))     return 4.0;
            if ("Mediocampista".equals(posicion)) return 2.0;
            if ("Defensor".equals(posicion))      return 0.5;
            if ("Arquero".equals(posicion))       return 0.05;
        } else if (tipo == TipoEvento.FALTA ||
                   tipo == TipoEvento.TARJETA_AMARILLA ||
                   tipo == TipoEvento.TARJETA_ROJA) {
            if ("Delantero".equals(posicion))     return 0.5;
            if ("Mediocampista".equals(posicion)) return 1.5;
            if ("Defensor".equals(posicion))      return 4.0;
            if ("Arquero".equals(posicion))       return 0.2;
        }
        return 1.0; // LESION y posiciones no reconocidas: peso uniforme
    }
}