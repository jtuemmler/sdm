package de.rbs.sdm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

/**
 * Class to render service-diagrams using PlantUML (http://plantuml.com)
 * 
 * @author tuemmler
 *
 */
public class PlantUmlRenderer {
	private boolean lollipopStyle = false;
	private boolean draft = false;
	private List<Pattern> serviceIncludeList = new ArrayList<>();

	/**
	 * Render a service-diagram an return a graphical representation of the specified format.
	 * 
	 * @param sab			ServiceAndBundleStore that contains a list of all services and bundles
	 * @param fileFormat	Format of the diagram (eg. FileFormat.SVG)
	 * @param renderBundles If true, render on bunlelevel. Otherwise render on component level 
	 * @return A byte-array containing the rendered diagram.
	 */
	public byte[] renderServiceDiagram(ServiceAndBundleStore sab, FileFormat fileFormat, boolean renderBundles) {
		byte[] result = new byte[0];		

		String source = getPlantUmlDiagram(sab, renderBundles);

		if (!source.isEmpty()) {
			source = "@startuml\n" +
					source +
					"@enduml\n";

			SourceStringReader reader = new SourceStringReader(source);

			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				reader.generateImage(os, new FileFormatOption(fileFormat));
				os.close();
				result = os.toByteArray(); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}
	
	/**
	 * Add text to the stringbuilder, if the identifier is in include-list
	 * @param identifier	Identifier to match
	 * @param sb			StringBuilder to add text
	 * @param text			Text to add if identifier matches
	 */
    private void appendConditional(String identifier, StringBuilder sb, String text) {
    	if (serviceIncludeList.size() > 0) {
			for (Pattern pattern : serviceIncludeList) {
				if (pattern.matcher(identifier).matches()) {
			    	sb.append(text);
					return;
				}
			}
    	}
    	else {
	    	sb.append(text);    		
    	}
    }
    
	/**
	 * Render a service-diagram to PlantUML commands.
	 * 
	 * @param sab			ServiceAndBundleStore that contains a list of all services and bundles
	 * @param renderBundles If true, render on bunlelevel. Otherwise render on component level 
	 * @return Service-diagram as PlantUML commands
	 */
	public String getPlantUmlDiagram(ServiceAndBundleStore sab, boolean renderBundles) {
		StringBuilder sb = new StringBuilder();

		if (sab.getServices().size() > 0) {

			if (lollipopStyle) {
				sb.append("!define LOLLY_STYLE 1\n");
			}
	
			sb.append("!include %plantumlinc%/osgi.iuml\n\n");
	
			if (draft) {
				sb.append("draft\n");
			}

			sab.getServices().forEach((s) -> appendConditional(s,sb,"service(" + s + ")\n"));

			if (renderBundles) {
				for (Entry<String, BundleDescription> bundle : sab.getBundles().entrySet()) {
					StringBuilder bsb = new StringBuilder();
					bundle.getValue().getInterfaces().forEach((s) -> appendConditional(s,bsb,"impl(" + bundle.getKey() + "," + s + ")\n"));
					bundle.getValue().getReferences().forEach(
							(s) -> {
								if (s.cardinality.isEmpty()) {
									appendConditional(s.name,bsb,"use(" + bundle.getKey() + "," + s.name + ")\n");
								}
								else {
									appendConditional(s.name,bsb,"useMulti(" + bundle.getKey() + "," + s.name + "," + s.cardinality + ")\n");							
								}
							});
					if (bsb.length() > 0) {
						sb.append("\nbundle(" + bundle.getKey() + ")\n");
						sb.append(bsb);
					}
				}
			}
			else {
				for (BundleDescription bundleDescription : sab.getBundles().values()) {
					for (ComponentDescription componentDescription : bundleDescription.components) {
						StringBuilder csb = new StringBuilder();
						componentDescription.getInterfaces().forEach((s) -> appendConditional(s,csb,"impl(" + componentDescription.getName() + "," + s + ")\n"));
						componentDescription.getReferences().forEach(
								(s) -> {
									if (s.cardinality.isEmpty()) {
										appendConditional(s.name,csb,"use(" + componentDescription.getName() + "," + s.name + ")\n");
									}
									else {
										appendConditional(s.name,csb,"useMulti(" + componentDescription.getName() + "," + s.name + "," + s.cardinality + ")\n");							
									}
								});
						if (csb.length() > 0) {
							sb.append("\ncomponent(" + componentDescription.getName() + ")\n");
							sb.append(csb);
						}
					}
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Defines whether to draw interfaces in "lollipop" style
	 * @param lollipopStyle 	True, to enable style
	 * @return Current instance of PlantUmlRenderer
	 */
	public PlantUmlRenderer setLollipopStyle(boolean lollipopStyle) {
		this.lollipopStyle = lollipopStyle;
		return this;
	}

	/**
	 * Defines whether to draw diagrams in "draft" style
	 * @param draft				True, to enable style
	 * @return Current instance of PlantUmlRenderer
	 */
	public PlantUmlRenderer setDraft(boolean draft) {
		this.draft = draft;
		return this;
	}
	
	/**
	 * Defines which services have to be included in diagram
	 * @param serviceIncludeList	List of regex-patterns that identify services to include
	 * @return Current instance of PlantUmlRenderer
	 */
	public PlantUmlRenderer setServiceIncludeList(List<Pattern> serviceIncludeList) {
		this.serviceIncludeList = serviceIncludeList;
		return this;
	}

}
