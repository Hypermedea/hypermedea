/**
 * @author Noé SAFFAF
 */

entryPointGet("https://www.wikidata.org/wiki/Q515").
//entryPointGet("https://www.w3.org/ns/sosa/").

!testUnit.

+!create_artifact_ldfu_evaluation(INFERRED_BOOL) : true <-
     .my_name(NAME);
     .concat("ldfu_artifact_",NAME, NAME_ART);
     makeArtifact(NAME_ART,"hypermedia.LinkedDataFuSpiderEvaluation",["get.n3",INFERRED_BOOL],ART_ID);
     focus(ART_ID);
     .

+!testUnit : true <-
    !create_artifact_ldfu_evaluation(false);
    .wait(1000);
	.print("Test : Unit measure crawl test");
	for (.range(I,1,1)){
	    for (entryPointGet(IRI))){
            get(IRI,LOCAL,false,TIME);
            .print("get exec time : ",TIME);
        };
	}
	displayTimeExecution(TOTALTIME);
	.print("crawl get Total time : ",TOTALTIME);
	.


{ include("ldfu_agent.asl") }

