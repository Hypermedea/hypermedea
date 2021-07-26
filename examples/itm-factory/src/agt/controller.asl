knownVocabulary("https://www.w3.org/2019/wot/td#") .
knownVocabulary("https://www.w3.org/2019/wot/security#") .

+?workshopRunning :
    true
    <-
    readProperty("conveyorSpeed", Speed)[artifact_name(vl10)] ;
    .print("conveyor speed: ", Speed) .

+!start :
    true
    <-
    // set credentials to access the Things of the IT'm factory
    setAuthCredentials("simu1", "simu1")[artifact_name(vl10)] ;
    setAuthCredentials("simu1", "simu1")[artifact_name(apas)] ;
    // check the status of the conveyor and starts it if it is idling
    !run .

+!run :
    true
    <-
    ?workshopRunning ;
    writeProperty("conveyorSpeed", 1)[artifact_name(vl10)] ;
    .print("conveyor speed set to 1") ;
    invokeAction("moveTo", json([kv(x, 0), kv(y, 0), kv(z, 0)]))[artifact_name(apas)] ;
    .print("robotic arm reset to position (0, 0, 0)");
    invokeAction("pickItem", [0, 0])[artifact_name(vl10)] ;
    .print("picked item at [0, 0]") .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }