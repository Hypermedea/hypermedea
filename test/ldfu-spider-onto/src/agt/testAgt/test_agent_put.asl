/**
 * @author Noé SAFFAF
 */

endPointPut("http://localhost:8083/").
entryPointCrawl("http://localhost:8083/").

!testUnit.

+!create_object_and_put : entryPointPut(IRI) <-
    .union(["rdf(http://www.w3.org/ns/sosa/ExampleSensor1,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            ["rdf(http://www.w3.org/ns/sosa/ExampleSensor2,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            OBJECT);
    !putPlan(IRI,OBJECT);
    .


+!testUnit : true <-
    !create_artifact_ldfu(false);
    !create_object_and_put;
    !crawlPlan;
    .wait(1000);
	.print("Test Assertion : Unit put test");
    .

{ include("ldfu_agent.asl") }

