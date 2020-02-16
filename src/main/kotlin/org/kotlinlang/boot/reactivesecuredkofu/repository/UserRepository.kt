package org.kotlinlang.boot.reactivesecuredkofu.repository

import org.springframework.data.r2dbc.core.*

data class UserEntity(val id: Long? = null, val name: String)

interface UserRepository {
    suspend fun save(entity: UserEntity): UserEntity
    suspend fun findById(id: Long): UserEntity?
    suspend fun deleteAll()
}

class UserRepositoryImpl(private val client: DatabaseClient) : UserRepository {

    // language=SQL
    private val insertStatement = "INSERT INTO users (name) VALUES(:name) RETURNING *"

    // language=SQL
    private val selectByIdStatement = "SELECT * FROM users WHERE id = :id"

    // language=SQL
    private val deleteAllStatement = "DELETE FROM users"

    override suspend fun save(entity: UserEntity): UserEntity =
            client.execute(insertStatement)
                    .bind("name", entity.name)
                    .asType<UserEntity>()
                    .fetch()
                    .awaitOne()

    override suspend fun findById(id: Long): UserEntity? =
            client.execute(selectByIdStatement)
                    .bind("id", id)
                    .asType<UserEntity>()
                    .fetch()
                    .awaitOneOrNull()

    override suspend fun deleteAll() =
            client.execute(deleteAllStatement).await()
}