/**
 * @author Noé SAFFAF
 */

entryPointRegister("https://www.w3.org/ns/sosa/",false,"ont1").
entryPointCrawl("https://www.w3.org/ns/sosa/",false,"rdf1").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu;
    !registerPlan(false);
    !crawlPlan(false);
	.


{ include("ldfu_agent.asl") }

