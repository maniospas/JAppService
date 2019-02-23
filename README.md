# JAppService
This is an application written in Spring Boot Swing that allows easy distribution of algorithms written in Java as RESTful services.

## Usage
The only requirement for the on developing the algorithm is to include a *public static String mainApplication(String arg1, String arg2, ...)* method in the project's main class which takes any number of *String* arguments and returns service's *String* response.

```
public class MainClass {
  public static void main(String[] args) throws Exception  {
    System.out.println(mainApplication(args[0], args[1]));
  }
  public static String mainApplication(String path1, String path2) {
    // insert calls to the program's functionality here and return its outcome
  }
}
```

Then, the project should be exported as an Executable Jar (so that the main class is included in the manifest). To deploy the application, create an appropriate entry in the *algorithms.properties* that is placed alongside the *eng.auth.services.jar*. Running the latter then sets up RESTful server that accepts two type of requests:
- POST/algorithms/{algorithm id}/requests/{query} requesting a new query to the algorithm on and returning a request id
- GET/algorithms/{algorithm}/requests/{query id} returning a request JSON object with fields "status" and "outcome".

## Algorithm Properties
TODO

## Execution Model
Requests recieved through POST operations are inserted in a queue and are assigned a "Pending" status. When a request is polled and passed to the algorithm, it is assigned a *Running* status and its outcome is constantly updated from the console. When the algorithm has successfully run the request, it is annotated as "Finished" and its output is set as outcome. If an exception has occured, a "Failed" status is returned and the outcome is set to its description.

Post requests donnot require the algorithm to finish running before returning the request id. This means that a client needs to periodically us the returned request id to GET the request's status.
