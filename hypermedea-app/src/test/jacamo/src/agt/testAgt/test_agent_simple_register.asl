/**
 * @author Noé SAFFAF
 */

entryPointRegister("http://www.w3.org/ns/sosa/").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(false);
    !registerPlan;
    .wait(1000);
	.print("Test Assertion : Unit simple register test");
	.count(class(_), C1);
	.count(objectProperty(_), C2);
	.count(dataProperty(_), C3);

	if (C1>0 & C2>0 & C3>0 & class("http://www.w3.org/ns/sosa/Sensor")) {
		.print("Test simple register : Passed");
	} else {
		.print("Test simple register : Failed, unexpected/missing beliefs");
	}
	.

{ include("ldfu_agent.asl") }