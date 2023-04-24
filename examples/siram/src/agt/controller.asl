// allowed values: "emg:0" (green), "emg:1" (red)
alert_goal(json([kv("data", "emg:0")])) .

thing(mir) .
//thing(ram) .

tag("logistique") .
tag("qualite") .
tag("zone_tampon") .
tag("dx10b5") .

//mir_tagged_location("logistique", [19.58, 7.44, 0], [0, 0, -0.56, 0.82]) .
mir_tagged_location("logistique", [19.79, 7.22, 0], [0, 0, 0.87, 0.47]) .
mir_tagged_location("qualite", [15.476616259551427, 5.4803723308303525, 0], [0, 0, -0.5534475888053139, 0.8328840053966604]) .
mir_tagged_location("zone_tampon", [13.261030169302074, 6.188439205363753, 0], [0, 0, 0.1958453875670178, 0.9806347863342013]) .
mir_tagged_location("dx10b5", [10.827581428659371, 10.95123100099946, 0], [0, 0, 0.1763894630109686, 0.984320454597334]) .

mir_move_base_payload(Tag, json([
    kv("goal", json([
          kv("pattern_type", 0),
          kv("target_pose", json([
                kv("header", json([
                    kv("frame_id", "map")
                ])),
                kv("pose", json([
                      kv("position", json([
                            kv("x", Xp),
                            kv("y", Yp),
                            kv("z", Zp)
                      ])),
                      kv("orientation", json([
                            kv("x", Xo),
                            kv("y", Yo),
                            kv("z", Zo),
                            kv("w", Wo)
                      ]))
                ]))
          ])),
          kv("move_task", 1)
    ]))
]))
    :- tag(Tag) & mir_tagged_location(Tag, [Xp, Yp, Zp], [Xo, Yo, Zo, Wo]) .

mir_mission_info("logistique", "chargement,grand pot,3") .
mir_mission_info("qualite", "déchargement,grand pot,1") .
mir_mission_info("zone_tampon", "déchargement,grand pot,3") .
mir_mission_info("dx10b5", "chargement,grand pot,1") .

mir_mission_payload(Tag, json([kv("data", Info)]))
    :- mir_mission_info(Tag, Info) .

ram_goto_payload(Tag, json([
    kv("toLocation", Tag),
    kv("orientation", json([
        kv("phi", 0.0),
        kv("theta", 0.0)
    ]))
]))
    :- tag(Tag) .

ram_gripper_location("logistique", "{'x': 0.9, 'y': 0, 'z': -0.01}") .
ram_gripper_location("qualite", "{'x': 0.9, 'y': 0.2, 'z': -0.265}") .
ram_gripper_location("zone_tampon", "{'x': 0.9, 'y': 0.2, 'z': -0.23}") .
ram_gripper_location("dx10b5", "{'x': 0.8, 'y': -0.6, 'z': 0.03}") .

ram_pick_payload(Tag, Nb, json([
    kv("_Object", "Pots grand format"),
    kv("targetLocation", Location),
    kv("nbObjects", Nb)
]))
    :- ram_gripper_location(Tag, Location) .

ram_place_payload(Tag, json([
    kv("targetLocation", Location)
]))
    :- ram_gripper_location(Tag, Location) .

// "Initialisée", "En cours", "Terminée", "Terminée avec défaut(s)"

action(goToStorage) .
action(pickStorage) .
action(goToBuffer) .
action(placeBuffer) .

mission(refillStorage, [
    goToStorage,
    pickStorage,
    goToBuffer,
    placeBuffer
]) .

mission(takeSample, [
    goToDX10,
    pickDX10,
    goToQualityControl,
    placeQualityControl
]) .

+!goToStorage :
    thing(mir) & mir_move_base_payload("logistique", GoToStoragePayload)
    <-
    writeProperty("move_base", GoToStoragePayload)[artifact_name(mir)] ;
    .print("executed goToStorage with MiR (no response)") .

+!goToStorage :
    thing(ram) & ram_goto_payload("logistique", GoToStoragePayload)
    <-
    invokeAction("goto", GoToStoragePayload, Response)[artifact_name(ram)] ;
    .print("executed goToStorage, got: ", Response) .

+!pickStorage :
    thing(mir) & mir_mission_payload("logistique", Payload)
    <-
    writeProperty("mir_mission", Payload)[artifact_name(mir)] ;
    .print("delegated pickStorage to human operator (no response)") ;
    readProperty("mir_mission_status", Val)[artifact_name(mir)] ;
    .print("received operator feedback") ;
    .

+!pickStorage :
    thing(ram) & ram_pick_payload("logistique", 3, PickStoragePayload)
    <-
    invokeAction("pick", PickStoragePayload, Response)[artifact_name(ram)] ;
    .print("executed pickStorage, got: ", Response) .

+!goToBuffer :
    thing(mir) & mir_move_base_payload("zone_tampon", GoToBufferPayload)
    <-
    writeProperty("move_base", GoToBufferPayload)[artifact_name(mir)] ;
    .print("executed goToBuffer with MiR (no response)") .

