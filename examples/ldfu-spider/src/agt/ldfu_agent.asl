+!start :
    entryPoint(URI)
    <-
    // register SOSA to retrieve OWL class/property definitions and derive unary/binary predicates
    register("http://www.w3.org/ns/sosa/") ;
    // crawl RDF triples according to the LD program registered by ldfu_artifact (get.n3)
    crawl(URI) ;
    // look up resource and print it
    !lookUpRDF ;
    !lookUpDerived ;
  .

+!lookUpRDF :
    rdf(Platform, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/ns/sosa/Platform")
    <-
    .print("found ", Platform, " (with query using the rdf/3 predicate).");
  .

+!lookUpDerived :
    platform(Platform)
    <-
    .print("found ", Platform, " (with query using the platform/1 predicate, derived from the OWL class sosa:Platform).");
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }