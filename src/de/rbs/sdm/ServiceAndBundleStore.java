package de.rbs.sdm;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to scan jars and extract information about services and bundles
 * 
 * @author tuemmler
 *
 */
public class ServiceAndBundleStore {
	boolean verbose = false;
	final Set<String> allServices = new HashSet<>();
	final Set<BundleDescription> allBundles = new HashSet<>();

	/**
	 * Print a message
	 */
	private void message(String m) {
		if (verbose) {
			System.out.println(m);
		}
	}
	
	/**
	 * Return attribute with the given name
	 */
	private org.w3c.dom.Node getAttribute(org.w3c.dom.Node node, String attribute) {
		if (node.hasAttributes()) {
			return node.getAttributes().getNamedItem(attribute);
		}
		return null;
	}

	/**
	 * Read XML description and extract information about a bundle
	 * @param buffer	Buffer holding XML description
	 * @param length	Bytes used in buffer
	 */
	private void examineXml(byte[] buffer, int length) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(buffer,0,length));
			doc.getDocumentElement().normalize();

			NodeList impl = doc.getElementsByTagName("implementation");
			if (impl.getLength() > 0) {
				org.w3c.dom.Node clazz = getAttribute(impl.item(0), "class");
				if (clazz != null) {
					BundleDescription bd = new BundleDescription(clazz.getNodeValue());
					allBundles.add(bd);

					NodeList service = doc.getElementsByTagName("service");
					if (service.getLength() > 0) {
						NodeList services = service.item(0).getChildNodes();
						for (int i = 0; i < services.getLength(); ++i) {
							org.w3c.dom.Node provide = getAttribute(services.item(i),"interface");
							if (provide != null) {
								bd.addInterface(provide.getNodeValue());
								allServices.add(provide.getNodeValue());
							}
						}
					}

					NodeList references = doc.getElementsByTagName("reference");
					for (int i = 0; i < references.getLength(); ++i) {
						org.w3c.dom.Node use = getAttribute(references.item(i),"interface");
						if (use != null) {
							bd.addReference(use.getNodeValue());
							allServices.add(use.getNodeValue());							
						}
					}
				}
			}

		} catch (ParserConfigurationException|SAXException|UnsupportedEncodingException e) {
			e.printStackTrace();
			printXml(buffer,length);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Print XML to stdout
	 * @param buffer	Buffer holding XML description
	 * @param length	Bytes used in buffer
	 */
	private void printXml(byte[] buffer, int length) {
		System.out.println(new String(buffer,0,length,StandardCharsets.UTF_8));
	}

	/**
	 * Scan a ZIP stream for XML descriptions.
	 * Recursively scans jars that are found in the stream.
	 * 
	 * Note: may be called more than once to scan a list of files.
	 * 
	 * @param inStream	Compressed stream
	 */
	public void examineZip(ZipInputStream inStream) {
		try {
			ZipEntry entry;

			while ((entry = inStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					//System.out.println(entry.getName());
					if (entry.getName().endsWith(".jar")) {
						message("Reading " + entry.getName() + " ...");
						examineZip(new ZipInputStream(inStream));
					}
					
					if (entry.getName().contains("OSGI-INF") && entry.getName().endsWith(".xml")) {
						message("  Reading " + entry.getName() + " ...");
						int len = 0;
						int offset = 0;
						byte[] buffer = new byte[100_000];
						while ((len = inStream.read(buffer,offset,buffer.length - offset)) > 0) {
							offset += len;
						}
						
						if (offset < buffer.length) {
							examineXml(buffer,offset);
						}
						else {
							System.err.println("Buffer too small!");
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Scan a ZIP file for XML descriptions.
	 * @param fileName	Name of the file
	 */
	public void examineZip(String fileName) {
		try {
			message("Reading " + fileName + " ...");
			examineZip(new ZipInputStream(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return All found services
	 */
	public Set<String> getServices() {
		return allServices;
	}

	/**
	 * @return All found bundles
	 */
	public Set<BundleDescription> getBundles() {
		return allBundles;
	}
	
	/**
	 * Set verbose-level
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
