package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hascoapi.vocabularies.VSTOI;

import static org.hascoapi.Constants.*;

@JsonFilter("componentInstanceFilter")
public class ComponentInstance extends VSTOIInstance {

	public ComponentInstance() {
		this.setTypeUri(VSTOI.COMPONENT_INSTANCE);
		this.setHascoTypeUri(VSTOI.COMPONENT_INSTANCE); 
	}

	public static ComponentInstance find(String uri) {
		ComponentInstance instance = new ComponentInstance();
		return (ComponentInstance)VSTOIInstance.find(instance,uri);
	} 

}
