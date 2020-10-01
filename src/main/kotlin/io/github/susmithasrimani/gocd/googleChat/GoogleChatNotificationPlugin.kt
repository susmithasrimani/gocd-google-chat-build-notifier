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
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse
import com.typesafe.config.ConfigFactory
import io.github.susmithasrimani.gocd.googleChat.chatMessage.*
import io.github.susmithasrimani.gocd.googleChat.notificationPlugin.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.io.File

@Extension
class GoogleChatNotificationPlugin() : GoPlugin {
    private val confPathEnvVarName = "GCHAT_NOTIFIER_CONF_PATH"
    private var accessor: GoApplicationAccessor? = null
    private val logger: Logger? = Logger.getLoggerFor(this.javaClass)
    private var serverHost: String = ""

    constructor (accessor: GoApplicationAccessor, serverHost: String) : this() {
        this.accessor = accessor
        this.serverHost = serverHost
    }

    @Load
    fun onLoad(context: PluginContext) {
        val pathname: String = System.getenv(confPathEnvVarName)
            ?: throw Exception("$confPathEnvVarName environment variable is not defined")
        val file = File(pathname)

        val config = ConfigFactory.parseFile(file)

        serverHost = config.getString("serverHost")
        logger?.info("Plugin loaded with server host: $serverHost")

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

    override fun pluginIdentifier() =
        GoPluginIdentifier(NOTIFICATION_PLUGIN_KIND, listOf(PLUGIN_API_VERSION))

    override fun handle(request: GoPluginApiRequest): GoPluginApiResponse {
        return when (request.requestName()) {
            REQUEST_NOTIFICATIONS_INTERESTED_IN -> handleNotificationsInterestedIn()
            REQUEST_STAGE_STATUS_CHANGED -> handleStageStatusChanged(request)
            PLUGIN_SETTINGS_GET_VIEW -> handlePluginSettingsGetView()
            PLUGIN_SETTINGS_GET_CONFIGURATION -> handlePluginSettingsGetConfiguration()
            PLUGIN_SETTINGS_VALIDATE_CONFIGURATION -> handlePluginSettingsValidateConfiguration(request)
            else -> DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE)
        }
    }

    private fun handlePluginSettingsValidateConfiguration(request: GoPluginApiRequest): GoPluginApiResponse {
        val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
        val pluginConfigToBeValidated = json.parse(ValidatePluginConfigRequest.serializer(), request.requestBody())
        val webhookUrl = pluginConfigToBeValidated.config.webhookUrl.value.trim()
        val successResponseBody = json.stringify(ValidationError.serializer().list, emptyList())

        if (webhookUrl == "") {
            val validationError = ValidationError(key = "webhookUrl", message = "Webhook URL cannot be empty")
            val responseBody = json.stringify(ValidationError.serializer().list, listOf(validationError))
            return DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE, responseBody)
        }

        return DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE, successResponseBody)
    }

    private fun handlePluginSettingsGetConfiguration(): GoPluginApiResponse {
        val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
        val responseBody = json.stringify(GetConfigResponse.serializer(),
            GetConfigResponse(webhookUrl = ConfigAttributes(
                displayName = "Webhook URL",
                displayOrder = "0",
                required = true,
                secure = true)))
        return DefaultGoPluginApiResponse(200, responseBody)
    }

    private fun handlePluginSettingsGetView(): GoPluginApiResponse {
        val json = Json(JsonConfiguration.Stable)
        val responseBody = json.stringify(GetViewResponse.serializer(),
            GetViewResponse(viewTemplate))
        return DefaultGoPluginApiResponse(200, responseBody)
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

        val hangoutsAPIRequestBody = buildChatMessage(failedStageID, failedStageURL, failedJobsConsoleLogURLs)

        val config = getConfig()

        val hangoutsAPIEndpoint = config!!.webhookUrl
        val (_, apiResponse, _) = Fuel.post(hangoutsAPIEndpoint)
            .jsonBody(hangoutsAPIRequestBody)
            .response()

        if (apiResponse.statusCode == 200) {
            return DefaultGoPluginApiResponse(SUCCESSFUL_RESPONSE_CODE, successResponseBody)
        }

        val failureResponseBody = json.stringify(StatusResponse.serializer(),
            errorStatus(listOf("could not send hangouts notification")))

        return DefaultGoPluginApiResponse(FAILURE_RESPONSE_CODE, failureResponseBody)
    }

    private fun getConfig(): Config? {
        val request = DefaultGoApiRequest(
            PLUGIN_SETTINGS_GET,
            "1.0",
            pluginIdentifier()
        )

        val jsonConfiguration = JsonConfiguration.Stable
        val json = Json(jsonConfiguration)
        val getPluginConfigRequestBody =
            json.stringify(GetPluginConfigRequest.serializer(),
                GetPluginConfigRequest("io.github.susmithasrimani.gocd.googleChat"))

        request.setRequestBody(getPluginConfigRequestBody)

        if (accessor == null) {
            return null
        }

        val response = accessor?.submit(request)

        if (response == null) {
            logger?.error("received null response while trying to get config from GoCD server")
            return null
        }

        if (response.responseCode() != 200) {
            logger?.error("The GoCD server sent an unexpected status code " + response.responseCode() +
                " with the response body " + response.responseBody())
            return null
        }

        return json.parse(Config.serializer(), response.responseBody())
    }

    fun buildChatMessage(
        failedStageID: String,
        failedStageURL: String,
        failedJobsConsoleLogURLs: Map<String, String>
    ): String {

        val keyValueWidgets: List<Widget> =
            failedJobsConsoleLogURLs.map { (jobName, jobURL) ->
                KeyValueWidgetWrapper(keyValue = KeyValue(
                    topLabel = "Job Name",
                    content = jobName,
                    button = TextButtonWrapper(
                        textButton = TextButton(
                            text = "View Console Logs",
                            onClick = OnClick(
                                openLink = URL(jobURL)
                            )
                        )
                    )
                ))
            }

        val hangoutsChatMessage = Cards(
            listOf(Card(header = Header(title = "Stage failed"), sections = listOf(Section(
                widgets = listOf(
                    KeyValueWidgetWrapper(keyValue = KeyValue(
                        topLabel = "Stage",
                        content = failedStageID,
                        onClick = OnClick(
                            openLink = URL(failedStageURL)
                        )
                    ))
                ) + keyValueWidgets
            ), Section(
                widgets = listOf(
                    ButtonsWidget(buttons = listOf(TextButtonWrapper(textButton = TextButton(
                        text = "Open Stage",
                        onClick = OnClick(
                            openLink = URL(failedStageURL)
                        )
                    ))))
                )
            ))
            ))
        )

        val jsonConfiguration = JsonConfiguration.Stable.copy(
            strictMode = false,
            encodeDefaults = false,
            prettyPrint = true,
            indent = "  "
        )
        val json = Json(jsonConfiguration)

        return json.stringify(Cards.serializer(), hangoutsChatMessage)
    }

    private fun handleNotificationsInterestedIn(): GoPluginApiResponse {
        val json = Json(JsonConfiguration.Stable)
        val responseBody = json.stringify(NotificationsInterestedResponse.serializer(),
            NotificationsInterestedResponse(listOf(STAGE_STATUS_NOTIFICATION)))
        return DefaultGoPluginApiResponse(200, responseBody)
    }
}
