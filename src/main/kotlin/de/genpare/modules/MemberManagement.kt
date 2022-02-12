package de.genpare.modules

import com.auth0.jwk.JwkProviderBuilder
import de.genpare.data.dtos.DeleteDTO
import de.genpare.data.dtos.MemberDTO
import de.genpare.data.dtos.NameChangeDTO
import de.genpare.data.dtos.SessionDTO
import de.genpare.data.enums.Gender
import de.genpare.data.enums.State
import de.genpare.database.entities.Member
import de.genpare.query.filters.AbstractFilter
import de.genpare.query.result_transformers.AbstractResultTransformer
import de.genpare.type_adapters.*
import de.genpare.util.Utils.getMemberBySessionId
import de.genpare.util.Utils.queryParameterOrError
import de.genpare.util.Utils.receiveOrNull
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun validateCredentials(credential: JWTCredential): JWTPrincipal? {
    val containsAudience = credential.payload.audience.contains(System.getenv("AUDIENCE"))

    return if (containsAudience) JWTPrincipal(credential.payload) else null
}

fun Application.memberManagement() {
    val jwkProvider = JwkProviderBuilder(System.getenv("ISSUER"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("auth0") {
            verifier(jwkProvider, System.getenv("ISSUER"))
            validate { credential ->
                log.debug("JWT ID: ${credential.jwtId}")
                validateCredentials(credential)
            }
        }
    }

    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter)
            registerTypeAdapter(AbstractResultTransformer::class.java, ResultTransformerDeserializer)
            registerTypeAdapter(AbstractFilter::class.java, FilterDeserializer)
            registerTypeAdapter(IntRange::class.java, IntRangeSerializer)
            registerTypeAdapter(Gender::class.java, GenderDeserializer)
            registerTypeAdapter(LevelOfEducationDeserializer::class.java, LevelOfEducationDeserializer)
            registerTypeAdapter(State::class.java, StateDeserializer)

            serializeNulls()
        }
    }

    install(CORS) {
        anyHost()

        header(HttpHeaders.ContentType)
        header("authorization")

        allowCredentials = true
        allowNonSimpleContentTypes = true

        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
        method(HttpMethod.Options)
    }

    routing {
        authenticate("auth0") {
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
                            gender = data.gender
                        }

                        MemberDTO(member.id.value, member.email, member.name, member.birthdate, member.gender)
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

                    if (data.sessionId.toLongOrNull() != actualSessionId) {
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
                    val member = data.sessionId.toLongOrNull()?.let { getMemberBySessionId(this, it) }
                        ?: return@patch

                    transaction {
                        member.name = data.name
                    }

                    call.respond(HttpStatusCode.NoContent)
                }

                get {
                    val sessionId = queryParameterOrError(this, "sessionId") ?: return@get

                    val member = getMemberBySessionId(this, sessionId.toLongOrNull()) ?: return@get

                    call.respond(member.toDTO())
                }

                route("/session") {
                    get {
                        val email = queryParameterOrError(this, "email") ?: return@get
                        val member = Member.findByEmail(email)

                        if (member == null) {
                            call.respond(HttpStatusCode.NotFound, "Unknown user.")
                            return@get
                        }

                        val sessionId = Random.nextLong()

                        transaction {
                            member.sessionId = sessionId
                        }

                        call.respond(SessionDTO(sessionId.toString()))
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
}