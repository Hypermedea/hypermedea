presenceProperty("mqtt://test.mosquitto.org/PresenceSensor/events/presenceDetected") .

// TODO from Plugfest instead?
coffeeMachineState("http://localhost:8080/smart-coffee-machine/properties/allAvailableResources") .
coffeeMachineAction("http://localhost:8080/smart-coffee-machine/actions/makeDrink{?drinkId,size,quantity}") .

preferredCoffee("espresso") .

+!start
    <-
    // !detectPresence ;
    !monitorCoffeeMachineState ;
    !makeCoffee ;
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
    h.expand_template(A, [kv(drinkId, Type), kv(size, s)], Ap);
    post(Ap) ;
    ?(json(Out)[source(Ap)]) ;
    .print(Out) ;
  .

+json(Val)[source(P)] : watching(P)
    <-
    .print(Val) ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
