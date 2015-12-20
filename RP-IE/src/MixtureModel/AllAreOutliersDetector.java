package MixtureModel;

public class AllAreOutliersDetector implements RankingOutlierDetector {

	@Override
	public boolean isOutlier(String doc) {
		return true;
	}

}
