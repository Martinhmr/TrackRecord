package biz.itonline.trackrecord

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    init { INSTANCE = this }
    companion object {
        lateinit var INSTANCE: MyApplication
            private set

        val applicationContext: Context get() { return INSTANCE.applicationContext }
    }


    override fun onCreate() {
        super.onCreate()

    }

}