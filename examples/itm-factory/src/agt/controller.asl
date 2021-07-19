knownVocabulary("https://www.w3.org/2019/wot/td#") .
knownVocabulary("https://www.w3.org/2019/wot/security#") .

+?workshopRunning :
    true
    <-
    readProperty("conveyorSpeed", Speed) ;
    .print("conveyor speed: ", Speed) .

+!start :
    true
    <-
    // set credentials to access the Thing (DX10 workshop of the IT'm factory)
    setAuthCredentials("simu1", "simu1") ;
    // check the status of the conveyor and starts it if it is idling
    !run .

+!run :
    true
    <-
    ?workshopRunning ;
    writeProperty("conveyorSpeed", 1) ;
    .print("conveyor speed set to 1") .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }