package MixtureModel;

public class RandomOutliersDetector implements RankingOutlierDetector {
	
	private double percentage;
	
	public RandomOutliersDetector(double percentage){
		this.percentage=percentage;
	}

	@Override
	public boolean isOutlier(String doc) {
		double v = Math.random();
		if(v<percentage){
			return true;
		}else{
			return false;
		}
	}

}
