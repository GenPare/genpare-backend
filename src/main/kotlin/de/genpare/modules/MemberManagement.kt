package de.genpare.modules

import de.genpare.data.dtos.*
import de.genpare.database.entities.Member
import de.genpare.database.entities.Salary
import de.genpare.util.LocalDateTypeAdapter
import de.genpare.util.Utils.getMemberBySessionId
import de.genpare.util.Utils.receiveOrNull
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.random.Random



fun Application.memberManagement() {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        }
    }

    routing {
        route("/members") {
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

            route("/salary") {
                suspend fun PipelineContext<Unit, ApplicationCall>.checkJobTitleLength(jobTitle: String?) =
                    if ((jobTitle?.length ?: 64) > 63) {
                        call.respond(HttpStatusCode.BadRequest, "Job title mustn't be longer than 63 characters.")
                        null
                    } else {
                        jobTitle
                    }

                post {
                    val data = receiveOrNull<NewSalaryDTO>() ?: return@post
                    val member = getMemberBySessionId(data.sessionId) ?: return@post

                    if (Salary.findByMemberId(member.id.value) != null) {
                        call.respond(HttpStatusCode.Conflict, "Salary entry already exists for this user.")
                        return@post
                    }

                    checkJobTitleLength(data.jobTitle) ?: return@post

                    val newSalary = transaction {
                        val salary = Salary.new {
                            memberId = member.id.value
                            salary = data.salary
                            gender = data.gender
                            jobTitle = data.jobTitle
                            state = data.state
                        }

                        NewSalaryDTO(data.sessionId, salary.salary, salary.gender, salary.jobTitle, salary.state)
                    }

                    call.respond(newSalary)
                }

                patch {
                    val data = receiveOrNull<ModifySalaryDTO>() ?: return@patch
                    val member = getMemberBySessionId(data.sessionId) ?: return@patch
                    val salary = Salary.findByMemberId(member.id.value)

                    if (salary == null) {
                        call.respond(HttpStatusCode.NotFound, "No existing salary entry was found.")
                        return@patch
                    }

                    checkJobTitleLength(data.jobTitle) ?: return@patch

                    val newSalary = transaction {
                        if (data.salary != null) salary.salary = data.salary
                        if (data.gender != null) salary.gender = data.gender
                        if (data.jobTitle != null) salary.jobTitle = data.jobTitle
                        if (data.state != null) salary.state = data.state

                        ModifySalaryDTO(data.sessionId, salary.salary, salary.gender, salary.jobTitle, salary.state)
                    }

                    call.respond(newSalary)
                }
            }
        }
    }
}