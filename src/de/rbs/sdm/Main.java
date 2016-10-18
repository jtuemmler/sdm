package de.rbs.sdm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.FileFormat;

public class Main {

	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new LinkedList<>();
		List<Pattern> bundleBlackList = new LinkedList<>();
		List<Pattern> blackList = new LinkedList<>();
		String outputFile = "";
		boolean writeSource = false;
		boolean verbose = false;
		boolean lollipopStyle = false;
		boolean draft = false;
		boolean renderBundles = true;

		blackList.add(Pattern.compile("java.lang.Object"));

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
				else if (args[i].equals("-c")) {
					renderBundles = false;
				}
				else if (args[i].equals("-e")) {
					bundleBlackList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-E")) {
					blackList.add(Pattern.compile(args[++i]));
					bundleBlackList.add(Pattern.compile(args[i]));
				}		
				else if (args[i].equals("-l")) {
					lollipopStyle = true;
				}
				else if (args[i].equals("-d")) {
					draft = true;
				}
				else {
					if (!args[i].startsWith("-")) {
						inputFiles.add(args[i]);
					}
					else {
						System.out.println("Ignored parameter: " + args[i]);
					}
				}
			}

			ServiceAndBundleStore sab = new ServiceAndBundleStore();
			sab.setVerbose(verbose);
			sab.setBundleBlacklist(bundleBlackList);
			sab.setBlacklist(blackList);

			for (String file : inputFiles) {
				sab.examineJar(file);
			}

			PlantUmlRenderer renderer = new PlantUmlRenderer();
			renderer.setDraft(draft);
			renderer.setLollipopStyle(lollipopStyle);
			
			byte[] result;
			
			if (writeSource) {
				result = renderer.getPlantUmlDiagram(sab, renderBundles).getBytes(StandardCharsets.UTF_8);
			}
			else {
				result = renderer.renderServiceDiagram(sab,FileFormat.SVG, renderBundles);
			}
			
			if (result.length > 0) {
				if (outputFile.isEmpty()) {
					System.out.println(new String(result,StandardCharsets.UTF_8));
				}
				else {
					if (verbose) {
						System.out.println("Writing diagram " + outputFile + " ...");
					}
					try (FileOutputStream os = new FileOutputStream(outputFile)) {
						os.write(result);
					}
				}
			}
			else {
				System.err.println("Error: Diagram contains no services!");
			}
		}
		else {
			System.out.println("sdm Version 1.02");
			System.out.println();
			
			System.out.println("Usage  : sdm [options] -o [output] [jar]+");
			System.out.println("Example: sdm -e \".*foo.*\" -o out.svg bar.jar baz.jar");

			System.out.println();
			System.out.println("Options are:");
			System.out.println("-o [file]      Write to given file instead of stdout");
			System.out.println("-s             Don't render diagram, instead generate PlantUML source-code");
			System.out.println("-e [regex]     Exclude bundle from diagram (may be defined more than once)");
			System.out.println("-E [regex]     Exclude identifier from diagram (may be defined more than once)");
			System.out.println("-l             Use 'lollipop' style");
			System.out.println("-d             Use 'draft' style");
			System.out.println("-c             Render on component-level (instead of bundle-level)");
			System.out.println("-v             Verbose output");
			
			System.out.println();
			System.out.println("Environment variables:");
			System.out.println("plantumlinc    Use this variable to point to the directory containing osgi.iuml.");
			System.out.println("  Example: set plantumlinc=d:\\tools\\sdm\\plantuml");
			System.out.println();
			System.out.println("GRAPHVIZ_DOT   Use this variable to point to the dot-tool of graphviz.");
			System.out.println("  Example: set GRAPHVIZ_DOT=c:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe");
		}
	}
}
