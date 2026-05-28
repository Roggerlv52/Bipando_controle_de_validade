package com.rogger.bp.data.model

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 28/05/2026
 * Hora: 20:10
 */

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Representa a imagem personalizada de um produto vinculada a um usuário específico.
 *
 * Coleção Firestore: users/{uid}/productImages/{barcode}
 * Storage path:      /produtos/{uid}/{barcode}.jpg
 *
 * Esta imagem NUNCA sobrescreve a imagem global (PostImage / imageProdutos/{barcode}).
 * Ela é privada: apenas o dono (uid) pode ler e escrever.
 */
data class UserProductImage(

    @get:PropertyName("barcode")
    @set:PropertyName("barcode")
    var barcode: String = "",

    @get:PropertyName("customImageUrl")
    @set:PropertyName("customImageUrl")
    var customImageUrl: String = "",

    /**
     * Timestamp da última atualização. Permite ordenação e auditoria.
     * Usar com @ServerTimestamp para garantir consistência de servidor.
     */
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null
)
