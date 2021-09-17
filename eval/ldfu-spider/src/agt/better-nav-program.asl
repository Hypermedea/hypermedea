hasPart(vocabularySpace, "http://www.w3.org/ns/sosa/") .
hasPart(vocabularySpace, "http://www.w3.org/ns/ssn/") .
hasPart(vocabularySpace, "https://w3id.org/bot#") .
hasPart(vocabularySpace, "http://www.w3.org/2003/01/geo/wgs84_pos#") .
hasPart(vocabularySpace, "https://www.w3.org/2019/wot/td#") .
hasPart(vocabularySpace, "https://ci.mines-stetienne.fr/kg/ontology#") .

+rdf(S, P, Target)[rdf_type_map(_, _, uri), crawler_source(Anchor)] :
    crawling
    <-
    if (hasPart(vocabularySpace, Vocab) & .substring(Vocab, P, 0)) {
        getParentURI(Target, Targetp) ;
        +barrier_resource(Anchor, Targetp)
    }
  .

+rdf("https://ci.mines-stetienne.fr/kg/", "http://www.w3.org/2000/01/rdf-schema#seeAlso", Target) :
    crawling
    <-
    getParentURI(Target, Targetp) ;
    +barrier_resource("https://ci.mines-stetienne.fr/kg/", Targetp) ;
  .

+!crawl(URI) :
    true
    <-
    +crawling ;
    get(URI) ;
    for (hasPart(vocabularySpace, Vocab)) { get(Vocab) } ;
  .

+visited(URI) :
    crawling
    <-
    //.print("Retrieved representation of ", URI) ;
    !expandCrawl(URI) ;
  .

+!expandCrawl(Anchor) :
    crawling
    <-
    //.print("Expanding crawl") ;
    for (barrier_resource(Anchor, URI)) {
        getParentURI(URI, URIp) ;
        if (not visited(URIp) | to_visit(URIp)) { get(URIp) }
    }
    !!checkEndCrawl ;
  .

+!checkEndCrawl :
    crawling
    <-
    //.print("Checking end crawl") ;
    if (crawler_status(false) & not .intend(expandCrawl(_))) { !endCrawl }
  .

+!endCrawl :
    crawling
    <-
    -crawling ;
    //.print("End crawling...") ;
    +ended(evaluateCrawlGoal) ;
  .