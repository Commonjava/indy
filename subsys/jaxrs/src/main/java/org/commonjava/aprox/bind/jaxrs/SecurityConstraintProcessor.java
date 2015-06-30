package org.commonjava.aprox.bind.jaxrs;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class SecurityConstraintProcessor {
	
	private SecurityConstraintConfig constraintConfig;
	
	public SecurityConstraintProcessor(String securityConfigFilePath) {
        try {
    		//read json file data to String
            byte[] jsonData = Files.readAllBytes(Paths.get(securityConfigFilePath));
             
            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
             
            //convert json string to object
			constraintConfig = objectMapper.readValue(jsonData, SecurityConstraintConfig.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	}

	public SecurityConstraintConfig getConstraintConfig() {
		return constraintConfig;
	}
	
	public static void main(String[] args) {
        try {
    		//convert Object to json string
            SecurityConstraintConfig config= createConstraintsConfig();
            
            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            
            //configure Object mapper for pretty print
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
             
            //writing to console, can write to any output stream such as file
            StringWriter configWritter = new StringWriter();
			objectMapper.writeValue(configWritter, config);
	        System.out.println("SecurityConstraintConfig JSON is\n"+configWritter);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static SecurityConstraintConfig createConstraintsConfig () {
		SecurityConstraintConfig config = new SecurityConstraintConfig();
		// 1.constraint
		SecurityConstraint securityConstraint1 = new SecurityConstraint("admin","/api/info/*",new String[] {"POST","GET"}); 
		// 2.constraint
		SecurityConstraint securityConstraint2 = new SecurityConstraint("user","/api/all/*",new String[] {"POST","GET","PUT","DELETE"}); 
		// 3.constraint
		SecurityConstraint securityConstraint3 = new SecurityConstraint("all","/api/test/*",new String[] {"POST","GET","PUT","TRACE"});
		
		List<SecurityConstraint> constraints = new ArrayList<SecurityConstraint>();
		constraints.add(securityConstraint1);
		constraints.add(securityConstraint2);
		constraints.add(securityConstraint3);
		
		config.setSecurityContraints(constraints);
		
		return config;
	}
	
}
