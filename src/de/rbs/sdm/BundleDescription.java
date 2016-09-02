package de.rbs.sdm;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to store information about a bundle.
 * 
 * @author tuemmler
 *
 */
public class BundleDescription {
	private final String name;
	private final Set<String> intf = new HashSet<>();
	private final Set<String> refs = new HashSet<>();
	
	public BundleDescription(String name) {
		this.name = name;
	}

	public void addInterface(String name) {
		intf.add(name);
	}
	
	public void addReference(String name) {
		refs.add(name);
	}
	
	public String getName() {
		return name;
	}

	public Set<String> getInterfaces() {
		return intf;
	}

	public Set<String> getReferences() {
		return refs;
	}
	
	@Override
	public String toString() {
		return "BundleDescription [name=" + name + ", intf=" + intf + ", refs=" + refs + "]";
	}
	
}
