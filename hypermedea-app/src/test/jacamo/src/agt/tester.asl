timeout(5000) .

test(testABox) .
test(testABoxAndTBox) .
test(testConsistentTBox) .
test(testInconsistentTBox) .
test(testABoxAndTBoxWithInference) .

+!awaitEndCrawl : timeout(T)
    <-
    if (not crawler_status(false)) { .wait({ +crawler_status(false) }, T) } .

+!testABox : timeout(T)
    <-
    get("ttl/test-abox.ttl") ;
    !awaitEndCrawl ;
    .count(rdf(S, P, O), L) ;
    ?(L = 5) .

+!testABoxAndTBox : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndCrawl ;
    get("ttl/test-abox.ttl") ;
    !awaitEndCrawl ;
    ?class(_) .

+!testConsistentTBox : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndCrawl ;
    ?(not kb_inconsistent) .

+!testInconsistentTBox : timeout(T)
    <-
    get("ttl/test-unsat-tbox.ttl") ;
    !awaitEndCrawl ;
    get("ttl/test-abox.ttl") ;
    !awaitEndCrawl ;
    ?kb_inconsistent .

+!testABoxAndTBoxWithInference : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndCrawl ;
    get("ttl/test-abox.ttl") ;
    !awaitEndCrawl ;
    ?upper_class(_) .

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