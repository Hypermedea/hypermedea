+!start :
    true
    <-
    readProperty("status", Status) ;
    .print("light status: ", Status) ;
    invokeAction("toggle") ;
    readProperty("status", NewStatus) ;
    .print("light status: ", NewStatus) ; .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
