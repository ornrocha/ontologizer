/*
 * Created on 08.02.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import ontologizer.association.AssociationContainer;
import ontologizer.io.dot.AbstractTermDotAttributesProvider;
import ontologizer.io.dot.OntologyDotWriter;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import sonumina.collections.ObjectIntHashMap;

/**
 * An abstraction of any result containing GO terms utilizing
 * AbstractGOTermProperties.
 *
 * @author Sebastian Bauer
 */
public class AbstractGOTermsResult implements Iterable<AbstractGOTermProperties>
{
	/** A linear list containing properties for go terms */
	protected ArrayList<AbstractGOTermProperties> list = new ArrayList<AbstractGOTermProperties>();

	/** Maps the go term to an integer (for accesses in constant time) */
	private ObjectIntHashMap<TermID> term2Index = new ObjectIntHashMap<TermID>();

	/** The current index for adding a new go term property */
	private int index = 0;

	/** The GO Graph */
	protected Ontology go;

	/** The association container */
	private AssociationContainer associations;

	/**
	 * Constructor. Needs to know the go structure as well as the associations of the genes.
	 *
	 * @param newGO
	 * @param newAssociations
	 */
	public AbstractGOTermsResult(Ontology newGO, AssociationContainer newAssociations)
	{
		this.go = newGO;
		this.associations = newAssociations;
	}

	/**
	 * Returns the iterator for receiving all go term properties.
	 */
	public Iterator<AbstractGOTermProperties> iterator()
	{
		return list.iterator();
	}

	/**
	 *
	 * @param prop
	 */
	public void addGOTermProperties(AbstractGOTermProperties prop)
	{
		if (prop.term == null)
			throw new IllegalArgumentException("prop.term mustn't be null");

		list.add(prop);
		term2Index.put(prop.term, index);
		index++;
	}

	/**
	 * Return the result term properties for the given term.
	 *
	 * @param termId the of the interesting term
	 * @return the result term's result properties
	 */
	public AbstractGOTermProperties getGOTermProperties(TermID termId)
	{
		int idx = term2Index.getIfAbsent(termId, -1);
		if (idx == -1) return null;
		return list.get(idx);
	}

	/**
	 * Return the result term properties for the given term.
	 *
	 * @param term
	 * @return the
	 */
	public AbstractGOTermProperties getGOTermProperties(Term term)
	{
		return getGOTermProperties(term.getID());
	}

	/**
	 * @return the associated associations
	 */
	public AssociationContainer getAssociations()
	{
		return associations;
	}

	/**
	 * @return the size of the result list, i.e., through how many
	 * elements you can iterate
	 */
	public int getSize()
	{
		return list.size();
	}

	/**
	 * @return the associated ontology.
	 */
	public Ontology getGO()
	{
		return go;
	}

	/**
	 * Writes out a basic dot file which can be used within graphviz. All terms
	 * of the terms parameter are included in the graph if they are within the
	 * sub graph originating at the rootTerm. In other words, all nodes
	 * representing the specified terms up to the given rootTerm node are
	 * included.
	 *
	 * @param graph the underlaying go graph.
	 * @param file
	 * 			defines the file in which the output is written to.
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 * @param provider
	 *          should provide for every property an appropiate id.
	 */
	public void writeDOT(Ontology graph, File file, TermID rootTerm, HashSet<TermID> terms, AbstractTermDotAttributesProvider provider)
	{
		/* FIXME: graph really necessary? (we have getGO()) */
		if (list.isEmpty())
			return;

		OntologyDotWriter.writeDOT(graph, file, rootTerm, terms, provider);
	}
}
