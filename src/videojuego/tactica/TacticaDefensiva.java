package videojuego.tactica;

public class TacticaDefensiva implements ITactica {
    @Override
    public double getProbabilidadGol() { return 0.15; }

    @Override
    public double getProbabilidadFalta() { return 0.40; }

    @Override
    public double getProbabilidadLesion() { return 0.10; }

    @Override
    public String getNombre() { return "Defensiva"; }
}
