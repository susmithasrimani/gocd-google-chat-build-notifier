package io.github.susmithasrimani.gocd.googleChat.notificationPlugin

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(val status: String, val messages: List<String>)

val SUCCESS_STATUS = StatusResponse("success", emptyList())

fun ErrorStatus(errorMessages: List<String>): StatusResponse {
    return StatusResponse("error", errorMessages)
}