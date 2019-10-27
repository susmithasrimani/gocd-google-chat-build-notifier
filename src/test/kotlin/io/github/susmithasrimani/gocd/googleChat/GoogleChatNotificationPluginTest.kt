package io.github.susmithasrimani.gocd.googleChat

import com.thoughtworks.go.plugin.api.GoApplicationAccessor
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse
import com.thoughtworks.go.plugin.api.response.GoApiResponse
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ua.com.lavi.komock.http.server.MockServer
import ua.com.lavi.komock.http.server.UnsecuredMockServer
import ua.com.lavi.komock.model.config.http.CaptureProperties
import ua.com.lavi.komock.model.config.http.HttpServerProperties
import ua.com.lavi.komock.model.config.http.RouteProperties

class GoogleChatNotificationPluginTest : FunSpec({
    test("buildChatMessage returns correct json request body for hangouts chat message") {
        val expectedJSON = """{
  "cards": [
    {
      "header": {
        "title": "Stage failed"
      },
      "sections": [
        {
          "widgets": [
            {
              "keyValue": {
                "topLabel": "Stage",
                "content": "somepipeline/1/stage/10",
                "onClick": {
                  "openLink": {
                    "url": "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10"
                  }
                }
              }
            },
            {
              "keyValue": {
                "topLabel": "Job Name",
                "content": "defaultJob",
                "button": {
                  "textButton": {
                    "text": "View Console Logs",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/defaultJob"
                      }
                    }
                  }
                }
              }
            },
            {
              "keyValue": {
                "topLabel": "Job Name",
                "content": "anotherJob",
                "button": {
                  "textButton": {
                    "text": "View Console Logs",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/anotherJob"
                      }
                    }
                  }
                }
              }
            }
          ]
        },
        {
          "widgets": [
            {
              "buttons": [
                {
                  "textButton": {
                    "text": "Open Stage",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10"
                      }
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}"""
        val jobsAndURLs = mapOf(
            "defaultJob" to "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/defaultJob",
            "anotherJob" to "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/anotherJob"
        )
        val plugin = GoogleChatNotificationPlugin()
        val jsonContent = plugin.buildChatMessage("somepipeline/1/stage/10",
            "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10",
            jobsAndURLs
        )

        jsonContent shouldBe expectedJSON
    }

    test("stage status changed for failed stage sends message to hangouts") {
        //        val mockGoApplicationAccessor = createMockGoApplicationAccessor()
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val mockGoApiResponse = mockk<GoApiResponse>()
        every { mockGoApiResponse.responseCode() } returns 200
        every { mockGoApiResponse.responseBody() } returns "{\"webhookUrl\":\"http://localhost:9090\"}"
        every { mockGoApplicationAccessor.submit(isNull(inverse = true)) } returns mockGoApiResponse
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")
        val mockServer = createMockHangoutsChatServer()
        mockServer.start()

        val request = DefaultGoPluginApiRequest("notification", "2", "stage-status")
        val requestBody = """
{
  "pipeline": {
    "name": "somepipeline",
    "counter": "1",
    "label": "1",
    "group": "defaultGroup",
    "build-cause": [
      {
        "material": {
          "git-configuration": {
            "shallow-clone": false,
            "branch": "master",
            "url": "https://github.com/susmithasrimani/gocd-google-chat-build-notifier"
          },
          "type": "git"
        },
        "changed": false,
        "modifications": [
          {
            "revision": "0fc4f4d4d6ad01cf409c71f8e8fed0f65be17dbf",
            "modified-time": "2019-08-29T17:28:17.000Z",
            "data": {}
          }
        ]
      }
    ],
    "stage": {
      "name": "stage",
      "counter": "10",
      "approval-type": "success",
      "approved-by": "anonymous",
      "state": "Failed",
      "result": "Failed",
      "create-time": "2019-08-29T22:57:26.120Z",
      "last-transition-time": "2019-08-29T22:57:46.719Z",
      "jobs": [
        {
          "name": "defaultJob",
          "schedule-time": "2019-08-29T22:57:26.120Z",
          "assign-time": "2019-08-29T22:57:33.922Z",
          "complete-time": "2019-08-29T22:57:46.719Z",
          "state": "Completed",
          "result": "Failed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        },
        {
          "name": "anotherJob",
          "schedule-time": "2019-08-29T22:57:36.120Z",
          "assign-time": "2019-08-29T22:57:43.922Z",
          "complete-time": "2019-08-29T22:57:56.719Z",
          "state": "Completed",
          "result": "Failed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        }
      ]
    }
  }
}
        """
        val expectedResponseBody = """{"status":"success","messages":[]}"""

        request.setRequestBody(requestBody)
        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody

        // check the request sent to the mock hangouts chat server
        val expectedHangoutsChatRequestBody = """{
  "cards": [
    {
      "header": {
        "title": "Stage failed"
      },
      "sections": [
        {
          "widgets": [
            {
              "keyValue": {
                "topLabel": "Stage",
                "content": "somepipeline/1/stage/10",
                "onClick": {
                  "openLink": {
                    "url": "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10"
                  }
                }
              }
            },
            {
              "keyValue": {
                "topLabel": "Job Name",
                "content": "defaultJob",
                "button": {
                  "textButton": {
                    "text": "View Console Logs",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/defaultJob"
                      }
                    }
                  }
                }
              }
            },
            {
              "keyValue": {
                "topLabel": "Job Name",
                "content": "anotherJob",
                "button": {
                  "textButton": {
                    "text": "View Console Logs",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/anotherJob"
                      }
                    }
                  }
                }
              }
            }
          ]
        },
        {
          "widgets": [
            {
              "buttons": [
                {
                  "textButton": {
                    "text": "Open Stage",
                    "onClick": {
                      "openLink": {
                        "url": "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10"
                      }
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}"""

        val capturedDataList = mockServer.getCapturedData()

        capturedDataList shouldHaveSize 1
        val capturedData = capturedDataList[0]

        capturedData.requestHeaders["Content-Type"] shouldBe "application/json"
        capturedData.requestBody shouldBe expectedHangoutsChatRequestBody

        verify { mockGoApiResponse.responseCode() }
        verify { mockGoApiResponse.responseBody() }
        verify { mockGoApplicationAccessor.submit(any()) }

        confirmVerified(mockGoApiResponse)
        confirmVerified(mockGoApplicationAccessor)
        mockServer.stop()
    }

    test("stage status changed for successful stage does not send message to hangouts") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")
        val mockServer = createMockHangoutsChatServer()
        mockServer.start()

        val request = DefaultGoPluginApiRequest("notification", "2", "stage-status")
        val requestBody = """
{
  "pipeline": {
    "name": "somepipeline",
    "counter": "1",
    "label": "1",
    "group": "defaultGroup",
    "build-cause": [
      {
        "material": {
          "git-configuration": {
            "shallow-clone": false,
            "branch": "master",
            "url": "https://github.com/susmithasrimani/gocd-google-chat-build-notifier"
          },
          "type": "git"
        },
        "changed": false,
        "modifications": [
          {
            "revision": "0fc4f4d4d6ad01cf409c71f8e8fed0f65be17dbf",
            "modified-time": "2019-08-29T17:28:17.000Z",
            "data": {}
          }
        ]
      }
    ],
    "stage": {
      "name": "stage",
      "counter": "10",
      "approval-type": "success",
      "approved-by": "anonymous",
      "state": "Passed",
      "result": "Passed",
      "create-time": "2019-08-29T22:57:26.120Z",
      "last-transition-time": "2019-08-29T22:57:46.719Z",
      "jobs": [
        {
          "name": "defaultJob",
          "schedule-time": "2019-08-29T22:57:26.120Z",
          "assign-time": "2019-08-29T22:57:33.922Z",
          "complete-time": "2019-08-29T22:57:46.719Z",
          "state": "Completed",
          "result": "Passed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        },
        {
          "name": "anotherJob",
          "schedule-time": "2019-08-29T22:57:36.120Z",
          "assign-time": "2019-08-29T22:57:43.922Z",
          "complete-time": "2019-08-29T22:57:56.719Z",
          "state": "Completed",
          "result": "Passed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        }
      ]
    }
  }
}
        """
        val expectedResponseBody = """{"status":"success","messages":[]}"""

        request.setRequestBody(requestBody)
        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody

        val capturedDataList = mockServer.getCapturedData()

        capturedDataList shouldHaveSize 0
        mockServer.stop()
    }

    test("stage status changed for building stage does not send message to hangouts") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")
        val mockServer = createMockHangoutsChatServer()
        mockServer.start()

        val request = DefaultGoPluginApiRequest("notification", "2", "stage-status")
        val requestBody = """
{
  "pipeline": {
    "name": "somepipeline",
    "counter": "1",
    "label": "1",
    "group": "defaultGroup",
    "build-cause": [
      {
        "material": {
          "git-configuration": {
            "shallow-clone": false,
            "branch": "master",
            "url": "https://github.com/susmithasrimani/gocd-google-chat-build-notifier"
          },
          "type": "git"
        },
        "changed": false,
        "modifications": [
          {
            "revision": "0fc4f4d4d6ad01cf409c71f8e8fed0f65be17dbf",
            "modified-time": "2019-08-29T17:28:17.000Z",
            "data": {}
          }
        ]
      }
    ],
    "stage": {
      "name": "stage",
      "counter": "10",
      "approval-type": "success",
      "approved-by": "anonymous",
      "state": "Building",
      "result": "Unknown",
      "create-time": "2019-08-29T22:57:26.120Z",
      "last-transition-time": "",
      "jobs": [
        {
          "name": "defaultJob",
          "schedule-time": "2019-08-29T22:57:26.120Z",
          "assign-time": "",
          "complete-time": "",
          "state": "Scheduled",
          "result": "Unknown",
          "agent-uuid": null
        },
        {
          "name": "anotherJob",
          "schedule-time": "2019-08-29T22:57:36.120Z",
          "assign-time": "",
          "complete-time": "",
          "state": "Scheduled",
          "result": "Unknown",
          "agent-uuid": null
        }
      ]
    }
  }
}
        """
        val expectedResponseBody = """{"status":"success","messages":[]}"""

        request.setRequestBody(requestBody)
        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody

        val capturedDataList = mockServer.getCapturedData()

        capturedDataList shouldHaveSize 0
        mockServer.stop()
    }

    test("stage status changed for failed stage fails to send message to hangouts " +
        "when hangouts is not reachable") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

        val request = DefaultGoPluginApiRequest("notification", "2", "stage-status")
        val requestBody = """
{
  "pipeline": {
    "name": "somepipeline",
    "counter": "1",
    "label": "1",
    "group": "defaultGroup",
    "build-cause": [
      {
        "material": {
          "git-configuration": {
            "shallow-clone": false,
            "branch": "master",
            "url": "https://github.com/susmithasrimani/gocd-google-chat-build-notifier"
          },
          "type": "git"
        },
        "changed": false,
        "modifications": [
          {
            "revision": "0fc4f4d4d6ad01cf409c71f8e8fed0f65be17dbf",
            "modified-time": "2019-08-29T17:28:17.000Z",
            "data": {}
          }
        ]
      }
    ],
    "stage": {
      "name": "stage",
      "counter": "10",
      "approval-type": "success",
      "approved-by": "anonymous",
      "state": "Failed",
      "result": "Failed",
      "create-time": "2019-08-29T22:57:26.120Z",
      "last-transition-time": "2019-08-29T22:57:46.719Z",
      "jobs": [
        {
          "name": "defaultJob",
          "schedule-time": "2019-08-29T22:57:26.120Z",
          "assign-time": "2019-08-29T22:57:33.922Z",
          "complete-time": "2019-08-29T22:57:46.719Z",
          "state": "Completed",
          "result": "Failed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        },
        {
          "name": "anotherJob",
          "schedule-time": "2019-08-29T22:57:36.120Z",
          "assign-time": "2019-08-29T22:57:43.922Z",
          "complete-time": "2019-08-29T22:57:56.719Z",
          "state": "Completed",
          "result": "Failed",
          "agent-uuid": "fd3a922c-a24a-4580-a02c-5d5c57350305"
        }
      ]
    }
  }
}
        """
        val expectedResponseBody = """{"status":"error","messages":["could not send hangouts notification"]}"""

        request.setRequestBody(requestBody)
        val response = plugin.handle(request)

        response.responseCode() shouldBe 500
        response.responseBody() shouldBe expectedResponseBody
    }

    test("notifications interested in gives stage status notifications in response") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

        val request = DefaultGoPluginApiRequest("notification", "2", "notifications-interested-in")
        val expectedResponseBody = """{"notifications":["stage-status"]}"""

        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody
    }

    test("get settings view request gives plugin settings view template in response") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

        val request = DefaultGoPluginApiRequest("notification", "2", "go.plugin-settings.get-view")
        val expectedResponseBody = "{\"template\":\"<div class=\\\"form_item_block\\\">" +
            "<label>Webhook URL:<span class='asterix'>*</span></label>" +
            "<input type=\\\"text\\\" ng-model=\\\"webhookUrl\\\" " +
            "placeholder=\\\"https://chat.googleapis.com/v1/spaces/ABCDEF/messages?key=abcdefgh&token=abcdefgh\\\"/>" +
            "<span class=\\\"form_error\\\" ng-show=\\\"GOINPUTNAME[webhookUrl].\$error.server\\\">" +
            "{{ GOINPUTNAME[webhookUrl].\$error.server}}" +
            "</span></div>\"}"

        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody
    }

    test("get plugin configuration request gives plugin configuration in response") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

        val request = DefaultGoPluginApiRequest("notification", "2", "go.plugin-settings.get-configuration")
        val expectedResponseBody = """{
  "webhookUrl": {
    "display-name": "Webhook URL",
    "display-order": "0",
    "required": true,
    "secure": true
  }
}"""

        val response = plugin.handle(request)

        response.responseCode() shouldBe 200
        response.responseBody() shouldBe expectedResponseBody
    }

    context("validate plugin configuration request") {
        test("returns plugin configuration is not valid in response when value is not present") {
            val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
            val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

            val request = DefaultGoPluginApiRequest("notification", "2", "go.plugin-settings.validate-configuration")
            val requestBody = """{
  "plugin-settings": {
      "webhookUrl":{}
  }
}"""
            request.setRequestBody(requestBody)
            val expectedResponseBody = """[
  {
    "key": "webhookUrl",
    "message": "Webhook URL cannot be empty"
  }
]"""

            val response = plugin.handle(request)

            response.responseCode() shouldBe 200
            response.responseBody() shouldBe expectedResponseBody
        }

        test("returns plugin configuration is not valid in response when it's not valid") {
            val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
            val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

            val request = DefaultGoPluginApiRequest("notification", "2", "go.plugin-settings.validate-configuration")
            val requestBody = """{
  "plugin-settings": {
      "webhookUrl": {
        "value": "   "
      }
  }
}"""
            request.setRequestBody(requestBody)
            val expectedResponseBody = """[
  {
    "key": "webhookUrl",
    "message": "Webhook URL cannot be empty"
  }
]"""

            val response = plugin.handle(request)

            response.responseCode() shouldBe 200
            response.responseBody() shouldBe expectedResponseBody
        }

        test("returns plugin configuration is valid in response when it's valid") {
            val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
            val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

            val request = DefaultGoPluginApiRequest("notification", "2", "go.plugin-settings.validate-configuration")
            val requestBody = """{
  "plugin-settings": {
      "webhookUrl": {
        "value": "https://some-url.over-here.com"
      }
  }
}"""
            request.setRequestBody(requestBody)
            val expectedResponseBody = """[
]"""

            val response = plugin.handle(request)

            response.responseCode() shouldBe 200
            response.responseBody() shouldBe expectedResponseBody
        }
    }

    test("miscellaneous or not handled request type gives a default response") {
        val mockGoApplicationAccessor = mockk<GoApplicationAccessor>()
        val plugin = GoogleChatNotificationPlugin(mockGoApplicationAccessor, "http://localhost:9090/", "https://gocd-server.com")

        val request = DefaultGoPluginApiRequest("notification", "2", "blah-bloo")

        val response = plugin.handle(request)

        val expectedResponse = DefaultGoPluginApiResponse(200)

        response.responseCode() shouldBe expectedResponse.responseCode()
        response.responseBody() shouldBe expectedResponse.responseBody()
    }
})

fun createMockHangoutsChatServer(): MockServer {
    val serverProperties = HttpServerProperties()
        .withName("mock-server")
        .withPort(9090)
        .withCapture(CaptureProperties().enabled())

    val mockServer = UnsecuredMockServer(serverProperties)
    val routeProperties = RouteProperties()
    routeProperties.httpMethod = "POST"
    routeProperties.contentType = "application/json"
    routeProperties.code = 200
    routeProperties.responseBody = "{}"
    routeProperties.url = "/"
    mockServer.addRoute(routeProperties)
    return mockServer
}
