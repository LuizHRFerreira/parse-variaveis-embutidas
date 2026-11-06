package br.fatec.lab04;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/** Gráfico de barras simples, salvo em PNG. */
public class Grafico {

    public static void salvar(String arquivo, String titulo, String eixoY,
                              List<String> modos, List<Double> valores, int casasDecimais) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(500)
                .title(titulo)
                .xAxisTitle("Modo de acesso")
                .yAxisTitle(eixoY)
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setLabelsVisible(false);
        chart.getStyler().setYAxisDecimalPattern("#,##0");
        chart.getStyler().setLocale(java.util.Locale.forLanguageTag("pt-BR"));
        chart.getStyler().setAxisTickLabelsFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setSeriesColors(new Color[]{new Color(0x2E, 0x6D, 0xA4)});
        // O valor vai junto do nome da barra, para ficar legível mesmo em barras muito baixas
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("pt-BR"));
        fmt.setMaximumFractionDigits(casasDecimais);
        fmt.setMinimumFractionDigits(casasDecimais);
        List<String> rotulos = new java.util.ArrayList<>();
        for (int i = 0; i < modos.size(); i++) {
            rotulos.add(modos.get(i) + ": " + fmt.format(valores.get(i)));
        }
        chart.addSeries(eixoY, rotulos, valores);
        BitmapEncoder.saveBitmap(chart, arquivo, BitmapEncoder.BitmapFormat.PNG);
    }
}
