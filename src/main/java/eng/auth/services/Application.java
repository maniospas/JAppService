package eng.auth.services;

import java.awt.Desktop;
import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
	public static void open() {
        try {
            String path = WebConfig.origin+"/";
            final Runtime rt = Runtime.getRuntime();
            rt.exec("explorer \""+path+"\"");
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	//instructions in https://spring.io/guides/gs/rest-service/ 
	//for errors check https://www.in28minutes.com/spring-boot-maven-eclipse-troubleshooting-guide-and-faq#error--files-downloaded-by-maven-are-corrupt
    public static void main(String[] args) {
    	try {
	    	ConfigurableApplicationContext app = SpringApplication.run(Application.class, args);
	    	if(!app.isActive()) {
	    		app.close();
	    		open();
	    		System.exit(0);
	    	}
	    	else
	    		open();
    	}
    	catch(Exception e) {
    		System.err.println(e.toString());
    		open();
    		//System.exit(0);
    	}
    }
}