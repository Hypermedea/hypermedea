package namespaceAPI;

import classes.Axiom_Jason;
import edu.kit.aifb.datafu.*;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.origins.RequestOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.notation3.ParseException;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import ontologyAPI.AxiomExtractor;
import org.apache.jena.query.*;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.yars.nx.Node;
import tools.IRITools;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class BeliefNamingStrategy {
    public HashMap<String,String> mappedLabels;
    public HashMap<String,String> mappedPreferredNamespaces;
    public HashMap<String,String> mappedKnownNamespaces;

    public HashMap<String, String> getMappedKnownNamespaces() {
        return mappedKnownNamespaces;
    }

    public void setMappedKnownNamespaces(HashMap<String, String> mappedKnownNamespaces) {
        this.mappedKnownNamespaces = mappedKnownNamespaces;
    }

    public HashMap<String, String> getMappedLabels() {
        return mappedLabels;
    }

    public void setMappedLabels(HashMap<String, String> mappedLabels) {
        this.mappedLabels = mappedLabels;
    }

    public HashMap<String, String> getMappedPreferredNamespaces() {
        return mappedPreferredNamespaces;
    }

    public void setMappedPreferredNamespaces(HashMap<String, String> mappedPreferredNamespaces) {
        this.mappedPreferredNamespaces = mappedPreferredNamespaces;
    }

    public BeliefNamingStrategy() {
        mappedLabels = new HashMap<>();
        mappedPreferredNamespaces = new HashMap<>();
        mappedKnownNamespaces = new HashMap<>();
    }

    public void computeMappedLabels(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        mappedLabels = AxiomExtractor.extractLabels(owlClassSet, owlOntology);
    }

    public void computeMappedPreferredNamespaces(String programFile, String uri){
        String queryString = "PREFIX vann: <http://purl.org/vocab/vann/> \n" +
                "CONSTRUCT {?s vann:preferredNamespaceUri ?o .}  \n" +
                "where \n" +
                "{ ?s vann:preferredNamespaceUri ?o . }";


        // set logging level to warning
        Logger log = Logger.getLogger("edu.kit.aifb.datafu");
        log.setLevel(Level.WARNING);
        LogManager.getLogManager().addLogger(log);

        try {
            InputStream is = new FileInputStream(programFile);
            Origin base = new FileOrigin(new File(programFile), FileOrigin.Mode.READ, null);
            Notation3Parser n3Parser = new Notation3Parser(is);
            ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(base);

            n3Parser.parse(programConsumer, base);
            is.close();

            Program program = programConsumer.getProgram(base);

            QueryConsumerImpl queryConsumer = new QueryConsumerImpl(base);
            SparqlParser sparqlParser = new SparqlParser(new StringReader(queryString));
            sparqlParser.parse(queryConsumer, new InternalOrigin(""));

            ConstructQuery query = queryConsumer.getConstructQueries().iterator().next();

            BindingConsumerCollection triples = new BindingConsumerCollection();
            program.registerConstructQuery(query, new BindingConsumerSink(triples));

            EvaluateProgramConfig config = new EvaluateProgramConfig();
            //config.setThreadingModel(EvaluateProgramConfig.ThreadingModel.SERIAL);
            EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
            eval.start();

            //Todo : Replacing String uri by merged ontology and make the program take that parameter as entrypoint
            //ep.getBaseConsumer().consume(new Binding(new Nodes(new Resource("http://example.org/foo"), RDF.TYPE, RDFS.RESOURCE));
            eval.getInputOriginConsumer().consume(new RequestOrigin(new URI(uri), Request.Method.GET));

            eval.awaitIdleAndFinish();
            eval.shutdown();

            System.out.println(triples.getCollection().size());

            for (Binding binding : triples.getCollection()) {
                Node[] st = binding.getNodes().getNodeArray();
                mappedKnownNamespaces.put(st[0].getLabel().toString(), st[2].getLabel().toString());
                //System.out.println(st[0].getLabel().toString() + " ::: " + st[1].getLabel().toString()+ " ::: "+st[2].getLabel().toString());
                //defineObsProperty("rdf", st[0].getLabel(), st[1].getLabel(), st[2].getLabel());
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (edu.kit.aifb.datafu.parser.sparql.ParseException e) {
            e.printStackTrace();
        }

        /*
        Query query = QueryFactory.create(queryString) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution() ;
                RDFNode x1 = soln.get("s");
                RDFNode x3 = soln.get("o");
                mappedKnownNamespaces.put(x1.toString(), x3.toString());
            }
        }*/
    }

    public void computeMappedKnownNamespaces(){
        Properties p = new Properties();
        try {
            FileInputStream f = new FileInputStream("res/known_namespaces/table.xml");
            p.loadFromXML(f);

            Enumeration<?> enumeration = p.propertyNames();

            while (enumeration.hasMoreElements())
            {
                String key = (String) enumeration.nextElement();
                String value = p.getProperty(key);
                mappedKnownNamespaces.put(IRITools.addWrapperUri(key),IRITools.addWrapperUri(value));
                //System.out.println(IRITools.addWrapperUri(key) + " = " + IRITools.addWrapperUri(value));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidPropertiesFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateNameBelief(Axiom_Jason axiom, boolean doLabelStrategy, boolean doPreferenceNamespaceStrategy, boolean doKnownPrefixStrategy){
        boolean hasBeenSet = false;
        if (doLabelStrategy) {
            hasBeenSet = executeLabelStrategy(axiom);
        }

        if (!hasBeenSet & doPreferenceNamespaceStrategy){
            hasBeenSet = executePreferenceNamespaceStrategy(axiom);
        }

        if (!hasBeenSet & doKnownPrefixStrategy){
            hasBeenSet = executeKnownPrefixStrategy(axiom);
        }

        if (!hasBeenSet) {
            executeDefaultStrategy(axiom);
        }
        //System.out.println(axiom);
    }

    private boolean executeLabelStrategy(Axiom_Jason axiom) {
        //System.out.println(axiom.getPredicateFullName());
        if (mappedLabels.containsKey(axiom.getPredicateFullName())){
            //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
            if (axiom.getPredicateFullName().contains(mappedLabels.get(axiom.getPredicateFullName()))) {
                //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
                axiom.setPredicateName(IRITools.firstCharToLowerCase(mappedLabels.get(axiom.getPredicateFullName())));
                //System.out.println("used");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean executePreferenceNamespaceStrategy(Axiom_Jason axiom) {
        for (String prefix : mappedPreferredNamespaces.keySet()){
            if (axiom.getPredicateFullName().startsWith(prefix)) {
                String suffix = IRITools.getNameByMatchingPrefix(axiom.getPredicateFullName(),prefix);
                axiom.setPredicateName(IRITools.firstCharToLowerCase(suffix));
                return true;
            }
        }
        return false;
    }

    private boolean executeKnownPrefixStrategy(Axiom_Jason axiom) {
        if (mappedKnownNamespaces.containsKey(axiom.getPredicateFullName())){
            //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
            if (axiom.getPredicateFullName().contains(mappedKnownNamespaces.get(axiom.getPredicateFullName()))) {
                //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
                axiom.setPredicateName(IRITools.firstCharToLowerCase(mappedKnownNamespaces.get(axiom.getPredicateFullName())));
                //System.out.println("used2");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void executeDefaultStrategy(Axiom_Jason axiom) {
        axiom.setPredicateName(IRITools.getSuffixIri(axiom.getPredicateFullName(),true));
    }
}
