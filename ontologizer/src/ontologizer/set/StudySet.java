package ontologizer.set;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.ItemAssociations;
import ontologizer.enumeration.TermAnnotations;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.filter.GeneFilter;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Ontology.ITermIDVisitor;
import ontologizer.ontology.TermID;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.sampling.StudySetSampler;
import ontologizer.types.ByteString;

/**
 * This class holds all gene names of a study and their associated
 * (optional) descriptions. The names are extracted directly by the
 * constructor given study file.
 *
 * The class implements the Iterable interface so you can
 * conveniently iterate over all includes gene names.
 *
 * @author Peter Robinson, Sebastian Bauer
 */
public class StudySet implements Iterable<ByteString>
{
	private static Logger logger = Logger.getLogger(StudySet.class.getName());

	/**
	 * HashMap containing the names of genes (or gene products) of the study
	 * and their optional description.
	 */
	private HashMap<ByteString, ItemAttribute> gene2Attribute = new HashMap<ByteString,ItemAttribute>();

	/**
	 * List containing genes which are not annotated
	 */
	private LinkedList<ByteString> unannotatedGeneNames = new LinkedList<ByteString>();

	/** The name of the study set */
	private String name;

	/** Cached TermEnumerator */
	private TermEnumerator termEnumerator;

	/** The current random ID. Used for unique study set names */
	private int randomID = 0;

	/**
	 * Construct an empty study set with the given name.
	 *
	 * @param name specifies the name of the studyset
	 *
	 */
	public StudySet(String name)
	{
		this.name = name;
	}


	/**
	 * Constructs a empty study set with a generated unique name.
	 */
	public StudySet()
	{
		name = generateUniqueName();
	}

	/**
	 * Obtain the number of genes or gene products within this studyset.
	 *
	 * @return the desired count.
	 */
	public int getGeneCount()
	{
		return gene2Attribute.size();
	}

	/**
	 * @return name of the Study
	 */
	public String getName()
	{
		return name;
	}

	/* for debugging */
	public String toString()
	{
		String str = name + " (n=" + (getGeneCount()) + ")";
		return str;
	}

	/**
	 * The iterator over gene names considered within this study
	 */
	public Iterator<ByteString> iterator()
	{
		return gene2Attribute.keySet().iterator();
	}

	/**
	 * @return the genes in order of the iterator as an array.
	 */
	public ByteString [] getGenes()
	{
		ByteString [] genes = new ByteString[gene2Attribute.size()];
		int i=0;
		for (ByteString gene : this)
			genes[i++]=gene;
		return genes;
	}

	/**
	 * Returns the associated attribute (i.e., description) of a gene within
	 * a study set.
	 *
	 * @param name the name of the gene whose description should be returned
	 * @return the description
	 */
	public ItemAttribute getItemAttribute(ByteString name)
	{
		return gene2Attribute.get(name);
	}

	/**
	 * Returns the gene description of the specified gene.
	 *
	 * @return the gene description or null if the gene is not contained.
	 */
	public String getGeneDescription(ByteString name)
	{
		ItemAttribute attr = gene2Attribute.get(name);
		if (attr == null) return "";
		if (attr.description == null) return "";
		return attr.description;
	}

	/**
	 * After all the calculation has been done, you should
	 * call this method in order to allow the garbage
	 * collector to free the enumerator and counter associated
	 * memory.
	 *
	 * TODO: Rename
	 */
	public void resetCounterAndEnumerator()
	{
		termEnumerator = null;
	}

	/**
	 * Add an additional gene to the study set.
	 *
	 * Note that after a gene has been added, countGOTerms() should
	 * be called again.
	 *
	 * @param geneName the name of the gene to be added
	 * @param description a description associated to the gene
	 */
	public void addGene(ByteString geneName, String description)
	{
		ItemAttribute attr = new ItemAttribute();
		attr.description = description;

		gene2Attribute.put(geneName,attr);

		resetCounterAndEnumerator();
	}

	/**
	 * Add an additional gene to the study set.
	 *
	 * @param geneName the name of gene to be added
	 * @param attribute the attribute
	 */
	public void addGene(ByteString geneName, ItemAttribute attribute)
	{
		gene2Attribute.put(geneName, attribute);
		resetCounterAndEnumerator();
	}

	/**
	 * Checks whether study set contains the given gene.
	 *
	 * @param geneName
	 * 		  the name of the gene which inclusion should be checked.
	 * @return true if study contains gene, else false.
	 */
	public boolean contains(ByteString geneName)
	{
		return gene2Attribute.containsKey(geneName);
	}

