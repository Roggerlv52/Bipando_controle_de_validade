package com.rogger.bp.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class PostProduct(

    // ── Room PrimaryKey ───────────────────────────────────────────────────
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val uuid: String = "",
    var name: String = "",
    var note: String = "",
    var barcode: String = "",

    val categoryId: Int = 0,
    var timestamp: Long = 0L,
    val imageUri: String = "",
    val deleted: Boolean = false,

    /** Timestamp do momento em que foi movido para a lixeira. Null se activo. */
    val deletedAt: Long? = null,

    // ── Campos calculados / transientes (@Ignore) ─────────────────────────
    @Ignore
    val categoryName: String = "",

    /**
     * Uri do ficheiro local temporário (câmera / galeria).
     * Usada durante o fluxo de add/edit antes do upload.
     * Não persiste no banco; ignorado pelo Room.
     */
    @Ignore
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
        timestamp   = timestamp,
        imageUri    = imageUri,
        deleted     = deleted,
        deletedAt   = deletedAt,
        categoryName = "",
        localUri    = null,
        publisher   = null
    )
}
