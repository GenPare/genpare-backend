package de.genpare.util

import de.genpare.database.entities.Member
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.Table
import java.time.LocalDate

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

    suspend inline fun <reified T : Any> receiveOrNull(context: PipelineContext<Unit, ApplicationCall>): T? =
        try {
            context.call.receive()
        } catch (e: Exception) {
            context.call.respond(HttpStatusCode.BadRequest, "Invalid JSON payload.")
            null
        }

    suspend fun getMemberBySessionId(context: PipelineContext<Unit, ApplicationCall>, sessionId: Long?) =
        checkMember(context, sessionId?.let { Member.findBySessionId(it) }, "Unknown session id.")

    suspend fun getMemberByEmail(context: PipelineContext<Unit, ApplicationCall>, email: String) =
        checkMember(context, Member.findByEmail(email), "Unknown email address.")

    private suspend fun checkMember(context: PipelineContext<Unit, ApplicationCall>, member: Member?, message: String): Member? {
        if (member == null)
            context.call.respond(HttpStatusCode.NotFound, message)

        return member
    }

    fun LocalDate.toAge() =
        this.until(LocalDate.now()).years

    fun Int.toRange(width: Int) =
        IntRange(this % width, this % width + 1)

    suspend fun queryParameterOrError(
        context: PipelineContext<Unit, ApplicationCall>,
        name: String
    ): String? {
        val param = context.call.request.queryParameters[name]

        if (param == null)
            context.call.respond(HttpStatusCode.BadRequest, "Missing $name parameter.")

        return param
    }
}