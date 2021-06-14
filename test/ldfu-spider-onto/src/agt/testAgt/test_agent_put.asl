+!create_object_and_put(IRI) : true <-
    .union(["rdf(http://www.w3.org/ns/sosa/ExampleSensor1,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            ["rdf(http://www.w3.org/ns/sosa/ExampleSensor2,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            OBJECT);
    !putPlan(IRI,OBJECT);
    .


+!testUnit : true <-
    !create_artifact_ldfu(false);
    .wait(10000);
	.print("Test Assertion : Unit put test");
    .

{ include("ldfu_agent.asl") }