	/**
	 * Returns a hashset containing all the gene names represented
	 * by this study set. Note that the returned hashset is decoupled
	 * from the study set, so any change within the hashset doesn't
	 * have any effect on the study set's genes.
	 *
	 * @return the hash set
	 */
	public HashSet<ByteString> getAllGeneNames()
	{
		HashSet<ByteString> geneSet = new HashSet<ByteString>();
		for (ByteString term : this)
			geneSet.add(term);
		return geneSet;
	}

	/**
	 * Some datasets contain gene entries which refer to the same gene
	 * but differ in their name (i.e. synonyms). This method filters
	 * them out by using the association container.
	 *
	 * @param associationContainer the association container the defines
	 */
	public StudySet filterOutDuplicateGenes(AssociationContainer associationContainer)
	{
		/* This will be filled with unique genes */
		HashMap<ByteString,ItemAttribute> uniqueGenes = new HashMap<ByteString,ItemAttribute>();

		for (ByteString geneName : gene2Attribute.keySet())
		{
			ItemAssociations gene2Association = associationContainer.get(geneName);
			if (gene2Association != null)
			{
				boolean add;
				ItemAttribute desc = uniqueGenes.get(gene2Association.name());

				if (!(add = (desc == null)))
				{
					ItemAttribute current = gene2Attribute.get(geneName);
					if (current != null)
					{
						add = desc.prefer(current);
					} else add = true;
				}

				if (add)
					uniqueGenes.put(gene2Association.name(),desc);
			} else
			{
				/* We don't want to filter out genes without an association here */
				uniqueGenes.put(geneName,gene2Attribute.get(geneName));
			}
		}

		if (uniqueGenes.size() != gene2Attribute.size())
		{
			logger.log(Level.INFO, (gene2Attribute.size() - uniqueGenes.size()) + " duplicate gene entries have been filtered out");
			gene2Attribute = uniqueGenes;
		}

		/* Reset counter and enumerator */
		this.resetCounterAndEnumerator();
		return this;
	}

	/**
	 * Filters out genes which don't contain an association
	 *
	 * @param associationContainer the association container that is used to determine
	 *  of a gene has associations or not.
	 * @return this studyset for convenience
	 */
	public StudySet filterOutAssociationlessGenes(AssociationContainer associationContainer)
	{
		int numObjectSymbol = 0;
		int numObjectID = 0;
		int numSynonyms = 0;

		/* Iterate over all gene names and put those who doesn't have an association
		 * into the unannotatedGeneNames list */
		for (ByteString geneName : gene2Attribute.keySet())
		{
			ItemAssociations gene2Association = associationContainer.get(geneName);
			if (gene2Association == null)
				unannotatedGeneNames.add(geneName);
			else
			{
				if (associationContainer.isObjectSymbol(geneName)) numObjectSymbol++;
				else if (associationContainer.isObjectID(geneName)) numObjectID++;
				else if (associationContainer.isSynonym(geneName)) numSynonyms++;
			}
		}

		/* Now remove them really (we can't do this in the former loop, because
		 * it is unclear whether this will conflict with the iterating) */
		for (ByteString geneName : unannotatedGeneNames)
			gene2Attribute.remove(geneName);

		logger.log(Level.INFO, unannotatedGeneNames.size() + " genes of " + getName() + " without any association have been filtered out. Now there are " + gene2Attribute.size() + " genes, of which " +
				   numObjectSymbol + " can be resolved using Object Symbols, " + numObjectID + " using Object IDs, and " + numSynonyms + " using Synonyms.");

		/* Reset counter and enumerator */
		this.resetCounterAndEnumerator();

		return this;
	}

	public TermEnumerator enumerateTerms(Ontology graph, AssociationContainer associationContainer)
	{
		return enumerateTerms(graph, associationContainer, null);
	}

	public TermEnumerator enumerateTerms(Ontology graph, AssociationContainer associationContainer, Set<ByteString> evidences)
	{
		return enumerateTerms(graph, associationContainer, evidences, null);
	}

	/**
	 * Enumerate genes annotated for every term. Multiple
	 * calls to this method are fast, if the gene set has not
	 * been changed in-between.
	 *
	 * @param graph defines the ontology
	 * @param associationContainer defines the associations
	 * @param evidences which type of annotation to respect
	 * @param remover an instance to decide whether a given term
	 *  is removed from the annotation set.
	 * @return the term enumerator
	 */
	public synchronized TermEnumerator enumerateTerms(Ontology graph, AssociationContainer associationContainer, Set<ByteString> evidences, TermEnumerator.IRemover remover)
	{
		/* Return cached enumerator if available */
		if (termEnumerator != null) return termEnumerator;

		termEnumerator =  new TermEnumerator(graph);

		/* Iterate over all gene names and add their annotations to the goTermCounter */
		for (ByteString geneName : gene2Attribute.keySet())
		{
			ItemAssociations geneAssociations = associationContainer.get(geneName);
			if (geneAssociations != null)
				termEnumerator.push(geneAssociations,evidences);
		}

		if (remover != null)
			termEnumerator.removeTerms(remover);
		return termEnumerator;
	}

