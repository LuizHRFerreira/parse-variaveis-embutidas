package br.fatec.lab04;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * As três formas de executar o mesmo select.
 * Todas retornam a soma dos valores lidos, só para garantir que os dados são usados.
 */
public class AcessoDados {

    /** Hardcoded: o valor vai concatenado no texto. Cada txid gera um comando SQL diferente. */
    public static double hardcoded(Connection con, long inicio, int total) throws SQLException {
        double soma = 0;
        try (Statement st = con.createStatement()) {
            for (long txid = inicio; txid < inicio + total; txid++) {
                String sql = "select amount"
                           + " from transactions"
                           + " where txid=" + Long.toString(txid);
                try (ResultSet rs = st.executeQuery(sql)) {
                    if (rs.next()) {
                        soma += rs.getDouble(1);
                    }
                }
            }
        }
        return soma;
    }

    /** Firmcoded: usa variável embutida (?), mas prepara o comando de novo a cada volta. */
    public static double firmcoded(Connection con, long inicio, int total) throws SQLException {
        double soma = 0;
        for (long txid = inicio; txid < inicio + total; txid++) {
            try (PreparedStatement st = con.prepareStatement("select amount"
                                                           + " from transactions"
                                                           + " where txid=?")) {
                st.setLong(1, txid);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        soma += rs.getDouble(1);
                    }
                }
            }
        }
        return soma;
    }

    /** Softcoded: prepara uma vez só, fora do laço; dentro dele só troca o valor da variável. */
    public static double softcoded(Connection con, long inicio, int total) throws SQLException {
        double soma = 0;
        try (PreparedStatement st = con.prepareStatement("select amount"
                                                       + " from transactions"
                                                       + " where txid=?")) {
            for (long txid = inicio; txid < inicio + total; txid++) {
                st.setLong(1, txid);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        soma += rs.getDouble(1);
                    }
                }
            }
        }
        return soma;
    }
}
