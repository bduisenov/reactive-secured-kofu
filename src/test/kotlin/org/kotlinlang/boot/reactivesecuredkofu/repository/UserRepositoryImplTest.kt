package org.kotlinlang.boot.reactivesecuredkofu.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.kotlinlang.boot.reactivesecuredkofu.dataConfig
import org.springframework.beans.factory.getBean
import org.springframework.boot.WebApplicationType.NONE
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.fu.kofu.application
import org.testcontainers.containers.PostgreSQLContainer

class UserRepositoryImplTest {

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

        context = application(NONE) { enable(dataConfig) }.run(profiles = "test")
        repository = context.getBean<UserRepositoryImpl>()
    }

    @AfterEach
    fun tearDown() {
        runBlocking { repository.deleteAll() }
    }

    @Test
    fun `when user is saved persists in database and generates an id`() {
        runBlocking {
            val result = repository.save(UserEntity(name = "Monika"))

            assertThat(result).extracting(UserEntity::id).isNotNull()
        }
    }

    @Test
    fun `when a valid id is specified then retrieves a user`() {
        runBlocking {
            val saved = repository.save(UserEntity(name = "Monika"))

            val result = repository.findById(saved.id!!)

            assertThat(result).isNotNull
        }
    }

    @AfterAll
    fun afterAll() {
        context.close()
        postgresqlContainer.close()
    }
}