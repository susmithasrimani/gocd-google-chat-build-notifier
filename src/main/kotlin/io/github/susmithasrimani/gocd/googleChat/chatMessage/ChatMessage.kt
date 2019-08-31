package io.github.susmithasrimani.gocd.googleChat.chatMessage

import kotlinx.serialization.Serializable

@Serializable
data class TextMessage(val text: String)