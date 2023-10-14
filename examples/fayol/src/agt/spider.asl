knownVocab("https://w3id.org/bot") .
//knownVocab("bot.ttl") .

knownResource(URI) :- rdf(_, _, _)[source(URI)] .

+!start :
    true
    <-
    // crawl the building's topology
    !crawl("https://territoire.emse.fr/kg/emse/fayol/index.ttl")
  .

+!crawl(URI) :
    true
    <-
    for (knownVocab(Vocab)) {
        .print("Retrieving OWL definitions of ", Vocab) ;
        get(Vocab) ;
    }
    +crawling ;
    .print("Retrieving ", URI) ;
    get(URI) ;
  .

+rdf(S, "https://w3id.org/bot#hasSpace", O)[rdf_type_map(_, _, uri), source(Anchor)] :
    crawling & h.target(O, Target)
    <-
    !!get(Target) ;
  .

+rdf(S, "https://w3id.org/bot#hasStorey", O)[rdf_type_map(_, _, uri), source(Anchor)] :
    crawling & h.target(O, Target)
    <-
    !!get(Target) ;
  .

+!get(URI) :
    crawling
    <-
    if (not (knownResource(URI) | .intend(get(URI)))) {
        get(URI) ;
        !!checkEndCrawl ;
    }
  .

+!checkEndCrawl :
    crawling
    <-
    if (not .intend(get(_))) { !endCrawl }
  .

+!endCrawl :
    crawling
    <-
    -crawling ;
    .print("End crawling...") ;
    !countTriples ;
    // TODO perform inference
    !countZones ;
  .

+!countTriples :
    true
    <-
    // all crawled triples are exposed to the agent as rdf/3 terms
    .count(rdf(S, P, O), Count) ;
    .print("found ", Count, " triples in the KG.") ;
  .

+!countZones :
    true
    <-
    // zone/1 is a unary predicate generated after vocabulary registration
    .count(zone(Zone), Count) ;
    .print("found ", Count, " zones in Espace Fauriel.") ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }