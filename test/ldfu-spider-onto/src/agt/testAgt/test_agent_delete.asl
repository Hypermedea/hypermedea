/**
 * @author Noé SAFFAF
 */

endPointPut("http://localhost:8083/").
endPointDelete(http://localhost:8083/").
entryPointCrawl("http://localhost:8083/").

!testUnit.

+!create_object_and_delete(IRI) : true <-
    .union(["rdf(http://www.w3.org/ns/sosa/ExampleSensor1,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            ["rdf(http://www.w3.org/ns/sosa/ExampleSensor2,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,http://www.w3.org/ns/sosa/Sensor)"],
            OBJECT);
    !putPlan(IRI,OBJECT);
    !deletePlan(IRI,OBJECT);
    .


+!testUnit : true <-
    !create_artifact_ldfu(false);
    !create_object_and_delete("https://www.w3.org/ns/sosa/")
    .wait(1000);
	.print("Test Assertion : Unit post test");
    .

{ include("ldfu_agent.asl") }

