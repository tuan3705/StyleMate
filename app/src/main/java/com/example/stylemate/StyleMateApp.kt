package com.example.stylemate

import android.app.Application
import com.example.stylemate.data.auth.AuthStorage
import com.example.stylemate.data.auth.AuthTokenProvider
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.repository.AuthRepository

class StyleMateApp : Application() {

    lateinit var authStorage: AuthStorage
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authStorage = AuthStorage(this)
        AuthTokenProvider.init(authStorage)
        authRepository = AuthRepository(RetrofitClient.stylemateApiService, authStorage)
    }
}

