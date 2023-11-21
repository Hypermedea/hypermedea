hasForm(AffordanceName, Form) :-
    rdf(Aff, "https://www.w3.org/2019/wot/td#name", AffordanceName) &
    rdf(Aff, "https://www.w3.org/2019/wot/td#hasForm", Form)
  .

hasTarget(Form, Target) :-
    rdf(Form, "https://www.w3.org/2019/wot/hypermedia#hasTarget", Target)
  .

hasOperationType(Form, OpType) :-
    rdf(Form, "https://www.w3.org/2019/wot/hypermedia#hasOperationType", OpType)
  .

+!start :
    true
    <-
    get("http://localhost:8080/td") ;
    !toggle ;
  .

+!toggle :
    true
    <-
    !readProperty("status", Status) ;
    .print("light status: ", Status) ;
    !invokeAction("toggle") ;
    !readProperty("status", NewStatus) ;
    .print("light status: ", NewStatus) .

+!readProperty(PropertyName, Val) :
    hasForm(PropertyName, Form) &
    hasTarget(Form, Target) &
    hasOperationType(Form, "https://www.w3.org/2019/wot/td#readProperty")
    <-
    get(Target) ;
    ?(json(Val)[source(Target)]) ;
  .

+!invokeAction(ActionName) :
    hasForm(ActionName, Form) &
    hasTarget(Form, Target) &
    hasOperationType(Form, "https://www.w3.org/2019/wot/td#invokeAction")
    <-
    // TODO POST without payload (payload below not taken into account)
    post(Target, json(null)) ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
