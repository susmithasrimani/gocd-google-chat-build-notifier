# gocd-google-chat-build-notifier

[Google Chat](https://chat.google.com) based GoCD build notifier

To use this plugin, download the jar from the releases page

Setup and configuration
* Place the plugin jar in GoCD server external plugins directory `$GOCD_SERVER_DIRECTORY/plugins/external/`
* Create a config file for example `gchat_notif.conf` like this:
```
webhookUrl: "https://chat.googleapis.com/v1/spaces/ABCDEF/messages?key=abcdefgh&token=abcdefgh"
```
* In the shell that runs GoCD server define an environment variable `GCHAT_NOTIFIER_CONF_PATH` and set it's value to the path of `gchat_notif.conf`, for example
```
export GCHAT_NOTIFIER_CONF_PATH="/usr/local/some/path/gchat_notif.conf"
```
Or you can set environment variables by checking [this doc](https://docs.gocd.org/current/installation/install/server/linux.html#overriding-default-startup-arguments-and-environment) in `wrapper-config/wrapper-properties.conf`, for example
```
...
set.GCHAT_NOTIFIER_CONF_PATH=/usr/local/some/path/gchat_notif.conf
...
```
* Finally restart the GoCD server so that the plugin is loaded and can send build notifications to [Google Chat](https://chat.google.com)
