domain(domain("hanoi", [
  action("move",
    and(smaller("?to", "?disc"), on("?disc", "?from"), clear("?disc"), clear("?to")),
    and(clear("?from"), on("?disc", "?to"), not(on("?disc", "?from")), not(clear("?to"))))
])) .

problem(problem("test", "hanoi", [
  smaller(disk1, disk2),
  smaller(disk3, disk2),
  smaller(disk1, disk3),
  on(disk1, disk2),
  clear(disk1),
  clear(disk3)
], clear(disk2))) .

+!start :
    domain(Domain) & problem(Problem)
    <-
    // build a Jason plan from a PDDL domain/problem definition
    buildPlan(Domain, Problem, Plan) ;
  .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }