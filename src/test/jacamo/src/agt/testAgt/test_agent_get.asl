origin("ttl/instances_sosa.ttl").

!testUnit.

+!testUnit :
    origin(URI)
    <-
    !create_artifact_ldfu;
    get(URI);
    .wait(1000);
	.print("Test Assertion : GET operation test");
    getIdleState(Idling);
    if(Idling) {
        .print("Test Consistency: Passed");
    } else {
        .print("Test GET: Failed");
    }
    .

{ include("ldfu_agent.asl") }

