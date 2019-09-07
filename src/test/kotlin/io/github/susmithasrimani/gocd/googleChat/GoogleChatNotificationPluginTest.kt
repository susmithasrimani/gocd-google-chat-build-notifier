package io.github.susmithasrimani.gocd.googleChat

import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
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
        val plugin = GoogleChatNotificationPlugin("http://localhost:9090/", "https://gocd-server.com")
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
        mockServer.stop()
    }

    test("stage status changed for successful stage does not send message to hangouts") {
        val plugin = GoogleChatNotificationPlugin("http://localhost:9090/", "https://gocd-server.com")
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
        val plugin = GoogleChatNotificationPlugin("http://localhost:9090/", "https://gocd-server.com")
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
        val plugin = GoogleChatNotificationPlugin("http://localhost:9090/", "https://gocd-server.com")

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
