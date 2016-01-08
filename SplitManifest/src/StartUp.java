import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class StartUp {
	
	private static final String DOWNLOAD_BASE_DIR = "\\TEMP\\";
	
	private static final String SUFFIX = "_UTF8";
	private static final ManifestUtils.Encoding ENCODING = ManifestUtils.Encoding.UTF8;
	private static final ManifestUtils.ClientManifestManipulation MANIPULATION = ManifestUtils.ClientManifestManipulation.ENCODING;
		
	private static final String SERVER = "skyhd.upload.akamai.com"; // SkyGo Netstorage
	private static final String USERNAME = "";
	private static final String PASSWORD = "";
	private static final String PROD_BASE_DIR = "/110218/Production/de_at/movies/"; //SkyGo base folder
		
	private static final Logger LOG = Logger.getLogger(StartUp.class);

	private static final boolean UPLOAD_ENABLED = true;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		FTPClient client = null;
		try {
			client = FTPUtils.connect(SERVER, USERNAME, PASSWORD, FTPUtils.LocalMode.PASSIVE, FTPUtils.TransferMode.STREAM);
			List<String> directories = FTPUtils.listDirectories(client, PROD_BASE_DIR);
			List<ManifestContainer> manifestContainerList = new ArrayList<ManifestContainer>();
			if ( directories != null ) {
				for ( String directory : directories ) {
					ManifestContainer manifestContainer = new ManifestContainer(directory);
					manifestContainer.setServerManifestFiles(FTPUtils.listFiles(client, directory, FTPUtils.SERVER_MANIFEST_FILTER));
					manifestContainer.setClientManifestFiles(FTPUtils.listFiles(client, directory, FTPUtils.CLIENT_MANIFEST_FILTER));
					manifestContainerList.add(manifestContainer);
				}
			}
			String clientManifestPath = null;
			String serverManifestPath = null;
			String splitClientManifestPath = null;
			String splitServerManifestPath = null;
			for ( ManifestContainer manifestContainer : manifestContainerList ) {
				try {
					if ( manifestContainer.getClientManifestFiles() == null || manifestContainer.getServerManifestFiles() == null ) {
						LOG.warn("Invalid manifest file set within " + manifestContainer.getBaseFolder());
						continue;
					}
					if ( manifestContainer.getClientManifestFiles().size() != 1 || manifestContainer.getServerManifestFiles().size() != 1 ) {
						LOG.warn("Invalid number of client/server manifest files within " + manifestContainer.getBaseFolder());
						continue;
					}
					clientManifestPath = manifestContainer.getClientManifestFiles().get(0);
					serverManifestPath = manifestContainer.getServerManifestFiles().get(0);
					splitClientManifestPath = clientManifestPath.replace(".ismc", SUFFIX + ".ismc");
					splitServerManifestPath = serverManifestPath.replace(".ism", SUFFIX + ".ism");
					ClientManifest clientManifest = null;
					if ( FTPUtils.download(DOWNLOAD_BASE_DIR + clientManifestPath , clientManifestPath, client) && FTPUtils.download(DOWNLOAD_BASE_DIR + serverManifestPath, serverManifestPath, client) ) {
						clientManifest = ManifestUtils.analyseClientManifest(DOWNLOAD_BASE_DIR + clientManifestPath);
						if ( ManifestUtils.fulfillsManipulationCriterion(clientManifest,  MANIPULATION) ) {
							LOG.info(manifestContainer.getBaseFolder() + " fulfills manipulation criterion...starting manipulation");
							ManifestUtils.manipulateClientManifest(clientManifest, DOWNLOAD_BASE_DIR + clientManifestPath, DOWNLOAD_BASE_DIR + splitClientManifestPath, MANIPULATION, ENCODING);
							ManifestUtils.manipulateServerManifest(clientManifest, DOWNLOAD_BASE_DIR + serverManifestPath, DOWNLOAD_BASE_DIR + splitServerManifestPath, SUFFIX, MANIPULATION, ENCODING );
							if ( UPLOAD_ENABLED ) {
								if ( !FTPUtils.upload(DOWNLOAD_BASE_DIR + splitClientManifestPath,splitClientManifestPath, false, client) ) {
									LOG.error("Upload to " + splitClientManifestPath + " failed");
									continue;
								}
								if ( !FTPUtils.upload(DOWNLOAD_BASE_DIR + splitServerManifestPath, splitServerManifestPath, false, client) ) {
									LOG.error("Upload to " + splitServerManifestPath + " failed");
									continue;
								}
							}
						}							
						else {
							LOG.info(manifestContainer.getBaseFolder() + " does not fulfill manipulation criterion");
						}
					}
					else {
						LOG.error("Download from " + manifestContainer.getBaseFolder() + " failed");
					}
				} catch (Exception e) {
					LOG.error("Processing " + manifestContainer.getBaseFolder() + " failed: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		} finally {
			if ( client != null ) {
				try {
					client.disconnect();
				} catch (Exception e) {
					LOG.error(e.getMessage());
				}
			}
		}
	}
}
