import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import pt.utl.ist.online.learning.utils.Pair;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.FromWeightedFileInitialWordLoader;
import edu.columbia.cs.ltrie.baseline.factcrawl.initial.wordLoader.InitialWordLoader;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.extractor.wrapping.impl.CompressedAdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.features.AllFieldsTermFrequencyFeatureExtractor;
import edu.columbia.cs.ltrie.features.FeaturesCoordinator;
import edu.columbia.cs.ltrie.features.MatchesQueryFeatureExtractor;
import edu.columbia.cs.ltrie.indexing.IndexConnector;
import edu.columbia.cs.ltrie.indexing.SimpleBooleanSimilarity;
import edu.columbia.cs.ltrie.online.svm.OnlineRankingModel;
import edu.columbia.cs.ltrie.sampling.ExplicitSamplingTechnique;
import edu.columbia.cs.ltrie.sampling.SamplingTechnique;


public class PredictRankingRecallWallenius {
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, InvalidVectorIndexException {
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
		String relationship = "VotingResult";
		int docsPerQuerySample = 10;
		int numQueries = 50;
		int sampleSize = 2000;
		int split = 5;

		System.out.println("Indexing collection (to do offline)");
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
		Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationNewIndex"));
		IndexConnector conn = new IndexConnector(analyzer, directory, "");
		Set<String> collectionFixed = conn.getAllFiles();

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

		Set<String> collection=new HashSet<String>(collectionFixed);
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
		System.out.println("\tThe sample contains " + sample.size() + " documents.");
		collection.removeAll(sample);
		System.out.println("\tThe collection without the sample contains " + collection.size() + " documents.");

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
		Map<String,Float> scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), new HashSet<String>(sample));
		
		collection.removeAll(sample);
		scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), collection);
		Map<String,Integer> relevance = new HashMap<String,Integer>();
		usefulSample = new HashSet<String>();
		int numRelElements = 0;
		int numNRelElements = 0;
		for(String doc : collection){
			List<Tuple> tuples = extractWrapper.getTuplesDocument(doc);
			if(tuples.size()!=0){
				relevance.put(doc, tuples.size());
				numRelElements++;
				usefulSample.add(doc);
			}else{
				numNRelElements++;
			}
		}
		
		System.out.println("This collection has " + numRelElements + " useful elements and " + numNRelElements + " useless elements.");
		
		List<String> sortedCollection = sortCollection(scoresCollection);
		List<String> secondSample = sortedCollection.subList(0, 1000);
		RankingRecallEstimator estimator = new RankingRecallEstimator(scoresCollection, usefulSample, new AverageBasedSampleParameterEstimator(secondSample,frac,sortedCollection.size()));		
		
		SortedCurve curve = new SortedCurve(collection.size(), sortCollection(scoresCollection), relevance);
		edu.columbia.cs.utils.Pair<double[],double[]> realRecallCurve = curve.getCurveRetrieval();
		RankingMethodCurve realCurve = new ExplicitCurve(realRecallCurve.first(), realRecallCurve.second());
		
		double[] obtainedRecall = realRecallCurve.second();
		double[] xAxis = new double[obtainedRecall.length];
		double[] yAxis = new double[obtainedRecall.length];
		for(int i=0; i<obtainedRecall.length; i++){
			xAxis[i]=(double)i/collection.size();
			yAxis[i]=estimator.getRecallNewtonMethod(i, collection.size(), 100);
			//System.out.println(xAxis[i] + " " + yAxis[i]);
		}
		RankingMethodCurve predictedCurve = new ExplicitCurve(xAxis, yAxis);
		
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Real", realCurve);
		gen.addRankingCurve("Predicted", predictedCurve);
		gen.generateR("test.png", 50, true);
		
		/*double point = 0.1;
		System.out.println("The secondSample has " + usefulSample.size() + " useful documents");
		System.out.println(getNumNecessaryDocuments(0.0, w, (int) Math.floor(secondSample.size()*frac), secondSample.size()));
		System.out.println(getNumNecessaryDocuments(point, w, (int) Math.floor(secondSample.size()*frac), secondSample.size()));
		System.out.println(getNumNecessaryDocuments(1.0, w, (int) Math.floor(secondSample.size()*frac), secondSample.size()));
		List<String> rankedSample = sortCollection(scoresCollection);
		int usefulDocumentsFound = 0;
		for(int i=0; i<rankedSample.size(); i++){
			String currentDocument = rankedSample.get(i);
			if(relevance.containsKey(currentDocument)){
				usefulDocumentsFound++;
			}
		}*/
	}
	
	private static List<String> sortCollection(Map<String,Float> scoresCollection){
		final Map<String,Float> scores = scoresCollection;
		List<String> collection = new ArrayList<String>(scoresCollection.keySet());
		Collections.sort(collection, new Comparator<String>() {
			@Override
			public int compare(String doc1, String doc2) {
				Float score2 = scores.get(doc2);
				if(score2==null){
					score2=0.0f;
				}
				Float score1 = scores.get(doc1);
				if(score1==null){
					score1=0.0f;
				}
				return (int) Math.signum(score2-score1);
			}
		});
		return collection;
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

	private static List<Pair<String,String>> getTopKQueries(OnlineRankingModel model,
			FeaturesCoordinator coordinator, int k) {
		Map<Long,Double> weightVector = model.getWeightVector();
		System.out.println(weightVector.size() + " features.");
		double rho = model.getRho();
		Map<Pair<String,String>, Double> termWeights = new HashMap<Pair<String,String>, Double>();
		for(Entry<Long,Double> entry : weightVector.entrySet()){
			Pair<String,String> term = coordinator.getTerm(entry.getKey());
			if(term!=null && entry.getValue()>rho){
				termWeights.put(term, entry.getValue());
			}
		}

		final Map<Pair<String,String>,Double> scores = termWeights;
		List<Pair<String,String>> queries = new ArrayList<Pair<String,String>>(termWeights.keySet());
		Collections.sort(queries, new Comparator<Pair<String,String>>() {
			@Override
			public int compare(Pair<String, String> o1, Pair<String, String> o2) {
				return (int) Math.signum(scores.get(o2)-scores.get(o1));
			}
		});

		List<Pair<String,String>> results = new ArrayList<Pair<String,String>>();
		int queriesSize=queries.size();
		for(int i=0; i<Math.min(queriesSize, k); i++){
			Pair<String,String> query = queries.get(i);
			results.add(query);
		}

		return results;
	}
}
