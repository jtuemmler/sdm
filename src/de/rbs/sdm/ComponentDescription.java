package de.rbs.sdm;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to store information about a component.
 * 
 * @author tuemmler
 *
 */
public class ComponentDescription {
	public static class Reference {
		public String name = "";
		public String cardinality = "";
		public String policy = "";
		
		@Override
		public String toString() {
			return "Reference [name=" + name + ", cardinality=" + cardinality + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cardinality == null) ? 0 : cardinality.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Reference other = (Reference) obj;
			if (cardinality == null) {
				if (other.cardinality != null)
					return false;
			} else if (!cardinality.equals(other.cardinality))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

	}
	
	private String name = "";
	private final Set<String> intf = new HashSet<>();
	private final Set<Reference> refs = new HashSet<>();
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void addInterface(String name) {
		intf.add(name);
	}
	
	public Set<String> getInterfaces() {
		return intf;
	}

	public void addReference(Reference reference) {
		refs.add(reference);
	}
	
	public Set<Reference> getReferences() {
		return refs;
	}
	
	@Override
	public String toString() {
		return "ComponentDescription [name=" + name + ", intf=" + intf + ", refs=" + refs + "]";
	}
	
}
