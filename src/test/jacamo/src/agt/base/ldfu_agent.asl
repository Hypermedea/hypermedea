/**
 * @author Noé SAFFAF
 */

+!create_artifact_ldfu(INFERRED_BOOL) : true <-
     .my_name(NAME);
     .concat("ldfu_artifact_",NAME, NAME_ART);
     makeArtifact(NAME_ART,"org.hypermedea.LinkedDataFuSpider",["get.n3",INFERRED_BOOL],ART_ID);
     focus(ART_ID);
     .


+!getPlan : true <-
    for (entryPointGet(IRI,LOCAL)){
        get(IRI,LOCAL);
    };
    .

+!putPlan(IRI, OBJECT) : true <-
    put(IRI, OBJECT);
    .

+!postPlan(IRI, OBJECT) : true <-
    post(IRI, OBJECT);
    .

+!deletePlan(IRI, OBJECT) : true <-
    delete(IRI, OBJECT);
    .

+!registerPlan : true <-
	for (entryPointRegister(IRI)){
       register(IRI);
 	}
	.

+!unregisterPlan(IRI) : true <-
    unregister(IRI);
	.



+!crawlPlan : true <-
	for (entryPointCrawl(IRI)){
    	crawl(IRI);
	}
	.


+!printRdf: true <-
	for (rdf(O1,O2,O3)){
		.print("rdf(",O1,", ",O2,", ",O3,")");
	}
	.

+!count :true <-
    .count(rdf(_, _, _), Count) ;
    .print("found ", Count, " triples.");
  	.

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }