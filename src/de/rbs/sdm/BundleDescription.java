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
	public static class Reference {
		public String name = "";
		public String cardinality = "";
		public String policy = "";

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return name.equals(obj);
		}

		@Override
		public String toString() {
			return "Reference [name=" + name + "]";
		}	
	}
	
	private String name = "";
	private final Set<String> intf = new HashSet<>();
	private final Set<Reference> refs = new HashSet<>();
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addInterface(String name) {
		intf.add(name);
	}
	
	public void addReference(Reference reference) {
		refs.add(reference);
	}
	
	public String getName() {
		return name;
	}

	public Set<String> getInterfaces() {
		return intf;
	}

	public Set<Reference> getReferences() {
		return refs;
	}
	
	@Override
	public String toString() {
		return "BundleDescription [name=" + name + ", intf=" + intf + ", refs=" + refs + "]";
	}
	
}
