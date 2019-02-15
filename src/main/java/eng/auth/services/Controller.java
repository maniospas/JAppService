package eng.auth.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    public final Polling polling = new Polling();
	
	public Controller() {
		super();
	}
	private String normalize(String arguments) {
		StringBuilder ret = new StringBuilder();
		for(int i=0;i<arguments.length();i++) {
			char c = arguments.charAt(i);
			if(c=='%' && i<arguments.length()-2) {
				ret.append((char)Integer.parseInt(""+arguments.charAt(i+1)+arguments.charAt(i+2), 16));
				i += 2;
			}
			else
				ret.append(c);
		}
		arguments = ret.toString();
		if(arguments.endsWith("="))
			arguments = arguments.substring(0, arguments.length()-1);
    	return arguments.toString();
    }

    @PostMapping("/algorithms/{algorithmid}/requests")
    public Long requestsPost(@PathVariable String algorithmid, @RequestBody String arguments) {
        return polling.getId(algorithmid, normalize(arguments), true);
    }
    @GetMapping("/algorithms/{algorithmid}/requests/{id}")
    public Polling.Request requestsGet(@PathVariable String algorithmid, @PathVariable Long id) {
    	Polling.Request request = polling.getRequest(id);
        if(!algorithmid.equals(request.getAlgorithm()))
        	return null;
        return request;
    }
    /*@PostMapping("/requests")
    public Long requestsPost(@RequestBody String arguments) {
        return polling.getId(null, normalize(arguments), false);
    }
    @GetMapping("/requests/{id}")
    public Polling.Request requestsGet(@PathVariable Long id) {
        return polling.getRequest(id);
    }*/
    @GetMapping("/algorithms/{algorithmid}")
    public String index(@PathVariable String algorithmid) {
    	try {
    		Polling.Algorithm algorithm = polling.getAlgorithm(algorithmid);
    		if(algorithm==null)
    			throw new Exception(algorithmid+" not found");
    		if(algorithm.getIndexhtml()==null)
    			throw new Exception(algorithm.getId()+" has no associated index");
    		byte[] encoded = Files.readAllBytes(Paths.get(algorithm.getIndexhtml()));
        	String ret = (new String(encoded, StandardCharsets.UTF_8)).replace("localhost:8080", WebConfig.origin);
        	if(polling.getAlgorithms().size()>1)
        		ret = ret.replace("</body>", "<div><center><br/><a href=\""+WebConfig.origin+"\">Visit other algorithms</a></center></div></body>");
        	return ret;
    	}
    	catch(Exception e) {
    		return e.toString();
    	}
    }
    @RequestMapping("/")
    public String index() {
    	if(polling.getAlgorithms().size()==1)
    		return index(polling.getAlgorithms().get(0).getId());
    	String ret = "";
    	for(Polling.Algorithm algorithm : polling.getAlgorithms()) {
    		ret += "<div>";
    		ret += "<h1><a href=\""+WebConfig.origin+"/algorithms/"+algorithm.getId()+"\">"+algorithm.getName()+"</a></h1>";
    		ret += "<div>"+algorithm.getDescription()+"</div>";
    		ret += "</div>";
    	}
    	return "<html><head><title>App</title></head><body>"+ret+"</body></hmtl>";
    }
}