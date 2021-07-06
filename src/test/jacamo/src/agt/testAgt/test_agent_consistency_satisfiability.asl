/**
 * @author Noé SAFFAF
 */

entryPointRegister("ttl/example_ontology.ttl").

!testUnit.

+!testUnit : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    .wait(1000);
	.print("Test Assertion : Unit Consistency test");
    isConsistent(C);
    if(C){
        .print("Test Consistency : Passed");
    } else {
        .print("Test Satisfiable and Consistency : Failed");
    }
    .

{ include("ldfu_agent.asl") }

