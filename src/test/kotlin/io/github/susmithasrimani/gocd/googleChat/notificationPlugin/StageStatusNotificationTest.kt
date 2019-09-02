package io.github.susmithasrimani.gocd.googleChat.notificationPlugin

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class StageStatusNotificationTest : WordSpec({
    val failedJob = Job("failedJob", JobState.Completed, JobResult.Failed)
    val successfulJob = Job("successfulJob", JobState.Completed, JobResult.Passed)
    val anotherFailedJob = Job("anotherFailedJob", JobState.Completed, JobResult.Failed)

    val failedStage = Stage("failedStage", "15", StageState.Failed,
            listOf(failedJob, successfulJob, anotherFailedJob))
    val failedPipeline = Pipeline("dummyPipeline", "10", failedStage)

    val successfulStage = Stage("successfulStage", "5", StageState.Passed,
            listOf(successfulJob))
    val successfulPipeline = Pipeline("anotherDummyPipeline", "20", successfulStage)

    "stageURL" should {
        "return correct URL given server host" {
            failedPipeline.stageURL("https://dummy-gocd-server.com")
                    .shouldBe("https://dummy-gocd-server.com/go/pipelines/dummyPipeline/10/" +
                            "failedStage/15")

        }
    }

    "stageID" should {
        "return correct ID" {
            failedPipeline.stageID()
                    .shouldBe("dummyPipeline/10/" +
                            "failedStage/15")

        }
    }

    "stageFailed" When {
        "stage has failed" should {
            "return true" {
                failedPipeline.stageFailed().shouldBe(true)
            }
        }

        "stage has not failed" should {
            "return false" {
                successfulPipeline.stageFailed().shouldBe(false)
            }
        }
    }

    "failedJobsConsoleLogURLs" When {
        "stage has not failed" should {
            "return an empty map" {
                successfulPipeline.failedJobsConsoleLogURLs("https://dummy-gocd-server.com").size.shouldBe(0)
            }
        }
        "when stage has failed" should {
            "return map with URLs" {
                val expectedMap = mapOf(
                        "failedJob" to "https://dummy-gocd-server.com/go/tab/build/detail" +
                                "/dummyPipeline/10/failedStage/15/failedJob",
                        "anotherFailedJob" to "https://dummy-gocd-server.com/go/tab/build/detail/" +
                                "dummyPipeline/10/failedStage/15/anotherFailedJob"
                )
                failedPipeline.failedJobsConsoleLogURLs("https://dummy-gocd-server.com").shouldBe(expectedMap)
            }
        }
    }

    "json.parse" should {
        "return a StageStatusNotification instance" {
            val requestBody = """
{
  "pipeline": {
    "name": "dummy",
    "counter": "6",
    "label": "6",
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
      "name": "defaultStage",
      "counter": "1",
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
        }
      ]
    }
  }
}
            """
            val jsonConfiguration = JsonConfiguration.Stable.copy(strictMode = false)
            val json = Json(jsonConfiguration)
            val stageStatusNotification = json.parse(StageStatusNotification.serializer(), requestBody)
            val pipeline = stageStatusNotification.pipeline
            val stage = pipeline.stage

            pipeline.name.shouldBe("dummy")
            pipeline.counter.shouldBe("6")

            stage.name.shouldBe("defaultStage")
            stage.counter.shouldBe("1")
            stage.state.shouldBe(StageState.Failed)
            stage.jobs.size.shouldBe(1)

            val job = stage.jobs[0]
            job.name.shouldBe("defaultJob")
            job.state.shouldBe(JobState.Completed)
            job.result.shouldBe(JobResult.Failed)
        }
    }
})

