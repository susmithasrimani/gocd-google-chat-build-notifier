# gocd-google-chat-build-notifier [![Build Status](https://travis-ci.org/susmithasrimani/gocd-google-chat-build-notifier.svg?branch=master)](https://travis-ci.org/susmithasrimani/gocd-google-chat-build-notifier)

[Google Chat](https://chat.google.com) based GoCD build notifier

## Downloading the plugin

To use this plugin, download the plugin jar from the [releases page](https://github.com/susmithasrimani/gocd-google-chat-build-notifier/releases)

## Setup and configuration
* Place the plugin jar in GoCD server external plugins directory `$GOCD_SERVER_DIRECTORY/plugins/external/`
* Create a config file for example `gchat_notif.conf` like this:

```
webhookUrl: "https://chat.googleapis.com/v1/spaces/ABCDEF/messages?key=abcdefgh&token=abcdefgh"
serverHost: "https://my-gocd-server-url.com"
```

`webhookUrl` - [Google Chat](https://chat.google.com) Incoming Webhook URL

`serverHost` - FQDN of the GoCD Server. All links on the Chat channel will be relative to this host

* In the shell that runs GoCD server define an environment variable `GCHAT_NOTIFIER_CONF_PATH` and set it's value to the path of `gchat_notif.conf`, for example
```
export GCHAT_NOTIFIER_CONF_PATH="/usr/local/some/path/gchat_notif.conf"
```
Or you can set environment variables by checking [this doc](https://docs.gocd.org/current/installation/install/server/linux.html#overriding-default-startup-arguments-and-environment) in `wrapper-config/wrapper-properties.conf`, for example
```
set.GCHAT_NOTIFIER_CONF_PATH=/usr/local/some/path/gchat_notif.conf
```
* Finally restart the GoCD server so that the plugin is loaded and can send build notifications to [Google Chat](https://chat.google.com)
