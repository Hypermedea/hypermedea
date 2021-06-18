/**
 * @author Noé SAFFAF
 */

entryPointGet("https://www.w3.org/ns/sosa/",false).
//entryPointGet("ttl/sosa.ttl",true).

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(false);
    !getPlan;
    .wait(1000);
	.print("Test Assertion : Unit get test");
    .

{ include("ldfu_agent.asl") }

