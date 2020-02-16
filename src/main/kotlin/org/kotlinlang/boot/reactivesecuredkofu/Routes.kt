package org.kotlinlang.boot.reactivesecuredkofu

import org.kotlinlang.boot.reactivesecuredkofu.controller.UserController
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.coRouter

// visible for testing
const val endpoint = "/user"

fun routes(userController: UserController) = coRouter {
    (endpoint and accept(MediaType.APPLICATION_JSON)).nest {
        GET("/{id}") {
            val id = it.pathVariable("id").toLongOrNull()
            userController.getUser(id)
        }
        POST("/", contentType(MediaType.APPLICATION_JSON)) {
            val body = it.awaitBody<Map<String, String>>()
            userController.saveUser(body)
        }
    }
}