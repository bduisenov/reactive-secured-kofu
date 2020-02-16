package org.kotlinlang.boot.reactivesecuredkofu

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.kotlinlang.boot.reactivesecuredkofu.repository.UserRepositoryImpl
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.fu.kofu.configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.WebFilterChainProxy
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

// MARK: db

data class PostgresqlR2dbcProperties(val host: String, val port: Int, val database: String, val username: String, val password: String)

fun postgresConnectionFactory(properties: PostgresqlR2dbcProperties) =
        PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(properties.host)
                .port(properties.port)
                .database(properties.database)
                .username(properties.username)
                .password(properties.password)
                .build())

fun postgresDatabaseClient(connectionFactory: ConnectionFactory) =
        DatabaseClient.builder().connectionFactory(connectionFactory).build()

// is there a way to register directly?
fun transactionalOperator(transactionManager: ReactiveTransactionManager) =
        TransactionalOperator.create(transactionManager)

val dataConfig = configuration {
    configurationProperties<PostgresqlR2dbcProperties>(prefix = "postgres")
    beans {
        bean(::postgresConnectionFactory)
        bean(::postgresDatabaseClient)
        bean<R2dbcTransactionManager>()
        bean(::transactionalOperator)
        bean<UserRepositoryImpl>()
    }
    // r2dbcPostgresql { } is not used because of https://github.com/spring-projects-experimental/spring-fu/issues/259#issuecomment-565734205
}

// MARK: security
// NOTE: DSL is not ready yet https://github.com/spring-projects-experimental/spring-security-kotlin-dsl/issues/17

fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

fun reactiveUserDetailsService(properties: SecurityProperties,
                               passwordEncoder: PasswordEncoder): MapReactiveUserDetailsService {
    val user = properties.user

    val password = passwordEncoder.encode(user.password)
    val roles = user.roles.toTypedArray()
    val userDetails = User.withUsername(user.name).password(password).roles(*roles).build()

    return MapReactiveUserDetailsService(userDetails)
}

fun authenticationManager(userDetailsService: ReactiveUserDetailsService) =
        UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)

fun httpSecurity(authenticationManager: ReactiveAuthenticationManager): ServerHttpSecurity =
        object : ServerHttpSecurity(), ApplicationContextAware {
            override fun setApplicationContext(applicationContext: ApplicationContext) {
                super.setApplicationContext(applicationContext)
            }
        }.authenticationManager(authenticationManager)

fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http.csrf().disable()
                .httpBasic().and()
                .authorizeExchange().anyExchange().authenticated().and()
                .build()

val securityConfig = configuration {
    configurationProperties<SecurityProperties>(prefix = "spring.security")
    beans {
        bean(::passwordEncoder)
        bean(::reactiveUserDetailsService)
        bean(::authenticationManager)
        bean(::httpSecurity, scope = BeanDefinitionDsl.Scope.PROTOTYPE)
        bean(::springWebFilterChain)
        bean<WebFilterChainProxy>()
    }
}