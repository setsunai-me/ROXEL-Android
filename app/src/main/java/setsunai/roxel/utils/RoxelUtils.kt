package setsunai.roxel.utils

import java.util.zip.CRC32

object RoxelUtils {
    fun String.toCRC32(): Long {
        val crc = CRC32()
        crc.update(this.toByteArray())
        return crc.value
    }
}