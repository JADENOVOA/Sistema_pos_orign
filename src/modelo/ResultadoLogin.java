package modelo;

public class ResultadoLogin {

    public enum Tipo { OK, INCORRECTO, BLOQUEADO, INACTIVO, ERROR }

    private final Tipo tipo;
    private final Usuario usuario;
    private final int intentosRestantes;
    private final int minutosRestantes;

    public ResultadoLogin(Tipo tipo, Usuario usuario, int intentosRestantes, int minutosRestantes) {
        this.tipo = tipo;
        this.usuario = usuario;
        this.intentosRestantes = intentosRestantes;
        this.minutosRestantes = minutosRestantes;
    }

    public Tipo getTipo() { return tipo; }
    public Usuario getUsuario() { return usuario; }
    public int getIntentosRestantes() { return intentosRestantes; }
    public int getMinutosRestantes() { return minutosRestantes; }
}
