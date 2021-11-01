package de.genpare.modules

import de.genpare.data.dtos.*
import de.genpare.database.entities.Member
import de.genpare.query.filters.AbstractFilter
import de.genpare.query.result_transformers.AbstractResultTransformer
import de.genpare.type_adapters.FilterDeserializer
import de.genpare.type_adapters.IntRangeSerializer
import de.genpare.type_adapters.LocalDateTypeAdapter
import de.genpare.type_adapters.ResultTransformerDeserializer
import de.genpare.util.Utils.queryParameterOrError
import de.genpare.util.Utils.receiveOrNull
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.random.Random

fun Application.memberManagement() {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter)
            registerTypeAdapter(AbstractResultTransformer::class.java, ResultTransformerDeserializer)
            registerTypeAdapter(AbstractFilter::class.java, FilterDeserializer)
            registerTypeAdapter(IntRange::class.java, IntRangeSerializer)

            serializeNulls()
        }
    }

    install(CORS) {
        anyHost()

        header(HttpHeaders.ContentType)

        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
        method(HttpMethod.Options)
    }

    routing {
        route("/members") {
            post {
                val data = receiveOrNull<MemberDTO>(this) ?: return@post

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
                val data = receiveOrNull<DeleteDTO>(this) ?: return@delete
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
                val data = receiveOrNull<NameChangeDTO>(this) ?: return@patch
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

            route("/session") {
                get {
                    val email = queryParameterOrError(this, "email") ?: return@get
                    val member = Member.findByEmail(email)

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
                    val email = queryParameterOrError(this, "email") ?: return@delete
                    val member = Member.findByEmail(email)

                    transaction {
                        member?.sessionId = 0
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}