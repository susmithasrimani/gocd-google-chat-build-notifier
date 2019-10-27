package io.github.susmithasrimani.gocd.googleChat.notificationPlugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetViewResponse(val template: String)

const val viewTemplate = "<div class=\"form_item_block\">" +
    "<label>Webhook URL:<span class='asterix'>*</span></label>" +
    "<input type=\"text\" ng-model=\"webhookUrl\" " +
    "placeholder=\"https://chat.googleapis.com/v1/spaces/ABCDEF/messages?key=abcdefgh&token=abcdefgh\"/>" +
    "<span class=\"form_error\" ng-show=\"GOINPUTNAME[webhookUrl].\$error.server\">" +
    "{{ GOINPUTNAME[webhookUrl].\$error.server}}" +
    "</span>" +
    "</div>"

@Serializable
data class GetConfigResponse(val webhookUrl: ConfigAttributes)

@Serializable
data class ConfigAttributes(
    @SerialName("display-name")
    val displayName: String,
    @SerialName("display-order")
    val displayOrder: String,
    val required: Boolean,
    val secure: Boolean
)

@Serializable
data class ConfigToValidate(val webhookUrl: StringValue)

@Serializable
data class StringValue(
    @Transient
    val value: String = ""
)

@Serializable
data class ValidatePluginConfigRequest(
    @SerialName("plugin-settings")
    val config: ConfigToValidate
)

@Serializable
data class ValidationError(
    val key: String,
    val message: String
)

@Serializable
data class Config(val webhookUrl: String)

@Serializable
data class GetPluginConfigRequest(
    @SerialName("plugin-id")
    val id: String
)
