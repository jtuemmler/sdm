package de.rbs.sdm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.rbs.sdm.ComponentDescription.Reference;

/**
 * Class to scan jars and extract information about services and bundles
 * 
 * @author tuemmler
 *
 */
public class ServiceAndBundleStore {
	private boolean verbose = false;
	private final Set<String> allServices = new HashSet<>();
	private final Map<String, BundleDescription> bundles = new HashMap<>();
	private List<Pattern> identifierBlackList = new ArrayList<>();
	private List<Pattern> componentBlackList = new ArrayList<>();
	private List<Pattern> bundleWhiteList = new ArrayList<>();
	private List<Pattern> componentWhiteList = new ArrayList<>();
	private List<Pattern> serviceWhiteList = new ArrayList<>();

	/**
	 * Print a message
	 */
	private void message(String m) {
		if (verbose) {
			System.out.println(m);
		}
	}
	
	/**
	 * Extract filename from path
	 * @param path	Path to extract filename from
	 * @return filename
	 */
	String getFilename(String path) {
		return (new File(path).getName()).replace('-', '_').replace(".jar", "");
	}

	/**
	 * @return Attribute with the given name
	 */
	private String getAttributeValue(org.w3c.dom.Node node, String attribute) {
		if (node.hasAttributes()) {
			org.w3c.dom.Node attributeNode = node.getAttributes().getNamedItem(attribute);
			if (attributeNode != null) {
				return attributeNode.getNodeValue();				
			}
		}
		return "";
	}

	/**
	 * Checks whether the given identifier is blacklisted. If not, call the consumer.
	 * @return True, if the identifier was consumed
	 */
	private boolean ifKeepIdentifier(String identifier, List<Pattern> blackList, List<Pattern> whiteList, Consumer<String> consumer) {
		if (whiteList != null) {
			if (whiteList.size() > 0) {
				boolean found = false;
				for (Pattern pattern : whiteList) {
					if (pattern.matcher(identifier).matches()) {
						found = true;
						break;
					}
				}
				if (!found) {
					message("  Ignoring: " + identifier);
					message("  because it is not in white-list.");
					return false;					
				}
			}
		}
		for (Pattern pattern : blackList) {
			if (pattern.matcher(identifier).matches()) {
				message("  Ignoring: " + identifier);
				message("  because of: " + pattern.pattern());
				return false;
			}
		}
		if (consumer != null) {
			consumer.accept(identifier);
		}

		return true;
	}

