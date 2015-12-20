import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.engines.ElasticNetLinearPegasosEngine;
import pt.utl.ist.online.learning.engines.LinearOnlineEngine;
import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashMap;
import pt.utl.ist.online.learning.utils.MemoryEfficientHashSet;

import edu.columbia.cs.ltrie.active.learning.ActiveLearningOnlineModel;
import edu.columbia.cs.ltrie.active.learning.classifier.util.Combiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.MaxCombiner;
import edu.columbia.cs.ltrie.active.learning.classifier.util.impl.SumCombiner;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.AllFieldsTermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.MatchesQueryFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;
import edu.columbia.cs.ltrie.online.svm.OnlineRankingModel;
import edu.columbia.cs.ltrie.sampling.ExplicitSamplingTechnique;
import edu.columbia.cs.ltrie.sampling.SamplingTechnique;
import edu.columbia.cs.ltrie.utils.SerializationHelper;


public class ComputeInitialScores {
	public static void main(String[] args) throws Exception {
		String path = "/home/goncalo/NYTValidationSplit/";
		String root = "../LearningToRankForIE/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";
		String[] relationships = new String[]{"ManMadeDisaster", "VotingResult", "OrgAff", "Outbreaks", "Indictment-Arrest-Trial"};
		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;
		int[] splits = new int[]{1,2};
		String comb= "Sum";
		boolean independent = true;
		int numEpochs= 5;
		String reg = "L1";
		double lambda = 0.5;
		boolean termsAreQueries= true;

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = conn.getAllFiles();

		for(String relationship : relationships){
			for(int split : splits){
				Map<String,Float> scoresCollectionRSVM= getInitialScoresRSVM(conn, new HashSet<String>(collectionFixed), split, relationship, root, numQueries, extractor, subPaths, folderDesign, path);
				Map<String,Float> scoresCollectionQBC= getInitialScoresQBC(conn, new HashSet<String>(collectionFixed), split, relationship, root, numQueries, extractor, subPaths, folderDesign, path,
						comb, independent, numEpochs, reg, lambda, termsAreQueries);

				String output = "precomputedScores";
				File outputDir = new File(output);
				if(!outputDir.exists()){
					outputDir.mkdir();
				}
				File splitDir = new File(output + "/" + split);
				if(!splitDir.exists()){
					splitDir.mkdir();
				}
				File relDir = new File(output + "/" + split + "/" + relationship);
				if(!relDir.exists()){
					relDir.mkdir();
				}
				SerializationHelper.write(relDir.getAbsolutePath() + "/RSVM.bin", scoresCollectionRSVM);
				SerializationHelper.write(relDir.getAbsolutePath() + "/QBC.bin", scoresCollectionQBC);
			}
		}
	}

	//private static Map<String,Integer> previouslyUsedValues = new MemoryEfficientHashSet<String>();
	private static Map<Query,Integer> previouslySeenValues = new MemoryEfficientHashMap<Query,Integer>();

	private static void addTupleFeatures(QueryParser p, List<Tuple> tuples) throws ParseException, IOException {
		int tuplesSize= tuples.size();
		Set<String> seenInThisDocument = new HashSet<String>();
		for (int i = 0; i < tuplesSize; i++) {
			Tuple t = tuples.get(i);

			Set<String> fields = t.getFieldNames();
			for (String field : fields) {
				String val = t.getData(field).getValue();
				String quer = "+\"" + QueryParser.escape(val) + "\"";
				Query q = p.parse(quer);
				Set<Term> terms = new HashSet<Term>();
				q.extractTerms(terms);
				if(terms.size()>1){
					String qToString = q.toString();

					if(!seenInThisDocument.contains(qToString)){
						Integer freq = previouslySeenValues.get(qToString);
						if(freq==null){
							freq=0;
						}
						previouslySeenValues.put(q, freq+1);
					}
					seenInThisDocument.add(qToString);

					/*if(previouslySeenValues.contains(qToString)  && !seenInThisDocument.contains(qToString) && !previouslyUsedValues.contains(qToString)){
								coord.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
								previouslyUsedValues.add(qToString);
								//System.out.println((++numQueries) + "/" + previouslySeenValues.size() + " " + qToString);
							}
							previouslySeenValues.add(qToString);
							seenInThisDocument.add(qToString);*/
				}
			}
		}
	}

