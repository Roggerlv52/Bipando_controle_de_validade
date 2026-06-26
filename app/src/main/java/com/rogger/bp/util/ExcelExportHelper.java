package com.rogger.bp.util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.rogger.bp.data.model.PostProduct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 25/06/2026
 * Hora: 20:32
 */
public class ExcelExportHelper {
    public static void exportToExcel(Context context, List<PostProduct> products) {
        if (products == null || products.isEmpty()) {
            Toast.makeText(context, "Nenhum produto ativo para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File excelFile = new File(downloadsDir, "bipando_relatorio_produtos.csv");

        SimpleDateFormat dateSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        try (FileOutputStream fos = new FileOutputStream(excelFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            // ✅ UTF-8 BOM (Byte Order Mark) garante que as acentuações abram perfeitamente no Excel
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // Cabeçalhos (Semicolons são nativos para Excel em computadores de língua portuguesa)
            writer.write("Produto;Código de Barras;Categoria;Vencimento;Dias Restantes;Notas\n");

            for (PostProduct product : products) {
                long daysLeft = com.rogger.bp.ui.base.Utils.calcDifferencInDays(product.getTimestamp());
                String daysStr = daysLeft < 0 ? "Vencido" : String.valueOf(daysLeft);

                String name = formatarCampoCsv(product.getName());
                String barcode = formatarCampoCsv(product.getBarcode());
                String category = formatarCampoCsv(product.getCategoryName());
                String dateStr = product.getTimestamp() > 0 ? dateSdf.format(new Date(product.getTimestamp())) : "";
                String note = formatarCampoCsv(product.getNote());

                writer.write(String.format("%s;%s;%s;%s;%s;%s\n", name, barcode, category, dateStr, daysStr, note));
            }

            writer.flush();
            Toast.makeText(context, "Planilha guardada em Downloads/bipando_relatorio_produtos.csv!", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao exportar planilha: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatarCampoCsv(String field) {
        if (field == null) return "";
        // Remove quebras de linha e limpa ponto e vírgula para não dividir colunas
        return field.replace("\n", " ").replace("\r", "").replace(";", ",");
    }
}