	/**
	 * Read XML description and extract information about a bundle
	 * 
	 * XML-files have the following structure:
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0" name="de.foo.bar.baz.provider" configuration-policy="require" activate="activate" deactivate="deactivate">
	 *  <implementation class="de.foo.bar.BazImpl"/>
	 *  <service>
	 *    <provide interface="de.foo.bar.Baz"/>
	 *  </service>
	 *  <reference name="ref1" interface="de.foo.bar.Ref1" field="ref1"/>
	 *  <reference name="ref2" cardinality="0..n" policy="dynamic" interface="de.foo.bar.Ref2" field="ref2" field-option="update" field-collection-type="service"/>
	 * </scr:component>
	 * 
	 * @param bundleName	Name of the bundle (jar)
	 * @param buffer		Buffer holding XML description
	 * @param length		Bytes used in buffer
	 */
	private void examineXml(String bundleName, byte[] buffer, int length) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		
		if (ifKeepIdentifier(bundleName, identifierBlackList, bundleWhiteList, null)) {
			try {
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(new ByteArrayInputStream(buffer,0,length));
				doc.getDocumentElement().normalize();
	
				NodeList impl = doc.getElementsByTagName("implementation");
				if (impl.getLength() > 0) {
					String componentName = getAttributeValue(impl.item(0), "class");
					if (!componentName.isEmpty()) {
						ComponentDescription cd = new ComponentDescription();
						ifKeepIdentifier(componentName, componentBlackList, componentWhiteList, name -> 
						{
							cd.setName(name);
							BundleDescription bundleDescription = bundles.get(bundleName);
							if (bundleDescription == null) {
								bundleDescription = new BundleDescription();
								bundles.put(bundleName, bundleDescription);
							}
							bundleDescription.addComponent(cd);
	
							NodeList service = doc.getElementsByTagName("service");
							if (service.getLength() > 0) {
								NodeList services = service.item(0).getChildNodes();
								for (int i = 0; i < services.getLength(); ++i) {
									String implementedInterface = getAttributeValue(services.item(i),"interface");
									if (!implementedInterface.isEmpty()) {
										ifKeepIdentifier(implementedInterface, 
												identifierBlackList,
												serviceWhiteList,
												identifier -> {
													cd.addInterface(identifier);
													allServices.add(identifier);
												});
									}
								}
							}
	
							NodeList references = doc.getElementsByTagName("reference");
							for (int i = 0; i < references.getLength(); ++i) {
								final String use = getAttributeValue(references.item(i),"interface");
								final String cardinality = getAttributeValue(references.item(i), "cardinality");
								final String policy = getAttributeValue(references.item(i), "policy");
								
								if (!use.isEmpty()) {
									ifKeepIdentifier(use,
											identifierBlackList,
											serviceWhiteList,
											identifier -> {
												Reference reference = new Reference();
												reference.name = identifier;
												if (!cardinality.equals("1..1")) {
													reference.cardinality = cardinality;												
												}
												reference.policy = policy;
												cd.addReference(reference);
												allServices.add(identifier);
											});
								}
							}
						});
					}
				}
	
			} catch (ParserConfigurationException|SAXException|UnsupportedEncodingException e) {
				e.printStackTrace();
				printXml(buffer,length);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	 * Scan a compressed JAR stream for XML descriptions.
	 * Recursively scans jars that are found in the stream.
	 * 
	 * Note: may be called more than once to scan a list of files.
	 * 
	 * @param jarName	Name of the file
	 * @param inStream	Compressed stream
	 */
	public void examineJar(String jarName, ZipInputStream inStream) {
		try {
			ZipEntry entry;

			while ((entry = inStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					//System.out.println(entry.getName());
					if (entry.getName().endsWith(".jar")) {
						message("Reading " + entry.getName() + " ...");
						examineJar(entry.getName(), new ZipInputStream(inStream));
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
							examineXml(getFilename(jarName), buffer,offset);
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
	 * Scan a JAR file for XML descriptions.
	 * @param jarName	Name of the file
	 */
	public void examineJar(String jarName) {
		try {
			message("Reading " + jarName + " ...");
			examineJar(jarName, new ZipInputStream(new FileInputStream(jarName)));
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
	public Map<String, BundleDescription> getBundles() {
		return bundles;
	}

	/**
	 * Set verbose-level
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	/**
	 * Set blacklist
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setBlacklist(List<Pattern> blackList) {
		this.identifierBlackList = blackList;
		return this;
	}

	/**
	 * Set blacklist for components
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setComponentBlacklist(List<Pattern> bundleBlackList) {
		this.componentBlackList = bundleBlackList;
		return this;
	}

	/**
	 * Set white-list for bundles
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setBundleWhitelist(List<Pattern> bundleWhiteList) {
		this.bundleWhiteList = bundleWhiteList;
		return this;
	}

	/**
	 * Set white-list for components
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setComponentWhitelist(List<Pattern> componentWhiteList) {
		this.componentWhiteList = componentWhiteList;
		return this;
	}

	/**
	 * Set white-list for services
	 * @return Current instance of ServiceAndBundleStore
	 */
	public ServiceAndBundleStore setServiceWhitelist(List<Pattern> serviceWhiteList) {
		this.serviceWhiteList = serviceWhiteList;
		return this;
	}
	
}
