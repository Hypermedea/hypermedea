+!start :
    true
    <-
    !tryQuery .

+!tryQuery :
    true
    <-
    ?(rdf(S, P, O) & rdf(S, Q, O) & P \== Q) .

-!tryQuery[code(Query)] :
    true
    <-
    .print(Query) ;
    submitQuery(Query)[artifact_name(wd)] .