import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ManifestUtils {
	
	static TransformerFactory TF = TransformerFactory.newInstance();
	
	static XPathFactory XPF = XPathFactory.newInstance();
	
	static XPath XPATH = XPF.newXPath();
	
	static XPathExpression ISM_PROFILES = null;
	static XPathExpression ISM_ISMC_PATH = null;
	static XPathExpression ISMC_VIDEO_QUALITY_LEVELS = null;
	static XPathExpression ISMC_STREAM_INDEX = null;
	
	// SERVER MANIFEST
	private static  XPathExpression getIsmProfiles() throws Exception {
		if ( ISM_PROFILES == null ) {
			ISM_PROFILES = XPATH.compile("//smil/body/switch/video");
		}
		return ISM_PROFILES;
	}
	
	//[contains(@src, '_5.ismv') or contains(@src, '_6.ismv') or contains(@src, '_7.ismv')]
	
	private static  XPathExpression getIsmcPath() throws Exception {
		if ( ISM_ISMC_PATH == null ) {
			ISM_ISMC_PATH = XPATH.compile("//smil/head/meta[@name='clientManifestRelativePath' and contains(@content, '.ismc')]");
		}
		return ISM_ISMC_PATH;
	}
	
	//CLIENT MANIFEST
	private static  XPathExpression getIsmcVideoQualityLevels() throws Exception {
		if ( ISMC_VIDEO_QUALITY_LEVELS == null ) {
			ISMC_VIDEO_QUALITY_LEVELS = XPATH.compile("//StreamIndex/QualityLevel[contains(@FourCC,'H264')]");
		}
		return ISMC_VIDEO_QUALITY_LEVELS;
	}
	
	private static  XPathExpression getIsmcStreamIndex() throws Exception {
		if ( ISMC_STREAM_INDEX == null ) {
			ISMC_STREAM_INDEX = XPATH.compile("//StreamIndex");
		}
		return ISMC_STREAM_INDEX;
	}
	
	public static ClientManifest analyseClientManifest(final String source) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document document = dbf.newDocumentBuilder().parse(source);
               
        NodeList nodeList = (NodeList) getIsmcVideoQualityLevels().evaluate(document, XPathConstants.NODESET);
        ClientManifest returnObject = new ClientManifest();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
        	returnObject.addVideoQualityLevel(
        			Integer.valueOf(nodeList.item(i).getAttributes().getNamedItem("Index").getNodeValue()),
        			Integer.valueOf(nodeList.item(i).getAttributes().getNamedItem("MaxWidth").getNodeValue()),
        			Integer.valueOf(nodeList.item(i).getAttributes().getNamedItem("MaxHeight").getNodeValue())
        			);
        }
        return returnObject;
    }
	
	public static void manipulateClientManifest(final ClientManifest clientManifest, final String source, final String target, final ClientManifestManipulation manipulationType, final Encoding encoding) throws Exception{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document document = dbf.newDocumentBuilder().parse(source);
		
        if ( manipulationType == ClientManifestManipulation.AT20 ) {
	        NodeList nodeList = (NodeList) getIsmcVideoQualityLevels().evaluate(document, XPathConstants.NODESET);
	        Node nodeToRemove = null;
	        for ( int i = 0; i < nodeList.getLength(); i++ ) {
	        	if ( clientManifest.isHiResQualityLevel(Integer.valueOf(nodeList.item(i).getAttributes().getNamedItem("Index").getNodeValue())) ) {
	        		nodeToRemove = nodeList.item(i);
	            	nodeToRemove.getParentNode().removeChild(nodeToRemove);
	        	}
	        }
	        Element element = (Element) getIsmcStreamIndex().evaluate(document, XPathConstants.NODE);
	        if ( element != null ) {
	        	element.setAttribute("MaxWidth", String.valueOf(clientManifest.getMaxLowResWidth()));
	        	element.setAttribute("MaxHeight", String.valueOf(clientManifest.getMaxLowResHeight()));
	        	element.setAttribute("DisplayWidth", String.valueOf(clientManifest.getMaxLowResWidth()));
	        	element.setAttribute("DisplayHeight", String.valueOf(clientManifest.getMaxLowResHeight()));
	        	element.setAttribute("QualityLevels", String.valueOf(clientManifest.getNumberOfLowResQualityLevels()));
	        }
        }
        else if ( manipulationType == ClientManifestManipulation.XBOX ) {
        	NodeList nodeList = (NodeList) getIsmcVideoQualityLevels().evaluate(document, XPathConstants.NODESET);
 	        for ( int i = 0; i < nodeList.getLength(); i++ ) {
 	        	Element element = (Element)nodeList.item(i);
 	        	if ( Integer.valueOf(nodeList.item(i).getAttributes().getNamedItem("MaxWidth").getNodeValue()) == 721) {
 	        		element.setAttribute("MaxWidth", String.valueOf(720));
 	        	}
 	        }
 	        Element element = (Element) getIsmcStreamIndex().evaluate(document, XPathConstants.NODE);
 	        if ( element != null ) {
 	        	if ( Integer.valueOf(element.getAttributes().getNamedItem("MaxWidth").getNodeValue()) == 721
 	        			&& Integer.valueOf(element.getAttributes().getNamedItem("DisplayWidth").getNodeValue()) == 721) {
 	        		element.setAttribute("MaxWidth", String.valueOf(720));
 	        		element.setAttribute("DisplayWidth", String.valueOf(720));
 	        	}
 	        }
        }
        Transformer t = TF. newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, encoding.getEncoding());
        t.transform(new DOMSource(document), new StreamResult(new File(target)));        
    }
	
	public static void manipulateServerManifest(final ClientManifest clientManifest, final String source, final String target, final String suffix, final ClientManifestManipulation manipulationType, final Encoding encoding) throws Exception{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document document = dbf.newDocumentBuilder().parse(source);
        if ( manipulationType == ClientManifestManipulation.AT20 ) {
	        NodeList nodeList = (NodeList) getIsmProfiles().evaluate(document, XPathConstants.NODESET);
	        Node nodeToRemove = null;
	        String compareString = null;
	        List<Integer> lowResIndices = clientManifest.getHiResIndices();
	        for ( int i = 0; i < nodeList.getLength(); i++ ) {
	        	for ( Integer index : lowResIndices ) {
	        		compareString = "_" + (index + 1) + ".ismv";
	        		if ( nodeList.item(i).getAttributes().getNamedItem("src").getNodeValue().endsWith(compareString) ) {
	        			nodeToRemove = nodeList.item(i);
	        			nodeToRemove.getParentNode().removeChild(nodeToRemove);
	        		}
	        	}
	        }
        }
        Element element = (Element) getIsmcPath().evaluate(document, XPathConstants.NODE);
        if ( element != null ) {
        	element.setAttribute("content", element.getAttribute("content").replace(".ismc", suffix + ".ismc"));
        }
        Transformer t = TF.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, encoding.getEncoding());
        t.transform(new DOMSource(document), new StreamResult(new File(target)));
	}
	
	public static boolean fulfillsManipulationCriterion(final ClientManifest clientManifest, final ClientManifestManipulation manipulationType) {
		if ( clientManifest == null || manipulationType == null ) {
			return false;
		}
		if ( manipulationType == ClientManifestManipulation.XBOX ) {
			return clientManifest.containsHiResVideo();
		}
		return true;
	}
	
	public enum ClientManifestManipulation {
		AT20,
		XBOX,
		ENCODING
	}
	
	public enum Encoding {
		UTF8("UTF-8"),
		UTF16("UTF-16");
		
		private final String encoding;
		
		private Encoding(final String encoding) {
			this.encoding = encoding;
		}
		
		public String getEncoding() {
			return encoding;
		}
	}
}
