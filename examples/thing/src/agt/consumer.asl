+!start :
    true
    <-
    readProperty("status", Status) ;
    .print("light status: ", Status) ;
    writeProperty("status", false) ;
    .print .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
