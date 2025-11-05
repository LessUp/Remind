package app.lessup.remind.util

object CsvParser {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> {
                    when (ch) {
                        '"' -> {
                            if (i + 1 < text.length && text[i + 1] == '"') {
                                cell.append('"')
                                i++
                            } else {
                                inQuotes = false
                            }
                        }
                        else -> cell.append(ch)
                    }
                }
                else -> {
                    when (ch) {
                        '"' -> inQuotes = true
                        ',' -> {
                            currentRow.add(cell.toString())
                            cell.clear()
                        }
                        '\n' -> {
                            currentRow.add(cell.toString())
                            cell.clear()
                            rows.add(currentRow)
                            currentRow = mutableListOf()
                        }
                        '\r' -> { /* ignore */ }
                        else -> cell.append(ch)
                    }
                }
            }
            i++
        }
        if (inQuotes) {
            throw IllegalArgumentException("Unterminated quoted field in CSV input")
        }
        currentRow.add(cell.toString())
        if (currentRow.isNotEmpty() || rows.isEmpty()) {
            rows.add(currentRow)
        }
        return rows.filterNot { row -> row.isEmpty() || (row.size == 1 && row[0].isEmpty()) }
    }
}
