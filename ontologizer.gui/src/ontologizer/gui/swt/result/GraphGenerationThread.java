package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.gui.swt.support.IGraphGenerationSupport;
import ontologizer.gui.swt.support.NewGraphGenerationThread;
import ontologizer.io.dot.AbstractTermDotAttributesProvider;
import ontologizer.io.dot.OntologyDotWriter;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;

import org.eclipse.swt.widgets.Display;

/**
 * Generates the graph by executing DOT. When finished
 * the finished method of the specified constructor argument
 * is executed in the context of the GUI thread.
 *
 * @author Sebastian Bauer
 */
public class GraphGenerationThread extends NewGraphGenerationThread
{
	public Ontology go;
	public Term emanatingTerm;
	public HashSet<TermID> leafTerms = new HashSet<TermID>();
	public AbstractGOTermsResult result;

	private IGraphGenerationFinished finished;
	private AbstractTermDotAttributesProvider provider;

	public GraphGenerationThread(Display display, String dotCMDPath, IGraphGenerationFinished f, AbstractTermDotAttributesProvider p)
	{
		super(display, dotCMDPath);

		this.finished = f;
		this.provider = p;

		setSupport(new IGraphGenerationSupport()
		{
			public void writeDOT(File dotFile)
			{
				if (result != null)
				{
					result.writeDOT(go, dotFile,
						emanatingTerm != null ? emanatingTerm.getID() : null,
						leafTerms, provider);
				} else
				{
					OntologyDotWriter.writeDOT(go, dotFile,
						emanatingTerm != null ? emanatingTerm.getID() : null,
						leafTerms, provider);
				}
			}

			public void layoutFinished(boolean success, String msg,
					File pngFile, File dotFile)
			{
				finished.finished(success, msg, pngFile, dotFile);
			}
		});
	}
};
