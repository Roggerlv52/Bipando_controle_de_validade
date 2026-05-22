package com.rogger.bp.data.dao

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 15:11
 */
interface Cache<T> {
    fun isCached(key: String): Boolean
    fun get(key: String): T?
    fun put(key: String, data: T)
    suspend fun remove(key: String)
    suspend fun clear()
}