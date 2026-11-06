package br.fatec.lab04;

public enum Modo {
    HARDCODED("Hardcoded"),
    FIRMCODED("Firmcoded"),
    SOFTCODED("Softcoded");

    public final String rotulo;

    Modo(String rotulo) {
        this.rotulo = rotulo;
    }
}
