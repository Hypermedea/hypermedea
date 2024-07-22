variable("MD70") . // Vitesse Jog
variable("M310.2") . // Pause
variable("MW322") . // Compteur pots

/*
A variant is an information resource that holds the current value of some variable.
*/
hasVariant(Var, Res)
    :-
    variable(Var) &
    rdf(Af, "https://www.w3.org/2019/wot/td#name", Var) &
    rdf(Af, "https://www.w3.org/2019/wot/td#hasForm", F) &
    rdf(F, "https://www.w3.org/2019/wot/hypermedia#hasTarget", Res) .

value(Res, Val)
    :-
    rdf(Res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#value", Val) .

mir_tagged_location("logistique", [19.79, 7.22, 0], [0, 0, 0.87, 0.47]) .
mir_tagged_location("qualite", [15.476616259551427, 5.4803723308303525, 0], [0, 0, -0.5534475888053139, 0.8328840053966604]) .
mir_tagged_location("zone_tampon", [13.261030169302074, 6.188439205363753, 0], [0, 0, 0.1958453875670178, 0.9806347863342013]) .
mir_tagged_location("dx10b5", [10.827581428659371, 10.95123100099946, 0], [0, 0, 0.1763894630109686, 0.984320454597334]) .
mir_tagged_location("chargement", [6.75, 14.7, 0], [0, 0, -0.56, 0.82]) .

mir_move_base_payload(Tag, json([
    kv("pattern_type", 0),
    kv("target_pose", [
        kv("header", [
            kv("frame_id", "map")
        ]),
        kv("pose", [
                kv("position", [
                    kv("x", Xp),
                    kv("y", Yp),
                    kv("z", Zp)
                ]),
                kv("orientation", [
                    kv("x", Xo),
                    kv("y", Yo),
                    kv("z", Zo),
                    kv("w", Wo)
                ])
        ])
    ]),
    kv("move_task", 1)
]))
    :-
    mir_tagged_location(Tag, [Xp, Yp, Zp], [Xo, Yo, Zo, Wo]) .

+!observe
    <-
    for (variable(V) & hasVariant(V, Res)) { get(Res) } ;
    .wait(1000) ;
    !!observe .

-!observe
    <-
    .wait(10000) ;
    !!observe .

+!set(V, Val) : variable(V) & hasVariant(V, Res)
    <-
    put(Res, [
        rdf(Res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#value", Val)[rdf_type_map(uri, uri, literal)]
    ], [
        "urn:hypermedea:opcua:datatype", "urn:hypermedea:opcua:Float"
    ]) .

+rdf(Res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#value", Val)
    <-
    if (hasVariant("MW322", Res) & Val > 2) { !sendToRefill } .

+!sendToRefill
    <-
    !sendTo("logistique") ;
    .wait(5000) ;
    !sendTo("zone_tampon") .
    // TODO reset "Compteur pots"?

+!sendTo(Loc) : mir_move_base_payload(Loc, P)
    <-
    post("ros+ws://10.1.0.65:9090/move_base", [P], ["https://github.com/RobotWebTools/rosbridge_suite/blob/ros1/ROSBRIDGE_PROTOCOL.md#messageType", "mir_actions/MirMoveBaseActionGoal"]) ;
    ?rdf(A, "https://github.com/RobotWebTools/rosbridge_suite/blob/ros1/ROSBRIDGE_PROTOCOL.md#goalId", Id) ;
    watch(Id) ;
    +watching(Loc, Id) ;
    .wait({ +sentTo(Loc) }) ;
    .print("Sent to ", Loc) .

+json(Status)[source(Id)] : watching(Loc, Id)
    <-
    if (.member(kv(status, 3), Status)) {
        +sentTo(Loc) ;
        -watching(Loc, Id) ;
        forget(Id)
    } .

+!start
    <-
    get("ua.ttl") ;
    !observe .

{ include("$jacamoJar/templates/common-cartago.asl") }
