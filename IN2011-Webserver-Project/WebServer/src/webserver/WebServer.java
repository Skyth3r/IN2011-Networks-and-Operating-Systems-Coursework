package webserver;

import in2011.http.Request;
import in2011.http.Response;
import in2011.http.StatusCodes;
import in2011.http.EmptyMessageException;
import in2011.http.MessageFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import org.apache.http.client.utils.DateUtils;

public class WebServer {

    private int port;
    private String rootDir;

    public WebServer(int port, String rootDir) {
        this.port = port;
        this.rootDir = rootDir;
    }

    public void start() throws IOException {
         ServerSocket serverSock = new ServerSocket(port);
            while (true) {
                // listen for a new connection on the server socket
                Socket conn = serverSock.accept();
                // get the input stream for receiving data from the client
                InputStream is = conn.getInputStream();

                   try{
                    // read the request message 
                       
                    Request req = Request.parse(is);      
                    String method = req.getMethod();
                   
                    // get the output stream for sending data to the client
                    try{
                    OutputStream os = conn.getOutputStream();
                    Response msg;
     
                    if(method.startsWith("GET")){
                        String uri = req.getURI();
                        String ps = rootDir + uri;
                        Path myPath = Paths.get(ps).toAbsolutePath().normalize();
                        
                        try{
                        InputStream input = Files.newInputStream(myPath);
                        }catch(Exception e){
                            msg = new Response(404); // Not found
                            msg.write(os);
                            conn.close();
                            return;
                        }
                        
                        System.out.println( "exists:"  + Files.exists(myPath));
                        System.out.println( "can read:" + Files.isReadable(myPath));
                        System.out.println( "can execute:" + Files.isExecutable(myPath) );
                        System.out.println( "can write:" + Files.isWritable(myPath));
                        
                        if(Files.exists(myPath) && Files.isReadable(myPath) && Files.isExecutable(myPath) && Files.isWritable(myPath)){
                            
                        if(!req.getVersion().equals(in2011.http.HTTPMessage.DEFAULT_HTTP_VERSION)){
                            
                            msg = new Response(505);
                            msg.write(os);
                            conn.close();
                            return;
                        }

                        BasicFileAttributes attr = Files.readAttributes(myPath, BasicFileAttributes.class);
                        System.out.println(!String.valueOf(Files.getLastModifiedTime(myPath)).equals(attr.creationTime()));
                        System.out.println("Creation Time" + attr.creationTime());
                        System.out.println("Modified" + Files.getLastModifiedTime(myPath));

                        if(!String.valueOf(Files.getLastModifiedTime(myPath)).equals(attr.creationTime())){
                            msg = new Response(304);
                            msg.write(os); 
                            conn.close();
                            return;
                        } 
     
                        msg = new Response(200); // OKAY
                        msg.write(os);
    
                        byte[] data = Files.readAllBytes(myPath);                    
                        os.write(data);  
                        
                        } else {
                            msg = new Response(403); // Forbidden
                            msg.write(os);
                        }
                        
                   } else {
                        msg = new Response(501); // Not implemented.
                        msg.write(os);
                    }
                    }catch(Exception e){
                        OutputStream os = conn.getOutputStream();
                        Response msg;
                        msg = new Response(500);
                        msg.write(os);
                    }

                } catch (MessageFormatException ex) {
                    OutputStream os = conn.getOutputStream();
                    Response msg;
                    msg = new Response(400); // Internal server error
                    msg.write(os);
                }
                conn.close();
            }
    }

    public static void main(String[] args) throws IOException {
        String usage = "Usage: java webserver.WebServer <port-number> <root-dir>";
        if (args.length != 2) {
            throw new Error(usage);
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        String rootDir = args[1];
        WebServer server = new WebServer(port, rootDir);
        server.start();
    }
}
