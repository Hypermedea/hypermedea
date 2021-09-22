/**
 * @author Noé SAFFAF
 */

entryPointRegister("https://www.w3.org/ns/sosa/").
entryPointRegister("https://www.w3.org/ns/ssn/").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(false);
    !registerPlan;
    .wait(1000);
	.print("Test Assertion : Unit merged register test");
	.count(class(_), C1);
	.count(objectProperty(_), C2);
	.count(dataProperty(_), C3);

	if (C1>0 & C2>0 & C3>0 & class("http://www.w3.org/ns/sosa/Sensor")) {
		.print("Test merged register : Passed");
	} else {
		.print("Test merged register : Failed, unexepected/missing beliefs");
	};
	.

{ include("ldfu_agent.asl") }
