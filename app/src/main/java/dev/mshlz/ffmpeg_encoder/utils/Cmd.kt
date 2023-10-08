package dev.mshlz.ffmpeg_encoder.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

const val TAG = "EXEC_CMD"

data class CommandResult(
    var stdout: String = "",
    var stderr: String = "",
    var ecode: Int? = null,
    var exception: Exception? = null
);

fun exec(cmd: String, debug: Boolean = true): CommandResult {
    val result = CommandResult()

    try {
        Log.d(TAG, cmd)
        Runtime.getRuntime().exec(cmd).run {
            val stdoutBuffer = StringBuffer()
            val stderrBuffer = StringBuffer()
            appendInputStreamToBuffer(this.inputStream, stdoutBuffer, debug)
            appendInputStreamToBuffer(this.errorStream, stderrBuffer, debug)

            result.ecode = this.waitFor()
            result.stdout = stdoutBuffer.toString()
            result.stderr = stderrBuffer.toString()

            Log.d(TAG, result.stdout)
            Log.w(TAG, result.stderr)
            Log.d(TAG, "Exit Code: ${result.ecode}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        result.exception = e
    }

    return result
}

// -------------------------------------------
private inline fun appendInputStreamToBuffer(
    inputStream: InputStream,
    stringBuffer: StringBuffer,
    debug: Boolean = false
) {
    val inpReader = InputStreamReader(inputStream)
    val bufReader = BufferedReader(inpReader)

    bufReader.forEachLine {
        stringBuffer.append(it + '\n')
        if (debug) {
            Log.d(TAG, it)
        }
    }
}