![Hypermedia Programming Framework](img/banner.png)

# Hypermedea

Medea is known in Greek mythology as Jason's wife.
Hypermedea is a companion framework to Jason, CArtAgO and Moise (known collectively as [JaCaMo](jacamo.sourceforge.net/)).
Hypermedea is designed for programming hypermedia multi-agent systems.

## Getting started

### Application

To get started, download the latest Hypermedea [release](https://github.com/Hypermedea/hypermedea/releases) or generate it from sources.

To generate a release from source, run `gradle install` in the root directory.
The shell script to run Hypermedea is then created under `hypermedea-app/build/install/hypermedea/bin/`.

To run example agents, go to the corresponding folder under `examples`.

### Library

If you already have JaCaMo installed, you can also add Hypermedea as a JaCaMo library.
To add it to JaCaMo, do the following:
1. run `gradle jacamoJar`
2. go to `hypermedea-lib/build/libs` where you should see `hypermedea-jacamo-lib-<version>.jar`
3. copy the `.jar` file
4. go to your JaCaMo project directory (where your `.jcm` file lies)
5. create a subdirectory `lib` and paste the `.jar` file in it

Your agents should then be able to use Hypermedea artifacts.

## Documentation

Hypermedea includes the following CArtAgO artifacts:
- `LinkedDataArtifact`: Linked Data crawler and reasoner
- `ThingArtifact`: wrapper for WoT 'things'
- `PlannerArtifact`: PDDL planner (to handle WoT affordances, among others)

For a complete documentation, run `gradle javadoc` and browse the generated
Javadoc starting from `hypermedea-lib/build/docs/javadoc/index.html`.
