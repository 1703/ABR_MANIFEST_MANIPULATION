import java.util.ArrayList;
import java.util.List;


public class ManifestContainer {
	
	private String baseFolder;
	private List<String> serverManifestFiles = new ArrayList<String>();
	private List<String> clientManifestFiles = new ArrayList<String>();
	
	@SuppressWarnings("unused")
	private ManifestContainer(){
		//
	}
	
	public ManifestContainer(final String baseFolder) {
		this.baseFolder = baseFolder;
	}
	
	public String getBaseFolder() {
		return baseFolder;
	}
	public void setBaseFolder(String baseFolder) {
		this.baseFolder = baseFolder;
	}
	public List<String> getServerManifestFiles() {
		return serverManifestFiles;
	}
	public void setServerManifestFiles(List<String> serverManifestFiles) {
		this.serverManifestFiles = serverManifestFiles;
	}
	public List<String> getClientManifestFiles() {
		return clientManifestFiles;
	}
	public void setClientManifestFiles(List<String> clientManifestFiles) {
		this.clientManifestFiles = clientManifestFiles;
	}
}