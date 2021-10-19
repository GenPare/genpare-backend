package de.genpare.util

import org.jetbrains.exposed.sql.Table

object Utils {
    inline fun <reified T : Enum<*>> enumDeclaration() =
        "ENUM(${enumValues<T>().joinToString { "'$it'" }})"

    inline fun <reified T : Enum<T>> Table.enumColumnDefinition(name: String) =
        customEnumeration(
            name,
            enumDeclaration<T>(),
            { enumValueOf<T>(it as String) },
            { it.name }
        )
}