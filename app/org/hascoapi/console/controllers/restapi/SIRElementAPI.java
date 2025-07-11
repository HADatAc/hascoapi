package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.List;
import java.util.ArrayList;

public class SIRElementAPI extends Controller {

    /**
     *   CREATE ELEMENT
     */

    public Result createElement(String elementType, String json) {
        System.out.println("Type: [" + elementType + "]  JSON [" + json + "]");
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {
            return ok(ApiUtil.createResponse("No valid elementType has been provided", false));
        }
        boolean success = true;
        String message = "";
        ObjectMapper objectMapper = new ObjectMapper();
        if (clazz == Actuator.class) {
            try {
                Actuator object;
                object = (Actuator)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ActuatorStem.class) {
            try {
                ActuatorStem object;
                object = (ActuatorStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Annotation.class) {
            try {
                Annotation object;
                object = (Annotation)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == AnnotationStem.class) {
            try {
                AnnotationStem object;
                object = (AnnotationStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Attribute.class) {
            try {
                Attribute object;
                object = (Attribute)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Codebook.class) {
            try {
                Codebook object;
                object = (Codebook)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == CodebookSlot.class) {
            try {
                CodebookSlot object;
                object = (CodebookSlot)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ContainerSlot.class) {
            // NOTE: Use ContainerSlot.createContainerSlots(container,totContainerSlots) to create container slots
        } else if (clazz == DA.class) {
            try {
                DA object;
                object = (DA)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DataFile.class) {
            try {
                DataFile object;
                object = (DataFile)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DD.class) {
            try {
                DD object;
                object = (DD)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Deployment.class) {
            try {
                Deployment object;
                object = (Deployment)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Deployment: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Detector.class) {
            try {
                Detector object;
                object = (Detector)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DetectorInstance.class) {
            DetectorInstance object;
            try {
                object = (DetectorInstance)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DetectorStem.class) {
            try {
                DetectorStem object;
                object = (DetectorStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DetectorStemType.class) {
            try {
                DetectorStemType object;
                object = (DetectorStemType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DP2.class) {
            try {
                DP2 object;
                object = (DP2)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DSG.class) {
            try {
                DSG object;
                object = (DSG)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Entity.class) {
            try {
                Entity object;
                object = (Entity)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == FundingScheme.class) {
            try {
                FundingScheme object;
                System.out.println("FUNDING SCHEME: " + json);
                object = (FundingScheme)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == INS.class) {
            try {
                INS object;
                object = (INS)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Instrument.class) {
            Instrument object;
            try {
                object = (Instrument)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == InstrumentInstance.class) {
            InstrumentInstance object;
            try {
                object = (InstrumentInstance)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == InstrumentType.class) {
            try {
                InstrumentType object;
                object = (InstrumentType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == KGR.class) {
            try {
                KGR object;
                object = (KGR)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing KGR: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Organization.class) {
            try {
                Organization object;
                object = (Organization)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Organization: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Person.class) {
            try {
                Person object;
                object = (Person)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Person: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Place.class) {
            try {
                Place object;
                object = (Place)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Place: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Platform.class) {
            try {
                Platform object;
                object = (Platform)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Platform: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == PlatformInstance.class) {
            PlatformInstance object;
            try {
                object = (PlatformInstance)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == PossibleValue.class) {
            try {
                PossibleValue object;
                object = (PossibleValue)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == PostalAddress.class) {
            try {
                PostalAddress object;
                object = (PostalAddress)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing PostalAddress: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == org.hascoapi.entity.pojo.Process.class) {
            try {
                org.hascoapi.entity.pojo.Process object;
                object = (org.hascoapi.entity.pojo.Process)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Process: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ProcessStem.class) {
            try {
                ProcessStem object;
                object = (ProcessStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing ProcessStem: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Project.class) {
            try {
                Project object;
                object = (Project)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Project: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == RequiredComponent.class) {
            try {
                RequiredComponent object;
                object = (RequiredComponent)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == RequiredInstrument.class) {
            try {
                RequiredInstrument object;
                object = (RequiredInstrument)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ResponseOption.class) {
            try {
                ResponseOption object;
                object = (ResponseOption)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SDD.class) {
            try {
                SDD object;
                object = (SDD)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SDDAttribute.class) {
            try {
                SDDAttribute object;
                object = (SDDAttribute)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                e.printStackTrace();
                return ok(ApiUtil.createResponse("Following JSON Exception while parsing JSON for " + clazz + ": " + e.getMessage(), false));
            } catch (Exception e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SDDObject.class) {
            try {
                SDDObject object;
                object = (SDDObject)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SemanticDataDictionary.class) {
            try {
                SemanticDataDictionary object;
                object = (SemanticDataDictionary)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SemanticVariable.class) {
            try {
                SemanticVariable object;
                object = (SemanticVariable)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == STR.class) {
            try {
                STR object;
                object = (STR)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Stream.class) {
            try {
                Stream object;
                object = (Stream)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing Stream: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == StreamTopic.class) {
            try {
                StreamTopic object;
                object = (StreamTopic)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                System.out.println("Error processing StreamTopic: " + e.getMessage());
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Study.class) {
            try {
                Study object;
                object = (Study)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == StudyObject.class) {
            try {
                StudyObject object;
                object = (StudyObject)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == StudyObjectCollection.class) {
            try {
                StudyObjectCollection object;
                object = (StudyObjectCollection)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == StudyRole.class) {
            try {
                StudyRole object;
                object = (StudyRole)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Subcontainer.class) {
            try {
                Subcontainer object;
                object = (Subcontainer)objectMapper.readValue(json, clazz);
                //System.out.println("SIRElementAPI.create(Subcontainer): JSON=[" + json + "]");
                object.saveAsSlot();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Task.class) {
            try {
                Task object;
                object = (Task)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Unit.class) {
            try {
                Unit object;
                object = (Unit)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == VirtualColumn.class) {
            try {
                VirtualColumn object;
                object = (VirtualColumn)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } 
        if (!success) {
            return ok(ApiUtil.createResponse("Error processing JSON: " + message, false));
        }
        return ok(ApiUtil.createResponse("Element has been saved", true));
    }

    /**
     *   DELETE ELEMENT
     */

    public Result deleteElement(String elementType, String uri) {
        //System.out.println("Delete element => Type: [" + elementType + "]  URI [" + uri + "]");
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No uri has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {
            return ok(ApiUtil.createResponse("No valid elementType has been provided", false));
        }
        if (clazz == Actuator.class) {
            Actuator object = Actuator.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ActuatorStem.class) {
            ActuatorStem object = ActuatorStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Annotation.class) {
            Annotation object = Annotation.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == AnnotationStem.class) {
            AnnotationStem object = AnnotationStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Attribute.class) {
            Attribute object = Attribute.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Codebook.class) {
            Codebook object = Codebook.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == CodebookSlot.class) {
            CodebookSlot object = CodebookSlot.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ContainerSlot.class) {
            //ContainerSlot object = ContainerSlot.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.delete();
        } else if (clazz == DA.class) {
            DA object = DA.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DataFile.class) {
            DataFile object = DataFile.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DD.class) {
            DD object = DD.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Deployment.class) {
            Deployment object = Deployment.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Detector.class) {
            Detector object = Detector.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DetectorInstance.class) {
            DetectorInstance object = DetectorInstance.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DetectorStem.class) {
            DetectorStem object = DetectorStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DP2.class) {
            DP2 object = DP2.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DSG.class) {
            DSG object = DSG.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Entity.class) {
            Entity object = Entity.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == FundingScheme.class) {
            FundingScheme object = FundingScheme.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == INS.class) {
            INS object = INS.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Instrument.class) {
            Instrument object = Instrument.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == InstrumentInstance.class) {
            InstrumentInstance object = InstrumentInstance.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == KGR.class) {
            KGR object = KGR.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Organization.class) {
            Organization object = Organization.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Person.class) {
            Person object = Person.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Place.class) {
            Place object = Place.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Platform.class) {
            Platform object = Platform.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == PlatformInstance.class) {
            PlatformInstance object = PlatformInstance.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == PossibleValue.class) {
            PossibleValue object = PossibleValue.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == PostalAddress.class) {
            PostalAddress object = PostalAddress.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == org.hascoapi.entity.pojo.Process.class) {
            org.hascoapi.entity.pojo.Process object = org.hascoapi.entity.pojo.Process.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ProcessStem.class) {
            ProcessStem object = ProcessStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Project.class) {
            Project object = Project.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == RequiredComponent.class) {
            RequiredComponent object = RequiredComponent.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == RequiredInstrument.class) {
            RequiredInstrument object = RequiredInstrument.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ResponseOption.class) {
            ResponseOption object = ResponseOption.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SDD.class) {
            SDD object = SDD.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SDDAttribute.class) {
            SDDAttribute object = SDDAttribute.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SDDObject.class) {
            SDDObject object = SDDObject.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SemanticDataDictionary.class) {
            SemanticDataDictionary object = SemanticDataDictionary.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SemanticVariable.class) {
            SemanticVariable object = SemanticVariable.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SlotElement.class) {
            //Subcontainer object = Subcontainer.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.deleteAndDetach();
        } else if (clazz == STR.class) {
            STR object = STR.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Stream.class) {
            Stream object = Stream.find(uri);
            System.out.println("Ended at: "+ object.getEndedAt());
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == StreamTopic.class) {
            StreamTopic object = StreamTopic.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Study.class) {
            Study object = Study.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == StudyObject.class) {
            StudyObject object = StudyObject.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == StudyObjectCollection.class) {
            StudyObjectCollection object = StudyObjectCollection.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == StudyRole.class) {
            StudyRole object = StudyRole.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Subcontainer.class) {
            //Subcontainer object = Subcontainer.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.deleteAndDetach();
        } else if (clazz == Task.class) {
            Task object = Task.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            object.delete();
        } else if (clazz == Unit.class) {
            Unit object = Unit.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == VirtualColumn.class) {
            VirtualColumn object = VirtualColumn.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } 
        return ok(ApiUtil.createResponse("Element with URI [" + uri + "] has been deleted", true));
    }

    /**
     *   GET ELEMENTS WITH PAGE
     */

    public Result getElementsWithPage(String elementType, int pageSize, int offset) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        if (pageSize <=0) {
            return ok(ApiUtil.createResponse("Page size needs to be greated than zero", false));
        }
        if (offset < 0) {
            return ok(ApiUtil.createResponse("Offset needs to be igual or greater than zero", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        List<Object> results = (List<Object>)GenericFind.findWithPages(clazz,pageSize,offset);
        if (results != null && results.size() >= 0) {
            ObjectMapper mapper = HAScOMapper.getFilteredByClass("essential", clazz);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
        return ok(ApiUtil.createResponse("method getElements() failed to retrieve elements", false));    
    }

    public Result getTotalElements(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotal(clazz);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElements() failed to retrieve total number of element", false));
    }
    
    /**
     *   GET ELEMENTS BY KEYWORD WITH PAGE
     */

    public Result getElementsByKeywordWithPage(String elementType, String keyword, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByKeywordWithPages(Instrument.class,keyword, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("instrumentinstance")) {
            GenericFind<InstrumentInstance> query = new GenericFind<InstrumentInstance>();
            List<InstrumentInstance> results = query.findByKeywordWithPages(InstrumentInstance.class,keyword, pageSize, offset);
            return VSTOIInstanceAPI.getInstrumentInstances(results);
        } else if (elementType.equals("detectorinstance")) {
            GenericFind<DetectorInstance> query = new GenericFind<DetectorInstance>();
            List<DetectorInstance> results = query.findByKeywordWithPages(DetectorInstance.class,keyword, pageSize, offset);
            return VSTOIInstanceAPI.getDetectorInstances(results);
        } else if (elementType.equals("platforminstance")) {
            GenericFind<PlatformInstance> query = new GenericFind<PlatformInstance>();
            List<PlatformInstance> results = query.findByKeywordWithPages(PlatformInstance.class,keyword, pageSize, offset);
            return VSTOIInstanceAPI.getPlatformInstances(results);
        } else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByKeywordWithPages(DetectorStem.class,keyword, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByKeywordWithPages(Detector.class,keyword, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByKeywordWithPages(Codebook.class,keyword, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByKeywordWithPages(ResponseOption.class,keyword, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByKeywordWithPages(AnnotationStem.class,keyword, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        } else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByKeywordWithPages(Annotation.class,keyword, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        } else if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByKeywordWithPages(SemanticVariable.class,keyword, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("entity")) {
            GenericFind<Entity> query = new GenericFind<Entity>();
            List<Entity> results = query.findByKeywordWithPages(Entity.class,keyword, pageSize, offset);
            return EntityAPI.getEntities(results);
        }  else if (elementType.equals("attribute")) {
            GenericFind<Attribute> query = new GenericFind<Attribute>();
            List<Attribute> results = query.findByKeywordWithPages(Attribute.class,keyword, pageSize, offset);
            return AttributeAPI.getAttributes(results);
        }  else if (elementType.equals("unit")) {
            GenericFind<Unit> query = new GenericFind<Unit>();
            List<Unit> results = query.findByKeywordWithPages(Unit.class,keyword, pageSize, offset);
            return UnitAPI.getUnits(results);
        }  else if (elementType.equals("ins")) {
            GenericFind<INS> query = new GenericFind<INS>();
            List<INS> results = query.findByKeywordWithPages(INS.class,keyword, pageSize, offset);
            return INSAPI.getINSs(results);
        }  else if (elementType.equals("da")) {
            GenericFind<DA> query = new GenericFind<DA>();
            List<DA> results = query.findByKeywordWithPages(DA.class,keyword, pageSize, offset);
            return DAAPI.getDAs(results);
        }  else if (elementType.equals("dd")) {
            GenericFind<DD> query = new GenericFind<DD>();
            List<DD> results = query.findByKeywordWithPages(DD.class,keyword, pageSize, offset);
            return DDAPI.getDDs(results);
        }  else if (elementType.equals("sdd")) {
            GenericFind<SDD> query = new GenericFind<SDD>();
            List<SDD> results = query.findByKeywordWithPages(SDD.class,keyword, pageSize, offset);
            return SDDAPI.getSDDs(results);
        }  else if (elementType.equals("semanticdatadictionary")) {
            GenericFind<SemanticDataDictionary> query = new GenericFind<SemanticDataDictionary>();
            List<SemanticDataDictionary> results = query.findByKeywordWithPages(SemanticDataDictionary.class,keyword, pageSize, offset);
            return SemanticDataDictionaryAPI.getSemanticDataDictionaries(results);
        }  else if (elementType.equals("sddattribute")) {
            GenericFind<SDDAttribute> query = new GenericFind<SDDAttribute>();
            List<SDDAttribute> results = query.findByKeywordWithPages(SDDAttribute.class,keyword, pageSize, offset);
            return SemanticDataDictionaryAPI.getSDDAttributes(results);
        }  else if (elementType.equals("sddobject")) {
            GenericFind<SDDObject> query = new GenericFind<SDDObject>();
            List<SDDObject> results = query.findByKeywordWithPages(SDDObject.class,keyword, pageSize, offset);
            return SemanticDataDictionaryAPI.getSDDObjects(results);
        }  else if (elementType.equals("dp2")) {
            GenericFind<DP2> query = new GenericFind<DP2>();
            List<DP2> results = query.findByKeywordWithPages(DP2.class,keyword, pageSize, offset);
            return DP2API.getDP2s(results);
        }  else if (elementType.equals("str")) {
            GenericFind<STR> query = new GenericFind<STR>();
            List<STR> results = query.findByKeywordWithPages(STR.class,keyword, pageSize, offset);
            return STRAPI.getSTRs(results);
        }  else if (elementType.equals("datafile")) {
            GenericFind<DataFile> query = new GenericFind<DataFile>();
            List<DataFile> results = query.findByKeywordWithPages(DataFile.class,keyword, pageSize, offset);
            return DataFileAPI.getDataFiles(results);
        }  else if (elementType.equals("dsg")) {
            GenericFind<DSG> query = new GenericFind<DSG>();
            List<DSG> results = query.findByKeywordWithPages(DSG.class,keyword, pageSize, offset);
            return DSGAPI.getDSGs(results);
        }  else if (elementType.equals("study")) {
            GenericFind<Study> query = new GenericFind<Study>();
            List<Study> results = query.findByKeywordWithPages(Study.class,keyword, pageSize, offset);
            return StudyAPI.getStudies(results);
        }  else if (elementType.equals("studyobjectcollection")) {
            GenericFind<StudyObjectCollection> query = new GenericFind<StudyObjectCollection>();
            List<StudyObjectCollection> results = query.findByKeywordWithPages(StudyObjectCollection.class,keyword, pageSize, offset);
            return StudyObjectCollectionAPI.getStudyObjectCollections(results);
        }  else if (elementType.equals("studyobject")) {
            GenericFind<StudyObject> query = new GenericFind<StudyObject>();
            List<StudyObject> results = query.findByKeywordWithPages(StudyObject.class,keyword, pageSize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  else if (elementType.equals("studyrole")) {
            GenericFind<StudyRole> query = new GenericFind<StudyRole>();
            List<StudyRole> results = query.findByKeywordWithPages(StudyRole.class,keyword, pageSize, offset);
            return StudyRoleAPI.getStudyRoles(results);
        }  else if (elementType.equals("virtualcolumn")) {
            GenericFind<VirtualColumn> query = new GenericFind<VirtualColumn>();
            List<VirtualColumn> results = query.findByKeywordWithPages(VirtualColumn.class,keyword, pageSize, offset);
            return VirtualColumnAPI.getVirtualColumns(results);
        }  else if (elementType.equals("platform")) {
            GenericFind<Platform> query = new GenericFind<Platform>();
            List<Platform> results = query.findByKeywordWithPages(Platform.class,keyword, pageSize, offset);
            return PlatformAPI.getPlatforms(results);
        }  else if (elementType.equals("stream")) {
            GenericFind<Stream> query = new GenericFind<Stream>();
            List<Stream> results = query.findByKeywordWithPages(Stream.class,keyword, pageSize, offset);
            return StreamAPI.getStreams(results);
        }  else if (elementType.equals("deployment")) {
            GenericFind<Deployment> query = new GenericFind<Deployment>();
            List<Deployment> results = query.findByKeywordWithPages(Deployment.class,keyword, pageSize, offset);
            return DeploymentAPI.getDeployments(results);
        }  else if (elementType.equals("person")) {
            GenericFind<Person> query = new GenericFind<Person>();
            List<Person> results = query.findByKeywordWithPages(Person.class,keyword, pageSize, offset);
            return PersonAPI.getPeople(results);
        }  else if (elementType.equals("organization")) {
            GenericFind<Organization> query = new GenericFind<Organization>();
            List<Organization> results = query.findByKeywordWithPages(Organization.class,keyword, pageSize, offset);
            return OrganizationAPI.getOrganizations(results);
        }  else if (elementType.equals("place")) {
            GenericFind<Place> query = new GenericFind<Place>();
            List<Place> results = query.findByKeywordWithPages(Place.class,keyword, pageSize, offset);
            return PlaceAPI.getPlaces(results);
        }  else if (elementType.equals("postaladdress")) {
            GenericFind<PostalAddress> query = new GenericFind<PostalAddress>();
            List<PostalAddress> results = query.findByKeywordWithPages(PostalAddress.class,keyword, pageSize, offset);
            return PostalAddressAPI.getPostalAddresses(results);
        }  else if (elementType.equals("process")) {
            GenericFind<org.hascoapi.entity.pojo.Process> query = new GenericFind<org.hascoapi.entity.pojo.Process>();
            List<org.hascoapi.entity.pojo.Process> results = query.findByKeywordWithPages(org.hascoapi.entity.pojo.Process.class,keyword, pageSize, offset);
            return ProcessAPI.getProcesses(results);
        }  else if (elementType.equals("processstem")) {
            GenericFind<ProcessStem> query = new GenericFind<ProcessStem>();
            List<ProcessStem> results = query.findByKeywordWithPages(ProcessStem.class,keyword, pageSize, offset);
            return ProcessAPI.getProcessStems(results);
        }  else if (elementType.equals("kgr")) {
            GenericFind<KGR> query = new GenericFind<KGR>();
            List<KGR> results = query.findByKeywordWithPages(KGR.class,keyword, pageSize, offset);
            return KGRAPI.getKGRs(results);
        }  else if (elementType.equals("fundingscheme")) {
            GenericFind<FundingScheme> query = new GenericFind<FundingScheme>();
            List<FundingScheme> results = query.findByKeywordWithPages(FundingScheme.class,keyword, pageSize, offset);
            return FundingSchemeAPI.getFundingSchemes(results);
        }  else if (elementType.equals("project")) {
            GenericFind<Project> query = new GenericFind<Project>();
            List<Project> results = query.findByKeywordWithPages(Project.class,keyword, pageSize, offset);
            return ProjectAPI.getProjects(results);
        }  else if (elementType.equals("actuator")) {
            GenericFind<Actuator> query = new GenericFind<Actuator>();
            List<Actuator> results = query.findByKeywordWithPages(Actuator.class,keyword, pageSize, offset);
            return ActuatorAPI.getActuators(results);
        }  else if (elementType.equals("actuatorstem")) {
            GenericFind<ActuatorStem> query = new GenericFind<ActuatorStem>();
            List<ActuatorStem> results = query.findByKeywordWithPages(ActuatorStem.class,keyword, pageSize, offset);
            return ActuatorStemAPI.getActuatorStems(results);
        }  else if (elementType.equals("task")) {
            GenericFind<Task> query = new GenericFind<Task>();
            List<Task> results = query.findByKeywordWithPages(Task.class,keyword, pageSize, offset);
            return TaskAPI.getTasks(results);
        } 
        return ok("[getElementsByKeywordWithPage] No valid element type.");
    }

    public Result getTotalElementsByKeyword(String elementType, String keyword) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        int totalElements = -1;
        if (elementType.equals("component")) {
            Class clazz = GenericFind.getElementClass("detector");
            int totalDetectors = GenericFind.findTotalByKeyword(clazz, keyword);
            if (totalDetectors < 0) {
                totalDetectors = 0;
            }
            clazz = GenericFind.getElementClass("actuator");
            int totalActuators = GenericFind.findTotalByKeyword(clazz, keyword);
            if (totalActuators < 0) {
                totalActuators = 0;
            }
            totalElements = totalDetectors + totalActuators;
        } else if (elementType.equals("componentinstance")) {
            Class clazz = GenericFind.getElementClass("detectorinstance");
            int totalDetectorInstances = GenericFind.findTotalByKeyword(clazz, keyword);
            if (totalDetectorInstances < 0) {
                totalDetectorInstances = 0;
            }
            clazz = GenericFind.getElementClass("actuatorinstance");
            int totalActuatorInstances = GenericFind.findTotalByKeyword(clazz, keyword);
            if (totalActuatorInstances < 0) {
                totalActuatorInstances = 0;
            }
            totalElements = totalDetectorInstances + totalActuatorInstances;
        } else {
            Class clazz = GenericFind.getElementClass(elementType);
            if (clazz == null) {        
                return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
            }
            totalElements = GenericFind.findTotalByKeyword(clazz, keyword);
        }
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByKeyword() failed to retrieve total number of [" + elementType + "]", false));
    }
        
    /**
     *   GET ELEMENTS BY KEYWORD AND LANGUAGE WITH PAGE
     */
                
    public Result getElementsByKeywordAndLanguageWithPage(String elementType, String keyword, String language, String type, String manageremail, String status, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (type.equals("_")) {
            type = "";
        }
        if (manageremail.equals("_")) {
            manageremail = "";
        }
        if (status.equals("_")) {
            status = "";
        }
        if (elementType.equals("actuator")) {
            GenericFind<Actuator> query = new GenericFind<Actuator>();
            List<Actuator> results = query.findByKeywordAndLanguageWithPages(Actuator.class, keyword, language, type, manageremail, status, pageSize, offset);
            return ActuatorAPI.getActuators(results);
        } else if (elementType.equals("actuatorstem")) {
            GenericFind<ActuatorStem> query = new GenericFind<ActuatorStem>();
            List<ActuatorStem> results = query.findByKeywordAndLanguageWithPages(ActuatorStem.class, keyword, language, type, manageremail, status, pageSize, offset);
            return ActuatorStemAPI.getActuatorStems(results);
        } else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByKeywordAndLanguageWithPages(Annotation.class, keyword, language, type, manageremail, status, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        } else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByKeywordAndLanguageWithPages(AnnotationStem.class, keyword, language, type, manageremail, status, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        }  else if (elementType.equals("attribute")) {
            GenericFind<Attribute> query = new GenericFind<Attribute>();
            List<Attribute> results = query.findByKeywordAndLanguageWithPages(Attribute.class, keyword, language, type, manageremail, status, pageSize, offset);
            return AttributeAPI.getAttributes(results);
        } else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByKeywordAndLanguageWithPages(Codebook.class, keyword, language, type, manageremail, status, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByKeywordAndLanguageWithPages(Detector.class, keyword, language, type, manageremail, status, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByKeywordAndLanguageWithPages(DetectorStem.class, keyword, language, type, manageremail, status, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("entity")) {
            GenericFind<Entity> query = new GenericFind<Entity>();
            List<Entity> results = query.findByKeywordAndLanguageWithPages(Entity.class, keyword, language, type, manageremail, status, pageSize, offset);
            return EntityAPI.getEntities(results);
        } else if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByKeywordAndLanguageWithPages(Instrument.class, keyword, language, type, manageremail, status, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        }  else if (elementType.equals("process")) {
            GenericFind<org.hascoapi.entity.pojo.Process> query = new GenericFind<org.hascoapi.entity.pojo.Process>();
            List<org.hascoapi.entity.pojo.Process> results = query.findByKeywordAndLanguageWithPages(org.hascoapi.entity.pojo.Process.class, keyword, language, type, manageremail, status, pageSize, offset);
            return ProcessAPI.getProcesses(results);
        }  else if (elementType.equals("processstem")) {
            GenericFind<ProcessStem> query = new GenericFind<ProcessStem>();
            List<ProcessStem> results = query.findByKeywordAndLanguageWithPages(ProcessStem.class, keyword, language, type, manageremail, status, pageSize, offset);
            return ProcessAPI.getProcessStems(results);
        } else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByKeywordAndLanguageWithPages(ResponseOption.class, keyword, language, type, manageremail, status, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByKeywordAndLanguageWithPages(SemanticVariable.class, keyword, language, type, manageremail, status, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        }  else if (elementType.equals("unit")) {
            GenericFind<Unit> query = new GenericFind<Unit>();
            List<Unit> results = query.findByKeywordAndLanguageWithPages(Unit.class, keyword, language, type, manageremail, status, pageSize, offset);
            return UnitAPI.getUnits(results);
        } 
        return ok("[getElementsByKeywordAndLanguageWithPage] No valid element type.");
    }

    public Result getTotalElementsByKeywordAndLanguage(String elementType, String keyword, String language, String type, String manageremail, String status) {
        //System.out.println("ElementType: " + elementType);
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (type.equals("_")) {
            type = "";
        }
        if (manageremail.equals("_")) {
            manageremail = "";
        }
        if (status.equals("_")) {
            status = "";
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotalByKeywordAndLanguage(clazz, keyword, language, type, manageremail, status);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByKeyword() failed to retrieve total number of [" + elementType + "]", false));
    }
        
    /**
     *   GET ELEMENTS BY MANAGER EMAIL WITH PAGE
     */

    public Result getElementsByManagerEmail(String elementType, String managerEmail, int pageSize, int offset) {
        if (managerEmail == null || managerEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No Manager Email has been provided", false));
        }
        if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByManagerEmailWithPages(SemanticVariable.class, managerEmail, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByManagerEmailWithPages(Instrument.class, managerEmail, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("instrumentinstance")) {
            GenericFind<InstrumentInstance> query = new GenericFind<InstrumentInstance>();
            List<InstrumentInstance> results = query.findByManagerEmailWithPages(InstrumentInstance.class, managerEmail, pageSize, offset);
            return VSTOIInstanceAPI.getInstrumentInstances(results);
        } else if (elementType.equals("detectorinstance")) {
            GenericFind<DetectorInstance> query = new GenericFind<DetectorInstance>();
            List<DetectorInstance> results = query.findByManagerEmailWithPages(DetectorInstance.class, managerEmail, pageSize, offset);
            return VSTOIInstanceAPI.getDetectorInstances(results);
        } else if (elementType.equals("platforminstance")) {
            GenericFind<PlatformInstance> query = new GenericFind<PlatformInstance>();
            List<PlatformInstance> results = query.findByManagerEmailWithPages(PlatformInstance.class, managerEmail, pageSize, offset);
            return VSTOIInstanceAPI.getPlatformInstances(results);
        }  else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByManagerEmailWithPages(DetectorStem.class, managerEmail, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        }  else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByManagerEmailWithPages(Detector.class, managerEmail, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        }  else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByManagerEmailWithPages(Codebook.class, managerEmail, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        }  else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByManagerEmailWithPages(ResponseOption.class, managerEmail, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }  else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByManagerEmailWithPages(AnnotationStem.class, managerEmail, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        }  else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByManagerEmailWithPages(Annotation.class, managerEmail, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        }  else if (elementType.equals("ins")) {
            GenericFind<INS> query = new GenericFind<INS>();
            List<INS> results = query.findByManagerEmailWithPages(INS.class, managerEmail, pageSize, offset);
            return INSAPI.getINSs(results);
        }  else if (elementType.equals("da")) {
            GenericFind<DA> query = new GenericFind<DA>();
            List<DA> results = query.findByManagerEmailWithPages(DA.class, managerEmail, pageSize, offset);
            return DAAPI.getDAs(results);
        }  else if (elementType.equals("dd")) {
            GenericFind<DD> query = new GenericFind<DD>();
            List<DD> results = query.findByManagerEmailWithPages(DD.class, managerEmail, pageSize, offset);
            return DDAPI.getDDs(results);
        }  else if (elementType.equals("sdd")) {
            GenericFind<SDD> query = new GenericFind<SDD>();
            List<SDD> results = query.findByManagerEmailWithPages(SDD.class, managerEmail, pageSize, offset);
            return SDDAPI.getSDDs(results);
        }  else if (elementType.equals("semanticdatadictionary")) {
            GenericFind<SemanticDataDictionary> query = new GenericFind<SemanticDataDictionary>();
            List<SemanticDataDictionary> results = query.findByManagerEmailWithPages(SemanticDataDictionary.class, managerEmail, pageSize, offset);
            return SemanticDataDictionaryAPI.getSemanticDataDictionaries(results);
        }  else if (elementType.equals("dp2")) {
            GenericFind<DP2> query = new GenericFind<DP2>();
            List<DP2> results = query.findByManagerEmailWithPages(DP2.class, managerEmail, pageSize, offset);
            return DP2API.getDP2s(results);
        }  else if (elementType.equals("str")) {
            GenericFind<STR> query = new GenericFind<STR>();
            List<STR> results = query.findByManagerEmailWithPages(STR.class, managerEmail, pageSize, offset);
            return STRAPI.getSTRs(results);
        }  else if (elementType.equals("datafile")) {
            GenericFind<DataFile> query = new GenericFind<DataFile>();
            List<DataFile> results = query.findByManagerEmailWithPages(DataFile.class, managerEmail, pageSize, offset);
            return DataFileAPI.getDataFiles(results);
        }  else if (elementType.equals("dsg")) {
            GenericFind<DSG> query = new GenericFind<DSG>();
            List<DSG> results = query.findByManagerEmailWithPages(DSG.class, managerEmail, pageSize, offset);
            return DSGAPI.getDSGs(results);
        }  else if (elementType.equals("study")) {
            GenericFind<Study> query = new GenericFind<Study>();
            List<Study> results = query.findByManagerEmailWithPages(Study.class, managerEmail, pageSize, offset);
            return StudyAPI.getStudies(results);
        }  else if (elementType.equals("studyobjectcollection")) {
            GenericFind<StudyObjectCollection> query = new GenericFind<StudyObjectCollection>();
            List<StudyObjectCollection> results = query.findByManagerEmailWithPages(StudyObjectCollection.class, managerEmail, pageSize, offset);
            return StudyObjectCollectionAPI.getStudyObjectCollections(results);
        }  else if (elementType.equals("studyobject")) {
            GenericFind<StudyObject> query = new GenericFind<StudyObject>();
            List<StudyObject> results = query.findByManagerEmailWithPages(StudyObject.class, managerEmail, pageSize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  else if (elementType.equals("studyrole")) {
            GenericFind<StudyRole> query = new GenericFind<StudyRole>();
            List<StudyRole> results = query.findByManagerEmailWithPages(StudyRole.class, managerEmail, pageSize, offset);
            return StudyRoleAPI.getStudyRoles(results);
        }  else if (elementType.equals("virtualcolumn")) {
            GenericFind<VirtualColumn> query = new GenericFind<VirtualColumn>();
            List<VirtualColumn> results = query.findByManagerEmailWithPages(VirtualColumn.class, managerEmail, pageSize, offset);
            return VirtualColumnAPI.getVirtualColumns(results);
        }  else if (elementType.equals("platform")) {
            GenericFind<Platform> query = new GenericFind<Platform>();
            List<Platform> results = query.findByManagerEmailWithPages(Platform.class, managerEmail, pageSize, offset);
            return PlatformAPI.getPlatforms(results);
        }  else if (elementType.equals("stream")) {
            GenericFind<Stream> query = new GenericFind<Stream>();
            List<Stream> results = query.findByManagerEmailWithPages(Stream.class, managerEmail, pageSize, offset);
            return StreamAPI.getStreams(results);
        }  else if (elementType.equals("deployment")) {
            GenericFind<Deployment> query = new GenericFind<Deployment>();
            List<Deployment> results = query.findByManagerEmailWithPages(Deployment.class, managerEmail, pageSize, offset);
            return DeploymentAPI.getDeployments(results);
        }  else if (elementType.equals("person")) {
            GenericFind<Person> query = new GenericFind<Person>();
            List<Person> results = query.findByManagerEmailWithPages(Person.class, managerEmail, pageSize, offset);
            return PersonAPI.getPeople(results);
        }  else if (elementType.equals("organization")) {
            GenericFind<Organization> query = new GenericFind<Organization>();
            List<Organization> results = query.findByManagerEmailWithPages(Organization.class, managerEmail, pageSize, offset);
            return OrganizationAPI.getOrganizations(results);
        }  else if (elementType.equals("place")) {
            GenericFind<Place> query = new GenericFind<Place>();
            List<Place> results = query.findByManagerEmailWithPages(Place.class, managerEmail, pageSize, offset);
            return PlaceAPI.getPlaces(results);
        }  else if (elementType.equals("postaladdress")) {
            GenericFind<PostalAddress> query = new GenericFind<PostalAddress>();
            List<PostalAddress> results = query.findByManagerEmailWithPages(PostalAddress.class, managerEmail, pageSize, offset);
            return PostalAddressAPI.getPostalAddresses(results);
        }  else if (elementType.equals("process")) {
            GenericFind<org.hascoapi.entity.pojo.Process> query = new GenericFind<org.hascoapi.entity.pojo.Process>();
            List<org.hascoapi.entity.pojo.Process> results = query.findByManagerEmailWithPages(org.hascoapi.entity.pojo.Process.class, managerEmail, pageSize, offset);
            return ProcessAPI.getProcesses(results);
        }  else if (elementType.equals("processstem")) {
            GenericFind<ProcessStem> query = new GenericFind<ProcessStem>();
            List<ProcessStem> results = query.findByManagerEmailWithPages(ProcessStem.class, managerEmail, pageSize, offset);
            return ProcessAPI.getProcessStems(results);
        }  else if (elementType.equals("kgr")) {
            GenericFind<KGR> query = new GenericFind<KGR>();
            List<KGR> results = query.findByManagerEmailWithPages(KGR.class, managerEmail, pageSize, offset);
            return KGRAPI.getKGRs(results);
        }  else if (elementType.equals("fundingscheme")) {
            GenericFind<FundingScheme> query = new GenericFind<FundingScheme>();
            List<FundingScheme> results = query.findByManagerEmailWithPages(FundingScheme.class, managerEmail, pageSize, offset);
            return FundingSchemeAPI.getFundingSchemes(results);
        }  else if (elementType.equals("project")) {
            GenericFind<Project> query = new GenericFind<Project>();
            List<Project> results = query.findByManagerEmailWithPages(Project.class, managerEmail, pageSize, offset);
            return ProjectAPI.getProjects(results);
        }  else if (elementType.equals("actuatorstem")) {
            GenericFind<ActuatorStem> query = new GenericFind<ActuatorStem>();
            List<ActuatorStem> results = query.findByManagerEmailWithPages(ActuatorStem.class, managerEmail, pageSize, offset);
            return ActuatorStemAPI.getActuatorStems(results);
        }  else if (elementType.equals("actuator")) {
            GenericFind<Actuator> query = new GenericFind<Actuator>();
            List<Actuator> results = query.findByManagerEmailWithPages(Actuator.class, managerEmail, pageSize, offset);
            return ActuatorAPI.getActuators(results);
        }  else if (elementType.equals("task")) {
            GenericFind<Task> query = new GenericFind<Task>();
            List<Task> results = query.findByManagerEmailWithPages(Task.class, managerEmail, pageSize, offset);
            return TaskAPI.getTasks(results);
        } 
        return ok("[getElementsByManagerEmail] No valid element type.");

    }

    public Result getTotalElementsByManagerEmail(String elementType, String managerEmail){
        //System.out.println("SIRElementAPI: getTotalElementsByManagerEmail");
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        
        int totalElements = totalElements = GenericFind.findTotalByManagerEmail(clazz, managerEmail);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByManagerEmail() failed to retrieve total number of element", false));

    }

    /**
     *   GET ELEMENTS BY STATUS WITH PAGE
     */

     public Result getElementsByStatus(String elementType, String hasStatus, int pageSize, int offset) {
        if (hasStatus == null || hasStatus.isEmpty()) {
            return ok(ApiUtil.createResponse("No status has been provided", false));
        }
        if (elementType.equals("semanticvariable")) {
            GenericFindWithStatus<SemanticVariable> query = new GenericFindWithStatus<SemanticVariable>();
            List<SemanticVariable> results = query.findByStatusWithPages(SemanticVariable.class, hasStatus, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("instrument")) {
            GenericFindWithStatus<Instrument> query = new GenericFindWithStatus<Instrument>();
            List<Instrument> results = query.findByStatusWithPages(Instrument.class, hasStatus, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("instrumentinstance")) {
            GenericFindWithStatus<InstrumentInstance> query = new GenericFindWithStatus<InstrumentInstance>();
            List<InstrumentInstance> results = query.findByStatusWithPages(InstrumentInstance.class, hasStatus, pageSize, offset);
            return VSTOIInstanceAPI.getInstrumentInstances(results);
        } else if (elementType.equals("detectorinstance")) {
            GenericFindWithStatus<DetectorInstance> query = new GenericFindWithStatus<DetectorInstance>();
            List<DetectorInstance> results = query.findByStatusWithPages(DetectorInstance.class, hasStatus, pageSize, offset);
            return VSTOIInstanceAPI.getDetectorInstances(results);
        } else if (elementType.equals("platforminstance")) {
            GenericFindWithStatus<PlatformInstance> query = new GenericFindWithStatus<PlatformInstance>();
            List<PlatformInstance> results = query.findByStatusWithPages(PlatformInstance.class, hasStatus, pageSize, offset);
            return VSTOIInstanceAPI.getPlatformInstances(results);
        }  else if (elementType.equals("detectorstem")) {
            GenericFindWithStatus<DetectorStem> query = new GenericFindWithStatus<DetectorStem>();
            List<DetectorStem> results = query.findByStatusWithPages(DetectorStem.class, hasStatus, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        }  else if (elementType.equals("detector")) {
            GenericFindWithStatus<Detector> query = new GenericFindWithStatus<Detector>();
            List<Detector> results = query.findByStatusWithPages(Detector.class, hasStatus, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        }  else if (elementType.equals("codebook")) {
            GenericFindWithStatus<Codebook> query = new GenericFindWithStatus<Codebook>();
            List<Codebook> results = query.findByStatusWithPages(Codebook.class, hasStatus, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        }  else if (elementType.equals("responseoption")) {
            GenericFindWithStatus<ResponseOption> query = new GenericFindWithStatus<ResponseOption>();
            List<ResponseOption> results = query.findByStatusWithPages(ResponseOption.class, hasStatus, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }  else if (elementType.equals("annotationstem")) {
            GenericFindWithStatus<AnnotationStem> query = new GenericFindWithStatus<AnnotationStem>();
            List<AnnotationStem> results = query.findByStatusWithPages(AnnotationStem.class, hasStatus, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        }  else if (elementType.equals("annotation")) {
            GenericFindWithStatus<Annotation> query = new GenericFindWithStatus<Annotation>();
            List<Annotation> results = query.findByStatusWithPages(Annotation.class, hasStatus, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        }  else if (elementType.equals("ins")) {
            GenericFindWithStatus<INS> query = new GenericFindWithStatus<INS>();
            List<INS> results = query.findByStatusWithPages(INS.class, hasStatus, pageSize, offset);
            return INSAPI.getINSs(results);
        }  else if (elementType.equals("da")) {
            GenericFindWithStatus<DA> query = new GenericFindWithStatus<DA>();
            List<DA> results = query.findByStatusWithPages(DA.class, hasStatus, pageSize, offset);
            return DAAPI.getDAs(results);
        }  else if (elementType.equals("dd")) {
            GenericFindWithStatus<DD> query = new GenericFindWithStatus<DD>();
            List<DD> results = query.findByStatusWithPages(DD.class, hasStatus, pageSize, offset);
            return DDAPI.getDDs(results);
        }  else if (elementType.equals("sdd")) {
            GenericFindWithStatus<SDD> query = new GenericFindWithStatus<SDD>();
            List<SDD> results = query.findByStatusWithPages(SDD.class, hasStatus, pageSize, offset);
            return SDDAPI.getSDDs(results);
        }  else if (elementType.equals("semanticdatadictionary")) {
            GenericFindWithStatus<SemanticDataDictionary> query = new GenericFindWithStatus<SemanticDataDictionary>();
            List<SemanticDataDictionary> results = query.findByStatusWithPages(SemanticDataDictionary.class, hasStatus, pageSize, offset);
            return SemanticDataDictionaryAPI.getSemanticDataDictionaries(results);
        }  else if (elementType.equals("dp2")) {
            GenericFindWithStatus<DP2> query = new GenericFindWithStatus<DP2>();
            List<DP2> results = query.findByStatusWithPages(DP2.class, hasStatus, pageSize, offset);
            return DP2API.getDP2s(results);
        }  else if (elementType.equals("str")) {
            GenericFindWithStatus<STR> query = new GenericFindWithStatus<STR>();
            List<STR> results = query.findByStatusWithPages(STR.class, hasStatus, pageSize, offset);
            return STRAPI.getSTRs(results);
        }  else if (elementType.equals("datafile")) {
            GenericFindWithStatus<DataFile> query = new GenericFindWithStatus<DataFile>();
            List<DataFile> results = query.findByStatusWithPages(DataFile.class, hasStatus, pageSize, offset);
            return DataFileAPI.getDataFiles(results);
        }  else if (elementType.equals("dsg")) {
            GenericFindWithStatus<DSG> query = new GenericFindWithStatus<DSG>();
            List<DSG> results = query.findByStatusWithPages(DSG.class, hasStatus, pageSize, offset);
            return DSGAPI.getDSGs(results);
        }  else if (elementType.equals("study")) {
            GenericFindWithStatus<Study> query = new GenericFindWithStatus<Study>();
            List<Study> results = query.findByStatusWithPages(Study.class, hasStatus, pageSize, offset);
            return StudyAPI.getStudies(results);
        }  else if (elementType.equals("studyobjectcollection")) {
            GenericFindWithStatus<StudyObjectCollection> query = new GenericFindWithStatus<StudyObjectCollection>();
            List<StudyObjectCollection> results = query.findByStatusWithPages(StudyObjectCollection.class, hasStatus, pageSize, offset);
            return StudyObjectCollectionAPI.getStudyObjectCollections(results);
        }  else if (elementType.equals("studyobject")) {
            GenericFindWithStatus<StudyObject> query = new GenericFindWithStatus<StudyObject>();
            List<StudyObject> results = query.findByStatusWithPages(StudyObject.class, hasStatus, pageSize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  else if (elementType.equals("studyrole")) {
            GenericFindWithStatus<StudyRole> query = new GenericFindWithStatus<StudyRole>();
            List<StudyRole> results = query.findByStatusWithPages(StudyRole.class, hasStatus, pageSize, offset);
            return StudyRoleAPI.getStudyRoles(results);
        }  else if (elementType.equals("virtualcolumn")) {
            GenericFindWithStatus<VirtualColumn> query = new GenericFindWithStatus<VirtualColumn>();
            List<VirtualColumn> results = query.findByStatusWithPages(VirtualColumn.class, hasStatus, pageSize, offset);
            return VirtualColumnAPI.getVirtualColumns(results);
        }  else if (elementType.equals("platform")) {
            GenericFindWithStatus<Platform> query = new GenericFindWithStatus<Platform>();
            List<Platform> results = query.findByStatusWithPages(Platform.class, hasStatus, pageSize, offset);
            return PlatformAPI.getPlatforms(results);
        }  else if (elementType.equals("stream")) {
            GenericFindWithStatus<Stream> query = new GenericFindWithStatus<Stream>();
            List<Stream> results = query.findByStatusWithPages(Stream.class, hasStatus, pageSize, offset);
            return StreamAPI.getStreams(results);
        }  else if (elementType.equals("deployment")) {
            GenericFindWithStatus<Deployment> query = new GenericFindWithStatus<Deployment>();
            List<Deployment> results = query.findByStatusWithPages(Deployment.class, hasStatus, pageSize, offset);
            return DeploymentAPI.getDeployments(results);
        }  else if (elementType.equals("person")) {
            GenericFindWithStatus<Person> query = new GenericFindWithStatus<Person>();
            List<Person> results = query.findByStatusWithPages(Person.class, hasStatus, pageSize, offset);
            return PersonAPI.getPeople(results);
        }  else if (elementType.equals("organization")) {
            GenericFindWithStatus<Organization> query = new GenericFindWithStatus<Organization>();
            List<Organization> results = query.findByStatusWithPages(Organization.class, hasStatus, pageSize, offset);
            return OrganizationAPI.getOrganizations(results);
        }  else if (elementType.equals("place")) {
            GenericFindWithStatus<Place> query = new GenericFindWithStatus<Place>();
            List<Place> results = query.findByStatusWithPages(Place.class, hasStatus, pageSize, offset);
            return PlaceAPI.getPlaces(results);
        }  else if (elementType.equals("postaladdress")) {
            GenericFindWithStatus<PostalAddress> query = new GenericFindWithStatus<PostalAddress>();
            List<PostalAddress> results = query.findByStatusWithPages(PostalAddress.class, hasStatus, pageSize, offset);
            return PostalAddressAPI.getPostalAddresses(results);
        }  else if (elementType.equals("process")) {
            GenericFindWithStatus<org.hascoapi.entity.pojo.Process> query = new GenericFindWithStatus<org.hascoapi.entity.pojo.Process>();
            List<org.hascoapi.entity.pojo.Process> results = query.findByStatusWithPages(org.hascoapi.entity.pojo.Process.class, hasStatus, pageSize, offset);
            return ProcessAPI.getProcesses(results);
        }  else if (elementType.equals("processstem")) {
            GenericFindWithStatus<ProcessStem> query = new GenericFindWithStatus<ProcessStem>();
            List<ProcessStem> results = query.findByStatusWithPages(ProcessStem.class, hasStatus, pageSize, offset);
            return ProcessAPI.getProcessStems(results);
        }  else if (elementType.equals("kgr")) {
            GenericFindWithStatus<KGR> query = new GenericFindWithStatus<KGR>();
            List<KGR> results = query.findByStatusWithPages(KGR.class, hasStatus, pageSize, offset);
            return KGRAPI.getKGRs(results);
        }  else if (elementType.equals("fundingscheme")) {
            GenericFindWithStatus<FundingScheme> query = new GenericFindWithStatus<FundingScheme>();
            List<FundingScheme> results = query.findByStatusWithPages(FundingScheme.class, hasStatus, pageSize, offset);
            return FundingSchemeAPI.getFundingSchemes(results);
        }  else if (elementType.equals("project")) {
            GenericFindWithStatus<Project> query = new GenericFindWithStatus<Project>();
            List<Project> results = query.findByStatusWithPages(Project.class, hasStatus, pageSize, offset);
            return ProjectAPI.getProjects(results);
        }  else if (elementType.equals("actuatorstem")) {
            GenericFindWithStatus<ActuatorStem> query = new GenericFindWithStatus<ActuatorStem>();
            List<ActuatorStem> results = query.findByStatusWithPages(ActuatorStem.class, hasStatus, pageSize, offset);
            return ActuatorStemAPI.getActuatorStems(results);
        }  else if (elementType.equals("actuator")) {
            GenericFindWithStatus<Actuator> query = new GenericFindWithStatus<Actuator>();
            List<Actuator> results = query.findByStatusWithPages(Actuator.class, hasStatus, pageSize, offset);
            return ActuatorAPI.getActuators(results);
        }  else if (elementType.equals("task")) {
            GenericFindWithStatus<Task> query = new GenericFindWithStatus<Task>();
            List<Task> results = query.findByStatusWithPages(Task.class, hasStatus, pageSize, offset);
            return TaskAPI.getTasks(results);
        } 
        return ok("[getElementsByStatusManagerEmail] No valid element type.");

    }

    public Result getTotalElementsByStatus(String elementType, String hasStatus){
        //System.out.println("SIRElementAPI: getTotalElementsByStatus");
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        
        int totalElements = totalElements = GenericFindWithStatus.findTotalByStatus(clazz, hasStatus);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByStatus() failed to retrieve total number of element", false));

    }

    /**
     *   GET ELEMENTS BY STATUS AND MANAGER EMAIL WITH PAGE
     */

     public Result getElementsByStatusManagerEmail(String elementType, String hasStatus, String managerEmail, boolean withCurrent, int pageSize, int offset) {
        if (managerEmail == null || managerEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No Manager Email has been provided", false));
        }
        if (elementType.equals("semanticvariable")) {
            GenericFindWithStatus<SemanticVariable> query = new GenericFindWithStatus<SemanticVariable>();
            List<SemanticVariable> results = query.findByStatusManagerEmailWithPages(SemanticVariable.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("instrument")) {
            GenericFindWithStatus<Instrument> query = new GenericFindWithStatus<Instrument>();
            List<Instrument> results = query.findByStatusManagerEmailWithPages(Instrument.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("instrumentinstance")) {
            GenericFindWithStatus<InstrumentInstance> query = new GenericFindWithStatus<InstrumentInstance>();
            List<InstrumentInstance> results = query.findByStatusManagerEmailWithPages(InstrumentInstance.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return VSTOIInstanceAPI.getInstrumentInstances(results);
        } else if (elementType.equals("detectorinstance")) {
            GenericFindWithStatus<DetectorInstance> query = new GenericFindWithStatus<DetectorInstance>();
            List<DetectorInstance> results = query.findByStatusManagerEmailWithPages(DetectorInstance.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return VSTOIInstanceAPI.getDetectorInstances(results);
        } else if (elementType.equals("platforminstance")) {
            GenericFindWithStatus<PlatformInstance> query = new GenericFindWithStatus<PlatformInstance>();
            List<PlatformInstance> results = query.findByStatusManagerEmailWithPages(PlatformInstance.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return VSTOIInstanceAPI.getPlatformInstances(results);
        }  else if (elementType.equals("detectorstem")) {
            GenericFindWithStatus<DetectorStem> query = new GenericFindWithStatus<DetectorStem>();
            List<DetectorStem> results = query.findByStatusManagerEmailWithPages(DetectorStem.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        }  else if (elementType.equals("detector")) {
            GenericFindWithStatus<Detector> query = new GenericFindWithStatus<Detector>();
            List<Detector> results = query.findByStatusManagerEmailWithPages(Detector.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        }  else if (elementType.equals("codebook")) {
            GenericFindWithStatus<Codebook> query = new GenericFindWithStatus<Codebook>();
            List<Codebook> results = query.findByStatusManagerEmailWithPages(Codebook.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        }  else if (elementType.equals("responseoption")) {
            GenericFindWithStatus<ResponseOption> query = new GenericFindWithStatus<ResponseOption>();
            List<ResponseOption> results = query.findByStatusManagerEmailWithPages(ResponseOption.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }  else if (elementType.equals("annotationstem")) {
            GenericFindWithStatus<AnnotationStem> query = new GenericFindWithStatus<AnnotationStem>();
            List<AnnotationStem> results = query.findByStatusManagerEmailWithPages(AnnotationStem.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        }  else if (elementType.equals("annotation")) {
            GenericFindWithStatus<Annotation> query = new GenericFindWithStatus<Annotation>();
            List<Annotation> results = query.findByStatusManagerEmailWithPages(Annotation.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        }  else if (elementType.equals("ins")) {
            GenericFindWithStatus<INS> query = new GenericFindWithStatus<INS>();
            List<INS> results = query.findByStatusManagerEmailWithPages(INS.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return INSAPI.getINSs(results);
        }  else if (elementType.equals("da")) {
            GenericFindWithStatus<DA> query = new GenericFindWithStatus<DA>();
            List<DA> results = query.findByStatusManagerEmailWithPages(DA.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DAAPI.getDAs(results);
        }  else if (elementType.equals("dd")) {
            GenericFindWithStatus<DD> query = new GenericFindWithStatus<DD>();
            List<DD> results = query.findByStatusManagerEmailWithPages(DD.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DDAPI.getDDs(results);
        }  else if (elementType.equals("sdd")) {
            GenericFindWithStatus<SDD> query = new GenericFindWithStatus<SDD>();
            List<SDD> results = query.findByStatusManagerEmailWithPages(SDD.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return SDDAPI.getSDDs(results);
        }  else if (elementType.equals("semanticdatadictionary")) {
            GenericFindWithStatus<SemanticDataDictionary> query = new GenericFindWithStatus<SemanticDataDictionary>();
            List<SemanticDataDictionary> results = query.findByStatusManagerEmailWithPages(SemanticDataDictionary.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return SemanticDataDictionaryAPI.getSemanticDataDictionaries(results);
        }  else if (elementType.equals("dp2")) {
            GenericFindWithStatus<DP2> query = new GenericFindWithStatus<DP2>();
            List<DP2> results = query.findByStatusManagerEmailWithPages(DP2.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DP2API.getDP2s(results);
        }  else if (elementType.equals("str")) {
            GenericFindWithStatus<STR> query = new GenericFindWithStatus<STR>();
            List<STR> results = query.findByStatusManagerEmailWithPages(STR.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return STRAPI.getSTRs(results);
        }  else if (elementType.equals("datafile")) {
            GenericFindWithStatus<DataFile> query = new GenericFindWithStatus<DataFile>();
            List<DataFile> results = query.findByStatusManagerEmailWithPages(DataFile.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DataFileAPI.getDataFiles(results);
        }  else if (elementType.equals("dsg")) {
            GenericFindWithStatus<DSG> query = new GenericFindWithStatus<DSG>();
            List<DSG> results = query.findByStatusManagerEmailWithPages(DSG.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DSGAPI.getDSGs(results);
        }  else if (elementType.equals("study")) {
            GenericFindWithStatus<Study> query = new GenericFindWithStatus<Study>();
            List<Study> results = query.findByStatusManagerEmailWithPages(Study.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return StudyAPI.getStudies(results);
        }  else if (elementType.equals("studyobjectcollection")) {
            GenericFindWithStatus<StudyObjectCollection> query = new GenericFindWithStatus<StudyObjectCollection>();
            List<StudyObjectCollection> results = query.findByStatusManagerEmailWithPages(StudyObjectCollection.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return StudyObjectCollectionAPI.getStudyObjectCollections(results);
        }  else if (elementType.equals("studyobject")) {
            GenericFindWithStatus<StudyObject> query = new GenericFindWithStatus<StudyObject>();
            List<StudyObject> results = query.findByStatusManagerEmailWithPages(StudyObject.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  else if (elementType.equals("studyrole")) {
            GenericFindWithStatus<StudyRole> query = new GenericFindWithStatus<StudyRole>();
            List<StudyRole> results = query.findByStatusManagerEmailWithPages(StudyRole.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return StudyRoleAPI.getStudyRoles(results);
        }  else if (elementType.equals("virtualcolumn")) {
            GenericFindWithStatus<VirtualColumn> query = new GenericFindWithStatus<VirtualColumn>();
            List<VirtualColumn> results = query.findByStatusManagerEmailWithPages(VirtualColumn.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return VirtualColumnAPI.getVirtualColumns(results);
        }  else if (elementType.equals("platform")) {
            GenericFindWithStatus<Platform> query = new GenericFindWithStatus<Platform>();
            List<Platform> results = query.findByStatusManagerEmailWithPages(Platform.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return PlatformAPI.getPlatforms(results);
        }  else if (elementType.equals("stream")) {
            GenericFindWithStatus<Stream> query = new GenericFindWithStatus<Stream>();
            List<Stream> results = query.findByStatusManagerEmailWithPages(Stream.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return StreamAPI.getStreams(results);
        }  else if (elementType.equals("deployment")) {
            GenericFindWithStatus<Deployment> query = new GenericFindWithStatus<Deployment>();
            List<Deployment> results = query.findByStatusManagerEmailWithPages(Deployment.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return DeploymentAPI.getDeployments(results);
        }  else if (elementType.equals("person")) {
            GenericFindWithStatus<Person> query = new GenericFindWithStatus<Person>();
            List<Person> results = query.findByStatusManagerEmailWithPages(Person.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return PersonAPI.getPeople(results);
        }  else if (elementType.equals("organization")) {
            GenericFindWithStatus<Organization> query = new GenericFindWithStatus<Organization>();
            List<Organization> results = query.findByStatusManagerEmailWithPages(Organization.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return OrganizationAPI.getOrganizations(results);
        }  else if (elementType.equals("place")) {
            GenericFindWithStatus<Place> query = new GenericFindWithStatus<Place>();
            List<Place> results = query.findByStatusManagerEmailWithPages(Place.class, managerEmail, hasStatus, withCurrent, pageSize, offset);
            return PlaceAPI.getPlaces(results);
        }  else if (elementType.equals("postaladdress")) {
            GenericFindWithStatus<PostalAddress> query = new GenericFindWithStatus<PostalAddress>();
            List<PostalAddress> results = query.findByStatusManagerEmailWithPages(PostalAddress.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return PostalAddressAPI.getPostalAddresses(results);
        }  else if (elementType.equals("process")) {
            GenericFindWithStatus<org.hascoapi.entity.pojo.Process> query = new GenericFindWithStatus<org.hascoapi.entity.pojo.Process>();
            List<org.hascoapi.entity.pojo.Process> results = query.findByStatusManagerEmailWithPages(org.hascoapi.entity.pojo.Process.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ProcessAPI.getProcesses(results);
        }  else if (elementType.equals("processstem")) {
            GenericFindWithStatus<ProcessStem> query = new GenericFindWithStatus<ProcessStem>();
            List<ProcessStem> results = query.findByStatusManagerEmailWithPages(ProcessStem.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ProcessAPI.getProcessStems(results);
        }  else if (elementType.equals("kgr")) {
            GenericFindWithStatus<KGR> query = new GenericFindWithStatus<KGR>();
            List<KGR> results = query.findByStatusManagerEmailWithPages(KGR.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return KGRAPI.getKGRs(results);
        }  else if (elementType.equals("fundingscheme")) {
            GenericFindWithStatus<FundingScheme> query = new GenericFindWithStatus<FundingScheme>();
            List<FundingScheme> results = query.findByStatusManagerEmailWithPages(FundingScheme.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return FundingSchemeAPI.getFundingSchemes(results);
        }  else if (elementType.equals("project")) {
            GenericFindWithStatus<Project> query = new GenericFindWithStatus<Project>();
            List<Project> results = query.findByStatusManagerEmailWithPages(Project.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ProjectAPI.getProjects(results);
        }  else if (elementType.equals("actuatorstem")) {
            GenericFindWithStatus<ActuatorStem> query = new GenericFindWithStatus<ActuatorStem>();
            List<ActuatorStem> results = query.findByStatusManagerEmailWithPages(ActuatorStem.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ActuatorStemAPI.getActuatorStems(results);
        }  else if (elementType.equals("actuator")) {
            GenericFindWithStatus<Actuator> query = new GenericFindWithStatus<Actuator>();
            List<Actuator> results = query.findByStatusManagerEmailWithPages(Actuator.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return ActuatorAPI.getActuators(results);
        }  else if (elementType.equals("task")) {
            GenericFindWithStatus<Task> query = new GenericFindWithStatus<Task>();
            List<Task> results = query.findByStatusManagerEmailWithPages(Task.class, hasStatus, managerEmail, withCurrent, pageSize, offset);
            return TaskAPI.getTasks(results);
        } 
        return ok("[getElementsByStatusManagerEmail] No valid element type.");

    }

    public Result getTotalElementsByStatusManagerEmail(String elementType, String hasStatus, String managerEmail, boolean withCurrent){
        //System.out.println("SIRElementAPI: getTotalElementsByManagerEmail");
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        
        int totalElements = totalElements = GenericFindWithStatus.findTotalByStatusManagerEmail(clazz, hasStatus, managerEmail, withCurrent);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByStatusManagerEmail() failed to retrieve total number of element", false));

    }

            
    /**
     *   GET ELEMENTS BY KEYWORD, TYPE, MANAGER EMAIL AND STATUS PAGE
     */
                
     public Result getElementsByKeywordTypeManagerEmailAndStatusWithPage(String elementType, String project, String keyword, String type, String managerEmail, String status, int pageSize, int offset) {

        if (project.equals("_")) {
            project = "";
        }
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (type.equals("_")) {
            type = "";
        }
        if (managerEmail.equals("_")) {
            managerEmail = "";
        }
        if (status.equals("_")) {
            status = "";
        }
        if (elementType.equals("organization")) {
            GenericFindSocial<Organization> query = new GenericFindSocial<Organization>();
            List<Organization> results = query.findByKeywordTypeManagerEmailandStatusWithPages(Organization.class, project, keyword, type, managerEmail, status, pageSize, offset);
            return OrganizationAPI.getOrganizations(results);
        } else if (elementType.equals("person")) {
            GenericFindSocial<Person> query = new GenericFindSocial<Person>();
            List<Person> results = query.findByKeywordTypeManagerEmailandStatusWithPages(Person.class, project, keyword, type, managerEmail, status, pageSize, offset);
            return PersonAPI.getPeople(results);
        } else if (elementType.equals("place")) {
            GenericFindSocial<Place> query = new GenericFindSocial<Place>();
            List<Place> results = query.findByKeywordTypeManagerEmailandStatusWithPages(Place.class, project, keyword, type, managerEmail, status, pageSize, offset);
            return PlaceAPI.getPlaces(results);
        } else if (elementType.equals("postaladdress")) {
            GenericFindSocial<PostalAddress> query = new GenericFindSocial<PostalAddress>();
            List<PostalAddress> results = query.findByKeywordTypeManagerEmailandStatusWithPages(PostalAddress.class, project, keyword, type, managerEmail, status, pageSize, offset);
            return PostalAddressAPI.getPostalAddresses(results);
        } 
        return ok("[getElementsByKeywordTypeManagerEmailAnStatusWithPage] No valid element type.");
    }

    public Result getTotalElementsByKeywordTypeManagerEmailAndStatus(String elementType, String project, String keyword, String type, String managerEmail, String status) {

        System.out.println("SIRElementAPI: getTotalElementsByKeywordType: project=[" + project + "]");


        if (project.equals("_")) {
            project = "";
        }
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (type.equals("_")) {
            type = "";
        }
        if (managerEmail.equals("_")) {
            managerEmail = "";
        }
        if (status.equals("_")) {
            status = "";
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFindSocial.findTotalByKeywordTypeManagerEmailAndStatus(clazz, project, keyword, type, managerEmail, status);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByKeyword() failed to retrieve total number of [" + elementType + "]", false));
    }
        
    public Result usage(String elementUri){
        HADatAcThing object = URIPage.objectFromUri(elementUri);
        if (object == null || object.getHascoTypeUri() == null) {
            return ok("[usage] No valid element type. Provided uri is null.");
        }
        String elementType = object.getHascoTypeUri();
        //System.out.println("SIREelementAPI: element type is " + elementType);
        if (elementType.equals(VSTOI.DETECTOR)) {
            List<ContainerSlot> results = Detector.usage(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return ContainerSlotAPI.getContainerSlots(results);
        } //else if (elementType.equals("detector")) {
        //    int totalDetectors = Detector.findTotalByManagerEmail(managerEmail);
        //    String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
        //    return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        //}
        return ok("[usage] No valid element type.");
    }

    public Result derivation(String elementUri){
        HADatAcThing object = URIPage.objectFromUri(elementUri);
        if (object == null || object.getHascoTypeUri() == null) {
            return ok("[derivation] No valid element type. Provided uri is null.");
        }
        String elementType = object.getHascoTypeUri();
        //System.out.println("SIREelementAPI: element type is " + elementType);
        if (elementType.equals(VSTOI.DETECTOR)) {
            List<Detector> results = Detector.derivationDetector(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return DetectorAPI.getDetectors(results);
        } //else if (elementType.equals("detector")) {
        //    int totalDetectors = Detector.findTotalByManagerEmail(managerEmail);
        //    String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
        //    return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        //}
        return ok("[derivation] No valid element type.");
    }

    public Result hascoType(String classUri){
        if (classUri == null || classUri.isEmpty()) {
            return ok("[hascoType] No valid classUri has been provided.");
        }
        GenericInstance instance = new GenericInstance();
        String response = instance.getHascoType(classUri);
        if (response != null) {
            String respJSON = "{\"hascoType\":\"" + response + "\"}";
            return ok(ApiUtil.createResponse(respJSON, true));
        }     
        return ok("No recognizable HASCO type.");
    }

    public Result pendingReviews(){
        GenericInstance instance = new GenericInstance();
        int[] response = GenericFind.findTotalsUnderReview();
        if (response != null) {
            String respJSON = "[" + 
                "{\"AnnotationStem\":\"" + response[0] + "\"}," + 
                "{\"Codebook\":\"" + response[1] + "\"}," + 
                "{\"Container\":\"" + response[2] + "\"}," + 
                "{\"Detector\":\"" + response[3] + "\"}," + 
                "{\"DetectorStem\":\"" + response[4] + "\"}," + 
                "{\"Actuator\":\"" + response[5] + "\"}," + 
                "{\"ActuatorStem\":\"" + response[6] + "\"}," + 
                "{\"ResponseOption\":\"" + response[7] + "\"}" + 
                "{\"ProcessStem\":\"" + response[8] + "\"}," + 
                "{\"Process\":\"" + response[9] + "\"}" + 
                "]";
            return ok(ApiUtil.createResponse(respJSON, true));
        }     
        return ok("Failed retrieving elements under review.");
    }

}
