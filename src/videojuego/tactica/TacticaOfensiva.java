package videojuego.tactica;

public class TacticaOfensiva implements ITactica {
    @Override
    public double getProbabilidadGol() { return 0.35; }

    @Override
    public double getProbabilidadFalta() { return 0.30; }

    @Override
    public double getProbabilidadLesion() { return 0.06; }

    @Override
    public String getNombre() { return "Ofensiva"; }

}
