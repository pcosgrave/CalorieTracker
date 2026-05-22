package com.philipcosgrave.calorietracker.model

data class AuthSession(
    val userSub: String,
    val accessToken: String,
    val idToken: String,
    val refreshToken: String? = null,
    val email: String? = null,
    val name: String? = null,
    val expiresAtEpochMs: Long,
)
