package com.rogger.bp.domain.repository

import com.rogger.bp.data.model.UserAuth
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Result<UserAuth>>
    fun loginWithGoogle(idToken: String): Flow<Result<UserAuth>>
    fun getCurrentUser(): UserAuth?
    fun logout()
}
