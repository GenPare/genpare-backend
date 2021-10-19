package de.genpare

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.typesafe.config.ConfigFactory
import de.genpare.data.dtos.*
import de.genpare.database.entities.Member
import de.genpare.modules.setup
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class ApplicationTest {
    private val gson = GsonBuilder()
        .serializeNulls()
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

    private fun insertTestUser() {
        transaction {
            Member.new {
                email = "test@example.com"
                name = "Foo"
                sessionId = 1337
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
                withJson(MemberDTO(null, "test@example.com", "Foo"))
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)

                val member = assertDeserialize<MemberDTO>(response.content!!) ?: return@apply
                assertEquals("test@example.com", member.email)
                assertEquals("Foo", member.name)
            }
        }
    }

    @Test
    fun createAccountWithNonNullId() {
        withApplication(testEnvironment) {
            handleRequest(
                HttpMethod.Post,
                "/members",
                withJson(MemberDTO(1337, "test@example.com", "Foo"))
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
                withJson(MemberDTO(null, "test@example.com", "Bar"))
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
}