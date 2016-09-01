package de.rbs.sdm;

import java.io.IOException;

public class Main {

	
	public static void main(String[] args) throws IOException {
		ServiceAndBundleStore sab = new ServiceAndBundleStore();
		
		sab.examineZip("/Users/Joern/Documents/Projects/osgi/dl-prototype/smaspw-osgi.jar");
		
	
		System.out.println("!include %plantumlinc%/osgi.iuml");
		sab.getServices().forEach((s) -> System.out.println("service(" + s + ")"));
		
		System.out.println();
		
		for (BundleDescription bd : sab.getBundles()) {
			System.out.println("bundle(" + bd.getName() + ")");
			bd.getInterfaces().forEach((s) -> System.out.println("impl(" + bd.getName() + "," + s + ")"));
			bd.getReferences().forEach((s) -> System.out.println("use(" + bd.getName() + "," + s + ")"));
			System.out.println();			
		}
		
	}

}
