presenceProperty("mqtt://test.mosquitto.org/PresenceSensor/events/presenceDetected") .

// TODO from Plugfest instead?
coffeeMachineState("http://localhost:8080/smart-coffee-machine/properties/allAvailableResources") .
coffeeMachineAction("http://localhost:8080/smart-coffee-machine/actions/makeDrink{?drinkId,size,quantity}") .

preferredCoffee("espresso") .

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

+!start
    <-
    // !detectPresence ;
    // !monitorCoffeeMachineState ;
    !makeCoffee ;
    // get("td.ttl") ;
    // !toggle ;
  .

+!detectPresence : presenceProperty(P)
    <-
    watch(P) ;
    +watching(P) ;
    // FIXME no notification received... (simulator is down?)
  .

+!monitorCoffeeMachineState : coffeeMachineState(S)
    <-
    if (not watching(S)) { +watching(S) } ;
    get(S) ;
    .wait(5000) ;
    !!monitorCoffeeMachineState ;
  .

+!makeCoffee : coffeeMachineAction(A) & not preferredCoffee(_)
    <-
    h.expand_template(A, [], Ap);
    post(Ap) ;
    .wait({ +json(Out)[source(A)] }) ;
    .print(Out) ;
  .

+!makeCoffee : coffeeMachineAction(A) & preferredCoffee(Type)
    <-
    h.expand_template(A, [kv(drinkId, Type)], Ap);
    post(Ap) ;
    .wait({ +json(Out)[source(A)] }) ;
    .print(Out) ;
  .

+json(Val)[source(P)] : watching(P)
    <-
    .print(Val) ;
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
    // TODO use meta-programming to invoke the right method (Op =.. [Method, ...])?
    post(Target, json(null)) ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
