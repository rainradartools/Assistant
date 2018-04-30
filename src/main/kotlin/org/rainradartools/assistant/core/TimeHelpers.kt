package org.rainradartools.assistant.core

import java.util.HashSet


class TimeHelpers {
    companion object {
        fun getNearest(resolutionMins: Int, offsetMins: Int, timestamp: Int): Int {
            return 0
        }

        fun commonTimestringBase(first: Long, second: Long): Long {
            val s = first.toString()
            val t = second.toString()
            val table = Array(s.length) { IntArray(t.length) }
            var longest = 0
            val result = HashSet<String>()

            for (i in 0..s.length - 1) {
                for (j in 0..t.length - 1) {
                    if (s[i] != t[j]) {
                        continue
                    }

                    table[i][j] = if (i == 0 || j == 0)
                        1
                    else
                        1 + table[i - 1][j - 1]
                    if (table[i][j] > longest) {
                        longest = table[i][j]
                        result.clear()
                    }
                    if (table[i][j] == longest) {
                        result.add(s.substring(i - longest + 1, i + 1))
                    }
                }
            }
            return result.iterator().next().toLong()
        }
    }
}
