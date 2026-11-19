package com.headunit.feature.media

/**
 * Navegação por pastas a partir da lista de arquivos (o Android indexa os arquivos,
 * não as pastas). Caminhos sem barras nas pontas; "" é a raiz do volume.
 */
object FolderTree {
    fun normalize(path: String): String = path.trim('/').replace(Regex("/+"), "/")

    /** Subpastas diretas de [path] (só as que têm mídia em algum nível abaixo). */
    fun subfolders(folders: Collection<String>, path: String): List<String> {
        val base = normalize(path)
        val prefix = if (base.isEmpty()) "" else "$base/"
        return folders.asSequence()
            .map(::normalize)
            .filter { it.startsWith(prefix) && it.length > prefix.length }
            .map { it.removePrefix(prefix).substringBefore('/') }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
    }

    /** Pasta de cima ("" para a raiz; null se já está na raiz). */
    fun parent(path: String): String? {
        val p = normalize(path)
        if (p.isEmpty()) return null
        return p.substringBeforeLast('/', missingDelimiterValue = "")
    }

    fun join(path: String, child: String): String = normalize(if (path.isEmpty()) child else "$path/$child")

    /** Quantos arquivos há em [path] e abaixo. */
    fun countUnder(folders: Collection<String>, path: String): Int {
        val p = normalize(path)
        return folders.count { val f = normalize(it); f == p || f.startsWith("$p/") }
    }
}
