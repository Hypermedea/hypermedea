domain(domain("hanoi", [
  action("move", ["?disc", "?from", "?to"],
    and(smaller("?disc", "?to"), on("?disc", "?from"), clear("?disc"), clear("?to")),
    and(clear("?from"), on("?disc", "?to"), not(on("?disc", "?from")), not(clear("?to"))))
])) .

smaller(disk1, disk2) .
smaller(disk3, disk2) .
smaller(disk1, disk3) .
on(disk1, disk2) .
clear(disk1) .
clear(disk3) .

goal(clear(disk2)) .

+!start
    <-
    !try
  .

+!try :
    goal(Goal)
    <-
    // try to reach goal for which no plan exists
    !Goal ;
    // check that goal has been reached
    ?Goal ;
    .print("goal reached!") ;
    .

-!try[code(Goal2)] :
    goal(Goal) & not retrying
    <-
    .print("no plan known to reach goal; synthesizing plan...") ;
    // look for domain definition in belief base
    ?domain(Domain) ;
    // build problem definition from current beliefs (init state)
    ?current_state(Facts) ;
    // build a Jason plan from a PDDL domain/problem definition
    Domain = domain(DomainName, Actions);
    .term2string(Goal, GoalName) ;
    buildPlan(Domain, problem(GoalName, DomainName, Facts, Goal), Plan) ;
    // add plan to the agent's plan library
    .add_plan(Plan) ;
    // retry to reach goal
    .print("found plan; retrying...") ;
    +retrying ;
    !try ;
    -retrying ;
    .

-!try :
    retrying
    <-
    .print("previously synthesized plan failed; giving up...") ;
    .

+?current_state(Facts)
    <-
    .setof(smaller(Disk1, Disk2), smaller(Disk1, Disk2), Facts1) ;
    .setof(on(Disk1, Disk2), on(Disk1, Disk2), Facts2) ;
    .setof(clear(Disk), clear(Disk), Facts3) ;
    .concat(Facts1, Facts2, Facts3, Facts) ;
    .

+!move(Disk, From, To) :
    smaller(Disk, To) & on(Disk, From) & clear(Disk) & clear(To)
    <-
    .print("moving disk ", Disk, " from ", From, " to ", To) ;
    +clear(From) ; +on(Disk, To) ; -on(Disk, From) ; -clear(To) ;
    .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }