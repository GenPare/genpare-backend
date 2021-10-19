package de.genpare.util

import de.genpare.database.entities.Member
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.Table

object Utils {
    inline fun <reified T : Enum<T>> enumDeclaration() =
        "ENUM(${enumValues<T>().joinToString { "'$it'" }})"

    inline fun <reified T : Enum<T>> Table.enumColumnDefinition(name: String) =
        customEnumeration(
            name,
            enumDeclaration<T>(),
            { enumValueOf<T>(it as String) },
            { it.name }
        )

    suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.receiveOrNull(): T? =
        try {
            call.receive()
        } catch (e: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid JSON payload.")
            null
        }

    suspend fun PipelineContext<Unit, ApplicationCall>.getMemberBySessionId(sessionId: Long) =
        checkMember(Member.findBySessionId(sessionId),"Unknown session id.")

    suspend fun PipelineContext<Unit, ApplicationCall>.getMemberByEmail(email: String) =
        checkMember(Member.findByEmail(email), "Unknown email address.")

    private suspend fun PipelineContext<Unit, ApplicationCall>.checkMember(member: Member?, message: String): Member? {
        if (member == null)
            call.respond(HttpStatusCode.NotFound, message)

        return member
    }
}