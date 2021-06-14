
/*
This example illustrate how to create a simple agent that create its own ldfu artifact in its workspace,
the ldfu artifact allows to create triples from a graph and add observable properties (beliefs to the agent)
into the belief base.

The "create_artifact_ldfu" plan creates an artifact to the workspace of the agent. If you want to create an artifact,
in a shared workspace for multiple agents, you can specify it instead in the .jcm file.
)
*/

entryPointRegister("https://www.w3.org/ns/sosa/").
entryPointRegister("https://www.w3.org/ns/ssn/").

!start.

+!start : true <-
    !create_artifact_ldfu(false);
    !registerPlan;
    .print("Hello from multiple_register");
    .



{ include("ldfu_agent.asl") }

