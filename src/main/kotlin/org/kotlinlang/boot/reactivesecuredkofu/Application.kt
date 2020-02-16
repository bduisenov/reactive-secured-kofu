package org.kotlinlang.boot.reactivesecuredkofu

import org.kotlinlang.boot.reactivesecuredkofu.controller.UserControllerImpl
import org.springframework.boot.WebApplicationType
import org.springframework.fu.kofu.application
import org.springframework.fu.kofu.webflux.webFlux
import reactor.blockhound.BlockHound
import reactor.tools.agent.ReactorDebugAgent

val app = application(WebApplicationType.REACTIVE) {
    webFlux { codecs { jackson { indentOutput = true } } }
    enable(securityConfig)
    beans {
        bean(::routes)
        bean<UserControllerImpl>()
    }
}

fun main(args: Array<String>) {
    BlockHound.install()
    ReactorDebugAgent.init()

    app.run(args)
}
