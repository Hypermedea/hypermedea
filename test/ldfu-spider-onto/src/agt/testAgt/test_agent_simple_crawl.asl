/**
 * @author Noé SAFFAF
 */

entryPointCrawl("https://www.w3.org/ns/sosa/").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(false);
    !crawlPlan;
    .wait(1000);
	.print("Test Assertion : Unit simple crawl test");
	.count(rdf(_, _, _), Count) ;
	if (Count>0) {
		.print("Test simple crawl : Passed")
	} else {
		.print("Test simple crawl : Failed, No rdf belief was added")
	}
	.


{ include("ldfu_agent.asl") }

