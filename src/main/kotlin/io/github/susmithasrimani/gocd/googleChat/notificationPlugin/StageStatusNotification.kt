package io.github.susmithasrimani.gocd.googleChat.notificationPlugin

import kotlinx.serialization.Serializable

@Serializable
data class StageStatusNotification(val pipeline: Pipeline)

@Serializable
data class Pipeline(
        val name: String,
        val counter: String,
        val stage: Stage
) {
    // returns a unique string to recognize a stage
    fun stageID(): String {
        return "$name/$counter/${stage.name}/${stage.counter}"
    }

    // returns the URL to the stage in the pipeline
    fun stageURL(goServerHost: String): String {
        return "$goServerHost/go/pipelines/${stageID()}"
    }

    // returns if the stage in the pipeline failed or not
    fun stageFailed(): Boolean {
        return stage.state == StageState.Failed
    }

    // returns a map of failed job names to their log urls
    fun failedJobsConsoleLogURLs(goServerHost: String): Map<String, String> {
        return stage.jobs.filter { job ->
            job.state == JobState.Completed && job.result == JobResult.Failed
        }.associate { job ->
            Pair(job.name, "$goServerHost/go/tab/build/detail/$name/$counter/${stage.name}/${stage.counter}/${job.name}")
        }
    }
}

@Serializable
data class Stage(
        val name: String,
        val counter: String,
        val state: StageState,
        val jobs: List<Job>
)

enum class StageState {
    Building,
    Passed,
    Failed,
    Cancelled
}

@Serializable
data class Job(
        val name: String,
        val state: JobState,
        val result: JobResult
)

enum class JobState {
    Scheduled,
    Completed
}

enum class JobResult {
    Unknown,
    Passed,
    Failed,
    Cancelled
}