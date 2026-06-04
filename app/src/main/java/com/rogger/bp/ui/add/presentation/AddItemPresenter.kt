package com.rogger.bp.ui.add.presentation

import android.content.Context
import com.rogger.bp.data.image.notification.ImageSyncScheduler
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.data.RegisterItemCallback
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.SaveImageCallback
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback


/**
 * Presenter do fluxo de cadastro de item.
 *
 * Responsabilidades:
 *  1. Verificar se imagem do barcode já existe (checkOrCreateImage).
 *  2. Fazer upload de imagem capturada pela câmera/galeria (uploadImage).
 *  3. Salvar o produto no banco (saveProduct).
 *
 * A View é notificada em onDestroy() para evitar memory leak.
 */
class AddItemPresenter(
    private var view: RegisterAdd.View?,
    private val repository: RegisterItemRepository,
    private val categoryRepository: CategoryRepository
) : RegisterAdd.Presenter {

    // ── 1. Verificar/buscar imagem pelo barcode ───────────────────────────

    /**
     * Chamado assim que o barcode é lido pelo scanner.
     *
     * Fluxo interno (gerenciado por [FireRegisterDataSource]):
     *  a. Busca imagem personalizada do utilizador
     *  b. Se não existir, busca imagem global
     *  c. Se não existir nenhuma, aguarda o utilizador fazer upload
     */
    override fun checkOrCreateImage(barcode: String) {
        if (barcode.isEmpty()) {
            view?.onFailure("Código de barras inválido")
            return
        }

        view?.showProgress(true)

        repository.createImage(PostImage(barcode = barcode), object : SaveImageCallback {
            override fun onSuccess(image: PostImage) {
                // Upload criado com sucesso (imagem global ou personalizada)
                view?.showProgress(false)
            }

            override fun onAlreadyExists(image: PostImage) {
                // Imagem encontrada (global ou personalizada) — mostra na UI
                view?.showProgress(false)
                view?.imageAlreadyExists(image)
            }

            override fun onFailure(message: String) {
                view?.showProgress(false)
                view?.onFailure(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 2. Upload de imagem após utilizador selecionar da câmera/galeria ──

    /**
     * Chamado quando o utilizador escolhe uma nova imagem.
     *
     * O [FireRegisterDataSource] decide automaticamente:
     *  - Se imagem global NÃO existe → cria imagem global
     *  - Se imagem global JÁ existe  → salva como imagem personalizada
     *
     * A View é notificada via [RegisterAdd.View.goToHome] em caso de sucesso.
     */
    override fun uploadImage(image: PostImage) {
        if (image.uri.isEmpty()) {
            view?.onFailure("Nenhuma imagem selecionada")
            return
        }

        view?.showProgress(true)

        repository.uploadImage(image, object : SaveImageCallback {
            override fun onSuccess(image: PostImage) {
                view?.showProgress(false)
                view?.goToHome()
            }

            override fun onAlreadyExists(image: PostImage) {
                // Race condition: imagem global criada por outro utilizador
                // durante o upload → usa URL existente
                view?.showProgress(false)
                view?.imageAlreadyExists(image)
            }

            override fun onFailure(message: String) {
                view?.showProgress(false)
                view?.onFailure(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 3. Salvar produto ─────────────────────────────────────────────────

    /**
     * Salva o produto no Firestore.
     *
     * Lógica de imageUri:
     *  - Se imageUri é local → [FireRegisterDataSource.uploadImage] resolve (global vs personalizada)
     *  - Se imageUri está vazia mas há barcode → tenta reusar imagem existente
     *  - Se imageUri é remota (https) → usa diretamente sem upload
     */
    override fun saveProduct(product: PostProduct,context : Context) {
        if (product.name.isBlank()) {
            view?.onFailure("Informe o nome do produto")
            return
        }
        // ✅ Inicia a sincronização em segundo plano de imagens salvas em modo offline
        ImageSyncScheduler.start(context)
        view?.showProgress(true)

        val isLocalImage = product.imageUri.startsWith("file://") ||
                (product.imageUri.startsWith("/") && !product.imageUri.startsWith("//"))

        when {
            // ── Caso 1: imagem local + barcode → upload via lógica nova ───
            isLocalImage && product.barcode.isNotEmpty() -> {
                val postImage = PostImage(
                    barcode = product.barcode,
                    name = product.name,
                    uri = product.imageUri
                )
                repository.uploadImage(postImage, object : SaveImageCallback {
                    override fun onSuccess(uploaded: PostImage) {
                        repository.create(product.copy(imageUri = uploaded.uri), createCallback())
                    }

                    override fun onAlreadyExists(existing: PostImage) {
                        // Imagem já existe (global ou personalizada) — usa URL existente
                        repository.create(product.copy(imageUri = existing.uri), createCallback())
                    }

                    override fun onFailure(message: String) {
                        view?.showProgress(false)
                        view?.onFailure(message)
                    }

                    override fun onComplete() {
                        /* fluxo controlado pelos callbacks acima */
                        // ✅ Inicia a sincronização em segundo plano de imagens salvas em modo offline

                    }
                })
            }

            // ── Caso 2: sem imagem, mas tem barcode → busca imagem existente
            !isLocalImage && product.imageUri.isEmpty() && product.barcode.isNotEmpty() -> {
                repository.createImage(
                    PostImage(barcode = product.barcode),
                    object : SaveImageCallback {
                        override fun onSuccess(image: PostImage) {
                            repository.create(product, createCallback())
                        }

                        override fun onAlreadyExists(existing: PostImage) {
                            // Reutiliza URL existente (re-adição após deleção)
                            repository.create(
                                product.copy(imageUri = existing.uri),
                                createCallback()
                            )
                        }

                        override fun onFailure(message: String) {
                            // Falha na busca → salva sem imagem (não bloqueia o cadastro)
                            repository.create(product, createCallback())
                        }

                        override fun onComplete() {
                            view?.showProgress(false)
                        }
                    }
                )
            }

            // ── Caso 3: URL remota ou sem barcode → salva diretamente ─────
            else -> {
                repository.create(product, createCallback())
            }
        }
    }

    override fun fetchCategories() {
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                view?.showCategories(categories)
            }

            override fun onFailure(message: String) {
                view?.onFailure(message)
            }

            override fun onComplete() {}
        })
    }

    private fun createCallback() = object : RegisterItemCallback {
        override fun onSuccess(image: PostImage?) {
            view?.goToHome()
        }

        override fun onFailure(message: String) {
            view?.onFailure(message)
        }

        override fun onComplete() {
            view?.showProgress(false)
        }
    }

    override fun onDestroy() {
        view = null
    }
}
