domain(domain(lighting, [
    action("readProperty", ["?property", "?value"],
        hasValue("?property", "?value"),
        nothing),
    action("writeProperty", ["?property", "?value"],
        nothing,
        hasValue("?property", "?value"))
])) .

problem(problem(switchOff, lighting, Facts, Goal)) :-
    .findall(hasValue(Property, Value), hasValue(Property, Value), HasValueFacts)
    & .concat(HasValueFacts, [nothing], Facts)
    & goal(Goal) .

goal(hasValue("status", false)) .

+!start :
    true
    <-
    //!toggle ;
    //!plan .
    get("http://localhost:5000/td.ttl") ;
    for (rdf(S, P, O)) {
        .print(rdf(S, P, O))
    } ;
    //post("http://localhost:5000/toremove", json([kv(plop, 2)])) ;
    //put("http://localhost:5000/toremove", text("Some content")) ;
    //post("file:///home/victor.charpenay/Bureau/toremove.txt", text("Some content")) ;
    //put("file:///home/victor.charpenay/Bureau/toremove.txt", text("Some more content?")) ;
    //delete("file:///home/victor.charpenay/Bureau/toremove.txt") ;
    //get("file:///home/victor.charpenay/Bureau/toremove.txt") ;
    //?text(Cnt) ; .print(Cnt) ;
    .

+!toggle :
    true
    <-
    readProperty("status", Status) ;
    .print("light status: ", Status) ;
    invokeAction("toggle") ;
    readProperty("status", NewStatus) ;
    .print("light status: ", NewStatus) .

+!plan :
    domain(Domain) & problem(Pb) & goal(Goal)
    <-
    getAsPDDL(Domain, DomainStr) ;
    getAsPDDL(Pb, PbStr) ;
    .print("planning with the following domain and problem: ", DomainStr, "\n", PbStr) ;
    buildPlan(Domain, Pb, Plan) ;
    .print("found the following Jason plan: ", Plan) ;
    .add_plan(Plan) ;
    !switchOff ;
    +Goal .

+property_value("status", LightStatus) :
    true
    <-
    .print("light status (from obs. property): ", LightStatus) .

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
