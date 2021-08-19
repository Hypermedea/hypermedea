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

goal(isAt(item, conveyor1Head)) .

+!evaluate :
    true
    <-
    !buildProblem(1) ;
    !printProblem ;
    !printPlan ;
  .

+!buildProblem(Size) :
    goal(Goal)
    <-
    .concat("problem", Size, Name) ;
    +fact(item(item)) ;
    +fact(isAt(item, storageRack)) ;
    // for (.range(I, 1, Size)) {
    //   // create AGV
    //   .concat("device", I, DeviceName) ;
    //   .term2string(Device, DeviceName) ;
    //   +fact(agv(Device)) ;
    //   +fact(transportationDevice(Device)) ;
    //   +fact(isAt(Device, storageRack)) ;
    //   // create conveyor
    //   .concat("conveyor", I, ConveyorName) ;
    //   .term2string(Conveyor, ConveyorName) ;
    //   +fact(transportationDevice(Conveyor)) ;
    //   +fact(conveyor(Conveyor)) ;
    //   // create conveyor tail/head with reachability statements
    //   .concat(Conveyor, "Tail", TailName) ;
    //   .term2string(Tail, TailName) ;
    //   .concat(Conveyor, "Head", HeadName) ;
    //   .term2string(Head, HeadName) ;
    //   +fact(isAt(Conveyor, Tail)) ;
    //   +fact(canReach(Conveyor, Tail)) ;
    //   +fact(canReach(Conveyor, Head)) ;
    // } ;
    for (fact(agv(Device)) & fact(conveyor(Conveyor)) & fact(isAt(Conveyor, Tail))) {
      // an AGV can reach any conveyor tail
      +fact(canReach(Device, Tail))
    } ;
    .findall(F, fact(F), Facts) ;
    +problem(problem(Name, "itm-factory", Facts, Goal)) ;
  .

+!printProblem :
    problem(Problem)
    <-
    .print(Problem) ;
  .

+!printPlan :
    domain(Domain) & problem(Problem)
    <-
    buildPlan(Domain, Problem, Plan) ;
    .print(Plan) ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }