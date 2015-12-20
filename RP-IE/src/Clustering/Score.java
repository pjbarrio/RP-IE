package Clustering;

import java.util.Arrays;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class Score implements Clusterable {

	private String id;
	private double[] ds;

	public Score(String id, double[] ds) {
		this.id = id;
		this.ds = ds;
	}

	@Override
	public double[] getPoint() {
		return ds;
	}

	@Override
	public String toString() {
		return "<" + id + "-" + Arrays.toString(ds) + ">";
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof Score){
			Score other = (Score)obj;
			return other.id.equals(id);
		}
		return false;
	}
	
	public String getId(){
		return id;
	}
	
}