	private static Set<String> previouslyUsedValues = new MemoryEfficientHashSet<String>();

	private static void submitTopTuples(IndexConnector conn, FeaturesCoordinator coord, int numNewQueries)
			throws IOException{
		List<Entry<Query,Integer>> frequencies = new ArrayList<Entry<Query,Integer>>(previouslySeenValues.entrySet());
		Collections.sort(frequencies, new Comparator<Entry<Query,Integer>>(){

			@Override
			public int compare(Entry<Query, Integer> arg0,
					Entry<Query, Integer> arg1) {
				return arg1.getValue()-arg0.getValue();
			}
		});

		int i=0;
		int submittedQueries=0;
		while(submittedQueries<numNewQueries && i<frequencies.size()){
			Query q = frequencies.get(i).getKey();
			String qToString = q.toString();
			if(!previouslyUsedValues.contains(qToString)){
				coord.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
				previouslyUsedValues.add(qToString);
				submittedQueries++;
			}

			i++;
		}
	}

	private static List<Query> loadQueries(QueryParser qp, String queryFile, int numQueries) throws ParseException, IOException {
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
		return words;
	}

	private static Map<String,Float> getInitialScoresRSVM(IndexConnector conn, Set<String> collection, int split,
			String relationship, String root, int numQueries, String extractor, String[] subPaths, String folderDesign, String path) throws ParseException, IOException, ClassNotFoundException, InvalidVectorIndexException{
		String[] fieldsVector = new String[]{NYTDocumentWithFields.TITLE_FIELD,
				NYTDocumentWithFields.LEAD_FIELD,NYTDocumentWithFields.BODY_FIELD};
		QueryParser qp = new MultiFieldQueryParser(
				Version.LUCENE_41, 
				fieldsVector,
				new StandardAnalyzer(Version.LUCENE_41));
		String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval
		String extr;
		String initialQueriesPath;
		if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
			extr = relationship;
			initialQueriesPath = root + "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + relationship + "-" + split;
		}else{
			extr = "SSK-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
			initialQueriesPath = root + "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
		}
		List<Query> initialQueries = loadQueries(qp, initialQueriesPath,numQueries);

