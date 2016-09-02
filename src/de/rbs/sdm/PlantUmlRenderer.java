package de.rbs.sdm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
	
	/**
	 * Render a service-diagram an return a graphical representation of the specified format.
	 * 
	 * @param sab			ServiceAndBundleStore that contains a list of all services and bundles
	 * @param fileFormat	Format of the diagram (eg. FileFormat.SVG)
	 * @return A byte-array containing the rendered diagram.
	 */
	public static byte[] renderServiceDiagram(ServiceAndBundleStore sab, FileFormat fileFormat) {
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
	public static String getPlantUmlDiagram(ServiceAndBundleStore sab) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("!include %plantumlinc%/osgi.iuml\n");		
		sab.getServices().forEach((s) -> sb.append("service(" + s + ")\n"));
		
		for (BundleDescription bd : sab.getBundles()) {
			sb.append("bundle(" + bd.getName() + ")\n");
			bd.getInterfaces().forEach((s) -> sb.append("impl(" + bd.getName() + "," + s + ")\n"));
			bd.getReferences().forEach((s) -> sb.append("use(" + bd.getName() + "," + s + ")\n"));
		}

		return sb.toString();
	}
}
