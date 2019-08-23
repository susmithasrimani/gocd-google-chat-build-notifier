package io.github.susmithasrimani.gocd.googleChat

import com.thoughtworks.go.plugin.api.GoApplicationAccessor
import com.thoughtworks.go.plugin.api.GoPlugin
import com.thoughtworks.go.plugin.api.GoPluginIdentifier
import com.thoughtworks.go.plugin.api.annotation.Extension
import com.thoughtworks.go.plugin.api.annotation.Load
import com.thoughtworks.go.plugin.api.annotation.UnLoad
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException
import com.thoughtworks.go.plugin.api.info.PluginContext
import com.thoughtworks.go.plugin.api.logging.Logger
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse

@Extension
class GoogleChatNotificationPlugin : GoPlugin {
    private var accessor: GoApplicationAccessor? = null
    private val logger: Logger? = Logger.getLoggerFor(this.javaClass)
    @Load
    fun onLoad(context: PluginContext) {
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

    // a GoPluginIdentifier tells GoCD what kind of a plugin this is
    // and what version(s) of the request/response API it supports
    override fun pluginIdentifier(): GoPluginIdentifier {
        return GoPluginIdentifier("notification", listOf("2.0"))
    }

    // handle the request and return a response
    // the response is very much like a HTTP response —
    // it has a status code, a response body and optional headers
    @Throws(UnhandledRequestTypeException::class)
    override fun handle(request: GoPluginApiRequest): GoPluginApiResponse {
        throw UnhandledRequestTypeException(request.requestName())
    }
}
