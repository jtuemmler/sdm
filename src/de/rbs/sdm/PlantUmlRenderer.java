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

			sab.getServices().forEach((s) -> sb.append("service(" + s + ")\n"));

			if (renderBundles) {
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
			}
			else {
				for (BundleDescription bundleDescription : sab.getBundles().values()) {
					for (ComponentDescription componentDescription : bundleDescription.components) {
						sb.append("\ncomponent(" + componentDescription.getName() + ")\n");
						componentDescription.getInterfaces().forEach((s) -> sb.append("impl(" + componentDescription.getName() + "," + s + ")\n"));
						componentDescription.getReferences().forEach(
								(s) -> {
									if (s.cardinality.isEmpty()) {
										sb.append("use(" + componentDescription.getName() + "," + s.name + ")\n");
									}
									else {
										sb.append("useMulti(" + componentDescription.getName() + "," + s.name + "," + s.cardinality + ")\n");							
									}
								});
					}
				}
			}
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
