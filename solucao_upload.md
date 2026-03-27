# Solução: Falha no Upload de Imagens para o Firebase Storage

Após analisar os logs e o código-fonte, identifiquei a causa exata do problema. O sistema de upload para o Firebase Storage está implementado corretamente no `FirebaseStorageDataSource`, mas a **ponte** entre a interface do usuário e o repositório está quebrada.

## 1. O Problema Identificado

Nos logs, vemos que uma nova imagem é aplicada ao produto:
`Nova imagem aplicada ao produto: /storage/emulated/0/Android/data/com.rogger.bp/files/Pictures/IMG_...jpg`

No entanto, o `ProdutoRepository` logo em seguida executa apenas a sincronização com o Firestore:
`Produto atualizado no Firestore: 8`

**Por que o upload não acontece?**
O `DataViewModel` e o `EditFragment` estão chamando a versão simplificada do método `atualizar(produto)`, que **não** inclui a lógica de upload de imagem.

## 2. Correções Necessárias

### Passo 1: Atualizar o `DataViewModel.java`
O `DataViewModel` precisa de um método que aceite o callback de upload para que a UI possa monitorar o progresso e o sucesso.

**Arquivo:** `app/src/main/java/com/rogger/bp/ui/viewmodel/DataViewModel.java`

```java
// Adicione este import
import com.rogger.bp.data.database.FirebaseStorageDataSource;

// Altere ou adicione estes métodos:
public void insert(Produto p, FirebaseStorageDataSource.UploadCallback callback) {
    repository.inserir(p, callback);
}

public void update(Produto p, String urlAntiga, FirebaseStorageDataSource.UploadCallback callback) {
    repository.atualizar(p, urlAntiga, callback);
}
```

### Passo 2: Atualizar o `EditFragment.java`
O `EditFragment` deve capturar o caminho da imagem antiga antes de aplicar a nova e passar o callback para o `DataViewModel`.

**Arquivo:** `app/src/main/java/com/rogger/bp/ui/edit/EditFragment.java`

```java
// No método setupClicks(), dentro do btnSave.setOnClickListener:
btnSave.setOnClickListener(v -> {
    if (!Utils.validEditText(edtName)) return;

    // 1. Pega a URL antiga ANTES de aplicar a nova imagem local
    String urlAntiga = editVM.getOldImagePath();
    
    // 2. Coleta os inputs (isso coloca o caminho local no produto)
    collectInputs();

    // 3. Chama o update com callback
    dataViewModel.update(produto, urlAntiga, new FirebaseStorageDataSource.UploadCallback() {
        @Override
        public void onProgresso(int porcentagem) {
            // Opcional: Mostrar barra de progresso
            Log.d("EditFragment", "Upload: " + porcentagem + "%");
        }

        @Override
        public void onSucesso(String urlDownload) {
            Toast.makeText(getContext(), "Produto e imagem salvos!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onErro(Exception e) {
            Log.e("EditFragment", "Erro no upload: " + e.getMessage());
        }
    });

    NavHostFragment.findNavController(this).popBackStack();
});
```

### Passo 3: Correção no `AddFragment.java` (ou similar)
O mesmo erro provavelmente ocorre na tela de adição de novos produtos. Certifique-se de que ao salvar um novo produto, você use `dataViewModel.insert(produto, callback)`.

## 3. Por que isso resolve?

1.  **Ativação do Fluxo de Upload:** Ao usar a assinatura do método que recebe o `callback`, o `ProdutoRepository` entra no bloco `if (temImagemLocal && callback != null)`, que é o único lugar onde o `storageDataSource.uploadImagem` é disparado.
2.  **Sincronização de Dados:** O `ProdutoRepository` cuidará de:
    *   Fazer o upload da imagem local para o Storage.
    *   Pegar a URL pública gerada.
    *   Atualizar o banco de dados local (Room) com a URL `https://...`.
    *   Atualizar o Firestore com a URL final.

## 4. Dica de Depuração (Regras do Storage)

Se após essas mudanças o log mostrar `Falha no upload: 403 Forbidden`, verifique suas regras no Firebase Console:

```firebase
match /produtos/{userId}/{produtoId}/imagem.jpg {
  allow write: if request.auth != null && request.auth.uid == userId;
  allow read: if true;
}
```

Com essas alterações, o caminho da imagem que você viu nos logs será corretamente enviado para o Firebase Storage.
