package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hascoapi.vocabularies.VSTOI;

import static org.hascoapi.Constants.*;

@JsonFilter("platformInstanceFilter")
public class PlatformInstance extends VSTOIInstance {

	public PlatformInstance() {
		this.setTypeUri(VSTOI.PLATFORM_INSTANCE);
		this.setHascoTypeUri(VSTOI.PLATFORM_INSTANCE); 
	}

	public static PlatformInstance find(String uri) {
		PlatformInstance instance = new PlatformInstance();
		return (PlatformInstance)VSTOIInstance.find(instance,uri);
	} 

}
