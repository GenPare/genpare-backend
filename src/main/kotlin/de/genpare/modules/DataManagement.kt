package de.genpare.modules

import de.genpare.data.dtos.*
import de.genpare.data.enums.Gender
import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State
import de.genpare.database.entities.Salary
import de.genpare.database.tables.MemberTable
import de.genpare.database.tables.SalaryTable
import de.genpare.query.filters.AbstractFilter
import de.genpare.util.Utils.getMemberBySessionId
import de.genpare.util.Utils.queryParameterOrError
import de.genpare.util.Utils.receiveOrNull
import de.genpare.util.Utils.toAge
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun checkJobTitleLength(context: PipelineContext<Unit, ApplicationCall>, jobTitle: String?) =
    if ((jobTitle?.length ?: 64) > 63) {
        context.call.respond(
            HttpStatusCode.BadRequest,
            "Job title mustn't be longer than 63 characters."
        )

        null
    } else {
        jobTitle
    }

data class IntermediateResult(
    val age: Int,
    val salary: Int,
    val gender: Gender,
    val jobTitle: String,
    val state: State,
    val levelOfEducation: LevelOfEducation
)

fun Application.dataManagement() {
    routing {
        route("/salary") {
            post {
                val data = receiveOrNull<FilterListDTO>(this) ?: return@post

                if (data.filters.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "Filters can't be empty!")
                    return@post
                }

                if (data.resultTransformers.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "Result transformers can't be empty!")
                    return@post
                }

                val results = transaction {
                    // Filters are combined via an AND operation in order to apply all of them at once
                    val intermediate = SalaryTable.innerJoin(MemberTable)
                        .slice(SalaryTable.columns)
                        .select(AndOp(data.filters.map(AbstractFilter::op)))
                        .map {
                            IntermediateResult(
                                age = it[MemberTable.birthdate].toAge(),
                                salary = it[SalaryTable.salary],
                                gender = it[MemberTable.gender],
                                jobTitle = it[SalaryTable.jobTitle],
                                state = it[SalaryTable.state],
                                levelOfEducation = it[SalaryTable.levelOfEducation]
                            )
                        }

                    ResultsDTO(data.resultTransformers.map { it.transform(intermediate) })
                }

                call.respond(results)
            }

            route("/info"){
                get {
                    val result = transaction {
                        Salary.all().map { it.jobTitle }.distinct()
                    }

                    call.respond(result)
                }
            }

            route("/own") {
                get {
                    val sessionId = queryParameterOrError(this, "sessionId")
                        ?.let(String::toLong) ?: return@get

                    val member = getMemberBySessionId(this, sessionId) ?: return@get
                    val salary = Salary.findByMemberId(member.id.value)

                    if (salary == null) {
                        call.respond(HttpStatusCode.NotFound, "No salary data for this user was found.")
                        return@get
                    }

                    call.respond(salary.toDTO())
                }

                put {
                    val data = receiveOrNull<NewSalaryDTO>(this) ?: return@put
                    val member = getMemberBySessionId(this, data.sessionId) ?: return@put

                    if (Salary.findByMemberId(member.id.value) != null) {
                        call.respond(HttpStatusCode.Conflict, "Salary entry already exists for this user.")
                        return@put
                    }

                    checkJobTitleLength(this, data.jobTitle) ?: return@put

                    val newSalary = transaction {
                        val salary = Salary.new {
                            memberId = member.id.value
                            salary = data.salary
                            jobTitle = data.jobTitle
                            state = data.state
                            levelOfEducation = data.levelOfEducation
                        }

                        NewSalaryDTO(
                            data.sessionId,
                            salary.salary,
                            salary.jobTitle,
                            salary.state,
                            salary.levelOfEducation
                        )
                    }

                    call.respond(newSalary)
                }

                patch {
                    val data = receiveOrNull<ModifySalaryDTO>(this) ?: return@patch
                    val member = getMemberBySessionId(this, data.sessionId) ?: return@patch
                    val salary = Salary.findByMemberId(member.id.value)

                    if (salary == null) {
                        call.respond(HttpStatusCode.NotFound, "No existing salary entry was found.")
                        return@patch
                    }

                    checkJobTitleLength(this, data.jobTitle) ?: return@patch

                    val newSalary = transaction {
                        if (data.salary != null) salary.salary = data.salary
                        if (data.jobTitle != null) salary.jobTitle = data.jobTitle
                        if (data.state != null) salary.state = data.state
                        if (data.levelOfEducation != null) salary.levelOfEducation = data.levelOfEducation

                        ModifySalaryDTO(
                            data.sessionId,
                            salary.salary,
                            salary.jobTitle,
                            salary.state,
                            salary.levelOfEducation
                        )
                    }

                    call.respond(newSalary)
                }
            }
        }
    }
}