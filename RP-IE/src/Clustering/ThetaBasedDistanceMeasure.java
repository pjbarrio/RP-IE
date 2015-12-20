package Clustering;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class ThetaBasedDistanceMeasure extends EuclideanDistance {

	private double theta;
	
	public ThetaBasedDistanceMeasure(double theta){
		this.theta = theta;
	}
	
	@Override
	public double compute(double[] a, double[] b) {
		
		return super.compute(transpose(a), transpose(b));
	}

	private double[] transpose(double[] a) {
		
		double[] at = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			at[i] = Math.abs(a[i] - theta);
		}
		return at;
		
	}
	
}
