package org.kotlinlang.boot.reactivesecuredkofu.controller

import org.kotlinlang.boot.reactivesecuredkofu.repository.UserEntity
import org.kotlinlang.boot.reactivesecuredkofu.service.UserService
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

interface UserController {
    suspend fun getUser(id: Long?): ServerResponse
    suspend fun saveUser(json: Map<String, String>): ServerResponse
}

class UserControllerImpl(private val userService: UserService) : UserController {

    override suspend fun getUser(id: Long?): ServerResponse {
        return when (val user = id?.let { userService.getUser(it) }) {
            is UserEntity -> ok().bodyValueAndAwait(user)
            else -> badRequest().buildAndAwait()
        }
    }

    override suspend fun saveUser(json: Map<String, String>): ServerResponse {
        return when (val user = json["name"]?.let { userService.saveUser(UserEntity(name = it)) }) {
            is UserEntity -> created(URI.create(user.id.toString())).buildAndAwait()
            else -> badRequest().buildAndAwait()
        }
    }
}