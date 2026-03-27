package com.rogger.bp.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.dao.ProdutoDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.model.Produto;

import java.util.ArrayList;
import java.util.List;

public class ExpirationWorker extends Worker {
    public ExpirationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ProdutoDao dao =
                BpdDatabase.getDatabase(getApplicationContext())
                        .produtoDao();
        String  userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<Produto> produtos = dao.listarProdutosAtivos(userId).getValue();

        long agora = System.currentTimeMillis();
        int diasAlerta = NotificationPrefs.getDays(getApplicationContext());

        List<Produto> vencidos = new ArrayList<>();
        List<Produto> vencendo = new ArrayList<>();

        for (Produto p : produtos) {
            if (p.getTimestamp() == 0L) continue;

            long diff = p.getTimestamp() - agora;
            long dias = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));

            if (dias < 0) {
                vencidos.add(p);
            } else if (dias <= diasAlerta) {
                vencendo.add(p);
            }
        }

        if (!vencendo.isEmpty()) {
            NotificationUtil.showVencendo(
                    getApplicationContext(), vencendo
            );
        }

        if (!vencidos.isEmpty()) {
            NotificationUtil.showVencidos(
                    getApplicationContext(), vencidos
            );
        }

        return Result.success();
    }
}
