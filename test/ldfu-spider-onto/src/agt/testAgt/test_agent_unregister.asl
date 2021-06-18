/**
 * @author Noé SAFFAF
 */

entryPointRegister("https://www.w3.org/ns/sosa/").
entryPointRegister("https://www.w3.org/ns/ssn/").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    !unregisterPlan("https://www.w3.org/ns/ssn/");
    .wait(1000);
	.print("Test Assertion : Unit merged register test");
	.count(class(_), C1);
	.count(annotationProperty(_), C2);
	.count(domainIncludes(_,_), C3);


    +isNotPresent(true);
	.puts("<http://www.w3.org/ns/ssn/Stimulus>",ST);
	for (class(S)){
	    if (S = ST){
	        -+isNotPresent(false);
	    }
	}

	if (C1>0 & C2>0 & C3>0 & class("http://www.w3.org/ns/sosa/Sensor")) {
	    if (isNotPresent(X) & X) {
	        .print("Test unregister : Passed");
	    } else {
	        .print("Test unregister : Failed, the belief Stimulus should have been removed");
	    }
	} else {
		.print("Test unregister : Failed, unexepected/missing beliefs");
	};
	.

{ include("ldfu_agent.asl") }
