import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TextUtils {

	public static List<String> extractFromFile(final String filePath) {
		FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
           
            String feed;
            feed = br.readLine();
            List<String> lines = new ArrayList<String>();
            while (feed != null) {
            	lines.add(feed);
            	feed = br.readLine();
            }	
            fr.close();
            return lines;
        }
        catch (IOException e){
            System.err.println(e.getMessage());
        }
        finally {
        	if ( fr != null ) {
        		try {
					fr.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
        	}
        	if ( br != null ) {
        		try {
					br.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
        	}
        }
        return null;
	}
}
