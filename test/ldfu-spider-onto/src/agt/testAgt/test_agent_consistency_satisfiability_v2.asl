
+!testUnit : true <-
    !create_artifact_ldfu(true);
    !registerPlan;
    .wait(10000);
	.print("Test Assertion : Unit Satisfiable and Consistency test");
    isConsistent(C,REPORT_C);
    isSatisfiable(true, S, REPORT_S);
    .print(REPORT_C);
    .print(REPORT_S);
    if(C & not S){
        .print("Test Satisfiable and Consistency : Passed");
    } else {
        .print("Test Satisfiable and Consistency : Failed");
    }
    .

{ include("ldfu_agent.asl") }

