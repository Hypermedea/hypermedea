hasPart(vocabularySpace, "http://www.w3.org/ns/sosa/") .
hasPart(vocabularySpace, "http://www.w3.org/ns/ssn/") .
hasPart(vocabularySpace, "https://w3id.org/bot#") .
hasPart(vocabularySpace, "http://www.w3.org/2003/01/geo/wgs84_pos#") .
hasPart(vocabularySpace, "https://www.w3.org/2019/wot/td#") .
hasPart(vocabularySpace, "https://ci.mines-stetienne.fr/kg/ontology#") .

withoutFragment(URI, URIp) :-
    (.nth(I, URI, "#") & .substring(URIp, URI, 0, I)) |
    (URIp = URI)
  .

barrier_resource(Anchor, Target) :-
    rdf(S, P, O)[rdf_type_map(_, _, uri), crawler_source(Anchor)] &
    hasPart(vocabularySpace, Vocab) & .substring(Vocab, P, 0) &
    withoutFragment(O, Target) &
    not resource(Target)
  .

barrier_resource("https://ci.mines-stetienne.fr/kg/", Target) :-
    rdf("https://ci.mines-stetienne.fr/kg/", "http://www.w3.org/2000/01/rdf-schema#seeAlso", URI) &
    withoutFragment(URI, Target) &
    not resource(Target)
  .

+!crawl(URI) :
    true
    <-
    +crawling ;
    get(URI) ;
    //for (hasPart(vocabularySpace, Vocab)) { get(Vocab) } ;
  .

+resource(URI) :
    crawling
    <-
    .print("Retrieved representation of ", URI) ;
    !expandCrawl(URI) ;
  .

+!expandCrawl(Anchor) :
    crawling
    <-
    for (barrier_resource(Anchor, URI)) { get(URI) } ;
  .

+crawler_status(false) :
    crawling
    <-
    if (not barrier_resource(_, _)) { !endCrawl }
    else {
        ?barrier_resource(Anchor, Target) ;
        .print("still missing link ", Anchor, " -> ", Target)
    }
  .

+!endCrawl :
    crawling
    <-
    -crawling ;
    .print("End crawling...") ;
    +ended(evaluateCrawlGoal) ;
  .