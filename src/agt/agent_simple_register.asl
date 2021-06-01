
/*
This example illustrate how to create a simple agent that create its own ldfu artifact in its workspace,
the ldfu artifact allows to create triplets from a graph and add observable properties (beliefs to the agent)
into the belief base.

The "create_artifact_ldfu" plan creates an artifact to the workspace of the agent. If you want to create an artifact,
in a shared workspace for multiple agents, you can specify it instead in the .jcm file.

The "registerPlan" plan execute the register external action of all referenced entrypoints. To add entrypoints, the agent must
own belief in the format
entryPointRegister(url,  // uri of the ontology/ or path to the ontology file
                   bool, // A bool to specify if its a local file (true = local directory, false = search the ontology at the uri)
                   key)  // A string for a key
It returns all unary/binary predicates from an ontology.

registerPlan takes two parameters, one to name the merged ontology (just put whatever you want, not used atm), the second is
to specify if we consider inferred axioms (true if the case, false otherwise)
*/

entryPointRegister("https://www.w3.org/ns/sosa/",false,"ont1").


!start.

+!start : true <-
    !create_artifact_ldfu;
    !registerPlan("simple_register",false);
    .print("Hello World");
    .



{ include("ldfu_agent.asl") }

