package com.rogger.bp.ui.commun

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 15:11
 */
interface Cache<T> {
    fun isCached(key: String): Boolean
    fun get(key: String): T?
    fun put(key: String, data: T)
    fun remove(key: String)
    fun clear()
}