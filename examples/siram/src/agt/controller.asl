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

/*
move_goal(json([
    kv("data", json([
          kv("pattern_type", 0),
          kv("target_pose", json([
                kv("header", json([
                    kv("stamp", json([
                        kv("secs", 1680530458),
                        kv("nsecs", 587473673)
                    ])),
                    kv("frame_id", "map"),
                    kv("seq", 0)
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
*/

+!start :
    true
    <-
    //!observe ;
    //!alertMiR ;
    !move .

+!observe :
    true
    <-
    readProperty("MD70", Val)[artifact_name(dx10)] ;
    .print("MD70: ", Val) .

+!alertMiR :
    alert_goal(Goal)
    <-
    writeProperty("light_cmd", Goal)[artifact_name(mir)] .

+!move :
    move_goal(Goal)
    <-
    writeProperty("move_base", Goal)[artifact_name(mir)] .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
