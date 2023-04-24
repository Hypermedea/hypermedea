hasAffordanceToMove(Thing, Affordance) :-
    (mir(Thing) | ram(Thing)) &
    hasAffordance(Thing, Affordance) .

+!goTo(Tag) :
    hasAffordanceToMove(Thing, Affordance)
    <-
    .print("TODO") .

//goto_payload(Tag, )

goto_payload(Tag, Payload)
    :-
    X = json([
        kv("toLocation", Tag),
        kv("orientation", json([
            kv("phi", 0.0),
            kv("theta", 0.0)
        ]))
    ]) .