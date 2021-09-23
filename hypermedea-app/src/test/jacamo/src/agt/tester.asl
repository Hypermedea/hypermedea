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
    register("http://www.w3.org/ns/sosa/") ;
    get("ttl/instances_sosa.ttl") ;
    .wait(1000) ;
    ?platform(_) .

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