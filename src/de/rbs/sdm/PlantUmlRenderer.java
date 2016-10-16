package de.rbs.sdm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

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
	
	/**
	 * Render a service-diagram an return a graphical representation of the specified format.
	 * 
	 * @param sab			ServiceAndBundleStore that contains a list of all services and bundles
	 * @param fileFormat	Format of the diagram (eg. FileFormat.SVG)
	 * @return A byte-array containing the rendered diagram.
	 */
	public byte[] renderServiceDiagram(ServiceAndBundleStore sab, FileFormat fileFormat) {
		byte[] result = new byte[0];		
		
		String source = "@startuml\n" +
						getPlantUmlDiagram(sab) +
						"@enduml\n";

		SourceStringReader reader = new SourceStringReader(source);

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			reader.generateImage(os, new FileFormatOption(fileFormat));
			os.close();
			result = os.toByteArray(); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	/**
	 * Render a service-diagram to PlantUML commands.
	 * 
	 * @param sab			ServiceAndBundleStore that contains a list of all services and bundles
	 * @return Service-diagram as PlantUML commands
	 */
	public String getPlantUmlDiagram(ServiceAndBundleStore sab) {
		StringBuilder sb = new StringBuilder();
		
		if (lollipopStyle) {
			sb.append("!define LOLLY_STYLE 1\n");
		}
		
		sb.append("!include %plantumlinc%/osgi.iuml\n\n");
		
		if (draft) {
			sb.append("draft\n");
		}
		
		sab.getServices().forEach((s) -> sb.append("service(" + s + ")\n"));
		
		for (Entry<String, BundleDescription> bundle : sab.getBundles().entrySet()) {
			sb.append("\nbundle(" + bundle.getKey() + ")\n");
			bundle.getValue().getInterfaces().forEach((s) -> sb.append("impl(" + bundle.getKey() + "," + s + ")\n"));
			bundle.getValue().getReferences().forEach(
					(s) -> {
						if (s.cardinality.isEmpty()) {
							sb.append("use(" + bundle.getKey() + "," + s.name + ")\n");
						}
						else {
							sb.append("useMulti(" + bundle.getKey() + "," + s.name + "," + s.cardinality + ")\n");							
						}
					});
		}

		return sb.toString();
	}

	/**
	 * Defines whether to draw interfaces in "lollipop" style
	 * @param lollipopStyle 	True, to enable style
	 */
	public void setLollipopStyle(boolean lollipopStyle) {
		this.lollipopStyle = lollipopStyle;
	}

	/**
	 * Defines whether to draw diagrams in "draft" style
	 * @param draft				True, to enable style
	 */
	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	
}
