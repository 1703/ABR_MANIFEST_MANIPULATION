import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.log4j.Logger;

public class FTPUtils {
	
	private static final Logger LOG = Logger.getLogger(FTPUtils.class);
	
	public final static FTPFileFilter SERVER_MANIFEST_FILTER = new FTPFileFilter() {
		 
	    @Override
	    public boolean accept(FTPFile ftpFile) {
	 
	    	return ftpFile != null
	    			&& ftpFile.isFile()
	    				&& ftpFile.getName().endsWith(".ism")
	    					&& !ftpFile.getName().endsWith("_1511.ism")
	    						&& !ftpFile.getName().toLowerCase().contains("samsung")
	    							&& !ftpFile.getName().toLowerCase().contains("xbox");
	 
	    }
	};
	
	public final static FTPFileFilter CLIENT_MANIFEST_FILTER = new FTPFileFilter() {
		 
	    @Override
	    public boolean accept(FTPFile ftpFile) {
	 
	    	return ftpFile != null
	    			&& ftpFile.isFile()
	    				&& ftpFile.getName().endsWith(".ismc")
	    					&& !ftpFile.getName().endsWith("_1511.ismc")
	    						&& !ftpFile.getName().toLowerCase().contains("samsung")
	    							&& !ftpFile.getName().toLowerCase().contains("xbox");
	 
	    }
	};
	
	public static List<String> listFiles(final FTPClient client, final String remoteDir, final FTPFileFilter filter) {
		if ( client == null ) {
			return null;
		}
		FTPFile[] ftpFiles = null;
		List<String> relativePathList = new ArrayList<String>();
		try {
			if ( filter != null ) {
				ftpFiles = client.listFiles(remoteDir, filter);
			} else {
				ftpFiles = client.listFiles(remoteDir);
			}
			int count = 0;
			if ( ftpFiles != null && ftpFiles.length > 0 ) {
				for ( FTPFile ftpFile : ftpFiles ) {
					if ( ftpFile != null && ftpFile.getName () != null ) {
						relativePathList.add(remoteDir + (remoteDir.endsWith("/")?"":"/") + ftpFile.getName());
					}
					else {
						count++;
					}
				}
			}
			System.err.println("erroneous entries for " + remoteDir + ": " + count);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return null;
		}
		return relativePathList;
	}
	
	public static List<String> listDirectories(final FTPClient client, final String remoteDir) {
		if ( client == null ) {
			return null;
		}
		FTPFile[] ftpDirectories = null;
		List<String> relativePathList = new ArrayList<String>();
		try {
			ftpDirectories = client.listDirectories(remoteDir);
			if ( ftpDirectories != null && ftpDirectories.length > 0 ) {
				for ( FTPFile ftpFile : ftpDirectories ) {
					relativePathList.add(remoteDir + (remoteDir.endsWith("/")?"":"/") + ftpFile.getName() + "/");
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return null;
		}
		return relativePathList;
	}
	
	/**
	 * @param host
	 * @param usr
	 * @param pwd
	 * @param localMode
	 * @return
	 */
	public static FTPClient connect(final String host, final String usr, final String pwd, final LocalMode localMode, final TransferMode transferMode) {
		FTPClient f = null;
		try {
			f = new FTPClient();
			f.connect(host);
			if ( localMode != null ) {
				if ( localMode == LocalMode.PASSIVE ) {
					f.enterLocalPassiveMode();
				}
				else {
					f.enterLocalActiveMode();
				}
			}
			f.login(usr, pwd);
			if ( transferMode != null ) {
				if ( transferMode == TransferMode.STREAM ) {
					f.setFileType(FTP.BINARY_FILE_TYPE);
					f.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
				}
			}
			return f;
		} catch (Exception e) {
			if ( f != null ) {
				disconnect(f);
			}
			return null;
		}
	}
	
	/**
	 * @param client
	 */
	public static void disconnect(final FTPClient client) {
		if ( client != null ) {
			try {
				client.disconnect();
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
	}
	
	/**
	 * @param localResultFile
	 * @param remoteSourceFile
	 * @param client
	 * @return
	 */
	public static boolean download( final String localResultFile, final String remoteSourceFile, final FTPClient client ) 
	{
		FileOutputStream fos = null;
		try {
			if ( client == null ) {
				return false;
			}
			File file = new File(localResultFile);
			if ( !file.getParentFile().exists() ) {
				if ( !file.getParentFile().mkdirs() ) {
					LOG.error("Failed to create baseDir");
					return false;
				}
			}
			fos = new FileOutputStream( file );
			return client.retrieveFile( remoteSourceFile, fos );
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return false;
		} finally {
			if ( fos != null ) {
				try {
					fos.close();
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
			}
		}
	}
	
	/**
	 * @param localSourceFile
	 * @param remoteResultFile
	 * @param client
	 * @return
	 */
	public static boolean upload( final String localSourceFile, final String remoteResultFile,  final boolean overwrite, final FTPClient client) 
	{
		FileInputStream fis = null;
		try {
			if ( client == null ) {
				return false;
			}
			if ( !overwrite && FTPUtils.ftpFileExists(remoteResultFile, client) ) {
				LOG.warn(remoteResultFile + " already exists...no overwrite");
				return true;
			}
			fis = new FileInputStream( localSourceFile );
			final String remoteDir = getFtpDirectory(remoteResultFile);
			final String remoteFileName = getFtpFileName(remoteResultFile);
			if ( remoteDir == null || remoteFileName == null) {
				return false;
			}
			if ( !client.changeWorkingDirectory(remoteDir) ) {
				return false;
			}
			return client.storeFile( remoteFileName, fis );
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return false;
		} finally {
			if ( fis != null ) {
				try {
					fis.close();
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
			}
		}
	}
	
	private static String getFtpDirectory(final String fileName) {
		String ftpDirectory = fileName;
		if ( ftpDirectory == null || ftpDirectory.isEmpty() ) {
			return null;
		}
		ftpDirectory = ftpDirectory.replace("\\", "/");
		int index = ftpDirectory.lastIndexOf("/");
		return index < 1 ? null : ftpDirectory.substring(0, ftpDirectory.lastIndexOf("/") + 1);
	}
	
	private static String getFtpFileName(final String fileName) {
		String ftpFile = fileName;
		if ( ftpFile == null || ftpFile.isEmpty() ) {
			return null;
		}
		ftpFile = ftpFile.replace("\\", "/");
		int index = ftpFile.lastIndexOf("/");
		return index < 1 ? null : ftpFile.substring(ftpFile.lastIndexOf("/") + 1);
	}
	
	public static boolean ftpFileExists(final String remoteFile, final FTPClient client) throws Exception {
		FTPFile[] ftpFile = client.listFiles(remoteFile);
		return ftpFile != null && ftpFile.length == 1 && ftpFile[0].isFile();
	}
	   
	public enum LocalMode {
		PASSIVE,
		ACTIVE
	}
	
	public enum TransferMode {
		STREAM
	}
}
