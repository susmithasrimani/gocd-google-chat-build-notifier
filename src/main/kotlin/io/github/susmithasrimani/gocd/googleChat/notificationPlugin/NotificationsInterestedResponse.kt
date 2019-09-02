package io.github.susmithasrimani.gocd.googleChat.notificationPlugin

import kotlinx.serialization.Serializable

@Serializable
data class NotificationsInterestedResponse(val notifications: List<String>)
