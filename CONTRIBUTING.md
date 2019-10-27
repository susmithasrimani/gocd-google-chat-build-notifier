# Contributing

First of all, thanks for contributing to this project. Everyone is welcome to contribute. 
You can follow the below steps to understand how to get started with contributing

## Dev environment setup
The easiest way to setup would be to use [`docker`](https://docker.com). That is, you can do the coding in your editor in host machine,
run tests, build and everything in docker. If you have Java installed in your machine, that's good too.
We are currently using Java 10 in our development environments. Follow the below steps, skip docker step if you don't
want to use docker and have Java 10 installed in your machine 😄

Common steps:
```
$ git clone https://github.com/susmithasrimani/gocd-google-chat-build-notifier
$ cd gocd-google-chat-build-notifier
$ # opening it in visual studio code, an example code editor
$ code .
```

If using docker, follow these first:
```
$ # running docker container to run tests, build and to build plugin jar
$ docker run --rm -it -v $(pwd):/gocd-google-chat-build-notifier openjdk:10 bash
$ # if you are using docker, now you will be inside the bash shell of docker container
$ # go to mounted volume directory inside docker container
$ cd /gocd-google-chat-build-notifier/
```

Common steps continued:
```
$ # first time test run alone will take a few minutes
$ ./gradlew test
$ # now run build to make sure everything compiles
$ ./gradlew build
$ # now build the plugin jar - a fat/uber jar
$ ./gradlew uberJar
$ # check the plugin jar
$ ls build/libs/gocd-google-chat-build-notifier-uber.jar
$ # to get code coverage report run this
$ ./gradlew jacocoTestReport
$ # check the report here. it can be opened with the browser
$ ls build/reports/jacoco/test/html/index.html
$ # to automatically lint your code using ktlint formatter
$ # you can use a git pre commit hook. add it like this
$ ./gradlew addKtlintFormatGitPreCommitHook
$ # to check the various other gradle tasks present
$ ./gradlew tasks
```

Subsequent gradle runs will be quite fast! Make changes to the code and run the 
above commands to test, build and build plugin jar. You can find the cards we are
working on in the [GitHub Projects page](https://github.com/susmithasrimani/gocd-google-chat-build-notifier/projects).
You can look for or create an issue out of the card and then inform in the issue you want to work on it.
We'll assign it to you and we'll help you in anything that you need with respect to the project.
