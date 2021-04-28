+!start :
    entryPoint(URI)
    <-
    // crawl RDF triples according to the LD program registered by ldfu_artifact (get.n3)
    crawl(URI) ;
    // count triples and display result
    !count ;
  .

+!count :
    true
    <-
    .count(rdf(_, _, _), Count) ;
    .print("found ", Count, " triples.");
  .
