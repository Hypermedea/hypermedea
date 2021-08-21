hasPart(vocabularySpace, "http://www.w3.org/ns/sosa/") .
hasPart(vocabularySpace, "http://www.w3.org/ns/ssn/") .
hasPart(vocabularySpace, "https://w3id.org/bot#") .
hasPart(vocabularySpace, "http://www.w3.org/2003/01/geo/wgs84_pos#") .
hasPart(vocabularySpace, "https://www.w3.org/2019/wot/td#") .
hasPart(vocabularySpace, "https://ci.mines-stetienne.fr/kg/ontology#") .

+!crawl(URI) :
    true
    <-
    +crawling ;
    get(URI) ;
    for (hasPart(vocabularySpace, Vocab)) { get(Vocab) } ;
    !debug ;
  .

+!debug :
    true
    <-
    .wait(5000) ;
    .count(rdf(_, _, _), Nb) ;
    .print("found triples: ", Nb) ;
  .

+rdf(Anchor, Rel, Target) :
    crawling &
    hasPart(vocabularySpace, Vocab) & .substring(Vocab, Rel, 0) &
    not processed(Target)
    <-
    +toProcess(Target) ;
  .

+rdf("https://ci.mines-stetienne.fr/kg/", "http://www.w3.org/2000/01/rdf-schema#seeAlso", Target) :
    crawling
    <-
    +toProcess(Target) ;
  .

+toProcess(URI) :
    true
    <-
    get(URI) ;
    -toProcess(URI) ; +processed(URI) ;
    if (not toProcess(OtherURI)) {
      -crawling ;
      for (processed(OtherURI)) { -processed(OtherURI) }
      +ended(evaluateCrawlGoal) ;
    }
  .