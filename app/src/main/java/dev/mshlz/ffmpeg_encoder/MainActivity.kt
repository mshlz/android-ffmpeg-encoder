package dev.mshlz.ffmpeg_encoder

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mshlz.ffmpeg_encoder.ui.theme.FFMPEGEncoderTheme
import dev.mshlz.ffmpeg_encoder.utils.CommandResult
import dev.mshlz.ffmpeg_encoder.utils.exec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    var DEFAULT_CMD_STRING: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // copy binary from apk
        copyAssets(arrayOf("ffmpeg", "Cactus_1920x1080_50.mp4"))

        // change permission of ffmpeg to allow exec
        exec("chmod 500 ${filesDir.absolutePath}/ffmpeg")

//        exec("ls -la ${filesDir.absolutePath}")
//        exec("${filesDir.absolutePath}/ffmpeg -version")
//        exec("${filesDir.absolutePath}/ffmpeg -codecs")
        val externalDir = getExternalFilesDir("")

        DEFAULT_CMD_STRING =
            "ffmpeg -y -i ${filesDir.absolutePath}/Cactus_1920x1080_50.mp4 -c:v hevc_mediacodec -b:v 5000k ${
                externalDir
            }/cactus4.mp4"

        val executorService = Executors.newSingleThreadExecutor()

        setContent {
            var cmdState = remember {
                mutableStateOf(
                    TextFieldValue(
                        DEFAULT_CMD_STRING!!
                    )
                )
            }
            var resultState = remember { mutableStateOf(CommandResult()) }

            val clipboardManager = LocalClipboardManager.current

            FFMPEGEncoderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    Column(
                        modifier = Modifier.verticalScroll(
                            rememberScrollState()
                        )
                    ) {
                        CommandInput(
                            modifier = Modifier.fillMaxWidth(),
                            state = cmdState,
                            placeHolder = "..."
                        )

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Button(
//                                enabled = false,
                                onClick = {
                                    executorService.execute {
                                        val cmd = cmdState.value.text.trim().let {
                                            if (it.startsWith("ffmpeg")) {
                                                "${filesDir.absolutePath}/$it"
                                            } else {
                                                it
                                            }
                                        }.replace("\$home", externalDir!!.absolutePath)
                                            .replace("\$internal", filesDir.absolutePath)

                                        exec(cmd).also {
                                            resultState.value = it
                                        }
                                    }
                                }
                            ) {
//                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(text = "Run")
                            }

                            Spacer(modifier = Modifier.size(4.dp))

                            Button(
                                onClick = {
                                    Executors.newSingleThreadExecutor().execute {
                                        exec("ps -ef").also {
                                            resultState.value = it
                                        }
                                    }
                                }
                            ) {
                                Text(text = "ps -ef")
                            }

                            Spacer(modifier = Modifier.size(4.dp))

                            Button(
                                onClick = {
                                    cmdState.value = TextFieldValue("")
                                }
                            ) {
                                Text(text = "clear cmd")
                            }

                            Button(
                                onClick = {
                                    cmdState.value = TextFieldValue(DEFAULT_CMD_STRING!!)
                                }
                            ) {
                                Text(text = "reset cmd")
                            }

                            OutlinedButton(
                                onClick = {
                                    resultState.value = CommandResult(
                                        stdout = """
                                        | Available alias:
                                        | ${'$'}home     -> alias for external storage path (android/data/.../files)
                                        | ${'$'}internal -> alias for internal storage (app protect storage)
                                        | 
                                        | # How to copy new build
                                        | 1. Copy the ffmpeg binary to Android/data/dev/mshlz.ffmpeg_encoder/files/bin
                                        | 2. Open the app, then click in the button "copy ext to internal"
                                        | 3. Run the follow command to fix the permission of the binary:
                                        |    chmod 500 ${'$'}internal/[nameOfTheBinary]
                                        | 4. Now you can run it with:
                                        |    ${'$'}internal/[nameOfTheBinary] ...
                                        | If the [nameOfTheBinary] starts with ffmpeg, then you can run
                                        |    [nameOfTheBinary] ...
                                    """.trimMargin()
                                    )
                                }
                            ) {
                                Text(text = "help")
                            }

                            OutlinedButton(
                                onClick = {

                                    executorService.execute {
                                        exec("cp -r $externalDir/bin/. ${filesDir.absolutePath}").also {
                                            runOnUiThread {
                                                Toast.makeText(
                                                    context,
                                                    "Files copied!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                    }
                                }
                            ) {
                                Text(text = "copy ext to internal")
                            }

                        }

                        Column(modifier = Modifier.padding(10.dp)) {

                            Text(text = "Exit Code: ${resultState.value.ecode}")

                            CommandOutput(
                                name = "STDOUT",
                                value = resultState.value.stdout,
                                clipboardManager = clipboardManager
                            )

                            Divider()

                            CommandOutput(
                                name = "STDOUT",
                                nameColor = Color.Red,
                                value = resultState.value.stderr,
                                clipboardManager = clipboardManager
                            )

                            Divider()

                            resultState.value.exception.let {
                                if (it != null) {
                                    CommandOutput(
                                        name = "Exception",
                                        nameColor = Color.Red,
                                        value = resultState.value.exception?.stackTraceToString()
                                            .orEmpty(),
                                        clipboardManager = clipboardManager
                                    )
                                }
                            }

                        }

                    }

                }
            }
        }
    }

    private fun copyAssets(filesToCopy: Array<String>) {
        val assetManager: AssetManager = assets
        var files: Array<String>? = null
        try {
            files = assetManager.list("")
        } catch (e: IOException) {
            Log.e("COPY_ASSETS", "Failed to get asset file list.", e)
        }
        for (filename in files!!.filter { filesToCopy.contains(it) }) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = assetManager.open(filename)
                val outFile = File(filesDir, filename)
                outputStream = FileOutputStream(outFile)
                copyFile(inputStream, outputStream)
            } catch (e: IOException) {
                Log.e("COPY_ASSETS", "Failed to copy asset file: $filename", e)
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        // NOOP
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close()
                    } catch (e: IOException) {
                        // NOOP
                    }
                }
            }
        }
    }

}

@Throws(IOException::class)
private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
    val buffer = ByteArray(1024)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
        outputStream.write(buffer, 0, read)
    }
}

// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandInput(
    modifier: Modifier = Modifier, state: MutableState<TextFieldValue>, placeHolder: String
) {
    TextField(
        modifier = modifier,
        value = state.value,
        placeholder = {
            Text(text = placeHolder)
        },
        onValueChange = { value -> state.value = value })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandOutput(
    name: String,
    nameColor: Color = Color.Unspecified,
    value: String,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {


    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, color = nameColor, fontWeight = FontWeight.Bold)
            TextButton(
                contentPadding = PaddingValues(1.dp),
                onClick = {
                    clipboardManager.setText(AnnotatedString(value))
                }
            ) {
                Text(text = "Copy")
            }
        }
        SelectionContainer {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }

}