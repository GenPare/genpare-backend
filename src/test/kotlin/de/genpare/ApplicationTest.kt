package de.genpare

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.typesafe.config.ConfigFactory
import de.genpare.data.dtos.*
import de.genpare.data.enums.Gender
import de.genpare.data.enums.State
import de.genpare.database.entities.Member
import de.genpare.database.entities.Salary
import de.genpare.modules.setup
import de.genpare.util.LocalDateTypeAdapter
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.test.*

class ApplicationTest {
    private val birthdate = LocalDate.of(1815, 12, 10)

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
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

    private fun insertTestUser() =
        transaction {
            Member.new {
                email = "test@example.com"
                name = "Foo"
                sessionId = 1337
                birthdate = LocalDate.of(1815, 12, 10)
            }
        }

    private fun insertTestSalary(_memberId: Long) {
        transaction {
            Salary.new {
                memberId = _memberId
                salary = 69420
                gender = Gender.DIVERSE
                jobTitle = "Bar-ist in Foo-logy"
                state = State.BERLIN
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
                withJson(MemberDTO(null, "test@example.com", "Foo", birthdate))
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
                withJson(MemberDTO(1337, "test@example.com", "Foo", birthdate))
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
                withJson(MemberDTO(null, "test@example.com", "Bar", birthdate))
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
                "/members/session",
                withJson(LoginDTO("test@example.com"))
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
                "/members/session",
                withJson(LoginDTO("test@example.com"))
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
                "/members/session",
                withJson(LogoutDTO("test@example.com"))
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
                "/members/session",
                withJson(LogoutDTO("test@example.com"))
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
    fun salaryPostSucceeds() {
        insertTestUser()

        withApplication(testEnvironment) {
            val salary = NewSalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN)
            handleRequest(
                HttpMethod.Post,
                "/salary",
                withJson(salary)
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)

                val newSalary = assertDeserialize<NewSalaryDTO>(response.content!!) ?: return@apply
                assertEquals(salary, newSalary)
            }
        }
    }

    @Test
    fun salaryPostInvalidSessionId() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/salary",
                withJson(NewSalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN))
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun salaryPostJobTitleExceedsLimits() {
        insertTestUser()

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/salary",
                withJson(NewSalaryDTO(1337, 69420, Gender.DIVERSE, (0..100).joinToString { "a" }, State.BERLIN))
            ).apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun salaryPostAlreadyExists() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/salary",
                withJson(NewSalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN))
            ).apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    private fun salaryPatchExecute(salary: ModifySalaryDTO) {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary",
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

        salaryPatchExecute(ModifySalaryDTO(1337, 42069, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN))
    }

    @Test
    fun salaryPatchSucceedsGender() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(ModifySalaryDTO(1337, 69420, Gender.FEMALE, "Bar-ist in Foo-logy", State.BERLIN))
    }

    @Test
    fun salaryPatchSucceedsJobTitle() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(ModifySalaryDTO(1337, 69420, Gender.DIVERSE, "Foo-ist in Bar-logy", State.BERLIN))
    }

    @Test
    fun salaryPatchSucceedsState() {
        val testUser = insertTestUser()
        insertTestSalary(testUser.id.value)

        salaryPatchExecute(ModifySalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BRANDENBURG))
    }

    @Test
    fun salaryPatchUnknownSessionId() {
        insertTestSalary(69)

        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Patch,
                "/salary",
                withJson(ModifySalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN))
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
                "/salary",
                withJson(NewSalaryDTO(1337, 69420, Gender.DIVERSE, (0..100).joinToString { "a" }, State.BERLIN))
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
                "/salary",
                withJson(NewSalaryDTO(1337, 69420, Gender.DIVERSE, "Bar-ist in Foo-logy", State.BERLIN))
            ).apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }
}