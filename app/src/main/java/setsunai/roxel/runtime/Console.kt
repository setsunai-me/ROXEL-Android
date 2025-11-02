package setsunai.roxel.runtime

import java.io.DataOutputStream
import java.io.InputStream

class Console {
    companion object {
        fun execute(command: String): InputStream? {
            return execute(arrayOf(command))
        }

        fun execute(commands: Array<String>): InputStream? {
            try {
                val process = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(process.outputStream)
                outputStream.writeBytes("${commands.joinToString("\n")}\n")
                outputStream.flush()
                outputStream.close()
                return process.inputStream
            } catch (_: Throwable) {
            }
            return null
        }
    }
}