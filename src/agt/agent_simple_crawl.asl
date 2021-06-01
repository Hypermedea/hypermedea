
/*
This example illustrate how to create a simple agent that create its own ldfu artifact in its workspace,
the ldfu artifact allows to create triplets from a graph and add observable properties (beliefs to the agent)
into the belief base.

The "create_artifact_ldfu" plan creates an artifact to the workspace of the agent. If you want to create an artifact,
in a shared workspace for multiple agents, you can specify it instead in the .jcm file.

The "crawlPlan" plan execute the crawl external action of all referenced entrypoints. To add entrypoints, the agent must
own belief in the format
entryPointCrawl(url,  // uri of the resource graph / or path to
                bool, // A bool to specify if its a local file (true = local directory, false = search the ressource graph at the uri)
                key)  // A string for a key
*/

entryPointCrawl("https://www.wikidata.org/entity/Q2814098",false,"ont1").

!start.

+!start : true <-
    !create_artifact_ldfu;
    !crawlPlan;
    !count;
    .print("Hello World");
    .



{ include("ldfu_agent.asl") }

