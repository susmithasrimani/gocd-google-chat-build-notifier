package io.github.susmithasrimani.gocd.googleChat

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.thoughtworks.go.plugin.api.GoApplicationAccessor
import com.thoughtworks.go.plugin.api.GoPlugin
import com.thoughtworks.go.plugin.api.GoPluginIdentifier
import com.thoughtworks.go.plugin.api.annotation.Extension
import com.thoughtworks.go.plugin.api.annotation.Load
import com.thoughtworks.go.plugin.api.annotation.UnLoad
import com.thoughtworks.go.plugin.api.info.PluginContext
import com.thoughtworks.go.plugin.api.logging.Logger
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse
import com.typesafe.config.ConfigFactory
import io.github.susmithasrimani.gocd.googleChat.chatMessage.TextMessage
import io.github.susmithasrimani.gocd.googleChat.notificationPlugin.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Extension
class GoogleChatNotificationPlugin : GoPlugin {
    private val confPathEnvVarName = "GCHAT_NOTIFIER_CONF_PATH"
    private var accessor: GoApplicationAccessor? = null
    private val logger: Logger? = Logger.getLoggerFor(this.javaClass)
    private var webhookURL: String = ""
    private var serverHost: String = ""
    @Load
    fun onLoad(context: PluginContext) {
        val pathname: String = System.getenv(confPathEnvVarName)
                ?: throw Exception("$confPathEnvVarName environment variable is not defined")
        val file = File(pathname)

        val config = ConfigFactory.parseFile(file)

        webhookURL = config.getString("webhookUrl")
        serverHost = config.getString("serverHost")
        logger?.info("Plugin loaded with webhook: $webhookURL and server host: $serverHost")

        logger?.info("Plugin loaded")
    }

    @UnLoad
    fun onUnload(context: PluginContext) {
        logger?.info("Plugin unloaded")
    }

    // this method is executed once at startup
    override fun initializeGoApplicationAccessor(accessor: GoApplicationAccessor) {
        this.accessor = accessor
    }

    override fun pluginIdentifier(): GoPluginIdentifier {
        return GoPluginIdentifier(NOTIFICATION_PLUGIN_KIND, listOf(PLUGIN_API_VERSION))
    }

    override fun handle(request: GoPluginApiRequest): GoPluginApiResponse {
        return when (request.requestName()) {
            REQUEST_NOTIFICATIONS_INTERESTED_IN -> handleNotificationsInterestedIn()
            REQUEST_STAGE_STATUS_CHANGED -> handleStageStatusChanged(request)
            else -> DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE)
        }
    }

    private fun handleStageStatusChanged(request: GoPluginApiRequest): GoPluginApiResponse {
        val jsonConfiguration = JsonConfiguration.Stable.copy(strictMode = false)
        val json = Json(jsonConfiguration)
        val stageStatusNotification = json.parse(StageStatusNotification.serializer(), request.requestBody())
        val successResponseBody = json.stringify(StatusResponse.serializer(), SUCCESS_STATUS)

        if (!stageStatusNotification.pipeline.stageFailed()) {
            return DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE, successResponseBody)
        }

        val failedStageID = stageStatusNotification.pipeline.stageID()
        val failedStageURL = stageStatusNotification.pipeline.stageURL(serverHost)
        val failedJobsConsoleLogURLs = stageStatusNotification.pipeline.failedJobsConsoleLogURLs(serverHost)

        val hangoutsChatMessage = TextMessage("Stage $failedStageID failed $failedStageURL. " +
                "Check job logs - ${failedJobsConsoleLogURLs.map { (job, jobURL) -> "$job : $jobURL" }.joinToString("\n")}")
        val hangoutsAPIRequestBody = json.stringify(TextMessage.serializer(), hangoutsChatMessage)

        val (_, apiResponse, _) = Fuel.post(webhookURL)
                .jsonBody(hangoutsAPIRequestBody)
                .response()

        if (apiResponse.statusCode == 200) {
            return DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE, successResponseBody)
        }

        val failureResponseBody = json.stringify(StatusResponse.serializer(),
                ErrorStatus(listOf("could not send hangouts notification")))

        return DefaultGoPluginApiResponse(FAILURE_RESPONSE_CODE, failureResponseBody)
    }

    private fun handleNotificationsInterestedIn(): GoPluginApiResponse {
        val json = Json(JsonConfiguration.Stable)
        val responseBody = json.stringify(NotificationsInterestedResponse.serializer(),
                NotificationsInterestedResponse(listOf(STAGE_STATUS_NOTIFICATION)))
        return DefaultGoPluginApiResponse(200, responseBody)
    }
}
