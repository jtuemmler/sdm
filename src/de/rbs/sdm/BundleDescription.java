package de.rbs.sdm;

import java.util.HashSet;
import java.util.Set;

import de.rbs.sdm.ComponentDescription.Reference;

public class BundleDescription {
	Set<ComponentDescription> components = new HashSet<>();
	
	public void addComponent(ComponentDescription cd) {
		components.add(cd);
	}
	
	public Set<ComponentDescription> getComponents() {
		return components;
	}
	
	public Set<String> getInterfaces() {
		Set<String> result = new HashSet<>();
		
		components.forEach(component -> result.addAll(component.getInterfaces()));
		
		return result;
	}

	public Set<Reference> getReferences() {
		Set<Reference> result = new HashSet<>();
		
		components.forEach(component -> result.addAll(component.getReferences()));
		
		return result;
	}
	

}
