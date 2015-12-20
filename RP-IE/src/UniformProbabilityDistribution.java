
public class UniformProbabilityDistribution implements
		ProbabilityDistribution {
	
	private double[] dist;
	
	public UniformProbabilityDistribution(int size){
		dist = new double[size];
		for(int i=0; i<size; i++){
			dist[i]=1.0/size;
		}
	}

	@Override
	public double getProbability(int pos) {
		return dist[pos];
	}

}
