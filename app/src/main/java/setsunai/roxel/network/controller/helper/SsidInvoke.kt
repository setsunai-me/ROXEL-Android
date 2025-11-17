package setsunai.roxel.network.controller.helper

import setsunai.roxel.runtime.Console
import java.io.BufferedReader
import java.io.InputStreamReader

class SsidInvoke {
    companion object {
        fun get(): String? {
            Console.execute("dumpsys netstats | grep -E 'iface=wlan.*'")?.apply {
                val searchKey = "wifiNetworkKey="
                val searchKeyAlt = "networkId="
                fun search(key: String, line: String): Int {
                    val start = line.indexOf(key)
                    val end = line.indexOf(",", kotlin.math.max(0, start))
                    if (start in (searchKey.length + 1)..<end) {
                        return start
                    }
                    return -1
                }

                fun parse(s: Int, offset: Int, line: String): String? {
                    val end = line.indexOf(",", kotlin.math.max(0, s))
                    if (s in (offset + 1)..<end) {
                        var result = line.substring(s + offset, end)
                        if (result.startsWith("'")) {
                            result = result.substring(1, result.lastIndexOf("'"))
                        } else if (result.startsWith("\"")) {
                            result = result.substring(1, result.lastIndexOf("\""))
                        }
                        return result
                    }
                    return null
                }
                for (line in BufferedReader(InputStreamReader(this)).lines()) {
                    var keyStart: Int
                    if (search(searchKey, line).also { keyStart = it } > 0) {
                        return parse(keyStart, searchKey.length, line)
                    } else if (search(searchKeyAlt, line).also { keyStart = it } > 0) {
                        return parse(keyStart, searchKeyAlt.length, line)
                    }
                }
            }
            return null
        }
    }
}