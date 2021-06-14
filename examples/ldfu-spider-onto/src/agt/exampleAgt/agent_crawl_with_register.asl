
/*
This example illustrate how to create an agent that first registers an ontology, then add (locally) an rdf
file to call the crawl method. If the recovered triple uses vocabulary that has been registered, it adds the
corresponding unary/binary beliefs in the belief base.

For example, vocabulary for classes as well Object an DataProperties is added through the register ontologies:

Example with https://www.w3.org/ns/sosa/

Registered Owl file                                           Observable Properties in Jacamo
sosa:Sensor a rdfs:Class , owl:Class ;                  =>    class("sosa:Sensor")
sosa:Platform a rdfs:Class , owl:Class ;                =>    class("sosa:Platform")
sosa:isHostedBy a owl:ObjectProperty ;                  =>    objectProperty("sosa:isHostedBy")


And crawl operation adds individual and properties as triples as well as unary and binary predicates (obs properties)
if the class and the properties are defined in the registered ontologies.


Rdf Graph file for crawl                                      Observable Properties in Jacamo
ex:ExampleSensor a sosa:Sensor ;                        =>    rdf("http://www.myexample.org/ExampleSensor","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/ns/sosa/Sensor")
                                                              sensor("ex:ExampleSensor")[predicate_uri("https://www.w3.org/ns/sosa/Sensor")]

ex:ExamplePlatform a sosaPlatform ;                     =>    rdf("http://www.myexample.org/ExamplePlatform","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/ns/sosa/Platform")
                                                              platform("ex:ExamplePlatform")[predicate_uri("https://www.w3.org/ns/sosa/Platform")]


ex:ExampleSensor sosa:isHostedBy ex:ExamplePlateform    =>    rdf("http://www.myexample.org/ExampleSensor","http://www.w3.org/ns/sosa/isHostedBy","http://www.myexample.org/ExamplePlateform")
                                                              isHostedBy("ex:ExampleSensor","ex:ExamplePlateform")[predicate_uri("https://www.w3.org/ns/sosa/isHostedBy")]


If we also consider inferred Axioms, we also generate inferred observable properties :

(There is no vocabulary named "System" in sosa, so this is a simple illustrative case)

Registered Owl file                                           Observable Properties in Jacamo
sosa:Sensor rdfs:subClassOf sosa:System ;               =>    subClassOf("sosa:Sensor","sosa:System)

Rdf Graph file for crawl                                      Observable Properties in Jacamo
ex:ExampleSensor a sosa:Sensor ;                        =>    rdf("http://www.myexample.org/ExampleSensor","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/ns/sosa/Sensor")
                                                              sensor("ex:ExampleSensor")[predicate_uri("https://www.w3.org/ns/sosa/Sensor")]
                                                              system("ex:ExampleSensor")[predicate_uri("https://www.w3.org/ns/sosa/System")]



*/

entryPointCrawl("ttl/instances_sosa.ttl").
entryPointRegister("https://www.w3.org/ns/sosa/").

!start.

+!start : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    !crawlPlan;
    .print("Hello from crawl_with_register");
    .



{ include("ldfu_agent.asl") }

