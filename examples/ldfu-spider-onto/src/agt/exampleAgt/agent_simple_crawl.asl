
/**
 This example illustrate how to create a simple agent that create its own ldfu artifact in its workspace,
 the ldfu artifact allows to create triples from a graph and add observable properties (beliefs to the agent)
 into the belief base.

 The "create_artifact_ldfu" plan creates an artifact to the workspace of the agent. If you want to create an artifact,
 in a shared workspace for multiple agents, you can specify it instead in the .jcm file. The instanciation of the artifacts
 takes 2 parameters, the program file for linked-data fu (which you shouldn't change), and a boolean to specify if registered
 ontologies as well as added triples should consider inferred axioms/triples. By default, it is set to false (not inferred).

 The "crawlPlan" plan execute the crawl external action of all referenced entrypoints. To add entrypoints, the agent must
 own beliefs in the format
 entryPointCrawl(uri)  // uri of the resource graph, it can be either a real uri (ie : "https://www.wikidata.org/entity/Q2814098"),
                       // or a local path (ie: "ttl/instances_sosa.ttl").

* @author Noé SAFFAF
*/

entryPointCrawl("https://www.wikidata.org/entity/Q2814098").

!start.

+!start : true <-
    !create_artifact_ldfu(false);
    !crawlPlan;
    !count;
    .print("Hello from agent_simple_crawl");
    .



{ include("ldfu_agent.asl") }

