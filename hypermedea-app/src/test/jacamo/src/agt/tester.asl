test(testRegisterGet) .

+!doBefore(ArtId)
    <-
    makeArtifact("test", "org.hypermedea.LinkedDataArtifact", [], ArtId) ;
    focus(ArtId) .

+!doAfter(ArtId)
    <-
    disposeArtifact(ArtId) .

+!testRegisterGet
    <-
    get("http://www.w3.org/ns/sosa/") ;
    get("ttl/instances_sosa.ttl") ;
    .wait({ +platform(_) }, 2000) .

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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