
+!testUnit : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    !crawlPlan;
    .wait(10000);
	.print("Test Assertion : Unit crawl with register test");
	.count(rdf(_, _, _), C1) ;
	.count(isHostedBy(_, _), C2);
	.count(sensor( _), C3);
	if (C1>0 & C2>0 & C3>0) {
		.print("Test simple crawl : Passed")
	} else {
		.print("Test simple crawl : Failed, at least one of rdf/sensor/isHostedBy belief is missing")
	}
	.


{ include("ldfu_agent.asl") }

