package org.sirapi.entity.fhir;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Experience;
import org.sirapi.entity.pojo.ResponseOption;

public class Item {

    private Detector detector;
    private List<AnswerOption> answerOptions;

    public Item(Detector detector) {
        this.detector = detector;
        answerOptions = new ArrayList<AnswerOption>();
        Experience experience = detector.getExperience();
        List<ResponseOption> responseOptions = experience.getResponseOptions();
        for (ResponseOption responseOption : responseOptions) {
            AnswerOption answerOption = new AnswerOption(responseOption);
            answerOptions.add(answerOption);
        }
    }

    public QuestionnaireItemComponent getFHIRObject() {
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();

        item.setDefinition(detector.getUri());
        item.setText(detector.getHasContent());
        item.setType(QuestionnaireItemType.CHOICE);

        for (AnswerOption answerOption : answerOptions) {
            QuestionnaireItemAnswerOptionComponent questionnaireItemAnswerOptionComponent
                = new QuestionnaireItemAnswerOptionComponent();
            Coding coding = answerOption.getFHIRObject();
            questionnaireItemAnswerOptionComponent.setValue(coding);
            item.addAnswerOption(questionnaireItemAnswerOptionComponent);
        }

        return item;
    }
}
