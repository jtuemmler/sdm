package de.rbs.sdm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.sourceforge.plantuml.FileFormat;

public class Main {

	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new LinkedList<>();
		List<Pattern> blackList = new LinkedList<>();
		List<Pattern> componentBlackList = new LinkedList<>();
		List<Pattern> bundleWhiteList = new LinkedList<>();
		List<Pattern> componentWhiteList = new LinkedList<>();
		List<Pattern> serviceWhiteList = new LinkedList<>();
		String plantUmlInc = "%plantumlinc%/osgi.iuml";
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
					blackList.add(Pattern.compile(args[++i]));
					componentBlackList.add(Pattern.compile(args[i]));
				}		
				else if (args[i].equals("-ec")) {
					componentBlackList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-ib")) {
					bundleWhiteList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-ic")) {
					componentWhiteList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-is")) {
					serviceWhiteList.add(Pattern.compile(args[++i]));
				}
				else if (args[i].equals("-isf")) {
					extendPatternList(serviceWhiteList, "^service\\((.+)\\)", args[++i]);
				}
				else if (args[i].equals("-l")) {
					lollipopStyle = true;
				}
				else if (args[i].equals("-d")) {
					draft = true;
				}
				else if (args[i].equals("-pumlinc")) {
				   plantUmlInc = args[++i];
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

			ServiceAndBundleStore sab = new ServiceAndBundleStore()
					.setVerbose(verbose)
					.setBlacklist(blackList)
					.setComponentBlacklist(componentBlackList)
					.setBundleWhitelist(bundleWhiteList)
					.setComponentWhitelist(componentWhiteList)
					.setServiceWhitelist(serviceWhiteList);

			for (String file : inputFiles) {
				sab.examineJar(file);
			}

			PlantUmlRenderer renderer = new PlantUmlRenderer()
					.setDraft(draft)
					.setLollipopStyle(lollipopStyle)
					.setPlantUmlInc(plantUmlInc);

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
			System.out.println("sdm Version 1.04");
			System.out.println();

			System.out.println("Usage  : sdm [options] -o [output] [jar]+");
			System.out.println("Example: sdm -e \".*foo.*\" -o out.svg bar.jar baz.jar");

			System.out.println();
			System.out.println("Options are:");
			System.out.println("-o [file]         Write to given file instead of stdout");
			System.out.println("-s                Don't render diagram, instead generate PlantUML source-code");
			System.out.println("-e [regex]        Exclude identifier from diagram (may be defined more than once)");
			System.out.println("-eb [regex]       Exclude bundle from diagram (may be defined more than once)");
			System.out.println("-ib [regex]       Include bundle (may be defined more than once)");
			System.out.println("-ic [regex]       Include component (may be defined more than once)");
			System.out.println("-is [regex]       Include service (may be defined more than once)");
			System.out.println("-isf [file]       Include service (may be defined more than once)");
			System.out.println("-pumlinc [string] Path to the PlantUML include file");
			System.out.println("-l                Use 'lollipop' style");
			System.out.println("-d                Use 'draft' style");
			System.out.println("-c                Render on component-level (instead of bundle-level)");
			System.out.println("-v                Verbose output");

			System.out.println();
			System.out.println("Environment variables:");
			System.out.println("plantumlinc    Use this variable to point to the directory containing osgi.iuml.");
			System.out.println("  Example: set plantumlinc=d:\\tools\\sdm\\plantuml");
			System.out.println();
			System.out.println("GRAPHVIZ_DOT   Use this variable to point to the dot-tool of graphviz.");
			System.out.println("  Example: set GRAPHVIZ_DOT=c:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe");
		}
	}

	private static void extendPatternList(List<Pattern> patternList, String pattern, String fileName)  {
		Pattern p = Pattern.compile(pattern);

		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
			stream.forEach(s -> {
				Matcher m = p.matcher(s);
				if (m.find()) {
					patternList.add(Pattern.compile(m.group(1)));
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}   
	}
}
