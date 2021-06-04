

+!create_artifact_ldfu : true <-
     .my_name(NAME);
     .concat("ldfu_artifact_",NAME, NAME_ART);
     makeArtifact(NAME_ART,"hypermedia.LinkedDataFuSpider",["get.n3"],ART_ID);
     focus(ART_ID);
     .

+!registerPlan(INFERRED_BOOL) : true <-
    .wait(1000);
	for (entryPointRegister(IRI, LOCAL_ONTOLOGY_BOOL,KEY)){
       	addIRIPendingRegister(IRI,LOCAL_ONTOLOGY_BOOL,KEY);
 	}
    register(INFERRED_BOOL);
	.

+!unregisterPlan(KEY) : true <-
    .wait(1000);
    unregisterIRIbyKey(KEY);
	.



+!crawlPlan(INFERRED_BOOL) : true <-
    .wait(1000);
	for (entryPointCrawl(IRI, LOCAL_ONTOLOGY_BOOL, KEY)){
		// crawl RDF triples according to the LD program registered by ldfu_artifact (get.n3)
    	crawl(IRI, LOCAL_ONTOLOGY_BOOL, INFERRED_BOOL);
    	// count triples and display result
    	!count;
    	//.wait(1000);
    	//!printRdf;
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