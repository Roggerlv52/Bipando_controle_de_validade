package com.rogger.bp.domain.policy

import kotlin.math.ceil

/*
 * Fonte única de verdade para regras de acesso, trial e limites de cadastro do Bipando.
 */
object AccessPolicy {

    /**
     * Verifica se o teste grátis (trial) está ativo.
     */
    fun isTrialActive(
        trialEndsAtMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return trialEndsAtMillis != null && currentTimeMillis < trialEndsAtMillis
    }

    /**
     * Verifica se o usuário tem acesso ilimitado (ou por assinatura Premium ou por Trial ativo).
     */
    fun hasUnlimitedAccess(
        isPremium: Boolean,
        trialEndsAtMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return isPremium || isTrialActive(trialEndsAtMillis, currentTimeMillis)
    }

    /**
     * Verifica se o limite de produtos foi atingido.
     */
    fun isLimitReached(
        isPremium: Boolean,
        trialEndsAtMillis: Long?,
        count: Int,
        freeLimit: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return !hasUnlimitedAccess(isPremium, trialEndsAtMillis, currentTimeMillis) && count >= freeLimit
    }

    /**
     * Retorna os dias restantes de trial arredondados para cima (ex.: 2.1 dias -> 3 dias).
     * Retorna 0 se o trial já expirou ou não existir.
     */
    fun getTrialDaysRemaining(
        trialEndsAtMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Int {
        if (trialEndsAtMillis == null) return 0
        val diffMillis = trialEndsAtMillis - currentTimeMillis
        if (diffMillis <= 0) return 0
        return ceil(diffMillis.toDouble() / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
}
