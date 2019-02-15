package eng.auth.services;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan
public class WebConfig implements WebMvcConfigurer {
	public static String origin = "http://localhost:8080";
	public static int threads = 1;
	public static int throughput = 100;
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
        }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
    	try {
    		FileInputStream fstream = new FileInputStream("algorithms.properties");
    		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    		String line;
    		ArrayList<String> properties = new ArrayList<String>();
    		while ((line = br.readLine()) != null)   {
    			line = line.trim();
    			String[] splt = line.split("=");
    			if(splt.length!=2)
    				continue;
    			splt[0] = splt[0].trim();
    			splt[1] = splt[1].trim();
    			if(splt[0].equals("origin"))
    				origin = splt[1];
    			if(splt[0].equals("threads"))
    				threads = Integer.parseInt(splt[1]);
    			if(splt[0].equals("throughputms"))
    				throughput = Integer.parseInt(splt[1]);
    		}
			properties.clear();
    		fstream.close();
    	}
    	catch(Exception e) {
			System.err.println(e.toString());
    	}
    	System.out.println("Cors origins: null, "+origin);
        registry.addMapping("/**")
            .allowedOrigins("null", origin)
            .allowedMethods("GET", "POST")
            .allowCredentials(true).maxAge(3600);
    }
    
    /*@Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }*/
}