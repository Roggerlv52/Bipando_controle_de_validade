package com.rogger.bp.ui.edit;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rogger.bp.data.model.Produto;

import java.io.File;

public class EditPresenterViewModel extends ViewModel {

    // 🖼️ Controle de imagens
    private File tempImageFile;        // imagem nova ainda não salva
    private String oldImagePath;        // imagem já persistida no produto

    // 📦 Estado do produto
    private final MutableLiveData<Produto> produtoLive = new MutableLiveData<>();
    private final MutableLiveData<Integer> categoriaSelecionadaId = new MutableLiveData<>();

    // -------------------- GETTERS --------------------

    public LiveData<Produto> getProduto() {
        return produtoLive;
    }

    public boolean hasNewImage() {
        return tempImageFile != null;
    }

    // -------------------- PRODUTO --------------------

    public void setProduto(@NonNull Produto produto) {
        produtoLive.setValue(produto);
        categoriaSelecionadaId.setValue(produto.getCategoryId());
        oldImagePath = produto.getImagem();
    }

    // -------------------- IMAGEM --------------------

    public void onNewImage(@NonNull File file) {
        deleteTempImage();
        tempImageFile = file;
    }

    /**
     * Aplica imagem nova no produto (somente no salvar)
     */
    public void applyImageToProduto(@NonNull Produto produto) {

        if (tempImageFile != null) {
            Log.d("EditPresenter", "Nova imagem: " + tempImageFile.getAbsolutePath());

            // Deleta imagem antiga
            if (oldImagePath != null) {
                File oldFile = new File(oldImagePath);
                if (oldFile.exists()) {
                    boolean deleted = oldFile.delete();
                    Log.d("EditPresenter", "Tentando deletar imagem antiga: " + oldImagePath + " → " + deleted);
                } else {
                    Log.d("EditPresenter", "Imagem antiga não existe: " + oldImagePath);
                }
            } else {
                Log.d("EditPresenter", "OldImagePath está nulo, nenhuma imagem para deletar.");
            }

            // Aplica a nova imagem
            produto.setImagem(tempImageFile.getAbsolutePath());
            oldImagePath = tempImageFile.getAbsolutePath();

            // Limpa a referência temporária
            tempImageFile = null;
            Log.d("EditPresenter", "Nova imagem aplicada ao produto: " + produto.getImagem());
        } else {
            Log.d("EditPresenter", "Nenhuma nova imagem para aplicar.");
        }
    }

    public void prepareForNewImage() {
        deleteTempImage();
    }

    private void deleteTempImage() {
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
            tempImageFile = null;
        }
    }

    // -------------------- CAMPOS --------------------
    public void setNome(@NonNull String nome) {
        Produto p = produtoLive.getValue();
        if (p != null) {
            p.setNome(nome);
            produtoLive.setValue(p);
        }
    }

    public void setTimestamp(long timestamp) {
        Produto p = produtoLive.getValue();
        if (p != null) {
            p.setTimestamp(timestamp);
            produtoLive.setValue(p);
        }
    }
    // -------------------- LIFECYCLE --------------------
    @Override
    protected void onCleared() {
        deleteTempImage();
    }

    public void setOldImagePath(String oldImagePath) {
        this.oldImagePath = oldImagePath;
    }

    public String getOldImagePath() {
        return oldImagePath;
    }
}
