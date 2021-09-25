timeout(50000) .

test(testABox) .
test(testABoxAndTBox) .
test(testConsistentTBox) .
test(testInconsistentTBox) .
//test(testABoxAndTBoxWithInference) .

+!awaitEndCrawl : timeout(T)
    <-
    if (not crawler_status(false)) { .wait({ +crawler_status(false) }, T) } .

+!testABox : timeout(T)
    <-
    get("ttl/instances_sosa.ttl") ;
    !awaitEndCrawl ;
    .count(rdf(S, P, O), L) ;
    ?(L = 7) .

+!testABoxAndTBox : timeout(T)
    <-
    get("ttl/sosa.ttl") ;
    !awaitEndCrawl ;
    get("ttl/instances_sosa.ttl") ;
    !awaitEndCrawl ;
    ?platform(_) .

+!testConsistentTBox : timeout(T)
    <-
    get("ttl/example-ontology.ttl") ;
    !awaitEndCrawl ;
    ?(not kb_inconsistent) .

+!testInconsistentTBox : timeout(T)
    <-
    get("ttl/unsatisfiable_ontology.ttl") ;
    get("ttl/instances_sosa.ttl") ;
    !awaitEndCrawl ;
    ?kb_inconsistent .

+!testABoxAndTBoxWithInference : timeout(T)
    <-
    get("ttl/sosa.ttl") ;
    get("ttl/instances_sosa.ttl") ;
    !awaitEndCrawl ;
    ?system(_) .

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

+!doBefore(ArtId)
    <-
    makeArtifact("test", "org.hypermedea.LinkedDataArtifact", [true], ArtId) ;
    focus(ArtId) .

+!doAfter(ArtId)
    <-
    disposeArtifact(ArtId) .

+!test
    <-
    for (test(Test)) {
        !doBefore(ArtId) ;
        !Test ;
        !doAfter(ArtId) ;
        .print(Test, ": passed.") ;
    } .

-!Test : test(Test)
    <-
    .print(Test, ": failed.") .