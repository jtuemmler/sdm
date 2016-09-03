package de.rbs.sdm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.FileFormat;

public class Main {

	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new LinkedList<>();
		List<Pattern> bundleBlackList = new LinkedList<>();
		List<Pattern> blackList = new LinkedList<>();
		String outputFile = "out.svg";
		boolean writeSource = false;
		boolean verbose = false;

		blackList.add(Pattern.compile("java.lang.Object"));

		if (args.length > 0) {
			for (int i = 0; i < args.length; ++i) {
				if (args[i].equals("-o")) {
					outputFile = args[++i];
				}
				else if (args[i].equals("-p")) {
					writeSource = true;
				}
				else if (args[i].equals("-v")) {
					verbose = true;
				}
				else if (args[i].equals("-e")) {
					bundleBlackList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-E")) {
					blackList.add(Pattern.compile(args[++i]));
					bundleBlackList.add(Pattern.compile(args[i]));
				}				
				else {
					inputFiles.add(args[i]);
				}
			}

			ServiceAndBundleStore sab = new ServiceAndBundleStore();
			sab.setVerbose(verbose);
			sab.setBundleBlacklist(bundleBlackList);
			sab.setBlacklist(blackList);

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
			System.out.println("Usage  : sdm [options] -o [output] [jar]+");
			System.out.println("Example: sdm -e \".*foo.*\" -o out.svg bar.jar baz.jar");

			System.out.println();
			System.out.println("Options are:");
			System.out.println("-p          Don't render diagram, instead print the PlantUML source-code");
			System.out.println("            for the diagram");
			System.out.println("-e [regex]  Exclude bundle from diagram (may be defined more than once)");
			System.out.println("-E [regex]  Exclude identifier from diagram (may be defined more than once)");
			System.out.println("-v          Verbose output");
		}
	}
}
