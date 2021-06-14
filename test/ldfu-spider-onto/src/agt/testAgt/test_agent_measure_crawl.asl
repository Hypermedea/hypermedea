
+!create_artifact_ldfu_evaluation(INFERRED_BOOL) : true <-
     .my_name(NAME);
     .concat("ldfu_artifact_",NAME, NAME_ART);
     makeArtifact(NAME_ART,"hypermedia.LinkedDataFuSpiderEvaluation",["get.n3",INFERRED_BOOL],ART_ID);
     focus(ART_ID);
     .

+!testUnit : true <-
    !create_artifact_ldfu(false);
    .wait(10000);
	.print("Test : Unit measure crawl test");
	for (.range(I,1,1)){
	    for (entryPointCrawl(IRI)){
            crawl(IRI, TIME);
            .print("crawl exec time : ",TIME);
        };
	}
	displayTimeExecution(TOTALTIME);
	.print("crawl exec Total time : ",TOTALTIME);
	.


{ include("ldfu_agent.asl") }

