package MixtureModel;

public class NoneAreOutliersDetector implements RankingOutlierDetector {

	@Override
	public boolean isOutlier(String doc) {
		return false;
	}

}
