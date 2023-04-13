// allowed values: "emg:0" (green), "emg:1" (red)
alert_goal(json([kv("data", "emg:0")])) .

move_goal(json([
    kv("goal", json([
          kv("pattern_type", 0),
          kv("target_pose", json([
                kv("header", json([
                    kv("frame_id", "map")
                ])),
                kv("pose", json([
                      kv("position", json([
                            kv("y", 6.5),
                            kv("x", 12.2),
                            kv("z", 0)
                      ])),
                      kv("orientation", json([
                            kv("x", 0),
                            kv("y", 0),
                            kv("z", 0.7660444585477029),
                            kv("w", 0.6427875912993006)
                      ]))
                ]))
          ])),
          kv("move_task", 1)
    ]))
])) .

ram_goto_goal(json([
    kv("toLocation", "point_de_prise"),
    kv("orientation", json([
        kv("phi", 0.0),
        kv("theta", 0.0)
    ]))
])) .

+!start :
    true
    <-
    //!observe ;
    //!alertMiR ;
    //!moveMiR
    !moveRAM .

+!observe :
    true
    <-
    readProperty("MD70", Val)[artifact_name(dx10)] ;
    .print("MD70: ", Val) .

+!alertMiR :
    alert_goal(Goal)
    <-
    writeProperty("light_cmd", Goal)[artifact_name(mir)] .

+!moveMiR :
    move_goal(Goal)
    <-
    writeProperty("move_base", Goal)[artifact_name(mir)] .

+!moveRAM :
    ram_goto_goal(Goal)
    <-
    invokeAction("goto", Goal)[artifact_name(ram)] .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
