import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class ThetaParameterFinder {
	
	private double CNeg;
	private double CPos;
	private int iter;
	
	public ThetaParameterFinder(double CPos, double CNeg, int iter){
		this.CNeg=CNeg;
		this.CPos=CPos;
		this.iter=iter;
	}
	
	public double findTheta(Collection<String> collection, Set<String> usefulDocs, Map<String,Float> scoresCollection){
		double theta = 0.0;
		for(Float f : scoresCollection.values()){
			theta+=f;
		}
		theta/=scoresCollection.size();
		for(int t=1; t<=iter; t++){
			double learning = 1.0/t;
			double sum = 0.0;
			for(String doc : collection){
				double y = 1.0;
				double C = CPos;
				if(!usefulDocs.contains(doc)){
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
		}
		
		return theta;
	}
}
