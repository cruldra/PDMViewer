package org.cruldra.pdmviewer.utils

fun String.toCamelCase(): String {
    return this.replaceFirst("^[^a-zA-Z]+".toRegex(), "")
        .replace("[^a-zA-Z0-9]".toRegex(), " ")
        .split("\\s+".toRegex())
        .joinToString("") { it.capitalize() }
        .decapitalize()
}
