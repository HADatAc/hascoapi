package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hascoapi.vocabularies.VSTOI;

import static org.hascoapi.Constants.*;

@JsonFilter("actuatorInstanceFilter")
public class ActuatorInstance extends VSTOIInstance {

	public ActuatorInstance() {
		this.setTypeUri(VSTOI.ACTUATOR_INSTANCE);
		this.setHascoTypeUri(VSTOI.ACTUATOR_INSTANCE); 
	}

	public static ActuatorInstance find(String uri) {
		ActuatorInstance instance = new ActuatorInstance();
		return (ActuatorInstance)VSTOIInstance.find(instance,uri);
	} 

}