		String resultsPath = root + "results" + relationship;
		System.out.println("Initiating IE programs");
		CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
		for(String subPath : subPaths){
			if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
			}else{
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
			}
		}

		System.out.println("Preparing feature extractors");
		FeaturesCoordinator coordinator = new FeaturesCoordinator();
		Set<String> fields = new HashSet<String>();
		for(String field : fieldsVector){
			fields.add(field);
		}
		coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD,false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
		for(Query q : initialQueries){
			coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
		}

		System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");

		SamplingTechnique sampler;
		String[] documents = new String[2];
		documents[0] = String.format(folderDesign, split*2-1);
		documents[1] = String.format(folderDesign, split*2);
		sampler = new ExplicitSamplingTechnique(path, documents);

		List<String> sample = sampler.getSample();
		System.out.println("Extracting information from the sample");
		List<String> relevantDocs = new ArrayList<String>();
		List<String> docs = new ArrayList<String>();
		Set<String> usefulSample = new HashSet<String>();
		for(String doc : sample){
			List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
			if(tuples.size()!=0){
				relevantDocs.add(doc);
				usefulSample.add(doc);
			}
			docs.add(doc);
			addTupleFeatures(qp, tuples);
		}
		double frac = (double)relevantDocs.size()/(double)docs.size();
		System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
		System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

		System.out.println("Initial training of the ranking model");
		LinearOnlineEngine<Long> engine = new ElasticNetLinearPegasosEngine<Long>(0.1,0.99, 1, false);
		//LinearOnlineEngine<Long> engine = new LinearPegasosEngine<Long>(0.1, 1.0,false);
		OnlineRankingModel model = new OnlineRankingModel(coordinator, docs, relevantDocs, engine, 10000);

		Map<Query,Double> queryScores = model.getQueryScores();
		Map<String,Float> scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), collection);

		return scoresCollection;
	}

	private static Map<String,Float> getInitialScoresQBC(IndexConnector conn, Set<String> collection, int split,
			String relationship, String root, int numQueries, String extractor, String[] subPaths, String folderDesign, String path,
			String comb, boolean independent, int numEpochs, String reg, double lambda,
			boolean termsAreQueries) throws Exception{
		String[] fieldsVector = new String[]{NYTDocumentWithFields.TITLE_FIELD,
				NYTDocumentWithFields.LEAD_FIELD,NYTDocumentWithFields.BODY_FIELD};
		QueryParser qp = new MultiFieldQueryParser(
				Version.LUCENE_41, 
				fieldsVector,
				new StandardAnalyzer(Version.LUCENE_41));
		String featSel = "ChiSquaredWithYatesCorrectionAttributeEval"; //InfoGainAttributeEval
		String extr;
		String initialQueriesPath;
		if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
			extr = relationship;
			initialQueriesPath = root + "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + relationship + "-" + split;
		}else{
			extr = "SSK-"+relationship+"-SF-"+(relationship.equals("ManMadeDisaster")? "HMM":"CRF")+"-relationWords_Ranker_";
			initialQueriesPath = root + "QUERIES/" + relationship + "/" + true + "/SelectedAttributes/" + extr + featSel + "_"+split+"_5000.words";
		}
		List<Query> initialQueries = loadQueries(qp, initialQueriesPath,numQueries);

		String resultsPath = root + "results" + relationship;
		System.out.println("Initiating IE programs");
		CompressedAdditiveFileSystemWrapping extractWrapper = new CompressedAdditiveFileSystemWrapping();
		for(String subPath : subPaths){
			if(relationship.equals("OrgAff") || relationship.equals("Outbreaks")){
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationship + ".data");
			}else{
				extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationship + ".data");
			}
		}

		System.out.println("Preparing feature extractors");
		FeaturesCoordinator coordinator = new FeaturesCoordinator();
		Set<String> fields = new HashSet<String>();
		for(String field : fieldsVector){
			fields.add(field);
		}
		coordinator.addFeatureExtractor(new AllFieldsTermFrequencyFeatureExtractor(conn, fields, false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.TITLE_FIELD,false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.LEAD_FIELD,false,false,true));
		//coordinator.addFeatureExtractor(new TermFrequencyFeatureExtractor(conn, NYTDocumentWithFields.BODY_FIELD,false,false,true));
		for(Query q : initialQueries){
			coordinator.addFeatureExtractor(new MatchesQueryFeatureExtractor(conn, q));
		}

		System.out.println("Obtaining initial sample (to use Pablo's sampling techniques)");

		SamplingTechnique sampler;
		String[] documents = new String[2];
		documents[0] = String.format(folderDesign, split*2-1);
		documents[1] = String.format(folderDesign, split*2);
		sampler = new ExplicitSamplingTechnique(path, documents);

		List<String> sample = sampler.getSample();
		System.out.println("Extracting information from the sample");
		List<String> relevantDocs = new ArrayList<String>();
		List<String> docs = new ArrayList<String>();
		Set<String> usefulSample = new HashSet<String>();
		for(String doc : sample){
			List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
			if(tuples.size()!=0){
				relevantDocs.add(doc);
				usefulSample.add(doc);
			}
			docs.add(doc);
			addTupleFeatures(qp, tuples);
		}
		double frac = (double)relevantDocs.size()/(double)docs.size();
		System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
		System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

		System.out.println("Initial training of the ranking model");
		Combiner combi =  comb.equals("Max")? new MaxCombiner() : new SumCombiner();
		ActiveLearningOnlineModel model = new ActiveLearningOnlineModel(docs,relevantDocs,coordinator,numEpochs,combi,reg,lambda,true, termsAreQueries);

		Map<String,Double> scoresCollection = model.getScores(collection,conn,independent);
		Map<String,Float> result = new HashMap<String, Float>();

		for(Entry<String,Double> entry : scoresCollection.entrySet()){
			result.put(entry.getKey(), entry.getValue().floatValue());
		}

		return result;
	}
}
