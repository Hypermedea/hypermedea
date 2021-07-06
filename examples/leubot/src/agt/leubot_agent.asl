reset_action(Thing, Action, "reset") :- thing(Thing) & base(Base) & .concat(Base, "reset", Action) .
move_to_action(Thing, Action, "moveTo") :- thing(Thing) & base(Base) & .concat(Base, "moveTo", Action) .
grasp_action(Thing, Action, "grasp") :- thing(Thing) & base(Base) & .concat(Base, "grasp", Action) .
release_action(Thing, Action, "release") :- thing(Thing) & base(Base) & .concat(Base, "release", Action) .

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
    //makeArtifact(Name, "org.hypermedea.ThingArtifact", [Thing, false], ArtId);
    //.println("PAY ATTENTION: I am in dryRun=True mode");
    makeArtifact("leubot", "org.hypermedea.ThingArtifact", [Thing, false], ArtId);
    focus(ArtId);

    // Set API key is a call of the operation setAPIKey on the ThingArtifact
    setAPIKey(Token)[artifact_name(Name)];

    // goal of potting Items using the thing
    !reset(Thing, Name);
    !interval;

    // then, you can invoke other actions: "moveTo", "grasp", "release"
    !move_to(Thing, Name, [12, 10, 10]);
    !interval;

    !grasp(Thing, Name);
    !interval;

    !release(Thing, Name);
    !interval;

    !reset(Thing, Name);
    .println("Leubot is back to its initial position")

  .

// Plan for invoking the action affordance reset
+!reset(Thing, Name) :
    thing(Thing)
    <-
    ?reset_action(Thing, ActionId, ActionName);
    .println("---> ",Thing," invoke operation ",ActionName);
    invokeAction(ActionName,[])[artifact_name(Name)];
    .println("---> ",Thing," operation invoked ",ActionName);
  .

// Plan for invoking the action affordance moveTo
+!move_to(Thing, Name, Coordinates) :
    thing(Thing)
    <-
    ?move_to_action(Thing, ActionId, ActionName);
    .println("---> ",Thing," invoke operation ",ActionName);
    invokeAction(ActionName, ["x", "y", "z"], Coordinates)[artifact_name(Name)];
    .println("---> ",Thing," operation invoked ",ActionName);
  .

// Plan for invoking the action affordance grasp
+!grasp(Thing, Name) :
    thing(Thing)
    <-
    ?grasp_action(Thing, ActionId, ActionName);
    .println("---> ",Thing," invoke operation ",ActionName);
    invokeAction(ActionName,[])[artifact_name(Name)];
    .println("---> ",Thing," operation invoked ",ActionName);
  .

// Plan for invoking the action affordance release
+!release(Thing, Name) :
    thing(Thing)
    <-
    ?release_action(Thing, ActionId, ActionName);
    .println("---> ",Thing," invoke operation ",ActionName);
    invokeAction(ActionName,[])[artifact_name(Name)];
    .println("---> ",Thing," operation invoked ",ActionName);
  .

// Time interval for safe interactions
+!interval : true <- .wait(3000).

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
