package com.example.practice

import android.app.Application
import com.example.practice.di.AppContainer

class PracticeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
