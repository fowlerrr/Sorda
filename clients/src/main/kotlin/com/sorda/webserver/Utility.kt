package com.sorda.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController

class Whitelist {
   companion object {
        val logger = LoggerFactory.getLogger(RestController::class.java)

        const val w1 = "http://localhost:3000"
        const val w2 = "http://localhost:3001"
   }
}


