# WoT Thing

In this example, the `consumer` agent controls a light bulb
described by a Web of Things (WoT) Thing Description.

The light bulb is simulated with a Node.js script, to run first:
```shell
node src/js/thing.js
```

The agent can then be executed with the Hypermedea executable:
```shell
<HYPERMEDEA_FOLDER>/bin/hypermedea thing.jcm
```
or with Gradle (v8.2.1 or higher):
```shell
gradle run
```