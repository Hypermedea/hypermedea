const fs = require('fs');
const ps = require('child_process');

const REPEATS = 10;
const MAX_MODEL_COMPLEXITY = 10;

let tsv = '# <model complexity (nb components)> <time in ms (FF)>\n';

for (let modelComplexity = 2; modelComplexity <= MAX_MODEL_COMPLEXITY; modelComplexity++) {
    console.log(`Evaluating with model complexity: ${modelComplexity}`);

    for (let i = 0; i < REPEATS; i++) {
        let before1 = new Date().getTime();
        ps.execSync(`/opt/FF-X/ff -o domain.pddl -f problem-${modelComplexity}.pddl`);
        let after1 = new Date().getTime();

        // TODO FF in PDDL4J?

        tsv += `${modelComplexity}\t${after1 - before1}\n`;
    }
}

// TODO average/min/max

fs.writeFileSync('results.tsv', tsv);