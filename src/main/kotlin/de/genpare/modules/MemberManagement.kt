package de.genpare.modules

import de.genpare.data.dtos.*
import de.genpare.database.entities.Member
import de.genpare.util.LocalDateTypeAdapter
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.request.ContentTransformationException
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.random.Random

suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.receiveOrNull(): T? {
    return try {
        call.receive()
    } catch (e: ContentTransformationException) {
        call.respond(HttpStatusCode.BadRequest, "Invalid JSON payload.")
        null
    }
}

fun Application.memberManagement() {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        }
    }

    routing {
        route("/members") {
            route("/session") {
                get {
                    val data = receiveOrNull<LoginDTO>() ?: return@get
                    val member = Member.findByEmail(data.email)

                    if (member == null) {
                        call.respond(HttpStatusCode.NotFound, "Unknown user.")
                        return@get
                    }

                    val session = SessionDTO(Random.nextLong())

                    transaction {
                        member.sessionId = session.sessionId
                    }

                    call.respond(session)
                }

                delete {
                    val data = receiveOrNull<LogoutDTO>() ?: return@delete
                    val member = Member.findByEmail(data.email)

                    transaction {
                        member?.sessionId = 0
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }

            post {
                val data = receiveOrNull<MemberDTO>() ?: return@post

                if (data.id != null) {
                    call.respond(HttpStatusCode.BadRequest, "ID mustn't be set for a new user!")
                    return@post
                }

                if (Member.findByEmail(data.email) != null) {
                    call.respond(HttpStatusCode.Conflict, "This email is already in use.")
                    return@post
                }

                val newMember = transaction {
                    val member = Member.newMember {
                        email = data.email
                        name = data.name
                        birthdate = data.birthdate
                    }

                    MemberDTO(member.id.value, member.email, member.name, member.birthdate)
                }

                call.respond(newMember)
            }

            delete {
                val data = receiveOrNull<DeleteDTO>() ?: return@delete
                val member = Member.findByEmail(data.email)

                if (member == null) {
                    call.respond(HttpStatusCode.NoContent)
                    return@delete
                }

                val actualSessionId = transaction { member.sessionId }

                if (data.sessionId != actualSessionId) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid session.")
                    return@delete
                }

                transaction {
                    member.delete()
                }

                call.respond(HttpStatusCode.NoContent)
            }

            patch {
                val data = receiveOrNull<NameChangeDTO>() ?: return@patch
                val member = Member.findBySessionId(data.sessionId)

                if (member == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid session.")
                    return@patch
                }

                transaction {
                    member.name = data.name
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}