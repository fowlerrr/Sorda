package com.sorda.webserver

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Illegal Argument Error Handler
 */
@ExceptionHandler(IllegalArgumentException::class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
fun handleError(e: IllegalArgumentException): ErrorMessage = ErrorMessage(e.message.toString())

/**
 * Not Found Error Handler
 */
@ExceptionHandler(NotFoundException::class)
@ResponseStatus(HttpStatus.NOT_FOUND)
fun handleNotFound(e: NotFoundException): ErrorMessage = ErrorMessage(e.message.toString())

/**
 * Illegal State Error Handler
 */
@ExceptionHandler(IllegalStateException::class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
fun handleErrorState(e: IllegalStateException) = e.message

class NotFoundException(message: String) : Exception(message)

data class ErrorMessage constructor(
        val message: String
)