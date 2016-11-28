# sdm

## Description
A tool to render OSGi service diagrams using bundles containing DS descriptions as input.

## Usage
java -jar sdm.jar -o diagram.svg mybundle.jar

*Note*: Set the plantumlinc-environment variable to the directory containing
	the osgi.iuml file.

## References
This tool uses PlantUML and GraphViz to render the diagrams into SVG.

PlantUML: http://plantuml.com/, http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22plantuml%22

Graphviz: http://www.graphviz.org/ 
