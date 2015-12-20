package Clustering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import Clustering.technique.KMeansTheta;

import edu.columbia.cs.ltrie.extractor.wrapping.impl.AdditiveFileSystemWrapping;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.uci.ics.jung.algorithms.util.KMeansClusterer;

public class ClusteringTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		
		int numPaths=672;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
		}
		String extractor = "Pablo-Sub-sequences";
		
		Map<String,Map<String,Map<Integer,Double>>> thetas = loadThetas("thetas.txt");
		
		String[] technique = {"RSVM","QBC"};
		
		String[] relationships = new String[]{"NaturalDisaster","PersonCareer","ManMadeDisaster", "VotingResult", "OrgAff", "Outbreaks", "Indictment-Arrest-Trial"};
		
		int[] split = {1,2};
		
		int num_clusters = 2;
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("clustersKmeans.csv")));
		
		bw.write("position,relation,split,ranking,cluster,mean,useful,score");
		
		for (int k = 0; k < technique.length; k++) {
			
			for (int i = 0; i < split.length; i++) {
				
				for (int j = 0; j < relationships.length; j++) {
					
					String resultsPath = "../LTRForIE/results" + relationships[j];
					
					AdditiveFileSystemWrapping extractWrapper = new AdditiveFileSystemWrapping();
					for(String subPath : subPaths){
						if(relationships[j].equals("OrgAff") || relationships[j].equals("Outbreaks")){
							extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + relationships[j] + ".data");
						}else{
							extractWrapper.addFiles(resultsPath + "/" + subPath + "_" + extractor + "_" + relationships[j] + ".data");
						}
					}
										
					File relDir = new File("precomputedScores/" + split[i] + "/" + relationships[j]);
					if(!relDir.exists()){
						relDir.mkdir();
					}
					Map<String,Float> scoresCollectionRSVM = (Map<String, Float>) SerializationHelper.read(relDir.getAbsolutePath() + "/"+technique[k]+".bin");
						
					double theta = thetas.get(relationships[j]).get(technique[k]).get(split[i]);
					
/*					KMeansClusterer<String> clusterAlg = new KMeansClusterer<String>();
					
					Map<String,double[]> object_locations = new HashMap<String, double[]>();
					
					for (Entry<String,Float> entry : scoresCollectionRSVM.entrySet()) {
						
						object_locations.put(entry.getKey(),new double[]{Math.abs(entry.getValue()-theta)});
						
					}
					
					List<Map<String, double[]>> clusters = new ArrayList<Map<String, double[]>>(clusterAlg.cluster(object_locations, num_clusters));
							
					System.out.println(relationships[j] + "," + split[i] + "," + technique[k] + "," + clusters.get(0).size() + "," + clusters.get(1).size());
				*/
					
					int maxIterations = 100;
					
					double tClus = theta - theta;
					
					DistanceMeasure measure = new ThetaBasedDistanceMeasure(tClus);
					
					RandomGenerator random = new ThetaRandomGenerator();
					
					List<Score> points = new ArrayList<Score>();
					
					Score th = new Score(new String("Theta"),new double[]{tClus});
					
					points.add(th);
					
					KMeansTheta<Score> algorithm = new KMeansTheta<Score>(num_clusters, maxIterations, measure, random, th);
					
					for (Entry<String,Float> entry : scoresCollectionRSVM.entrySet()) {
						
						points.add(new Score(entry.getKey(),new double[]{Math.abs(entry.getValue()-theta)}));
						
					}
					
					List<CentroidCluster<Score>> clusters = algorithm.cluster(points);
					
					System.out.println(relationships[j] + "," + split[i] + "," + technique[k] + "," + clusters.get(0).getPoints().size() + "," + clusters.get(1).getPoints().size() +
							"," + clusters.get(0).getCenter().getPoint()[0] + "," + clusters.get(1).getCenter().getPoint()[0]);
		
					int pos = 0;
					
					for (Score score : clusters.get(0).getPoints()){
						if (scoresCollectionRSVM.get(score.getId()) != null){
							bw.newLine();
							bw.write(pos + "," + relationships[j] + "," + split[i] + "," + technique[k] + ",0," + clusters.get(0).getCenter().getPoint()[0] + "," + (extractWrapper.getNumTuplesDocument(score.getId())>0? 1 : 0) + "," + scoresCollectionRSVM.get(score.getId())/*score.getPoint()[0]*/);
							pos++;
						}
					}
					
					pos = 0;
					for (Score score : clusters.get(1).getPoints()){
						if (scoresCollectionRSVM.get(score.getId()) != null){
							bw.newLine();
							bw.write(pos + "," + relationships[j] + "," + split[i] + "," + technique[k] + ",1," + clusters.get(1).getCenter().getPoint()[0] + "," +(extractWrapper.getNumTuplesDocument(score.getId())>0? 1 : 0) + "," + scoresCollectionRSVM.get(score.getId())/*score.getPoint()[0]*/);
							pos++;
						}
					}
				}
				
			}
			
		}
		
		
		bw.close();
		

	}

	private static Map<String, Map<String, Map<Integer, Double>>> loadThetas(
			String fileName) throws IOException {
		
		List<String> lines = FileUtils.readLines(new File(fileName));

		Map<String,Map<String,Map<Integer,Double>>> ret = new HashMap<String, Map<String,Map<Integer,Double>>>();
		
		for (int i = 0; i < lines.size(); i++) {
			
			String[] spls = lines.get(i).split(",");
			
			String rel = spls[0];
			Integer spl = Integer.valueOf(spls[1]);
 			String rank = spls[2];
 			Double theta = Double.valueOf(spls[3]);
 			
 			Map<String,Map<Integer,Double>> rels = ret.get(rel);
 			if (rels == null){
 				rels = new HashMap<String, Map<Integer,Double>>();
 				ret.put(rel, rels);
 			}
 			
 			Map<Integer,Double> splis = rels.get(rank);
 			
 			if (splis == null){
 				splis = new HashMap<Integer, Double>();
 				rels.put(rank, splis);
 			}
 			
 			splis.put(spl, theta);
 			
		}
		
		return ret;
		
	}

}
