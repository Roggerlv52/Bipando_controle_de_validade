package com.rogger.bp.ui.base;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rogger.bp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public interface OnDateSelectedListener {
        void onDateSelected(long timestamp, String dataFormatada);
    }

    /**
     * Calcula a diferença em dias entre o timestamp fornecido e o momento atual.
     * Retorna a diferença em dias inteiros.
     */
    public static long calcDifferencInDays(long timestamp) {
        // Zera as horas do momento atual para comparação justa por data
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Zera as horas do timestamp alvo
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long diffInMillis = target.getTimeInMillis() - today.getTimeInMillis();

        // ✅ CORREÇÃO SÉNIOR: Usa divisão decimal e arredondamento próximo para evitar truncamento por fuso horário
        double diffInDaysDouble = (double) diffInMillis / (1000L * 60 * 60 * 24);
        return Math.round(diffInDaysDouble);
    }

    /**
     * Retorna o timestamp do início do dia atual (00:00:00).
     */
    public static long getCurrentTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public interface CategoryCallback {
        void onCategoryCreated(String name);
    }

    public static String getCurrentDateFormatted() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    public static void showDatePicker(
            Context context,
            OnDateSelectedListener listener
    ) {
        showDatePicker(context, System.currentTimeMillis(), listener);
    }

    // ✅ NOVO: Converte texto de data exibido no botão em timestamp de forma segura
    // ✅ ATUALIZAÇÃO SÉNIOR: Aceita o Context para traduzir o padrão de leitura da data de forma inteligente
    public static long parseDateToTimestamp(Context context, String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            String cleanStr = dateStr.trim();
            String padraoData = context.getString(R.string.date_format);

            SimpleDateFormat sdf = new SimpleDateFormat(padraoData, Locale.getDefault());
            Date date = sdf.parse(cleanStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public static void showDatePicker(
            Context context,
            long initialTimestamp,
            OnDateSelectedListener listener
    ) {
        SpinerData sd = new SpinerData();

        DatePickerDialog.OnDateSetListener dateSetListener =
                (datePicker, year, month, day) -> {

                    // Data formatada para exibição
                    String dataFormatada =
                            sd.makeDateString(day, month + 1, year);

                    // Timestamp (data real para salvar no banco)
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day, 0, 0, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    long timestamp = calendar.getTimeInMillis();

                    // Retorna os dois valores
                    if (listener != null) {
                        listener.onDateSelected(timestamp, dataFormatada);
                    }
                };

        // Usa o timestamp recebido para definir o dia, mês e ano iniciais
        Calendar calendar = Calendar.getInstance();
        if (initialTimestamp > 0) {
            calendar.setTimeInMillis(initialTimestamp);
        }
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        context,
                        AlertDialog.THEME_HOLO_LIGHT,
                        dateSetListener,
                        year,
                        month,
                        day
                );

        datePickerDialog.setTitle(context.getString(R.string.edit_validate_fields));
        datePickerDialog.show();
    }

    public static boolean validEditText(EditText editText,Context c) {
        if (editText == null) return false;

        String texto = editText.getText().toString().trim();

        if (texto.isEmpty()) {
            editText.setError(c.getString(R.string.edit_txt_mandatory));
            editText.requestFocus();
            return false;
        }

        if (texto.length() < 4) {
            editText.setError(c.getString(R.string.edit_txt_mandatory_2));
            editText.requestFocus();
            return false;
        }

        editText.setError(null); // limpa erro se estiver ok
        return true;
    }

    public static void dialogCategory(Context context, String title, CategoryCallback callback) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_create_category, null);

        EditText edtNome = view.findViewById(R.id.edt_category_nome);
        TextView txtTitle = view.findViewById(R.id.txt_category_title);
        txtTitle.setText(title);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(context.getString(R.string.dialog_button_cancel), (d, which) -> d.dismiss())
                .setPositiveButton("OK", null) // controla manualmente
                .create();

        dialog.setOnShowListener(d -> {
            Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnOk.setOnClickListener(v -> {
                String nome = edtNome.getText().toString().trim();

                if (nome.isEmpty()) {
                    edtNome.setError(context.getString(R.string.edit_txt_mandatory_3));
                    return;
                }

                if (nome.length() < 4) {
                    edtNome.setError(context.getString(R.string.edit_txt_mandatory_2));
                    return;
                }

                callback.onCategoryCreated(nome);
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}
