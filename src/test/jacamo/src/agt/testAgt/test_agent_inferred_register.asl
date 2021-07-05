/**
 * @author Noé SAFFAF
 */

entryPointRegister("ttl/example_ontology.ttl").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    .wait(1000);
	.print("Test Assertion : Unit inferred register test");
    !checkBelief;
	.

+!checkBelief : system(exampleSensor) <-
      .print("Test inferred register : Passed");
      .

-!checkBelief : true <-
      .print("Test inferred register : Failed, the inferred belief in the example has not been added");
      .

{ include("ldfu_agent.asl") }

/*
    .puts("get.n3",S)
     makeArtifact(ArtName, "hypermedia.LinkedDataFuSpider", [S], ArtId);
     focus(ArtId);
     { include("$jasonJar/test/jason/inc/tester_agent.asl") }
       @[test]
*/
