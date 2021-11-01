package de.genpare

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.typesafe.config.ConfigFactory
import de.genpare.data.dtos.*
import de.genpare.data.enums.Gender
import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State
import de.genpare.database.entities.Member
import de.genpare.database.entities.Salary
import de.genpare.modules.setup
import de.genpare.query.filters.AbstractFilter
import de.genpare.query.filters.JobTitleFilter
import de.genpare.query.result_transformers.AbstractResultTransformer
import de.genpare.query.result_transformers.AverageResultTransformer
import de.genpare.type_adapters.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.*

class ApplicationTest {
    private val birthdate = LocalDate.of(1815, 12, 10)

    private val testSalary = NewSalaryDTO(
        1337,
        69420,
        "Bar-ist in Foo-logy",
        State.BERLIN,
        LevelOfEducation.DOKTOR
    )

    private val testModifySalary = ModifySalaryDTO(
        testSalary.sessionId,
        testSalary.salary,
        testSalary.jobTitle,
        testSalary.state,
        testSalary.levelOfEducation
    )

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter)
        .registerTypeAdapter(AbstractResultTransformer::class.java, ResultTransformerDeserializer)
        .registerTypeAdapter(AbstractFilter::class.java, FilterSerializer)
        .registerTypeAdapter(AbstractFilter::class.java, FilterDeserializer)
        .registerTypeAdapter(AbstractResultTransformer.AbstractResult::class.java, ResultDeserializer)
        .create()

    private val testEnvironment = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }

    private fun withJson(obj: Any): TestApplicationRequest.() -> Unit = {
        addHeader("Content-Type", "application/json")
        setBody(gson.toJson(obj))
    }

    private inline fun <reified T : Any> assertDeserialize(json: String, message: String? = null): T? =
        try {
            gson.fromJson(json, T::class.java)
        } catch (e: JsonSyntaxException) {
            assertTrue(false, message)
            null
        }

    private fun assertFilterSQL(filter: AbstractFilter, stmt: String) =
        QueryBuilder(false).apply {
            transaction { filter.op.toQueryBuilder(this@apply) }
            assertEquals(stmt, toString())
        }

    private fun insertTestUser() =
        transaction {
            Member.new {
                email = "test@example.com"
                name = "Foo"
                sessionId = 1337
                birthdate = LocalDate.of(1815, 12, 10)
                gender = Gender.DIVERSE
            }
        }

    private fun insertTestSalary(_memberId: Long) {
        transaction {
            Salary.new {
                memberId = _memberId
                salary = testSalary.salary
                jobTitle = testSalary.jobTitle
                state = testSalary.state
                levelOfEducation = testSalary.levelOfEducation
            }
        }
    }

    private fun insertTestSalaries() {
        transaction {
            Salary.new {
                memberId = Random.nextLong().absoluteValue
            }
        }
    }

    @BeforeTest
    fun reset() {
        testEnvironment.application.setup()

        transaction {
            Member.all().forEach { it.delete() }
        }
    }

    @Test
    fun helloWorld() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Get,
                "/"
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }

    @Test
    fun createAccountSucceeds() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/members",
                withJson(MemberDTO(null, "test@example.com", "Foo", birthdate, Gender.DIVERSE))
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)

                val member = assertDeserialize<MemberDTO>(response.content!!) ?: return@apply
                assertEquals("test@example.com", member.email)
                assertEquals("Foo", member.name)
                assertEquals(birthdate, member.birthdate)
            }
        }
    }

    @Test
    fun createAccountWithNonNullId() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/members",
                withJson(MemberDTO(1337, "test@example.com", "Foo", birthdate, Gender.DIVERSE))
            ).apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun createAccountWithExistingEmail() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/members",
                withJson(MemberDTO(null, "test@example.com", "Bar", birthdate, Gender.DIVERSE))
            ).apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    @Test
    fun sessionIdReturnsCorrectly() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Get,
                "/members/session?email=test@example.com"
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())

                assertNotNull(response.content)
                assertDeserialize<SessionDTO>(response.content!!)
            }
        }
    }

    @Test
    fun sessionIdForUnknownUser() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Get,
                "/members/session?email=test@example.com"
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun logoutSucceeds() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Delete,
                "/members/session?email=test@example.com"
            ).apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun logoutSilentlyFails() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Delete,
                "/members/session?email=test@example.com"
            ).apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun nameChangeSucceeds() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/members",
                withJson(NameChangeDTO("Maria Musterfrau", 1337))
            ).apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun nameChangeWithInvalidSessionId() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/members",
                withJson(NameChangeDTO("Maria Musterfrau", 420))
            ).apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun accountDeletionSucceeds() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Delete,
                "/members",
                withJson(DeleteDTO("test@example.com", 1337))
            ).apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun accountDeletionWithInvalidSessionId() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Delete,
                "/members",
                withJson(DeleteDTO("test@example.com", 69))
            ).apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun accountDeletionWithUnknownUserSilentlyFails() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Delete,
                "/members",
                withJson(DeleteDTO("test@example.com", 1337))
            ).apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun salaryGetSucceeds() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Get,
                "/salary/own?sessionId=${testUser.sessionId}"
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)

                assertDeserialize<SalaryDTO>(response.content!!) ?: return@apply
            }
        }
    }

    @Test
    fun salaryPutSucceeds() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Put,
                "/salary/own",
                withJson(testSalary)
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
                val content = response.content!!
                println(content)

                val newSalary = assertDeserialize<NewSalaryDTO>(content) ?: return@apply
                assertEquals(testSalary, newSalary)
            }
        }
    }

    @Test
    fun salaryPutInvalidSessionId() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Put,
                "/salary/own",
                withJson(testSalary)
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun salaryPutJobTitleExceedsLimits() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Put,
                "/salary/own",
                withJson(testSalary.copy(jobTitle = (0..100).joinToString { "a" }))
            ).apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun salaryPutAlreadyExists() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Put,
                "/salary/own",
                withJson(testSalary)
            ).apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    private fun salaryPatchExecute(salary: ModifySalaryDTO) {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary/own",
                withJson(salary)
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)

                val newSalary = assertDeserialize<ModifySalaryDTO>(response.content!!) ?: return@apply
                assertEquals(salary, newSalary)
            }
        }
    }

    @Test
    fun salaryPatchSucceedsSalary() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(testModifySalary.copy(salary = 42069))
    }

    @Test
    fun salaryPatchSucceedsJobTitle() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(testModifySalary.copy(jobTitle = "Foo-ist in Bar-logy"))
    }

    @Test
    fun salaryPatchSucceedsState() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(testModifySalary.copy(state = State.BRANDENBURG))
    }

    @Test
    fun salaryPatchSucceedsLevelOfEducation() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(testModifySalary.copy(levelOfEducation = LevelOfEducation.NONE))
    }

    @Test
    fun salaryPatchUnknownSessionId() {
        insertTestSalary(69)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary/own",
                withJson(testModifySalary)
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun salaryPatchJobTitleExceedsLimits() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary/own",
                withJson(testSalary.copy(jobTitle = (0..100).joinToString { "a" }))
            ).apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun salaryPatchUnknownSalary() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary/own",
                withJson(testModifySalary)
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun jobTitleFilter() {
        assertFilterSQL(JobTitleFilter("foobar"), "salary.job_title = 'foobar'")
    }
}