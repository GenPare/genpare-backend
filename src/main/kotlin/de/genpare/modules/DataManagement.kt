package de.genpare.modules

import de.genpare.data.dtos.ModifySalaryDTO
import de.genpare.data.dtos.NewSalaryDTO
import de.genpare.database.entities.Salary
import de.genpare.util.Utils.getMemberBySessionId
import de.genpare.util.Utils.receiveOrNull
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun PipelineContext<Unit, ApplicationCall>.checkJobTitleLength(jobTitle: String?) =
    if ((jobTitle?.length ?: 64) > 63) {
        call.respond(HttpStatusCode.BadRequest, "Job title mustn't be longer than 63 characters.")
        null
    } else {
        jobTitle
    }

fun Application.dataManagement() {
    routing {
        route("/salary") {
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