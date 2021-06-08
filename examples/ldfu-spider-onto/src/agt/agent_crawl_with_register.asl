
/*
This example illustrate how to create an agent that first registers an ontology, then add (locally) an rdf
file to call the crawl method. If the recovered triplet uses vocabulary that has been registered, it adds the
corresponding unary/binary beliefs in the belief base

*/

entryPointCrawl("ttl/instances_sosa.ttl",true,"crawlUri1"). //true because we will use a local file, change to false if you add an uri ressource
entryPointRegister("https://www.w3.org/ns/sosa/",false,"registerUri1").

!start.

+!start : true <-
    !create_artifact_ldfu;
    !registerPlan(true);
    !crawlPlan(false);
    .



{ include("ldfu_agent.asl") }

