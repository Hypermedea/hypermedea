(define (domain restdesc)
	(:predicates (rel0 ?var0 ?var1) (rel1 ?var0 ?var1) (relGoal ?var0 ?var1))
	(:action a0 :parameters (?var0 ?var1) :precondition (rel0 ?var0 ?var1) :effect (rel1 ?var0 ?var1))
(:action a1 :parameters (?var0 ?var1) :precondition (rel1 ?var0 ?var1) :effect (relGoal ?var0 ?var1))

)