package com.rogger.bp.ui.add.presentation

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

    // ── 1. Verificar/criar imagem pelo barcode ─────────────────────────────

    override fun checkOrCreateImage(barcode: String) {
        if (barcode.isEmpty()) {
            view?.onFailure("Código de barras inválido")
            return
        }

        view?.showProgress(true)

        repository.createImage(PostImage(barcode = barcode), object : SaveImageCallback {
            override fun onSuccess(image: PostImage) {
                view?.showProgress(false)
            }

            override fun onAlreadyExists(image: PostImage) {
                view?.showProgress(false)
                view?.imageAlreadyExists(image)
            }

            override fun onFailure(message: String) {
                view?.onFailure(message)
                view?.showProgress(false)
            }

            override fun onComplete() {}
        })
    }

    // ── 2. Upload de imagem capturada (câmera / galeria) ──────────────────

    override fun uploadImage(image: PostImage) {
        if (image.uri.isEmpty()) {
            view?.onFailure("Nenhuma imagem selecionada")
            return
        }

        view?.showProgress(true)
        repository.uploadImage(image, object : SaveImageCallback {
            override fun onSuccess(image: PostImage) {
                view?.goToHome()
            }

            override fun onAlreadyExists(image: PostImage) {
                view?.imageAlreadyExists(image)
            }

            override fun onFailure(message: String) {
                view?.onFailure(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 3. Salvar produto ──────────────────────────────────────────────────

    override fun saveProduct(product: PostProduct) {
        if (product.name.isBlank()) {
            view?.onFailure("Informe o nome do produto")
            return
        }

        view?.showProgress(true)

        val isLocalImage = product.imageUri.startsWith("file://") ||
                (product.imageUri.startsWith("/") && !product.imageUri.startsWith("//"))

        when {
            // ── Caso 1: usuário selecionou imagem local + tem barcode ──────
            // Faz upload para o Storage global; se já existir, usa a URL existente.
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
                        // Imagem global já existe → usa a URL remota sem fazer novo upload
                        repository.create(product.copy(imageUri = existing.uri), createCallback())
                    }

                    override fun onFailure(message: String) {
                        view?.onFailure(message)
                        view?.showProgress(false)
                    }

                    override fun onComplete() { /* callbacks acima controlam o fluxo */
                    }
                })
            }

            // ── Caso 2: sem imagem local, imageUri vazia, mas tem barcode ──
            // BUGFIX: consulta se já existe imagem global para este barcode.
            // Cobre o cenário de "deletar e re-adicionar o mesmo produto".
            !isLocalImage && product.imageUri.isEmpty() && product.barcode.isNotEmpty() -> {
                repository.createImage(
                    PostImage(barcode = product.barcode),
                    object : SaveImageCallback {
                        override fun onSuccess(image: PostImage) {
                            // Documento recém-criado no Firestore, ainda sem URL de imagem
                            // (uri vazia) — salva o produto sem imagem mesmo.
                            repository.create(product, createCallback())
                        }

                        override fun onAlreadyExists(existing: PostImage) {
                            // Imagem global encontrada → usa a URL existente no produto.
                            // É exatamente o caso de "re-adição após deleção".
                            repository.create(
                                product.copy(imageUri = existing.uri),
                                createCallback()
                            )
                        }

                        override fun onFailure(message: String) {
                            // Falha na consulta → salva o produto sem imagem.
                            // Não bloqueia o cadastro por causa da imagem.
                            repository.create(product, createCallback())
                        }

                        override fun onComplete() {}
                    }
                )
            }

            // ── Caso 3: imageUri já é uma URL remota (http/https) ──────────
            // ou produto sem barcode — salva diretamente sem nenhuma consulta.
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