reset_action(Thing, Action) :- thing(Thing) & base(Base) & .concat(Base, "reset", Action) .

+!start :
    true
    <-
    .print("Leubot agent started.");
    !!run("leubot");
  .

+!run(Name) :
    thing(Thing)
    & api_key(Token)
    <-
    // To also execute the requests, remove the second init parameter (dryRun flag).
    // When dryRun is set to true, the requests are printed (but not executed).
    //makeArtifact(Name, "hypermedia.ThingArtifact", [Thing, false], ArtId);
    //.println("PAY ATTENTION: I am in dryRun=True mode");
    makeArtifact("leubot", "hypermedia.ThingArtifact", [Thing, false], ArtId);
    focus(ArtId);

    // Set API key is a call of the operation setAPIKey on the ThingArtifact
    setAPIKey(Token)[artifact_name(Name)];

    // goal of potting Items using the thing
    !reset(Thing, Name);

    // then, you can invoke other actions, such as "gripper", "wristle/angle", ...
  .

// Plan for invoking the action affordance reset
+!reset(Thing, Name) :
    thing(Thing)
    <-
    ?reset_action(Thing, ActionName);
    .println("---> ",Thing," invoke operation ",ActionName);
    invokeAction(ActionName,[])[artifact_name(Name)];
    .println("---> ",Thing," operation invoked ",ActionName);
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
