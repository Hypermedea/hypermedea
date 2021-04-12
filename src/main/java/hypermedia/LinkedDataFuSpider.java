package hypermedia;

import cartago.*;
import edu.kit.aifb.datafu.*;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.InputOrigin;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.origins.RequestOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A CArtAgO artifact for browsing Linked Data.
 *
 * Contributors:
 * - Victor Charpenay (author), Mines Saint-Étienne
 *
 */
public class LinkedDataFuSpider extends Artifact {

	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";

	private Program program;

	private BindingConsumerCollection triples;

	private Timer timer;

	public LinkedDataFuSpider() {
	  // set logging level to warning
    Logger log = Logger.getLogger("edu.kit.aifb.datafu");
    log.setLevel(Level.WARNING);
    LogManager.getLogManager().addLogger(log);
  }

	public void init(String programFile) {
		try {
			InputStream is = new FileInputStream(programFile);
			Origin base = new FileOrigin(new File(programFile), FileOrigin.Mode.READ,  null);
			Notation3Parser n3Parser = new Notation3Parser(is);
			ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(base);

			n3Parser.parse(programConsumer, base);
			is.close();

			program = programConsumer.getProgram(base);

			QueryConsumerImpl queryConsumer = new QueryConsumerImpl(base);
			SparqlParser sparqlParser = new SparqlParser(new StringReader(COLLECT_QUERY));
			sparqlParser.parse(queryConsumer, new InternalOrigin(""));

			ConstructQuery query = queryConsumer.getConstructQueries().iterator().next();

			triples = new BindingConsumerCollection();
			program.registerConstructQuery(query, new BindingConsumerSink(triples));

			// TODO recurrent polling with computation of +/- delta?
			//timer = new Timer();
			//timer.schedule(new TimerTask() { @Override public void run() { crawl(); } }, 0, 10000);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO report error

			program = null;
		}
	}

	@OPERATION
	public void isPresent(String st1, String st2, String st3, OpFeedbackParam<Boolean> result) {
		result.set(hasObsPropertyByTemplate​("rdf",st1,st2,st3));
	}

	@OPERATION
	public void update(String st1, String st2, String st3, int index, String st4) {
		ObsProperty op = getObsPropertyByTemplate​("rdf",st1,st2,st3);
		op.updateValue(index,st4);
	}

	/**
	 * executes the Linked Data program and notifies agent with collected triples.
	 */
	@OPERATION
	public void crawl(String originURI) {
		if (program == null) return;

		EvaluateProgramConfig config = new EvaluateProgramConfig();
		//config.setThreadingModel(EvaluateProgramConfig.ThreadingModel.SERIAL);
		EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
		eval.start();

		try {
      eval.getInputOriginConsumer().consume(new RequestOrigin(new URI(originURI), Request.Method.GET));

			eval.awaitIdleAndFinish();
			eval.shutdown();

			for (Binding binding : triples.getCollection()) {
				Node[] st = binding.getNodes().getNodeArray();
        defineObsProperty("rdf", st[0].getLabel(), st[1].getLabel(), st[2].getLabel());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
		  // TODO throw it to make operation fail?
      e.printStackTrace();
    }
	}

}
