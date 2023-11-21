![Hypermedia Programming Framework](img/banner.png)

Website: https://hypermedea.github.io/

# Hypermedea

Medea is known in Greek mythology as Jason's wife.
Hypermedea is a companion framework to Jason, CArtAgO and Moise (known collectively as [JaCaMo](http://jacamo.sourceforge.net/)).
Hypermedea is designed for programming hypermedia multi-agent systems.

## Getting started

### Application

To get started, download the latest Hypermedea [release](https://github.com/Hypermedea/hypermedea/releases) or generate it from sources.

To generate a release from source, run `./gradlew install` in the root directory.
The shell script to run Hypermedea is then created under `hypermedea-jacamo/build/install/hypermedea-jacamo/bin/`.

To run example agents, go to the corresponding folder under `examples`.

### Library

You can also add Hypermedea as a Gradle dependency in an existing `build.gradle` configuration.
Examples of Gradle configurations are provided in the examples.

## Documentation

For a documentation of operations made available to agents, refer to the [Javadoc of the latest release](https://hypermedea.github.io/javadoc/latest).

For documentation of the latest code base, run `./gradlew javadoc` and browse the generated
Javadoc starting from `hypermedea-lib/build/docs/javadoc/index.html`.
