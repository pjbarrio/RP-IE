package Clustering.technique;

import java.util.Collection;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.RandomGenerator;

import Clustering.Score;

public class KMeansTheta<T extends Clusterable> extends KMeansPlusPlusClusterer<T> {

	private Score theta;

	public KMeansTheta(int k, int maxIterations, DistanceMeasure measure,
			RandomGenerator random, Score theta) {
		super(k, maxIterations, measure, random);
		this.theta = theta;
	}

	@Override
	protected Clusterable centroidOf(Clusterable center, Collection<T> points, int dimension) {
		if (center.equals(theta)){
			return center;
		}
		return super.centroidOf(center,points, dimension);
	}
	
}
