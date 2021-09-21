/**
 * Evaluation criteria:
 *  - native ldfu program (using crawl/1) vs. Jason program (using get/1)
 *  - with rdf/3 vs. with derived predicates
 *  - native ldfu reasoning vs. Prolog reasoning vs. OWL reasoning
 */
task(evaluateCrawlGoal) .

iterations(3) .

entryPoint("https://ci.mines-stetienne.fr/kg/") .

+!evaluate :
    task(Task)
    <-
    !!start(Task)
  .

+!start(Task) :
    iterations(I) & I > 0
    <-
    !setUp("spider") ;
    !t(T); +startedAt(Task, T) ;
    !Task ;
  .

+!start(Task) :
    iterations(0)
    <-
    .print("done.") ;
  .

+ended(Task) :
    true
    <-
    !t(T); +endedAt(Task, T) ;
    -ended(Task) ;
    !showResult(Task) ;
    !clean("spider") ;
    !inc ;
    !!start(Task) ;
  .

+!inc :
    iterations(I)
    <-
    -iterations(I) ; +iterations(I - 1) ;
  .

/////////////////////////////////////////////////////////////// evaluation tasks

+!evaluateCrawlOp :
    entryPoint(URI)
    <-
    crawl(URI);
    +ended(evaluateCrawlOp) ;
  .

+!evaluateCrawlGoal :
    entryPoint(URI)
    <-
    !crawl(URI) ;
    // +ended called after all resources are processed
  .

////////////////////////////////////////////////////////////////////// utilities

+!setUp(Name) :
    true
    <-
    makeArtifact(Name, "org.hypermedea.LinkedDataFuSpider", ["crawl.n3"], ArtId) ;
    focus(ArtId) ;
  .

+!clean(Name) :
    true
    <-
    lookupArtifact(Name, ArtId) ;
    disposeArtifact(ArtId) ;
  .

+!t(T) :
    true
    <-
    .time(H, M, S, MS) ;
    T = MS + (S + (M + H * 60) * 60) * 1000;
  .

+!count(Nb) :
    true
    <-
    .count(rdf(S, P, O), Nb)
  .

+!showResult(Task) :
    startedAt(Task, T1) & endedAt(Task, T2)
    <-
    .count(rdf(S, P, O), Nb) ;
    .print(Nb, ",", T2 - T1) ;
    -startedAt(Task, T1) ; -endedAt(Task, T2) ;
  .

{ include("nav-program.asl") }
//{ include("better-nav-program.asl") }

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }