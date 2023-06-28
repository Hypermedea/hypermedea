timeout(5000) .

test(testABox) .
test(testABoxAndTBox) .
test(testConsistentTBox) .
test(testInconsistentTBox) .
test(testABoxAndTBoxWithInference) .

+!awaitEndCrawl : timeout(T)
    <-
    if (not crawler_status(false)) { .wait({ +crawler_status(false) }, T) } .

+!awaitEndReasoning : timeout(T)
    <-
    if (not reasoner_status(false)) { .wait({ +reasoner_status(false) }, T) } .

+!awaitEndProcessing <- !awaitEndCrawl ; !awaitEndReasoning .

+!testABox : timeout(T)
    <-
    get("ttl/test-abox.ttl") ;
    !awaitEndProcessing ;
    .count(rdf(S, P, O), L) ;
    ?(L = 5) .

+!testABoxAndTBox : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndProcessing ;
    get("ttl/test-abox.ttl") ;
    !awaitEndProcessing ;
    ?class(_) .

+!testConsistentTBox : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndProcessing ;
    ?(not kb_inconsistent) .

+!testInconsistentTBox : timeout(T)
    <-
    get("ttl/test-unsat-tbox.ttl") ;
    !awaitEndProcessing ;
    get("ttl/test-abox.ttl") ;
    !awaitEndProcessing ;
    ?kb_inconsistent .

+!testABoxAndTBoxWithInference : timeout(T)
    <-
    get("ttl/test-tbox.ttl") ;
    !awaitEndProcessing ;
    get("ttl/test-abox.ttl") ;
    !awaitEndProcessing ;
    ?upper_class(_) .

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

+!doBefore([LDArtId, OWLArtId])
    <-
    makeArtifact("test-ld", "org.hypermedea.NavigationArtifact", [], LDArtId) ;
    focus(LDArtId) ;
    makeArtifact("test-owl", "org.hypermedea.OntologyArtifact", [true], OWLArtId) ;
    focus(OWLArtId) .

+!doAfter(ArtIds)
    <-
    for (.member(ArtId, ArtIds)) { disposeArtifact(ArtId) } .

+!test
    <-
    for (test(Test)) {
        !doBefore(ArtIds) ;
        !Test ;
        !doAfter(ArtIds) ;
        .print(Test, ": passed.") ;
    } .

-!Test : test(Test)
    <-
    .print(Test, ": failed.") .