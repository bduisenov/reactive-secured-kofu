package org.kotlinlang.boot.reactivesecuredkofu

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            val expectedJson = """{ "name": "Monika" }""".trimIndent()

            client.get()
                    .uri("$endpoint/1")
                    .exchange()
                    .expectStatus().is2xxSuccessful
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .json(expectedJson)
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
    }

    @AfterAll
    fun afterAll() {
        context.close()
        postgresqlContainer.close()
    }

}
