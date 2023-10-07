package dev.mshlz.ffmpeg_encoder

import android.content.res.AssetManager
import android.content.res.Resources
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

class Testing {
    fun test() {
        try {
            Runtime.getRuntime().exec("ffmpeg1 --version").run {
                val inpReader = InputStreamReader(this.inputStream)
                val bufReader = BufferedReader(inpReader)

                bufReader.forEachLine {
                    println(it)
                }

                val ec = this.waitFor()
                println ("Exit Code: $ec")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}