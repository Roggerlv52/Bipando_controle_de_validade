package com.rogger.bp.ui.base;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

public class NameTextwatcher implements TextWatcher {

    private final EditText editText;
    private final int minLength;
    private boolean hasError = false;

    public NameTextwatcher(@NonNull EditText editText, int minLength) {
        this.editText = editText;
        this.minLength = minLength;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        String text = s.toString().trim();

        if (text.isEmpty()) {
            setError("Campo obrigatório");
            return;
        }

        if (text.length() < minLength) {
            setError("Mínimo de " + minLength + " caracteres");
            return;
        }

        clearError();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void setError(String message) {
        if (!hasError) {
            editText.setError(message);
            editText.requestFocus();
            hasError = true;
        }
    }

    private void clearError() {
        editText.setError(null);
        hasError = false;
    }

    public boolean isValid() {
        return !hasError;
    }
}

