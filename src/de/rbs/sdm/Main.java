package de.rbs.sdm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.plantuml.FileFormat;

public class Main {

	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new LinkedList<>();
		String outputFile = "out.svg";
		boolean writeSource = false;
		boolean verbose = false;
		
		if (args.length > 0) {
			for (int i = 0; i < args.length; ++i) {
				if (args[i].equals("-o")) {
					outputFile = args[++i];
				}
				else if (args[i].equals("-s")) {
					writeSource = true;
				}
				else if (args[i].equals("-v")) {
					verbose = true;
				}
				else {
					inputFiles.add(args[i]);
				}
			}
			
			ServiceAndBundleStore sab = new ServiceAndBundleStore();
			sab.setVerbose(verbose);
			
			for (String file : inputFiles) {
				sab.examineZip(file);
			}
			
			if (writeSource) {
				System.out.println(PlantUmlRenderer.getPlantUmlDiagram(sab));
			}
			else {
				if (verbose) {
					System.out.println("Writing diagram " + outputFile + " ...");
				}
				try (FileOutputStream os = new FileOutputStream(outputFile)) {
					os.write(PlantUmlRenderer.renderServiceDiagram(sab,FileFormat.SVG));
				}
			}
		}
		else {
			System.out.println("Usage: sdm -o [output] [jar]+");
		}
	}
}
