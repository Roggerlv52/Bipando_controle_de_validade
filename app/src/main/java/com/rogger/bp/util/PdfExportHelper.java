package com.rogger.bp.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.rogger.bp.R;
import com.rogger.bp.data.model.PostProduct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 25/06/2026
 * Hora: 20:01
 */
public class PdfExportHelper {

    public static void exportToPdf(Context context, List<PostProduct> products) {
        if (products == null || products.isEmpty()) {
            Toast.makeText(context,  context.getString(R.string.msg_no_active_products), Toast.LENGTH_SHORT).show();
            return;
        }
        // ✅ ATUALIZAÇÃO SÉNIOR: Ordenação explícita do menor número de dias restantes para o maior
        products.sort(new Comparator<PostProduct>() {
            @Override
            public int compare(PostProduct p1, PostProduct p2) {
                long dias1 = com.rogger.bp.ui.base.Utils.calcDifferencInDays(p1.getTimestamp());
                long dias2 = com.rogger.bp.ui.base.Utils.calcDifferencInDays(p2.getTimestamp());
                return Long.compare(dias1, dias2);
            }
        });

        PdfDocument document = new PdfDocument();
        // Tamanho de página padrão A4 (595 x 842 pontos)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // 1. Cabeçalho do Relatório
        paint.setColor(Color.parseColor("#1A272F")); // Cor do tema escuro do Bipando
        canvas.drawRect(0, 0, 595, 80, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText(context.getString(R.string.bp_expiration_date_control), 24, 46, paint);

        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format_hour), Locale.getDefault());
        canvas.drawText(context.getString(R.string.report_generated_on) + sdf.format(new Date()), 24, 66, paint);

        // 2. Títulos das Colunas da Tabela
        paint.setColor(Color.BLACK);
        paint.setTextSize(11);
        paint.setFakeBoldText(true);

        canvas.drawText(context.getString(R.string.product), 24, 115, paint);
        canvas.drawText(context.getString(R.string.txt_toolbar_barcode), 220, 115, paint);
        canvas.drawText(context.getString(R.string.expiration_date), 390, 115, paint);
        canvas.drawText(context.getString(R.string.days_remaining), 490, 115, paint);

        // Linha divisória
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(24, 122, 571, 122, paint);

        // 3. Preenchimento Dinâmico das Linhas
        paint.setFakeBoldText(false);
        paint.setTextSize(9);
        int y = 145;

        SimpleDateFormat dateSdf = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());

        for (PostProduct product : products) {
            // Se exceder a margem inferior da folha, abre uma nova página A4 automaticamente
            if (y > 800) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            // Trunca nomes muito longos para não quebrar as colunas
            String name = product.getName();
            if (name != null && name.length() > 28) {
                name = name.substring(0, 25) + "...";
            }

            long daysLeft = com.rogger.bp.ui.base.Utils.calcDifferencInDays(product.getTimestamp());
            String daysStr;
            if (daysLeft < 0) {
                daysStr = context.getString(R.string.group_expired);
            } else if (daysLeft == 0) {
                daysStr = context.getString(R.string.group_today);
            } else if (daysLeft == 1) {
                daysStr = context.getString(R.string.group_tomorrow);
            } else {
                daysStr = String.valueOf(daysLeft);
            }

            canvas.drawText(name != null ? name : "", 24, y, paint);
            canvas.drawText(product.getBarcode() != null ? product.getBarcode() : "", 220, y, paint);
            canvas.drawText(product.getTimestamp() > 0 ? dateSdf.format(new Date(product.getTimestamp())) : "", 390, y, paint);
            canvas.drawText(daysStr, 490, y, paint);

            y += 24;
        }

        document.finishPage(page);

        // Guarda o documento na pasta pública de "Downloads" do telemóvel (Não exige permissões na API 29+)
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pdfFile = new File(downloadsDir, "bipando_relatorio_produtos.pdf");

        try {
            document.writeTo(new FileOutputStream(pdfFile));

            // ✅ CORREÇÃO 3: Gera o URI seguro usando o FileProvider pré-configurado do seu app
            Uri pdfUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    pdfFile
            );

            // ✅ CORREÇÃO 4: Lança o Intent para partilhar o ficheiro em lote com outras apps de forma nativa
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);

            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Compartilhar Relatório PDF:");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
}
