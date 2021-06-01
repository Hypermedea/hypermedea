
+!testUnit : true <-
    .wait(10000);
	.print("Test Assertion : Unit inferred register test");
    !checkBelief;
	.

+!checkBelief : system("<http://www.semanticweb.org/noesaffaf/ontologies/2021/4/untitled-ontology-2#ExampleSensor>") <-
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
