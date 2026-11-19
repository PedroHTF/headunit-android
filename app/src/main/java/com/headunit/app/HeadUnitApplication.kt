package com.headunit.app

import android.app.Application

open class HeadUnitApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
    }

    /** Os testes de tela trocam por um container sem serviço de mídia. */
    protected open fun createContainer() = AppContainer(this)
}
