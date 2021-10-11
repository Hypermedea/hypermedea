const fs = require('fs');
const { off } = require('process');

if (process.argv.length < 3) {
    console.log('Usage: node generator.js <nb_models>');
    return;
}

const NB_MODELS = Number(process.argv[2]);

const MAX_PROCESS_DEPTH = Math.ceil(Math.log2(NB_MODELS));
const NB_SUBMODELS_PER_MODEL = 1; // 2 for a tree

const NB_WORKSTATIONS_PER_MODEL = 1;

///////////////////////////////////////////////// randomly generate a model tree

let rootModel = { id: 'model0' };
let models = [rootModel];

function expandModels(modelsAtDepth, depth) {
    if (depth >= MAX_PROCESS_DEPTH) return;

    let allChildren = [];

    modelsAtDepth.forEach(m => {
        if (models.length >= NB_MODELS) return;

        m.children = [];

        for (let i = 0; i < NB_SUBMODELS_PER_MODEL && models.length < NB_MODELS; i++) {
            let subModel = { id: `model${models.length}` };
            m.children.push(subModel);
            models.push(subModel);
        }

        allChildren.push(...m.children);
    });

    expandModels(allChildren, ++depth);
}

expandModels(models, 1);

///////////////////////////////// for each model, generate workstations or items

let workstations = models
    .filter(m => m.children)
    .map((m, i) => ({
        id: `workstation${i}`,
        a: 'Workstation',
        hasStatus: { id: 'off' },
        producesModel: m,
        consumesModel: []
    }));

workstations.forEach(w => {
    let m = w.producesModel;
    m.children.forEach(subm => w.consumesModel.push(subm));
});

let items = models
    .filter(m => !m.children)
    .map((m, i) => ({ id: `item${i}`, a: 'Item', isAt: { id: 'locstorage' }, model: m }));

//////////////////// randomly assign workstations to positions on a map (matrix)

const MAP_WIDTH = Math.ceil(Math.sqrt(workstations.length));

let locations = [
    {
        id: 'locstorage',
        hasPathTo: [{ id: 'loc0' }]
    }, {
        id: 'loccharging',
        hasPathTo: [{ id: 'loc0' }],
        '@reverse': {
            isAt: { id: 'agv', a: 'TransportationDevice' }
        }
    }
];

function shuffle(array) {
    let pos = {};
    array.forEach(elem => pos[elem] = Math.random());
    array.sort((elem1, elem2) => pos[elem1] < pos[elem2]);
}

shuffle(workstations);

for (let i = 0; i < MAP_WIDTH; i++) {
    for (let j = 0; (j < MAP_WIDTH); j++) {
        let n = i * MAP_WIDTH + j;
        
        let loc = {
            id: `loc${n}`,
            hasPathTo: []
        };

        let up = n + MAP_WIDTH;
        let down = n - MAP_WIDTH;
        let left = n - 1;
        let right = n + 1;

        [up, down, left, right].forEach(other => {
            if (other >= 0 && other < MAP_WIDTH * MAP_WIDTH) loc.hasPathTo.push({ id: `loc${other}` });
        });

        if (n == 0) {
            loc.hasPathTo.push({ id: 'locstorage' });
            loc.hasPathTo.push({ id: 'loccharging' });
        }

        locations.push(loc);

        let ws = workstations[n];

        if (ws) ws.isAt = loc;
    }
}

////////////////////////////////////////////////////////// fill-in PDDL template

function workstationFacts(ws) {
    let facts = [
        `(workstation ${ws.id})`,
        `(off ${ws.id})`,
        `(isat ${ws.id} ${ws.isAt.id})`,
        `(producesmodel ${ws.id} ${ws.producesModel.id})`
    ];

    ws.consumesModel.forEach(m => facts.push(`(consumesmodel ${ws.id} ${m.id})`));
    
    return facts.join(' ');
}

function itemFacts(i) {
    return [
        `(item ${i.id})`,
        `(model ${i.id} ${i.model.id})`,
        `(isat ${i.id} locstorage)`
    ].join(' ');
}

function locationFacts(loc) {
    return loc.hasPathTo
        .map(other => `(haspathto ${loc.id} ${other.id})`)
        .join(' ');
}

function itemGoal(i) {
    return `(and (model ${i.id} ${rootModel.id}) (isat ${i.id} locstorage))`;
}

let objects = models
    .concat(workstations, items, locations)
    .map(obj => obj.id)
    .join(' ');

let init = workstations
    .map(workstationFacts)
    .concat(items.map(itemFacts))
    .concat(locations.map(locationFacts))
    .join(' ');

let goal = items
    .map(itemGoal)
    .join(' ');

let pddl = fs.readFileSync('problem.tpl.pddl', 'utf-8')
    .replace('{objects}', objects)
    .replace('{init}', init)
    .replace('{goal}', goal);

fs.writeFileSync(`problem-${models.length}.pddl`, pddl);

/////////////////////////////////////////////////////// fill-in JSON-LD template

let graph = {
    '@context': {
        '@base': 'http://example.org/',
        '@vocab': 'http://example.org/',
        'id': '@id',
        'a': '@type'
    },
    '@graph': models.concat(workstations, items, locations)
}

fs.writeFileSync(`init-${models.length}.jsonld`, JSON.stringify(graph));