package io.github.susmithasrimani.gocd.googleChat

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.modules.plus

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
                Pair("defaultJob", "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/defaultJob"),
                Pair("anotherJob", "https://gocd-server.com/go/tab/build/detail/somepipeline/1/stage/10/anotherJob")
        )
        val plugin = GoogleChatNotificationPlugin()
        val jsonContent = plugin.buildChatMessage("somepipeline/1/stage/10",
                "https://gocd-server.com/go/pipelines/somepipeline/1/stage/10",
                jobsAndURLs
        )

        jsonContent shouldBe expectedJSON
    }
})
