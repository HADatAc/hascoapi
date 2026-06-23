package test.delete;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import play.mvc.Result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.hascoapi.console.controllers.restapi.ComponentStemAPI;
import static play.test.Helpers.contentAsString;

import static test.Responses.*;

@Order(value = 003)
public class ResetComponentStems {

    // Unit test deleteComponentStemsForTesting
    @Test
    public void test003ResetComponentStems() {
		System.out.println("Testing 003 reset ComponentStems");
        ComponentStemAPI controller = new ComponentStemAPI();
        Result result = controller.deleteComponentStemsForTesting();
        assertEquals(200, result.status());
    }

}
