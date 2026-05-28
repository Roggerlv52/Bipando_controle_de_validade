package com.rogger.bp.data.image.repository

import android.util.Log
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource
/*
 * Desenvolvido por Roger de Oliveira
 * Data: 28/05/2026
 * Hora: 20:19
 */
/**
 * Repositório central de resolução de imagem de produto.
 *
 * RESPONSABILIDADE ÚNICA: orquestrar o fallback em 3 camadas:
 *
 *   1. Imagem personalizada do utilizador  → users/{uid}/productImages/{barcode}
 *   2. Imagem global                       → imageProdutos/{barcode}
 *   3. Sem imagem                          → permitir upload global
 *
 * Este repositório NUNCA decide sozinho se vai sobrescrever a imagem global.
 * Quem chama (Presenter) decide o que fazer com cada [ImageResult].
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  ARQUITETURA DE IMAGENS                                             │
 * │                                                                     │
 * │  resolveImage(barcode)                                              │
 * │      │                                                              │
 * │      ├─ UserImageDataSource.fetchUserImage()                        │
 * │      │      ├─ CustomImage  ──► retorna imediatamente               │
 * │      │      └─ NoImage / Error ──► continua                         │
 * │      │                                                              │
 * │      └─ GlobalImageDataSource.fetchGlobalImage()                    │
 * │             ├─ GlobalImage ──► retorna                              │
 * │             └─ NoImage     ──► retorna (UI oferece upload)           │
 * │                                                                     │
 * │  uploadGlobalImage()   ──► cria imagem global (1ª vez por barcode)  │
 * │  saveUserImage()       ──► cria/substitui imagem personalizada       │
 * └─────────────────────────────────────────────────────────────────────┘
 */
class ImageResolutionRepository(
    private val userImageDataSource: UserImageDataSource,
    private val globalImageDataSource: GlobalImageDataSource
) {

    private val TAG = "ImageResolutionRepo"

    // ── 1. Resolver imagem com fallback ───────────────────────────────────

    /**
     * Resolve a imagem para exibição dado um [barcode].
     *
     * Prioridade:
     *  1. Imagem personalizada do utilizador (privada)
     *  2. Imagem global (partilhada, imutável após criação)
     *  3. [ImageResult.NoImage] — UI deve oferecer upload
     *
     * Erros de rede são propagados como [ImageResult.Error].
     * Erros na busca personalizada NÃO bloqueiam a busca global
     * (degradação graciosa).
     */
    suspend fun resolveImage(barcode: String): ImageResult {
        if (barcode.isBlank()) {
            return ImageResult.NoImage
        }

        Log.d(TAG, "Resolvendo imagem para barcode=$barcode")

        // ── Passo 1: Imagem personalizada ─────────────────────────────────
        val userResult = userImageDataSource.fetchUserImage(barcode)
        if (userResult is ImageResult.CustomImage) {
            Log.d(TAG, "Usando imagem personalizada para barcode=$barcode")
            return userResult
        }

        // Erros de autenticação são fatais — não faz sentido continuar
        if (userResult is ImageResult.Error && userResult.message == "Utilizador não autenticado") {
            return userResult
        }

        // ── Passo 2: Imagem global ────────────────────────────────────────
        val globalResult = globalImageDataSource.fetchGlobalImage(barcode)
        if (globalResult is ImageResult.GlobalImage) {
            Log.d(TAG, "Usando imagem global para barcode=$barcode")
            return globalResult
        }

        // ── Passo 3: Sem imagem ───────────────────────────────────────────
        Log.d(TAG, "Nenhuma imagem encontrada para barcode=$barcode")
        return ImageResult.NoImage
    }

    // ── 2. Upload de imagem global (1ª vez — cria, nunca sobrescreve) ─────

    /**
     * Cria a imagem GLOBAL para o [barcode] se não existir.
     *
     * Use apenas quando [resolveImage] retornar [ImageResult.NoImage].
     *
     * Se a imagem global já existir (race condition entre utilizadores),
     * retorna [UploadResult.Error] com prefixo "ALREADY_EXISTS:<url>".
     * O Presenter deve detetar esse caso e usar a URL existente.
     *
     * @param barcode      código de barras do produto
     * @param productName  nome do produto para gravar nos metadados
     * @param imageUri     URI local da imagem
     */
    suspend fun uploadGlobalImage(
        barcode: String,
        productName: String,
        imageUri: String
    ): UploadResult {
        Log.d(TAG, "Criando imagem global para barcode=$barcode")
        return globalImageDataSource.createGlobalImageIfAbsent(barcode, productName, imageUri)
    }

    // ── 3. Salvar imagem personalizada (utilizador altera a sua view) ──────

    /**
     * Salva a imagem personalizada do utilizador para o [barcode].
     *
     * NUNCA afeta a imagem global.
     * Pode ser chamado quantas vezes o utilizador quiser.
     *
     * @param barcode   código de barras do produto
     * @param imageUri  URI local da nova imagem
     */
    suspend fun saveUserImage(barcode: String, imageUri: String): UploadResult {
        Log.d(TAG, "Salvando imagem personalizada para barcode=$barcode")
        return userImageDataSource.saveUserImage(barcode, imageUri)
    }

    // ── 4. Remover imagem personalizada ───────────────────────────────────

    /**
     * Remove a imagem personalizada do utilizador.
     * Após remoção, o fallback para a imagem global volta a funcionar.
     *
     * @return true se removido com sucesso
     */
    suspend fun removeUserImage(barcode: String): Boolean {
        Log.d(TAG, "Removendo imagem personalizada para barcode=$barcode")
        return userImageDataSource.removeUserImage(barcode)
    }

    /**
     * Verifica se existe imagem global para o [barcode] sem carregar a URL.
     * Útil para decisões de upload sem precisar do resultado completo.
     */
    suspend fun globalImageExists(barcode: String): Boolean {
        val result = globalImageDataSource.fetchGlobalImage(barcode)
        return result is ImageResult.GlobalImage
    }
}
