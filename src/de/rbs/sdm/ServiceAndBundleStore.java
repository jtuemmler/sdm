package de.rbs.sdm;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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

public class ServiceAndBundleStore {
	final Set<String> allServices = new HashSet<>();
	final Set<BundleDescription> allBundles = new HashSet<>();
	
	private org.w3c.dom.Node getAttribute(org.w3c.dom.Node node, String attribute) {
		if (node.hasAttributes()) {
			return node.getAttributes().getNamedItem(attribute);
		}
		return null;
	}
	
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
    		
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			printXml(buffer,length);
		} catch (SAXException e) {
			e.printStackTrace();
			printXml(buffer,length);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void printXml(byte[] buffer, int length) {
		System.out.println(new String(buffer,0,length,StandardCharsets.UTF_8));
	}

	public void examineZip(ZipInputStream inStream) {
		ZipEntry entry;
		try {
			while ((entry = inStream.getNextEntry()) != null) {
			      if (!entry.isDirectory()) {

			          //System.out.println(entry.getName());
			          if (entry.getName().endsWith(".jar")) {
			        	  //System.out.println("## Scanning " + entry.getName() + " ...");
			        	  examineZip(new ZipInputStream(inStream));
			          }
			          if (entry.getName().contains("OSGI-INF") && entry.getName().endsWith(".xml")) {
			        	  System.out.println("Reading " + entry.getName() + " ...");
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
	
	public void examineZip(String fileName) {
		try {
			examineZip(new ZipInputStream(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Set<String> getServices() {
		return allServices;
	}

	public Set<BundleDescription> getBundles() {
		return allBundles;
	}
}
