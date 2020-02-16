package org.kotlinlang.boot.reactivesecuredkofu.controller

import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

interface UserController {
    suspend fun getUser(id: Long?): ServerResponse
    suspend fun saveUser(json: Map<String, String>): ServerResponse
}

class UserControllerImpl : UserController {

    override suspend fun getUser(id: Long?): ServerResponse {
        return ok().bodyValueAndAwait(mapOf("name" to "Monika"))
    }

    override suspend fun saveUser(json: Map<String, String>): ServerResponse {
        return created(URI.create("1")).buildAndAwait()
    }
}