package MixtureModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
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
import edu.columbia.cs.utils.Pair;

public class FindThetaTests {
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, InvalidVectorIndexException, WriteException {
		
		System.setErr(new PrintStream(new File("out.RSVM")));
		
		String path = "/local/pjbarrio/Files/Downloads/NYTValidationSplit/";
		String root = "../LearningToRankForIE/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		String folderDesign = "%0" + String.valueOf(numPaths).length() + "d";
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format(folderDesign, i);
		}
		String extractor = "Pablo-Sub-sequences";
		String output = "DocumentScoresAnalysis.xls";
		File file = new File(output);
		WorkbookSettings wbSettings = new WorkbookSettings();
		wbSettings.setLocale(new Locale("en", "EN"));
		WritableWorkbook workbook = Workbook.createWorkbook(file, wbSettings);
		int currentSheet=0;
		for(int split : new int[]{1,2}){
			for(String relationship : new String[]{"Indictment-Arrest-Trial", "VotingResult", "PersonCareer","NaturalDisaster", "ManMadeDisaster","OrgAff","Outbreaks"}){
				int docsPerQuerySample = 10;
				int numQueries = 50;
				int sampleSize = 2000;

				System.out.println("Indexing collection (to do offline)");
				Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
				//Directory directory = new SimpleFSDirectory(new File("/home/goncalo/NYTValidationRealIndex"));
				Directory directory = new SimpleFSDirectory(new File("/proj/db-files2/NoBackup/pjbarrio/Dataset/indexes/NYTValidationNewIndex"));
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
					if (relationship.equals("Outbreaks")){
						initialQueriesPath = root + "QUERIES/diseaseOutbreak-true-" + split;
					}else{
						initialQueriesPath = root + "QUERIES/orgAff-" + true + "-" + split;
					}

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
				double fracUseful = (double)relevantDocs.size()/(double)docs.size();
				System.out.println("\tThere are " +relevantDocs.size() +" relevant documents in the sample.");
				System.out.println("\tThere are " +(docs.size()-relevantDocs.size()) +" non relevant documents in the sample.");

				System.out.println("Initial training of the ranking model");
				LinearOnlineEngine<Long> engine = new ElasticNetLinearPegasosEngine<Long>(0.1,0.99, 1, false);
				//LinearOnlineEngine<Long> engine = new LinearPegasosEngine<Long>(0.1, 1.0,false);
				OnlineRankingModel model = new OnlineRankingModel(coordinator, docs, relevantDocs, engine, 10000);

				
				
				Map<Query,Double> queryScores = model.getQueryScores();
				Map<String,Float> scoresCollection = conn.getScores(queryScores, new SimpleBooleanSimilarity(), new HashSet<String>(sample));
				
				double theta = 0.0;
				double CNeg = fracUseful;
				double CPos = (1-fracUseful);
				for(int t=1; t<100; t++){
					double learning = 1.0/t;
					double sum = 0.0;
					for(String doc : sample){
						double y = 1.0;
						double C = CPos;
						if(!usefulSample.contains(doc)){
							y=-1.0;
							C = CNeg;
						}
						double f = scoresCollection.get(doc);
						double decision = y*(f-theta);
						if(1>decision){
							sum+=C*y;
						}
					}
					theta = theta - learning*sum;
					System.out.println(theta + " " + sum);
				}
				
				System.err.println(relationship + "," + split + "," + theta);
			}
		}

//		workbook.write();
		workbook.close();
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

	private static List<Query> loadQueries(QueryParser qp, String queryFile, int numQueries) throws ParseException, IOException {
		InitialWordLoader iwl = new FromWeightedFileInitialWordLoader(qp,queryFile);
		List<Query> words = iwl.getInitialQueries().subList(0, numQueries);
		return words;
	}
}
