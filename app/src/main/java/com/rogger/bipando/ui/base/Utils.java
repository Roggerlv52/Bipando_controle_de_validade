package com.rogger.bipando.ui.base;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rogger.bipando.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public interface OnDateSelectedListener {
        void onDateSelected(long timestamp, String dataFormatada);
    }
    public static long calcDifferencInDays(long timestamp) {

        long agora = System.currentTimeMillis();

        // diferença em milissegundos
        long diferenca = timestamp - agora;

        // converter para horas
        long horas = diferenca / (1000 * 60 * 60);

        return horas;
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
        SpinerData sd = new SpinerData();

        DatePickerDialog.OnDateSetListener dateSetListener =
                (datePicker, year, month, day) -> {

                    // 📌 Data formatada para exibição
                    String dataFormatada =
                            sd.makeDateString(day, month + 1, year);

                    // 📌 Timestamp (data real para salvar no banco)
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day, 0, 0, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    long timestamp = calendar.getTimeInMillis();

                    // 🔁 Retorna os dois valores
                    if (listener != null) {
                        listener.onDateSelected(timestamp, dataFormatada);
                    }
                };

        int[] resultado = sd.retornaVetor("");
        int day = resultado[0];
        int month = resultado[1];
        int year = resultado[2];

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        context,
                        AlertDialog.THEME_HOLO_LIGHT,
                        dateSetListener,
                        year,
                        month,
                        day
                );

        datePickerDialog.setTitle("Selecione a data de vencimento");
        datePickerDialog.show();
    }

    public static boolean validEditText(EditText editText) {
        if (editText == null) return false;

        String texto = editText.getText().toString().trim();

        if (texto.isEmpty()) {
            editText.setError("Campo obrigatório");
            editText.requestFocus();
            return false;
        }

        if (texto.length() < 4) {
            editText.setError("Mínimo de 4 caracteres");
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
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .setPositiveButton("OK", null) // controla manualmente
                .create();

        dialog.setOnShowListener(d -> {
            Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnOk.setOnClickListener(v -> {
                String nome = edtNome.getText().toString().trim();

                if (nome.isEmpty()) {
                    edtNome.setError("Informe um nome");
                    return;
                }

                if (nome.length() < 4) {
                    edtNome.setError("Mínimo 4 caracteres");
                    return;
                }

                callback.onCategoryCreated(nome);
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}