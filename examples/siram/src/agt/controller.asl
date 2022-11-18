// allowed values: "emg:0" (green), "emg:1" (red)
ros_goal(json([kv("data", "emg:0")])) .

+!start :
    true
    <-
    !observe .

+!observe :
    ros_goal(Goal)
    <-
    writeProperty("light_cmd", Goal) .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
