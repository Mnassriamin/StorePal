package com.example.elmnassri

object UserSession {
    var currentUser: User? = null

    fun isLoggedIn(): Boolean {
        return currentUser != null
    }

    fun isAdmin(): Boolean {
        return currentUser?.role == "admin"
    }

    fun logout() {
        currentUser = null
    }
}