+!goToBuffer :
    thing(ram) & ram_goto_payload("zone_tampon", GoToBufferPayload)
    <-
    invokeAction("goto", GoToBufferPayload, Response)[artifact_name(ram)] ;
    .print("executed goToBuffer") .

+!placeBuffer :
    thing(mir) & mir_mission_payload("zone_tampon", Payload)
    <-
    writeProperty("mir_mission", Payload)[artifact_name(mir)] ;
    .print("delegated placeBuffer to human operator (no response)") ;
    readProperty("mir_mission_status", Val)[artifact_name(mir)] ;
    .print("received operator feedback") ;
    .

+!placeBuffer :
    thing(ram) & ram_place_payload("zone_tampon", PlaceBufferPayload)
    <-
    invokeAction("place", PlaceBufferPayload, Response)[artifact_name(ram)] ;
    .print("executed placeBuffer") .

+!goToDX10 :
    thing(mir) & mir_move_base_payload("dx10b5", Payload)
    <-
    writeProperty("move_base", Payload)[artifact_name(mir)] ;
    .print("executed goToDX10 with MiR (no response)") .

+!goToDX10 :
    thing(ram) & ram_goto_payload("dx10b5", Payload)
    <-
    invokeAction("goto", Payload, Response)[artifact_name(ram)] .

+!pickDX10 :
    thing(mir) & mir_mission_payload("dx10b5", Payload)
    <-
    writeProperty("mir_mission", Payload)[artifact_name(mir)] ;
    .print("delegated pickDX10 to human operator (no response)") ;
    readProperty("mir_mission_status", Val)[artifact_name(mir)] ;
    .print("received operator feedback") ;
    .

+!pickDX10 :
    thing(ram) & ram_pick_payload("dx10b5", 1, Payload)
    <-
    invokeAction("pick", Payload, Response)[artifact_name(ram)] .

+!goToQualityControl :
    thing(mir) & mir_move_base_payload("qualite", Payload)
    <-
    writeProperty("move_base", Payload)[artifact_name(mir)] ;
    .print("executed goToQualityControl with MiR (no response)") .

+!goToQualityControl :
    thing(ram) & ram_goto_payload("qualite", Payload)
    <-
    invokeAction("goto", Payload, Response)[artifact_name(ram)] .

+!placeQualityControl :
    thing(mir) & mir_mission_payload("qualite", Payload)
    <-
    writeProperty("mir_mission", Payload)[artifact_name(mir)] ;
    .print("delegated placeQualityControl to human operator (no response)") ;
    readProperty("mir_mission_status", Val)[artifact_name(mir)] ;
    .print("received operator feedback") ;
    .

+!placeQualityControl :
    thing(ram) & ram_place_payload("qualite", Payload)
    <-
    invokeAction("place", Payload, Response)[artifact_name(ram)] .

+!execute(Mission, I) :
    mission(Mission, ActionList) & .nth(I, ActionList, Action) & .length(ActionList, NbActions)
    <-
    .print("executing ", Action, " in mission ", Mission) ;
    !Action ; // TODO build term with parameter?
    /*
      Response = resource(URI) ;
      .print("action ID: ", URI) ;
    */
    if (I+1 < NbActions) {
        .wait(2000) ;
        !!execute(Mission, I+1)
    } else {
        !!finalize(Mission)
    } ;
    .

/*
 * TODO monitor action status with queryaction operation, instead
 */
-!execute(Mission, I)
    <-
    .wait(1000) ;
    //.print("action ", I, " of mission ", Mission, " failed. Retrying...") ;
    !!execute(Mission, I) .

+!performQualityControl :
    true
    <-
    readProperty("Accumulation_Aval_B5", Val)[artifact_name(b5)] ;
    .print("B5: ", Val) ;
    if (Val) {
        writeProperty("M310.2", true)[artifact_name(ua)] ;
        .print("paused DX10") ;
        !!execute(takeSample, 0)
    } else {
        .wait(2000) ;
        !!performQualityControl
    } .

+!finalize(takeSample) :
    true
    <-
    .wait(10000) ;
    !!performQualityControl ;
    .

+!feedLine :
    true
    <-
    readProperty("MW322", Val)[artifact_name(ua)] ;
    .print("Compteur pots: ", Val) ;
    if (Val > 3) {
        !!execute(refillStorage, 0)
    } else {
        .wait(2000) ;
        !!feedLine
    } .

+!finalize(refillStorage)
    true
    <-
    writeProperty("MW322", 0)[artifact_name(ua)] ;
    .print("Reset 'Compteur pots' to 0") ;
    !!feedLine ;
    .

+!get_status
    <-
    readProperty("ram_status", Res)[artifact_name(ram)] ;
    .print(Res) .
    // TODO get nbObjects and check if RAM is busy

+!start :
    true
    <-
    //!execute(refillStorage, 0) ;
    //!execute(takeSample, 2) ;
    !!feedLine ;
    //!!performQualityControl ;
    .

{ include("inc/actions.asl") }

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
