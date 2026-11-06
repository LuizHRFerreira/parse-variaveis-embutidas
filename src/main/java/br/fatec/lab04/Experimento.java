package br.fatec.lab04;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Component
public class Experimento implements CommandLineRunner {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final DataSource dataSource;

    @Value("${experimento.execucoes}") private int execucoes;
    @Value("${experimento.txid-inicial}") private long txidInicial;
    @Value("${experimento.rodadas}") private int rodadas;
    @Value("${experimento.incluir-firmcoded}") private boolean incluirFirmcoded;
    @Value("${experimento.saida}") private String saida;

    public Experimento(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Resultado de uma rodada de um modo. */
    record Medicao(Modo modo, int rodada, long tempoMs, Map<String, Long> deltas) {}

    @Override
    public void run(String... args) throws Exception {
        if (List.of(args).contains("--somente-graficos")) {
            somenteGraficos();
            return;
        }
        List<Modo> modos = new ArrayList<>(List.of(Modo.HARDCODED, Modo.SOFTCODED));
        if (incluirFirmcoded) {
            modos.add(1, Modo.FIRMCODED);
        }
        System.out.printf(PT_BR, "%n== Laboratório 04: %,d selects por modo, %d rodada(s) ==%n%n", execucoes, rodadas);

        List<Medicao> medicoes = new ArrayList<>();
        // Uma única conexão: os contadores da v$mystat são da sessão
        try (Connection con = dataSource.getConnection()) {
            // Aquecimento: carrega classes do driver e a tabela no cache de dados
            AcessoDados.softcoded(con, txidInicial, Math.min(execucoes, 5000));

            for (int r = 1; r <= rodadas; r++) {
                for (Modo modo : modos) {
                    EstatisticasOracle.limparCache(con);
                    Map<String, Long> antes = EstatisticasOracle.ler(con);

                    long t0 = System.nanoTime();
                    switch (modo) {
                        case HARDCODED -> AcessoDados.hardcoded(con, txidInicial, execucoes);
                        case FIRMCODED -> AcessoDados.firmcoded(con, txidInicial, execucoes);
                        case SOFTCODED -> AcessoDados.softcoded(con, txidInicial, execucoes);
                    }
                    long tempoMs = (System.nanoTime() - t0) / 1_000_000;

                    Map<String, Long> depois = EstatisticasOracle.ler(con);
                    Map<String, Long> deltas = new java.util.LinkedHashMap<>();
                    for (String nome : EstatisticasOracle.NOMES) {
                        deltas.put(nome, depois.getOrDefault(nome, 0L) - antes.getOrDefault(nome, 0L));
                    }
                    medicoes.add(new Medicao(modo, r, tempoMs, deltas));
                    System.out.printf("rodada %d  %-10s %8.2f s   hard parses: %,d%n",
                            r, modo.rotulo, tempoMs / 1000.0, deltas.get("parse count (hard)"));
                }
            }
        }

        salvarResultados(modos, medicoes);
    }

    private void gerarGraficos(Path dir, List<Modo> modos, List<Double> tempos,
                               List<Double> hardParses, List<Double> cpuParse) throws Exception {
        List<String> rotulos = modos.stream().map(m -> m.rotulo).toList();
        Grafico.salvar(dir.resolve("tempo_total").toString(),
                String.format(PT_BR, "Tempo para %,d selects (média de %d rodadas)", execucoes, rodadas),
                "Segundos", rotulos, tempos, 1);
        Grafico.salvar(dir.resolve("linhas_por_segundo").toString(),
                "Linhas retornadas por segundo", "Linhas/s",
                rotulos, tempos.stream().map(t -> execucoes / t).toList(), 0);
        Grafico.salvar(dir.resolve("hard_parses").toString(),
                "Hard parses no servidor Oracle (v$mystat)", "Hard parses", rotulos, hardParses, 0);
        Grafico.salvar(dir.resolve("cpu_parse").toString(),
                "CPU do servidor gasta com parse (v$mystat)", "Segundos", rotulos, cpuParse, 1);
    }

    /** Refaz os gráficos a partir do resultados/resumo.csv, sem rodar o experimento de novo. */
    private void somenteGraficos() throws Exception {
        Path dir = Path.of(saida);
        List<Modo> modos = new ArrayList<>();
        List<Double> tempos = new ArrayList<>(), hard = new ArrayList<>(), cpuParse = new ArrayList<>();
        List<String> linhas = Files.readAllLines(dir.resolve("resumo.csv"));
        for (String linha : linhas.subList(1, linhas.size())) {
            String[] c = linha.split(";");
            modos.add(Modo.valueOf(c[0].toUpperCase()));
            tempos.add(Double.parseDouble(c[1]));
            hard.add(Double.parseDouble(c[3]));
            cpuParse.add(Double.parseDouble(c[4]));
        }
        gerarGraficos(dir, modos, tempos, hard, cpuParse);
        System.out.println("Gráficos refeitos em " + dir.toAbsolutePath());
    }

    private void salvarResultados(List<Modo> modos, List<Medicao> medicoes) throws Exception {
        Path dir = Path.of(saida);
        Files.createDirectories(dir);

        // CSV com todas as rodadas
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("medicoes.csv")))) {
            out.println("modo;rodada;tempo_ms;parse_total;parse_hard;parse_time_cpu_cs;cpu_sessao_cs");
            for (Medicao m : medicoes) {
                out.printf("%s;%d;%d;%d;%d;%d;%d%n", m.modo().rotulo, m.rodada(), m.tempoMs(),
                        m.deltas().get("parse count (total)"), m.deltas().get("parse count (hard)"),
                        m.deltas().get("parse time cpu"), m.deltas().get("CPU used by this session"));
            }
        }

        // Médias por modo
        Map<Modo, double[]> medias = new EnumMap<>(Modo.class);
        for (Modo modo : modos) {
            List<Medicao> doModo = medicoes.stream().filter(m -> m.modo() == modo).toList();
            double tempo = doModo.stream().mapToLong(Medicao::tempoMs).average().orElse(0) / 1000.0;
            double hard = doModo.stream().mapToLong(m -> m.deltas().get("parse count (hard)")).average().orElse(0);
            double total = doModo.stream().mapToLong(m -> m.deltas().get("parse count (total)")).average().orElse(0);
            double parseCpu = doModo.stream().mapToLong(m -> m.deltas().get("parse time cpu")).average().orElse(0) / 100.0;
            double cpu = doModo.stream().mapToLong(m -> m.deltas().get("CPU used by this session")).average().orElse(0) / 100.0;
            medias.put(modo, new double[]{tempo, total, hard, parseCpu, cpu});
        }

        double tempoHard = medias.get(Modo.HARDCODED)[0];
        System.out.printf("%n%-10s %10s %12s %12s %14s %14s %10s%n", "Modo", "Tempo (s)", "Parse total",
                "Hard parse", "CPU parse (s)", "CPU sessão (s)", "Linhas/s");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("resumo.csv")))) {
            out.println("modo;tempo_medio_s;parse_total;parse_hard;cpu_parse_s;cpu_sessao_s;linhas_por_s;ganho_vs_hardcoded");
            for (Modo modo : modos) {
                double[] v = medias.get(modo);
                double linhasPorSeg = execucoes / v[0];
                System.out.printf("%-10s %10.2f %,12.0f %,12.0f %14.2f %14.2f %,10.0f%n",
                        modo.rotulo, v[0], v[1], v[2], v[3], v[4], linhasPorSeg);
                out.printf(Locale.ROOT, "%s;%.3f;%.0f;%.0f;%.2f;%.2f;%.0f;%.2f%n",
                        modo.rotulo, v[0], v[1], v[2], v[3], v[4], linhasPorSeg, tempoHard / v[0]);
            }
        }

        gerarGraficos(dir, modos,
                modos.stream().map(m -> medias.get(m)[0]).toList(),
                modos.stream().map(m -> medias.get(m)[2]).toList(),
                modos.stream().map(m -> medias.get(m)[3]).toList());

        System.out.printf("%nResultados salvos em %s/ (medicoes.csv, resumo.csv e 4 gráficos PNG)%n",
                dir.toAbsolutePath());
    }
}
