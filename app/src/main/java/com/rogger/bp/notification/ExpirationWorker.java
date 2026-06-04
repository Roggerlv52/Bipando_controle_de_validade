package com.rogger.bp.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

public class ExpirationWorker extends Worker {
    private static final String TAG = "ExpirationWorker";

    private static final String PREF_USER = "shared_key_date";
    private static final String KEY_UID   = "userUid";

    public ExpirationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker iniciado — verificando validade dos produtos.");

        String userId = obterUserId();

        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "userId não disponível. Worker encerrado sem verificar.");
            return Result.success();
        }

        Log.d(TAG, "Verificando produtos para userId: " + userId);

        // ── ✅ CORREÇÃO CRÍTICA: Carrega e filtra os produtos do Room offline ──
        BpDatabase database = BpDatabase.Companion.getDatabase(getApplicationContext());
        List<PostProduct> todosProdutosCached = database.productDao().getAllCachedProducts();

        List<PostProduct> produtos = new ArrayList<>();

        if (todosProdutosCached != null) {
            for (PostProduct p : todosProdutosCached) {
                // Filtra apenas produtos do usuário ativo e que não estão na lixeira
                if (p.getUserId() != null && p.getUserId().equals(userId) && !p.getDeleted()) {
                    produtos.add(p);
                }
            }
        }

        if (produtos.isEmpty()) {
            Log.d(TAG, "Nenhum produto ativo encontrado no Room para este usuário.");
            return Result.success();
        }

        // Zera horas do dia atual para comparação apenas por data
        Calendar calHoje = Calendar.getInstance();
        calHoje.set(Calendar.HOUR_OF_DAY, 0);
        calHoje.set(Calendar.MINUTE,      0);
        calHoje.set(Calendar.SECOND,      0);
        calHoje.set(Calendar.MILLISECOND, 0);
        long hojeMs = calHoje.getTimeInMillis();

        int diasAlerta = NotificationPrefs.getDays(getApplicationContext());
        Log.d(TAG, "Dias de alerta configurados: " + diasAlerta);

        List<PostProduct> vencidos  = new ArrayList<>();
        List<PostProduct> vencendo  = new ArrayList<>();

        for (PostProduct p : produtos) {
            if (p.getTimestamp() == 0L) continue;

            // Zera horas do timestamp do produto para comparação justa por dia
            Calendar calProd = Calendar.getInstance();
            calProd.setTimeInMillis(p.getTimestamp());
            calProd.set(Calendar.HOUR_OF_DAY, 0);
            calProd.set(Calendar.MINUTE,      0);
            calProd.set(Calendar.SECOND,      0);
            calProd.set(Calendar.MILLISECOND, 0);
            long prodMs = calProd.getTimeInMillis();

            long diffMs   = prodMs - hojeMs;
            long diffDias = diffMs / (1000L * 60 * 60 * 24);

            if (diffDias < 0) {
                vencidos.add(p);
            } else if (diffDias <= diasAlerta) {
                vencendo.add(p);
            }
        }

        Log.d(TAG, "Resultado: " + vencidos.size() + " vencido(s), "
                + vencendo.size() + " vencendo.");

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
        if (user != null && user.getUid() != null) {
            Log.d(TAG, "userId obtido via FirebaseAuth.");
            return user.getUid();
        }

        // Tentativa 2: SharedPreferences (fallback offline)
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREF_USER, Context.MODE_PRIVATE);
        String uid = prefs.getString(KEY_UID, null);

        if (uid != null) {
            Log.d(TAG, "userId obtido via SharedPreferences (fallback).");
        } else {
            Log.w(TAG, "userId não encontrado em nenhuma fonte.");
        }

        return uid;
    }
}