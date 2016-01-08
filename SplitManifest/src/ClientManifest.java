import java.util.ArrayList;
import java.util.List;


public class ClientManifest {
	
	private static final int MIN_HD_WIDTH = 1280;
	private static final int MIN_HD_HEIGHT = 720;
	
	private List<VideoQualityLevel> videoQualityLevels = new ArrayList<VideoQualityLevel>();
	
	public ClientManifest () {
		
	}
	
	public void addVideoQualityLevel(final int index, final int width, final int height) {
		videoQualityLevels.add(new VideoQualityLevel(index, width, height));
	}
	
	public boolean containsHiResVideo() {
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.width >= MIN_HD_WIDTH && vql.height >= MIN_HD_HEIGHT ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isHiResQualityLevel(final int index) {
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.index == index && vql.width >= MIN_HD_WIDTH && vql.height >= MIN_HD_HEIGHT ) {
				return true;
			}
		}
		return false;
	}

	public int getMaxLowResWidth() {
		int returnValue = -1;
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.width < MIN_HD_WIDTH && vql.height < MIN_HD_HEIGHT ) {
				if ( vql.width > returnValue ) {
					returnValue = vql.width;
				}
			}
		}
		return returnValue;
	}

	public int getMaxLowResHeight() {
		int returnValue = -1;
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.width < MIN_HD_WIDTH && vql.height < MIN_HD_HEIGHT ) {
				if ( vql.height > returnValue ) {
					returnValue = vql.height;
				}
			}
		}
		return returnValue;
	}

	public int getNumberOfLowResQualityLevels() {
		int returnValue = 0;
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.width < MIN_HD_WIDTH && vql.height < MIN_HD_HEIGHT ) {
				returnValue++;
			}
		}
		return returnValue;
	}

	public List<Integer> getHiResIndices() {
		List<Integer> returnList = new ArrayList<Integer>();
		for ( VideoQualityLevel vql : videoQualityLevels ) {
			if ( vql.width >= MIN_HD_WIDTH && vql.height >= MIN_HD_HEIGHT ) {
				returnList.add(vql.index);
			}
		}
		return returnList;
	}

	private class VideoQualityLevel {
		private int index, width, height;
		
		@SuppressWarnings("unused")
		private VideoQualityLevel() {
			
		}
		public VideoQualityLevel(final int index, final int width, final int height) {
			this.index = index;
			this.width = width;
			this.height = height;
		}
	}
}