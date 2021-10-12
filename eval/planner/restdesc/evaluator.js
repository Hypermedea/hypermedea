const fs = require('fs');
const ps = require('child_process');

const REPEATS = 10;
const MAX_CHAIN_LENGTH = 1024;

let tsv = '# <chain length> <time in ms (FF)> <time in ms (PDDL4J)>\n';

for (let chainLength = 2; chainLength <= MAX_CHAIN_LENGTH; chainLength *= 2) {
    for (let i = 0; i < REPEATS; i++) {
        let before1 = new Date().getTime();
        ps.execSync(`FF-2000/ff -o domain-${chainLength}.pddl -f problem.pddl`);
        let after1 = new Date().getTime();

        let before2 = new Date().getTime();
        ps.execSync(`java -jar ../../../lib/pddl4j-3.8.3-hypermedea.jar -p 1 -o domain-${chainLength}.pddl -f problem.pddl`);
        let after2 = new Date().getTime();

        tsv += `${chainLength}\t${after1 - before1}\t${after2 - before2}\n`;
    }
}

// TODO calculate average/min/max

fs.writeFileSync('results.tsv', tsv);