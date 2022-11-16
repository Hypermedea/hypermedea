+!start :
    true
    <-
    !init .
    
+!init :
    true
    <-
    registerBinding("opc.tcp", "org.hypermedea.opcua.OpcUaBinding") .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
