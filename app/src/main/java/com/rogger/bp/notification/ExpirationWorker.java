package com.rogger.bp.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rogger.bp.data.dao.ProdutoDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.model.Produto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpirationWorker extends Worker {
    private static final String TAG = "ExpirationWorker";

    public ExpirationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando verificação de validade...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "Nenhum usuário logado. Pulando verificação.");
            return Result.success();
        }

        String userId = user.getUid();
        ProdutoDao dao = BpdDatabase.getDatabase(getApplicationContext()).produtoDao();

        // ⚠️ CORREÇÃO: Usar um método síncrono para o Worker (Worker não lida bem com LiveData)
        // Como o Room não permite Query síncrona que retorna LiveData, vamos buscar todos e filtrar.
        List<Produto> produtos = dao.listarProdutosAtivosSync(userId);

        if (produtos == null || produtos.isEmpty()) {
            Log.d(TAG, "Nenhum produto ativo encontrado para o usuário: " + userId);
            return Result.success();
        }

        // Configura data atual (zerando horas para comparação apenas por dia)
        Calendar calHoje = Calendar.getInstance();
        calHoje.set(Calendar.HOUR_OF_DAY, 0);
        calHoje.set(Calendar.MINUTE, 0);
        calHoje.set(Calendar.SECOND, 0);
        calHoje.set(Calendar.MILLISECOND, 0);
        long hojeMs = calHoje.getTimeInMillis();

        int diasAlerta = NotificationPrefs.getDays(getApplicationContext());
        Log.d(TAG, "Dias para alerta configurados: " + diasAlerta);

        List<Produto> vencidos = new ArrayList<>();
        List<Produto> vencendo = new ArrayList<>();

        for (Produto p : produtos) {
            if (p.getTimestamp() == 0L) continue;

            // Zera horas do timestamp do produto para comparação justa
            Calendar calProd = Calendar.getInstance();
            calProd.setTimeInMillis(p.getTimestamp());
            calProd.set(Calendar.HOUR_OF_DAY, 0);
            calProd.set(Calendar.MINUTE, 0);
            calProd.set(Calendar.SECOND, 0);
            calProd.set(Calendar.MILLISECOND, 0);
            long prodMs = calProd.getTimeInMillis();

            long diffMs = prodMs - hojeMs;
            long diffDias = diffMs / (1000 * 60 * 60 * 24);

            if (diffDias < 0) {
                vencidos.add(p);
            } else if (diffDias <= diasAlerta) {
                vencendo.add(p);
            }
        }

        Log.d(TAG, "Resultados: " + vencidos.size() + " vencidos, " + vencendo.size() + " vencendo.");

        if (!vencendo.isEmpty()) {
            NotificationUtil.showVencendo(getApplicationContext(), vencendo);
        }

        if (!vencidos.isEmpty()) {
            NotificationUtil.showVencidos(getApplicationContext(), vencidos);
        }

        return Result.success();
    }
}
