# gocd-google-chat-build-notifier [![Build Status](https://travis-ci.org/susmithasrimani/gocd-google-chat-build-notifier.svg?branch=master)](https://travis-ci.org/susmithasrimani/gocd-google-chat-build-notifier)

[Google Chat](https://chat.google.com) based GoCD build notifier

## Downloading the plugin

To use this plugin, download the plugin jar from the [releases page](https://github.com/susmithasrimani/gocd-google-chat-build-notifier/releases)

## Setup and configuration
* Place the plugin jar in GoCD server external plugins directory `$GOCD_SERVER_DIRECTORY/plugins/external/`
* Create a config file for example `gchat_notif.conf` like this:

```
serverHost: "https://my-gocd-server-url.com"
```

`serverHost` - FQDN of the GoCD Server. All links on the Chat channel will be relative to this host

* In the shell that runs GoCD server define an environment variable `GCHAT_NOTIFIER_CONF_PATH` and set it's value to the path of `gchat_notif.conf`, for example
```
export GCHAT_NOTIFIER_CONF_PATH="/usr/local/some/path/gchat_notif.conf"
```
Or you can set environment variables by checking [this doc](https://docs.gocd.org/current/installation/install/server/linux.html#overriding-default-startup-arguments-and-environment) in `wrapper-config/wrapper-properties.conf`, for example
```
set.GCHAT_NOTIFIER_CONF_PATH=/usr/local/some/path/gchat_notif.conf
```
* Restart the GoCD server so that the plugin is loaded

* Finally, add the Google Chat webhook URL in GoCD UI: `Admin > Plugins > Cogwheel next to Google Chat Build Notifier`. 

* The plugin can send build notifications to [Google Chat](https://chat.google.com)

## Setup and Configuration for GoCD on Kubernetes Using Helm

### Adding the plugin
- In order to add this plugin, you have to use a local values.yaml file that will override the default [values.yaml](https://github.com/helm/charts/blob/master/stable/gocd/values.yaml) present in the official GoCD helm chart repo. 
- Add the .jar file link from the [releases section](https://github.com/susmithasrimani/gocd-google-chat-build-notifier/releases) to the `env.extraEnvVars` section as a new environment variable.
- The environment variable name must have the `GOCD_PLUGIN_INSTALL` prefixed to it.
- Example

```
env:
  extraEnvVars:
    - name: GOCD_PLUGIN_INSTALL_google-chat-notification-plugin
      value: https://github.com/susmithasrimani/gocd-google-chat-build-notifier/releases/download/v0.1.0-alpha/gocd-google-chat-build-notifier-plugin.jar
```
- Make sure to replace the value with the latest release.

### Mounting the config file
- You can mount the config file as a kubernetes secret.
- You first have to create a file that has the config values, for example `gchat_notif.conf`
- Then create the secret using this file in the proper namespace 

```
kubectl create secret generic gchat-config \
--from-file=gchat_notif.conf=gchat_notif.conf \
--namespace=gocd
```

- Then you have to mount the secret to a location.

```
persistence:
  extraVolumes:
    - name: gchat-config
      secret:
        secretName: gchat-config
        defaultMode: 0744

  extraVolumeMounts:
    - name: gchat-notifier
      mountPath: /tmp/gchat
      readOnly: true
```
- And finally add the location of the config file as an environment variable
```
env:
  extraEnvVars:
    - name: GCHAT_NOTIFIER_CONF_PATH
      value: /tmp/gchat/gchat-notif.conf
```

## Contributing

Check [CONTRIBUTING.md](CONTRIBUTING.md) ! 😄
