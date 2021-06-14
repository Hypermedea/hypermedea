
+!testUnit : true <-
    !create_artifact_ldfu(false);
    !getPlan;
    .wait(10000);
	.print("Test Assertion : Unit get test");
    .

{ include("ldfu_agent.asl") }

