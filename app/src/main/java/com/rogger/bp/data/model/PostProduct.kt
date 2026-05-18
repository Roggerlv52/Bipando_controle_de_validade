package com.rogger.bp.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

data class PostProduct(

    // ── Room PrimaryKey ───────────────────────────────────────────────────
    var id: Int = 0,
    var userId: String = "",
    val uuid: String = "",
    var name: String = "",
    var note: String = "",
    var barcode: String = "",

    var categoryId: Int = 0,
    var timestamp: Long = 0L,
    var imageUri: String = "",
    var deleted: Boolean = false,

    /** Timestamp do momento em que foi movido para a lixeira. Null se activo. */
    var deletedAt: Long? = null,

    // ── Campos calculados / transientes (@Ignore) ─────────────────────────
    val categoryName: String = "",

    /**
     * Uri do ficheiro local temporário (câmera / galeria).
     * Usada durante o fluxo de add/edit antes do upload.
     * Não persiste no banco; ignorado pelo Room.
     */
    val localUri: Uri? = null,

    /**
     * Dados do utilizador autenticado — preenchidos no DataSource.
     * Não persiste no banco; ignorado pelo Room.
     */
    @Ignore
    val publisher: UserAuth? = null
) {
    /**
     * Construtor secundário sem os campos @Ignore — necessário para o Room
     * reconstruir entidades a partir do banco sem os campos ignorados.
     */
    constructor(
        id: Int,
        userId: String,
        uuid: String,
        name: String,
        note: String,
        barcode: String,
        categoryId: Int,
        categoryName: String,
        timestamp: Long,
        imageUri: String,
        deleted: Boolean,
        deletedAt: Long?
    ) : this(
        id          = id,
        userId      = userId,
        uuid        = uuid,
        name        = name,
        note        = note,
        barcode     = barcode,
        categoryId  = categoryId,
        categoryName = categoryName,
        timestamp   = timestamp,
        imageUri    = imageUri,
        deleted     = deleted,
        deletedAt   = deletedAt,
        localUri    = null,
        publisher   = null
    )
}
