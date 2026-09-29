package com.alpkcgl.fikirhane.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PublicUser(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val role: Role = Role.USER,
    val isBanned: Boolean = false,
    val createdAt: String,
)

@Serializable
enum class Role { USER, MODERATOR, ADMIN }

@Serializable
data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    /** Unix zamanı (saniye). */
    val expiresAt: Long? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val displayName: String,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class LoginResponse(val user: PublicUser, val session: SessionTokens)

@Serializable
data class RegisterResponse(
    val user: PublicUser,
    val session: SessionTokens? = null,
    val emailConfirmationRequired: Boolean = false,
)

@Serializable
data class RefreshResponse(val session: SessionTokens)

@Serializable
data class UserResponse(val user: PublicUser)

@Serializable
data class MessageResponse(val message: String)
