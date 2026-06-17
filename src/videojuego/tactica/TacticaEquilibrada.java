package videojuego.tactica;

public class TacticaEquilibrada implements ITactica {
    @Override
    public double getProbabilidadGol() { return 0.25; }

    @Override
    public double getProbabilidadFalta() { return 0.30; }

    @Override
    public double getProbabilidadLesion() { return 0.04; }

    @Override
    public String getNombre() { return "Equilibrada"; }
}
