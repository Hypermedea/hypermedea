const fs = require('fs');

if (process.argv.length < 3) {
    console.log('Usage: node generator.js <chain_length>');
    return;
}

const CHAIN_LENGTH = Number(process.argv[2]);

let predicates = '';
let actions = '';

for (let i = 0; i < CHAIN_LENGTH; i++) {
    let precondition = `(rel${i} ?var0 ?var1)`;

    let ending = i < CHAIN_LENGTH - 1
        ? ' '
        : ' (relGoal ?var0 ?var1)';

    let effect = i < CHAIN_LENGTH - 1
        ? `(rel${i + 1} ?var0 ?var1)`
        : `(relGoal ?var0 ?var1)`;

    predicates += `${precondition}${ending}`;

    actions += `(:action a${i} :parameters (?var0 ?var1) :precondition ${precondition} :effect ${effect})\n`;
}


let pddl = `(define (domain restdesc)\n\
\t(:predicates ${predicates})\n\
\t${actions}\n\
)`;

fs.writeFileSync(`domain-${CHAIN_LENGTH}.pddl`, pddl);