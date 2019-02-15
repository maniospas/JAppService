package eng.auth.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Polling {	
	public static class Request {
		private final long id;
		private final String text;
		private String status;
		private String outcome;
	    private String algorithm;
		public Request(String errorMessage) {
			id = -1;
			this.text = "";
			status = "Invalid";
			outcome = errorMessage;
			algorithm = "";
		}
		public Request(long id, String algorithm, String text) {
			this.id = id;
			this.text = text;
			status = "Pending";
			outcome = "";
			this.algorithm = algorithm;
		}
		public String getAlgorithm() {
			return algorithm;
		}
		public long getId() {
			return id;
		}
		public String getText() {
			return text;
		}
		public String getStatus() {
			return status;
		}
		public String getOutcome() {
			return outcome;
		}
		public synchronized void setOutcome(String outcome) {
			this.outcome = outcome;
		}
		public synchronized void updateStatus(String status) {
			this.status = status;
		}
	}
	
    private HashMap<String, Long> requestToId = new HashMap<String, Long>();
    private HashMap<Long, Request> idToRequest = new HashMap<Long, Request>();
    private long numberOfIds;
	private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
	private ObjectReader objectReader = new ObjectMapper().reader();
	
	private HashMap<String, Algorithm> algorithms = new HashMap<String, Algorithm>();
	public static class Algorithm {
		private String id = "-1";
		private String name = "";
		private String externalJar;
		private String indexhtml;
		private String description = "";
		public Algorithm(ArrayList<String> properties) {
			for(String line : properties) {
				String[] splt = line.split("=");
				if(splt.length!=2)
					continue;
				splt[0] = splt[0].trim();
				splt[1] = splt[1].trim();
				if(splt[0].equals("id"))
					id = splt[1];
				if(splt[0].equals("name"))
					name = splt[1];
				if(splt[0].equals("index"))
					indexhtml = splt[1];
				if(splt[0].equals("jar"))
					externalJar = splt[1];
				if(splt[0].equals("about"))
					description = splt[1];
			}
			System.out.println("Loaded /"+id+" for "+externalJar);
		}
		/*
		public Algorithm(String id, String name, String externalJar, String indexhtml) {
			this.id = id;
			this.name = name;
			this.externalJar = externalJar;
			this.indexhtml = indexhtml;
		}*/
		public String getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public String getExternalJar() {
			return externalJar;
		}
		public String getIndexhtml () {
			return indexhtml;
		}
		public String getDescription () {
			return description;
		}
	}
	
	public ArrayList<Algorithm> getAlgorithms() {
		return new ArrayList<Algorithm>(algorithms.values());
	}
	
    public Polling() {
    	try {
    		FileInputStream fstream = new FileInputStream("algorithms.properties");
    		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    		String line;
    		ArrayList<String> properties = new ArrayList<String>();
    		boolean started = false;
    		while ((line = br.readLine()) != null)   {
    			line = line.trim();
    			if(line.equals("[Algorithm]")) {
    				if(!properties.isEmpty() && started) {
	    				Algorithm algorithm = new Algorithm(properties);
	    				algorithms.put(algorithm.getId(), algorithm);
    				}
    				started = true;
    				properties.clear();
    			}
    			else 
    				properties.add(line);
    		}
			Algorithm algorithm = new Algorithm(properties);
			algorithms.put(algorithm.getId(), algorithm);
			properties.clear();
    		fstream.close();
    	}
    	catch(Exception e) {
			System.err.println(e.toString());
    	}
        numberOfIds = 0;
        Thread runner = new Thread() {
        	@Override
        	public void run() {
        		while(true) {
        			try {
	        		    sleep(WebConfig.throughput);
	        		    ArrayList<Request> pending = discoverPending();
	        		    if(!pending.isEmpty()) {
	        		    	Request request = pending.get(0);
	        		    	solveRequest(request);
	        		    }
        			}
        			catch(Exception e) {
        				System.err.println(e.toString());
        			}
        		}
        	}
        };
        runner.start();
    }
    synchronized public ArrayList<Request> discoverPending() {
    	ArrayList<Request> pending = new ArrayList<Request>();
	    for(Request request : idToRequest.values())
	    	if(request.getStatus().equals("Pending"))
	    		pending.add(request);
	    return pending;
    }
    public Algorithm getAlgorithm(String algorithm) {
    	return algorithms.get(algorithm);
    }
    synchronized public long getId(String algorithm, String text, boolean createIfNeeded) {
    	if(text.isEmpty())
    		return -1;
        Long ret = requestToId.get(algorithm+":"+text);
        if(ret==null && createIfNeeded && algorithm!=null) {
        	requestToId.put(algorithm+":"+text, numberOfIds);
        	idToRequest.put(numberOfIds, new Request(numberOfIds, algorithm, text));
        	ret = numberOfIds;
        	numberOfIds++;
        }
        if(ret==null)
        	return -1;
        return ret;
    }
    synchronized public Request getRequest(long id) {
    	Request ret = idToRequest.get(id);
    	if(ret==null)
    		return null;
    	return ret;
    }
    private void solveRequest(final Request request) {
			PrintStream old = System.out;
	    	try {
	    		Algorithm algorithm = algorithms.get(request.getAlgorithm());
	    		if(algorithm==null)
	    			throw new Exception("Algorithm "+request.getAlgorithm()+" not declared");
		    	request.updateStatus("Running");
		    	final String mainClass;
		    	final JarFile jarFile = new JarFile(algorithm.getExternalJar());
		    	try {
		    	    final Manifest manifest = jarFile.getManifest();
		    	    mainClass = manifest.getMainAttributes().getValue("Main-Class");
		    	} finally {
		    	    jarFile.close();
		    	}
		    	final URLClassLoader child = new URLClassLoader(new URL[]{(new File(algorithm.getExternalJar())).toURI().toURL()}, this.getClass().getClassLoader());
		    	final Class<?> classToLoad = Class.forName(mainClass, true, child);
		    	ArrayList<Class<?>> parameterTypes = new ArrayList<Class<?>>();
		    	ArrayList<String> inputs = new ArrayList<String>();
		    	
		    	String text = request.getText();
		    	if(text.endsWith("\""))
		    		text = text.substring(1,text.length()-1);
		    	JsonNode node = objectReader.readTree(text);
		    	Iterator<String> iterator = node.fieldNames();
		    	while(iterator.hasNext()) {
		    		String field = iterator.next();
		    		parameterTypes.add(String.class);
		    		inputs.add(node.get(field).asText());
		    	}
		    	//System.out.println(Arrays.toString(inputs.toArray(new String[inputs.size()])));
		    	
		    	final Method method = classToLoad.getDeclaredMethod("mainApplication", parameterTypes.toArray(new Class<?>[parameterTypes.size()]));

	    		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
	    			@Override
	    			public void flush() {
	    				try {
							super.flush();
						}
	    				catch (IOException e) {
							e.printStackTrace();
						}
	    				request.setOutcome(toString().replace("\n", "<br/>"));
	    			}
	    		};
	    		PrintStream ps = new PrintStream(baos,true);
	    		System.out.flush();
	    		System.setOut(ps);
		    	Object outcome = method.invoke(null, inputs.toArray(new String[inputs.size()]));
	    		System.out.flush();
	    		System.setOut(old);
		    	if(outcome==null)
		    		throw new Exception(algorithm.getExternalJar()+" returned null");
		    	if(outcome!=null) {
		    		String json = outcome.toString();
		    		request.setOutcome(json);
		    	}
		    	request.updateStatus("Finished");
	    	}
	    	catch(Exception e) {
	    		System.out.flush();
	    		System.setOut(old);
		    	request.setOutcome(e.toString());
		    	request.updateStatus("Failed");
	    	}
	   }
}