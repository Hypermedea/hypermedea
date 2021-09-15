/**
 * Evaluation criteria:
 *  - with increased numbers of candidate Things
 *  - (TODO) with increased plan complexity
 */
domain(domain("itm-factory", [
  action("moveTo", [ "?device", "?from", "?to" ],
    and(
      transportationDevice("?device"),
      isAt("?device", "?from"),
      canReach("?device", "?to")
    ),
    and(
      not(isAt("?device", "?from")),
      isAt("?device", "?to")
    )
  ),
  action("pick", [ "?device", "?item", "?location" ],
    and(
      transportationDevice("?device"),
      not(busy("?device")),
      isAt("?device", "?location"),
      item("?item"),
      isAt("?item", "?location")
    ),
    and(
      busy("?device"),
      carry("?device", "?item"),
      not(isAt("?item", "?location"))
    )
  ),
  action("release", [ "?device", "?item", "?location" ],
    and(
      transportationDevice("?device"),
      carry("?device", "?item"),
      isAt("?device", "?location")
    ),
    and(
      isAt("?item", "?location"),
      not(busy("?device")),
      not(carry("?device", "?item"))
    )
  ),
  action("turnOn", [ "?workstation", "?item" ],
    and(
      workstation("?workstation"),
      not(on("?workstation"))
    ),
    on("?workstation")
  )
])) .

// TODO add "exists item at the tail of a workstation => item then at the head"

// problem(problem("test", "itm-factory", [
//   transportationDevice(device1),
//   isAt(device1, somewhere),
//   canReach(device1, storageRack),
//   canReach(device1, conveyor1Tail),
//   transportationDevice(conveyor1),
//   isAt(conveyor1, conveyor1Tail),
//   canReach(conveyor1, conveyor1Tail),
//   canReach(conveyor1, conveyor1Head),
//   item(item1),
//   workstation(ws1),
//   isAt(item1, storageRack)
// ], isAt(item1, conveyor1Head))) .

planner("HSP") .
// planner("FF") .

goal(isAt(item, conveyor1Head)) .

iterations(1) .
maxSize(20) .

average([Val], Val) .
average([Val1 | Sublist], Avg) :- average(Sublist, Val2) & Avg = (Val1 + Val2) / 2 .

+!evaluate :
    planner(P) & iterations(IMax) & maxSize(SizeMax)
    <-
    makeArtifact("planner", "org.hypermedea.PlannerArtifact", [P], ArtId) ;
    focus(ArtId) ;
    for (.range(Size, 1, SizeMax)) {
      !buildProblem(Size) ;
      for (.range(I, 1, IMax)) {
        !plan(Plan, T) ;
        if (.substring("fail", Plan)) {
            .print("no plan found for size ", Size)
        }
        +planningTime(T) ;
      }
      !showResult ;
      !clean ;
    }
    disposeArtifact(ArtId) ;
  .

+!buildProblem(Size) :
    goal(Goal)
    <-
    .concat("problem", Size, Name) ;
    +fact(item(item)) ;
    +fact(isAt(item, storageRack)) ;
    for (.range(I, 1, Size)) {
      // create AGV
      .concat("device", I, DeviceName) ;
      .term2string(Device, DeviceName) ;
      +optionalFact(agv(Device)) ;
      +fact(transportationDevice(Device)) ;
      +fact(isAt(Device, storageRack)) ;
      // create conveyor
      .concat("conveyor", I, ConveyorName) ;
      .term2string(Conveyor, ConveyorName) ;
      +optionalFact(conveyor(Conveyor)) ;
      +fact(transportationDevice(Conveyor)) ;
      // create conveyor tail/head with reachability statements
      .concat(Conveyor, "Tail", TailName) ;
      .term2string(Tail, TailName) ;
      .concat(Conveyor, "Head", HeadName) ;
      .term2string(Head, HeadName) ;
      +fact(isAt(Conveyor, Tail)) ;
      +fact(canReach(Conveyor, Tail)) ;
      +fact(canReach(Conveyor, Head)) ;
    } ;
    for (optionalFact(agv(Device)) & optionalFact(conveyor(Conveyor)) & fact(isAt(Conveyor, Tail))) {
      // an AGV can reach any conveyor tail
      +fact(canReach(Device, Tail))
    } ;
    .findall(F, fact(F), Facts) ;
    +problem(problem(Name, "itm-factory", Facts, Goal)) ;
  .

+!plan(Plan, T) :
    domain(Domain) & problem(Problem)
    <-
    !t(T1) ;
    buildPlan(Domain, Problem, Plan) ;
    !t(T2) ;
    T = T2 - T1 ;
  .

////////////////////////////////////////////////////////////////////// utilities

+!printProblem :
    problem(Problem)
    <-
    .print(Problem) ;
  .

+!printPlan :
    plan(Plan)
    <-
    .print(Plan) ;
  .

+!showResult :
    true
    <-
    .count(fact(_), Nb) ;
    .findall(T, planningTime(T), List) ;
    ?average(List, Avg) ;
    .min(List, Min) ;
    .max(List, Max) ;
    .print(Nb, ",", Avg, ",", Min, ",", Max) ;
  .

+!clean :
    true
    <-
    .abolish(fact(_)) ;
    .abolish(optionalFact(_)) ;
    .abolish(planningTime(_)) ;
  .

+!t(T) :
    true
    <-
    .time(H, M, S, MS) ;
    T = MS + (S + (M + H * 60) * 60) * 1000;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }