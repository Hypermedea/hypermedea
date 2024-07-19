ua("opc.tcp://10.1.9.1:49321/") .

nodeId("ns=2;s=ITm%20Factory.DX10.Vitesse%20Jog") .
nodeId("ns=2;s=ITm%20Factory.DX10.Pause") .
nodeId("ns=2;s=ITm%20Factory.VL10.Compteur%20pots") .

variable(V)
    :-
    ua(Endpoint) & nodeId(Id) &
    .concat(Endpoint, "?nodeId=", Id, V) .

value(Var, Val)
    :-
    rdf(Var, "http://www.w3.org/1999/02/22-rdf-syntax-ns#value", Val) .

+!observe
    <-
    for (variable(V)) { get(V) } ;
    .wait(1000) ;
    !!observe .

+!start <- !observe .

{ include("$jacamoJar/templates/common-cartago.asl") }
