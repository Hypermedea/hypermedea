// note: the BOT ontology (https://w3id.org/bot#) includes DL-unsafe axioms.
knownVocabulary("bot.ttl") .

+!start :
    true
    <-
    // create ldfu spider
    makeArtifact(spider, "org.hypermedea.LinkedDataFuSpider", ["crawl.n3", true], ArtId) ;
    focus(ArtId) ;
    // register OWL vocabularies/ontologies for idiomatic programming
    for (knownVocabulary(Vocab)) { register(Vocab) } ;
    // crawl the building's topology according to a predefined program
    crawl("https://territoire.emse.fr/kg/emse/fayol/index.ttl") ;
    !countTriples ;
    !countZones .

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