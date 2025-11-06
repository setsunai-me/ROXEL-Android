package setsunai.roxel.utils

import java.util.zip.CRC32

object RoxelUtils {
    private val crc = CRC32()

    fun String.toCRC32(): Long {
        crc.update(this.toByteArray())
        return crc.value
    }
}