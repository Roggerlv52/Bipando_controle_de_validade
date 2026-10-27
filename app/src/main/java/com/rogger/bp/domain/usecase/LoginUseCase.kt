package com.rogger.bp.domain.usecase

import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val repository: AuthRepository) {
    fun login(email: String, password: String): Flow<Result<UserAuth>> {
        return repository.login(email, password)
    }

    fun loginWithGoogle(idToken: String): Flow<Result<UserAuth>> {
        return repository.loginWithGoogle(idToken)
    }
}
