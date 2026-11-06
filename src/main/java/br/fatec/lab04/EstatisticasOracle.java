package br.fatec.lab04;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lê da v$mystat os contadores de parse da sessão atual, para mostrar o custo no servidor. */
public class EstatisticasOracle {

    public static final String[] NOMES = {
        "parse count (total)",
        "parse count (hard)",
        "parse time cpu",
        "CPU used by this session"
    };

    public static Map<String, Long> ler(Connection con) throws SQLException {
        String sql = """
            select n.name, s.value
              from v$mystat s join v$statname n on n.statistic# = s.statistic#
             where n.name in ('parse count (total)', 'parse count (hard)',
                              'parse time cpu', 'CPU used by this session')
            """;
        Map<String, Long> valores = new LinkedHashMap<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                valores.put(rs.getString(1), rs.getLong(2));
            }
        }
        return valores;
    }

    /** Limpa o cache de SQL do servidor para cada rodada começar do zero. */
    public static void limparCache(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("alter system flush shared_pool");
        }
    }
}
