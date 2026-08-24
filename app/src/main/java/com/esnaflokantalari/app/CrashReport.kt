package com.esnaflokantalari.app

import android.content.Context
import java.io.File

/**
 * Uzaktan hata ayıklama için basit bir çökme kaydedici. Beklenmeyen bir
 * istisna uygulamayı kapatmadan önce dosyaya yazılır; bir sonraki açılışta
 * okunup ekranda gösterilir, böylece kullanıcı ekran görüntüsü alıp
 * gönderebilir (adb/logcat gerekmeden).
 */
object CrashReport {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(appContext.filesDir, FILE_NAME).writeText(throwable.stackTraceToString())
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Kaydedilmiş bir çökme varsa metnini döner ve dosyayı siler. */
    fun readAndClear(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()?.also { file.delete() }
    }
}