	/**
	 * @return an unique name derived from the study sets' name
	 */
	private String generateUniqueName()
	{
		String name = getName() + "-random-" + randomID;
		randomID++;
		return name;
	}

	/**
	 * Generate a studyset which contains desiredSize random
	 * selected genes of the population.
	 *
	 * @param desiredSize specifies the desired size of
	 *        the studyset.
	 *
	 * @return the generated random studyset.
	 */
	public StudySet generateRandomStudySet(int desiredSize)
	{
		StudySetSampler sampler = new StudySetSampler(this);

		return sampler.sampleRandomStudySet(desiredSize);
	}

	public void setName(String newStudySetName)
	{
		name = newStudySetName;
	}

	/**
	 * Write minimum subsumer matrix.
	 *
	 * @param graph
	 * @param associations
	 * @param file
	 */
	public void writeMinimumSubsumerMatrix(final Ontology graph,  AssociationContainer associations, File file)
	{
		TermEnumerator enumerator = termEnumerator;

		/* If terms weren't already annotated do it now, but
		 * remove the local reference then
		 */
		if (enumerator == null)
		{
			enumerator = enumerateTerms(graph,associations);
			termEnumerator = null;
		}

		class ParentFetcher implements ITermIDVisitor
		{
			private HashSet<TermID> set = new HashSet<TermID>();


			public boolean visited(TermID tid)
			{

//	TODO:
//				if (goTermID.equals(graph.getBpTerm().getID())) return;
//				if (goTermID.equals(graph.getMfTerm().getID())) return;
//				if (goTermID.equals(graph.getCcTerm().getID())) return;
				if (!graph.isRootTerm(tid))
					set.add(tid);

				return true;
			}

			public HashSet<TermID> getSet()
			{
				return set;
			}
		};

		ArrayList<TermID> terms = new ArrayList<TermID>();
		for (TermID t : enumerator)
		{
			if (!graph.isRootTerm(t))
				terms.add(t);
		}

		int totalTerms = terms.size();
		int [][] matrix = new int[totalTerms][totalTerms];

		for (int i=0;i<totalTerms;i++)
		{
			for (int j=0;j<totalTerms;j++)
			{
				TermID ti = terms.get(i);
				TermID tj = terms.get(j);

				ParentFetcher pi = new ParentFetcher();
				ParentFetcher pj = new ParentFetcher();

				graph.walkToSource(ti,pi);
				graph.walkToSource(tj,pj);

				HashSet<TermID> sharedParents = new HashSet<TermID>();
				for (TermID t : pi.getSet())
				{
					if (pj.getSet().contains(t))
						sharedParents.add(t);
				}

				for (TermID t : pj.getSet())
				{
					if (pi.getSet().contains(t))
						sharedParents.add(t);
				}

				int min = Integer.MAX_VALUE;

				for (TermID t : sharedParents)
				{
					int c = enumerator.getAnnotatedGenes(t).totalAnnotatedCount();
					if (c < min) min = c;
				}

				matrix[i][j] = min;
			}
		}

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			out.write("GOID");
			for (int i=0;i<terms.size();i++)
			{
				out.write("\t");
				out.write(terms.get(i).toString());
			}

			for (int i=0;i<matrix.length;i++)
			{
				out.write(terms.get(i).toString());

				for (int j=0;j < matrix[i].length;j++)
				{
					out.write("\t");
					out.write(matrix[i][j] + "");
				}
				out.write("\n");
			}
			out.close();
		} catch (IOException e)
		{
		}

	}

	/**
	 * Write out a list of all terms and the names of the genes annotated to them.
	 *
	 * @param graph the ontology
	 * @param associations the associations
	 * @param file the file that should be written
	 */
	public void writeTermAnnotatedGenes(Ontology graph, AssociationContainer associations, File file)
	{
		TermEnumerator enumerator = termEnumerator;

		/* If terms weren't already annotated do it now, but
		 * remove the local reference then
		 */
		if (enumerator == null)
		{
			enumerator = enumerateTerms(graph,associations);
			termEnumerator = null;
		}

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			for (TermID id : enumerator)
			{
				out.write(id.toString());
				out.write('\t');
				out.write("genes={");
				TermAnnotations genes = enumerator.getAnnotatedGenes(id);
				boolean first = true;
				for (ByteString gene : genes.totalAnnotated)
				{
					if (!first) out.write(',');
					else first = false;
					out.write(gene.toString());
				}
				out.write("}");
			}
			out.flush();
			out.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param graph specifies the graph.
	 * @param associations specifies the associations
	 * @param file defines the file to which is written to
	 */
	public void writeSetWithAnnotations(Ontology graph, AssociationContainer associations, File file)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			for (ByteString gene : this)
			{
				/* gene name */
				out.write(gene.toString());
				out.write('\t');

				/* description */
				String desc = this.getGeneDescription(gene);
				if (desc != null)
					out.write(desc);
				out.write('\t');

				ItemAssociations geneAssociations = associations.get(gene);
				if (geneAssociations != null)
				{
					final HashSet<TermID> direct = new HashSet<TermID>();
					final HashSet<TermID> indirect = new HashSet<TermID>();

					/* direct annotations */
					boolean first = true;
					out.write("annotations={");
					for (Association assoc : geneAssociations)
					{
						if (first == false) out.write(',');
						else first = false;
						out.write(assoc.getTermID().toString());

						direct.add(assoc.getTermID());
					}
					out.write("}");

					/* indirect annotation are those located upstream (nearer to the root),
					 * i.e., those inferred by the propagation annotation rule  */
					graph.walkToSource(direct, new ITermIDVisitor()
					{
						public boolean visited(TermID tid)
						{
							if (!direct.contains(tid))
								indirect.add(tid);

							return true;
						}
					});

					out.write(" ancestors_annotations={");
					first = true;
					for (TermID t : indirect)
					{
						if (first == false) out.write(',');
						else first = false;
						out.write(t.toString());
					}
					out.write("}");
				}


				out.write('\n');
			}
			out.flush();
			out.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Applies the given filter to this study set. Only simple mapping
	 * is supported for now. Genes which have no entry withing the map
	 * will remain unaffected while genes which map to "-" will be
	 * discarded.
     *
	 * @param filter specifies the filter object to use
	 */
	public void applyFilter(GeneFilter filter)
	{
		int unmappedGenes = 0;
		int mappedGenes = 0;
		int discaredGenes = 0;

		resetCounterAndEnumerator();

		HashMap<ByteString, ItemAttribute> newGene2Attributes = new HashMap<ByteString,ItemAttribute>();

		for (Entry<ByteString,ItemAttribute> entry : gene2Attribute.entrySet())
		{
			ByteString newName = filter.mapGene(entry.getKey());

			/* If no mapping exists we'll accept the original gene name */
			if (newName == null)
			{
				newGene2Attributes.put(entry.getKey(),entry.getValue());
				unmappedGenes++;
			}
			else
			{
				/* Decide whether gene should be discarded or mapped */
				if (!newName.equals("-"))
				{
					/* It's possible that more than one gene map to another
					 * but we wouldn't like to loss the information.
					 * Therefore we merge the attributes */

					ItemAttribute attr = newGene2Attributes.get(newName);
					if (attr != null)
						attr = attr.merge(entry.getValue());
					else attr = entry.getValue();

					newGene2Attributes.put(newName,attr);
					mappedGenes++;
				} else discaredGenes++;
			}
		}

		System.err.println("In studyset " + getName() + " mapped " + mappedGenes + " to new genes, "
					+ unmappedGenes + " remained unaffected, "
					+ discaredGenes + " were discarded");

		gene2Attribute = newGene2Attributes;
	}

	public void removeGenes(Collection<ByteString> toBeRemoved)
	{
		for (ByteString g : toBeRemoved)
			gene2Attribute.remove(g);
	}

	public void addGenes(Collection<ByteString> toBeAdded)
	{
		for (ByteString g : toBeAdded)
			gene2Attribute.put(g,new ItemAttribute());
	}


	/**
	 * Adds the genes of the study set to this study set.
	 *
	 * @param studySet the study set from which the genes should be added.
	 * @return this study set for convenience
	 */
	public StudySet addGenes(StudySet studySet)
	{
		for (ByteString g : studySet)
			addGene(g, studySet.getItemAttribute(g));
		return studySet;
	}

	/**
	 * @return whether the given study set has only valued item attributes.
	 */
	public boolean hasOnlyValuedItemAttributes()
	{
		boolean hasOnlyValuedItemAttributes = true;

		for (ByteString gene : this)
		{
			ItemAttribute item = getItemAttribute(gene);
			if (!(item instanceof ValuedItemAttribute))
			{
				hasOnlyValuedItemAttributes = false;
				break;
			}
		}

		return hasOnlyValuedItemAttributes;
	}
}
