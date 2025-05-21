package eu.sedimark.catalogue.handlers;

import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.fuseki.servlets.HttpAction;
import java.io.IOException;

/**
 * A minimal test handler just to confirm handler registration works
 */
public class TestHandler implements ActionProcessor {
    
    @Override
    public void process(HttpAction action) {
        System.out.println("========== TEST HANDLER CALLED ==========");
        
        try {
            // Set a test header
            action.getResponse().setHeader("X-Test-Handler", "Yes");
            action.getResponse().setContentType("text/plain");
            action.getResponseOutputStream().write("TestHandler processed the request!".getBytes());
        } catch (IOException e) {
            System.err.println("Error in test handler: " + e.getMessage());
        }
    }
}