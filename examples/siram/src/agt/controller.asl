// allowed values: "emg:0" (green), "emg:1" (red)
ros_goal(json([kv("data", "emg:0")])) .

+!start :
    true
    <-
    !init ;
    !observe .
    
+!init :
    true
    <-
    registerBinding("ros+ws", "org.hypermedea.ros.ROSBinding") .
    //registerBinding("opc.tcp", "org.hypermedea.opcua.OpcUaBinding") .

+!observe :
    ros_goal(Goal)
    <-
    writeProperty("light_cmd", Goal) .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
