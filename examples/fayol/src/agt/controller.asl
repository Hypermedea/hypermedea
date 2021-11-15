knownVocab("https://w3id.org/bot#") .

+!start :
    true
    <-
    // create Linked Data artifact
    makeArtifact(ld, "org.hypermedea.LinkedDataArtifact", [], LDArtId) ;
    focus(LDArtId) ;
    // create ontology artifact
    makeArtifact(owl, "org.hypermedea.OntologyArtifact", [true], OWLArtId) ;
    focus(OWLArtId) ;
    // crawl the building's topology
    !crawl("https://territoire.emse.fr/kg/emse/fayol/index.ttl") .

+!crawl(URI) :
    true
    <-
    for (knownVocab(Vocab)) {
        .print("Retrieving OWL definitions of ", Vocab) ;
        get(Vocab) ;
        // synchronous call (wait for action's end)
        .wait({ +visited(_) }) ;
    }
    +crawling ;
    get(URI) ;
  .

+rdf(S, "https://w3id.org/bot#hasSpace", O)[rdf_type_map(_, _, uri), crawler_source(Anchor)] :
    crawling
    <-
    getParentURI(O, Target) ;
    +barrier_resource(Anchor, Target)
  .

+rdf(S, "https://w3id.org/bot#hasStorey", O)[rdf_type_map(_, _, uri), crawler_source(Anchor)] :
    crawling
    <-
    getParentURI(O, Target) ;
    // append index.ttl at the end of the resource URI (until redirection is fixed)
    .concat(Target, "index.ttl", TargetIndex) ;
    +barrier_resource(Anchor, TargetIndex) ;
  .

+visited(URI) :
    crawling
    <-
    .print("Retrieved representation of ", URI) ;
    !expandCrawl(URI) ;
  .

+!expandCrawl(Anchor) :
    crawling
    <-
    for (barrier_resource(Anchor, URI)) {
        getParentURI(URI, URIp) ;
        if (not visited(URIp) | to_visit(URIp)) { get(URIp) }
    }
    !!checkEndCrawl ;
  .

+!checkEndCrawl :
    crawling
    <-
    if (crawler_status(false) & not .intend(expandCrawl(_))) { !endCrawl }
  .

+!endCrawl :
    crawling
    <-
    -crawling ;
    .print("End crawling...") ;
    !countTriples ;
    !countZones ;
  .

+!countTriples :
    true
    <-
    // all crawled triples are exposed to the agent as rdf/3 terms
    .count(rdf(S, P, O), Count) ;
    .print("found ", Count, " triples in the KG.") .

+!countZones :
    true
    <-
    // zone/1 is a unary predicate generated after vocabulary registration
    .count(zone(Zone), Count) ;
    .print("found ", Count, " zones in Espace Fauriel.") .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }