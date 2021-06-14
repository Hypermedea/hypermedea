This project aims to create an linked data fu spider artifact usable by MAS agents written in Jacamo that can register ontologies and execute linked data-fu programs to retrieve queries results from integrated data.

A simple use of this artifact is to register owl ontologies, which adds in the belief base of an agent focussed on the artifact observable properties unary binary predicates of A_Box and T_Box axioms, and then the exernal action crawl adds to the belief base all triplets retrieved by the linked data-fu program as well as all unary/binary predicates for properties/classes that has been previously registered.

A base ldfu_agent written in jacamo with plans for using the external actions of the artifact as well as example agents that describe simple uses of the artifact can be found in the project (src/agt).

### To Start

IMPORTANT : Use the build.gradle file in the project repository (not in the example resository), and run the "runLdfuSpider" task.

This will execute the jcm file in /example repository and their agents. It will use the Ldfu-spider artifact from the main project (src).

Run those simple example agent and read their description to understand how works the artifact.


### Main Operations (External actions of the ldfu-spider artifact)

There are currently two main operations in the artifact.

A first operation, named **register**, takes an ontology (owl) file as parameter and add its vocabulary to the artifact as well as creating observable properties in Jacamo (see below for examples).

A second operation, named **crawl**, executes the linked data-fu program over linked datas of the web. It extract an rdf graph and add all triples as observable properties by default. Additionnaly, if the vocabulary for the crawled triples is registered, it also adds unary/binary predicates (Observable properties) corresponding to that triple. 

The ldfu artifact provide functionnality for simple reasoning which can add inferred predicates to the observable properties base (see below for example).


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
                                                                                                                        
