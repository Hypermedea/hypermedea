
+!testUnit : true <-
    !create_artifact_ldfu(false);
    !crawlPlan;
    .wait(10000);
	.print("Test Assertion : Unit simple crawl test");
	.count(rdf(_, _, _), Count) ;
	if (Count>0) {
		.print("Test simple crawl : Passed")
	} else {
		.print("Test simple crawl : Failed, No rdf belief was added")
	}
	.


{ include("ldfu_agent.asl") }

