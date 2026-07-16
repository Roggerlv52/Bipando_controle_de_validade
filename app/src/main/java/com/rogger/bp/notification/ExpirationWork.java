package com.rogger.bp.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rogger.bp.data.database.BpDatabase;
import com.rogger.bp.data.model.PostProduct;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpirationWork extends Worker {

    private static final String PREF_USER = "shared_key_date";
    private static final String KEY_UID = "userUid";

    public ExpirationWork(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        String userId = obterUserId();

        if (userId == null || userId.isEmpty()) {
            return Result.success();
        }


        // ── ✅ CORREÇÃO CRÍTICA: Carrega e filtra os produtos do Room offline ──
        BpDatabase database = BpDatabase.Companion.getDatabase(getApplicationContext());
        List<PostProduct> todosProdutosCached = database.productDao().getAllCachedProducts();

        List<PostProduct> produtos = new ArrayList<>();

        if (todosProdutosCached != null) {
            for (PostProduct p : todosProdutosCached) {
                // Filtra apenas produtos do usuário ativo e que não estão na lixeira
                p.getUserId();
                if (p.getUserId().equals(userId) && !p.getDeleted()) {
                    produtos.add(p);
                }
            }
        }

        if (produtos.isEmpty()) {
            return Result.success();
        }

        // Zera horas do dia atual para comparação apenas por data
        Calendar calHoje = Calendar.getInstance();
        calHoje.set(Calendar.HOUR_OF_DAY, 0);
        calHoje.set(Calendar.MINUTE, 0);
        calHoje.set(Calendar.SECOND, 0);
        calHoje.set(Calendar.MILLISECOND, 0);
        long hojeMs = calHoje.getTimeInMillis();

        int diasAlerta = NotificationPrefs.getDays(getApplicationContext());

        List<PostProduct> vencidos = new ArrayList<>();
        List<PostProduct> vencendo = new ArrayList<>();

        for (PostProduct p : produtos) {
            if (p.getTimestamp() == 0L) continue;

            // Zera horas do timestamp do produto para comparação justa por dia
            Calendar calProd = Calendar.getInstance();
            calProd.setTimeInMillis(p.getTimestamp());
            calProd.set(Calendar.HOUR_OF_DAY, 0);
            calProd.set(Calendar.MINUTE, 0);
            calProd.set(Calendar.SECOND, 0);
            calProd.set(Calendar.MILLISECOND, 0);
            long prodMs = calProd.getTimeInMillis();

            long diffMs = prodMs - hojeMs;
            long diffDias = diffMs / (1000L * 60 * 60 * 24);

            if (diffDias < 0) {
                vencidos.add(p);
            } else if (diffDias <= diasAlerta) {
                vencendo.add(p);
            }
        }

        if (!vencendo.isEmpty()) {
            NotificationUtil.showVencendo(getApplicationContext(), vencendo);
        }

        if (!vencidos.isEmpty()) {
            NotificationUtil.showVencidos(getApplicationContext(), vencidos);
        }

        return Result.success();
    }

    private String obterUserId() {
        // Tentativa 1: Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getUid();
            return user.getUid();
        }

        // Tentativa 2: SharedPreferences (fallback offline)
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREF_USER, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UID, null);
    }
}