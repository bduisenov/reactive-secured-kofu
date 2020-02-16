package org.kotlinlang.boot.reactivesecuredkofu.service

import org.kotlinlang.boot.reactivesecuredkofu.repository.UserEntity
import org.kotlinlang.boot.reactivesecuredkofu.repository.UserRepository

interface UserService {
    suspend fun saveUser(user: UserEntity): UserEntity
    suspend fun getUser(id: Long): UserEntity?
}

class UserServiceImpl(private val userRepository: UserRepository) : UserService {

    override suspend fun saveUser(user: UserEntity): UserEntity =
            userRepository.save(user)

    override suspend fun getUser(id: Long): UserEntity? =
            userRepository.findById(id)
}