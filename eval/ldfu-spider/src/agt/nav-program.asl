hasPart(vocabularySpace, "http://www.w3.org/ns/sosa/") .
hasPart(vocabularySpace, "http://www.w3.org/ns/ssn/") .
hasPart(vocabularySpace, "https://w3id.org/bot#") .
hasPart(vocabularySpace, "http://www.w3.org/2003/01/geo/wgs84_pos#") .
hasPart(vocabularySpace, "https://www.w3.org/2019/wot/td#") .
hasPart(vocabularySpace, "https://ci.mines-stetienne.fr/kg/ontology#") .

barrier_resource(Target) :-
    rdf(Anchor, Rel, Target)[rdf_type_map(_, _, uri)] &
    hasPart(vocabularySpace, Vocab) & .substring(Vocab, Rel, 0) &
    not (toRequest(Target) | requested(Target))
  .

barrier_resource(Target) :-
    rdf("https://ci.mines-stetienne.fr/kg/", "http://www.w3.org/2000/01/rdf-schema#seeAlso", Target) &
    not (toRequest(Target) | requested(Target))
  .

+!crawl(URI) :
    true
    <-
    +crawling ;
    +toRequest(URI) ;
    //for (hasPart(vocabularySpace, Vocab)) { +toRequest(Vocab) } ;
  .

+toRequest(URI) :
    true
    <-
    get(URI);
  .

+resource(URI) :
    crawling
    <-
    -toRequest(URI) ;
    // TODO requested/1 redundant with resource/1 ?
    +requested(URI) ;
    !!proceed ;
  .

+!proceed :
    true
    <-
    .findall(URI, barrier_resource(URI), URIs) ;
    if (not .empty(URIs)) { !expandCrawl(URIs) }
    else {
        ?crawler_status(IsActive) ;
        if (not IsActive) { !endCrawl }
        // else: wait for requests to end
    }
  .

+!expandCrawl(URIs) :
    crawling
    <-
    for (.member(URI, URIs)) { +toRequest(URI) }
  .

+!endCrawl :
    crawling
    <-
    -crawling ;
    +ended(evaluateCrawlGoal) ;
  .