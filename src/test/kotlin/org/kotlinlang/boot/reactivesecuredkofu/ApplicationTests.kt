package org.kotlinlang.boot.reactivesecuredkofu

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.kotlinlang.boot.reactivesecuredkofu.repository.UserEntity
import org.kotlinlang.boot.reactivesecuredkofu.repository.UserRepository
import org.kotlinlang.boot.reactivesecuredkofu.repository.UserRepositoryImpl
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.testcontainers.containers.PostgreSQLContainer

class ApplicationTests {

    private val unauthenticatedClient = WebTestClient.bindToServer().baseUrl("http://localhost:8080").build()

    private val client = unauthenticatedClient.mutateWith { builder, _, _ ->
        builder.filter(basicAuthentication("test-username", "test-password"))
    }

    private lateinit var context: ConfigurableApplicationContext

    private lateinit var repository: UserRepository

    private val postgresqlContainer: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>()
            .apply {
                withDatabaseName("postgres")
                withUsername("postgres")
                withPassword("postgres")
                withExposedPorts(5432)
                withInitScript("docker-entrypoint-initdb.d/schema.sql")
            }

    @BeforeAll
    fun beforeAll() {
        postgresqlContainer.start()
        val postgresPort = postgresqlContainer.getMappedPort(5432)
        System.setProperty("postgres.port", postgresPort.toString())

        context = app.run(profiles = "test")
        repository = context.getBean<UserRepositoryImpl>()
    }

    @AfterEach
    fun tearDown() {
        runBlocking { repository.deleteAll() }
    }

    @Nested
    inner class Authentication {

        @Test
        fun `when authentication is not provided returns unauthorized`() {
            unauthenticatedClient.get()
                    .uri("$endpoint/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isUnauthorized
        }

        @Test
        fun `when credentials are invalid returns unauthorized`() {
            unauthenticatedClient.mutateWith { builder, _, _ -> builder.filter(basicAuthentication("username", "wrong-password")) }
                    .get()
                    .uri("$endpoint/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isUnauthorized
        }
    }

    @Nested
    inner class UserEndpoint {

        @Test
        fun `when a valid id is given then returns a user`() {
            val id = runBlocking { repository.save(UserEntity(name = "Monika")).id }

            val expectedJson = """{ "name": "Monika" }""".trimIndent()

            client.get()
                    .uri("$endpoint/$id")
                    .exchange()
                    .expectStatus().is2xxSuccessful
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .json(expectedJson)
        }

        @Test
        fun `when an invalid id is given then returns badRequest`() {
            client.get()
                    .uri("$endpoint/1")
                    .exchange()
                    .expectStatus().isBadRequest
        }

        @Test
        fun `when a valid json is given then returns the id of a created resource`() {
            val json = """{ "name": "Monika" }""".trimIndent()

            client.post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(json))
                    .exchange()
                    .expectStatus().is2xxSuccessful
                    .expectHeader().exists(LOCATION)
        }

        @Test
        fun `when an invalid json is given then returns badRequest`() {
            client.post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue("""{"some": "value"}"""))
                    .exchange()
                    .expectStatus().isBadRequest
        }
    }

    @AfterAll
    fun afterAll() {
        context.close()
        postgresqlContainer.close()
    }

}
