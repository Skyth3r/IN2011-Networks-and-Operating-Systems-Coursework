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
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
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
                        
                        if(req.getVersion().equals(in2011.http.HTTPMessage.DEFAULT_HTTP_VERSION)){
                        String uri = req.getURI();
                        String ps = rootDir + uri;
                        Path myPath = Paths.get(ps).toAbsolutePath().normalize();
                        
                        try{
                       // InputStream input = null;
                        InputStream input = Files.newInputStream(myPath);

                        
                        BasicFileAttributes attr = Files.readAttributes(myPath, BasicFileAttributes.class);
                        

                        FileTime lastAccess = attr.lastAccessTime();
                        FileTime lastModified = Files.getLastModifiedTime(myPath);
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                        String access = df.format(lastAccess.toMillis());
                        String modified = df.format(lastModified.toMillis());
                        System.out.println(df.parse(modified).before(df.parse(access)));
                        
                        System.out.println(access);
                        System.out.println(modified);
                        
                        if(df.parse(modified).before(df.parse(access))){
                            msg = new Response(304); // NOT MODFIED
                            msg.write(os); 
                            conn.close();

                        } else {

                        msg = new Response(200); // OKAY
                        msg.write(os);
    
                        byte[] data = Files.readAllBytes(myPath);                    
                        os.write(data);  
                        }
                      
                        }catch(Exception e){
                            
                        if(e.toString().equals("java.nio.file.AccessDeniedException: " + myPath)){
                            msg = new Response(403); // forbidden
                            msg.write(os);
                            conn.close();
                            
                         } else {
                             msg = new Response(404); // Not found
                             msg.write(os);
                             conn.close();
                         }
                             
                        }

                        }else{   
                            msg = new Response(505); //not http1
                            msg.write(os);
                            conn.close();
                            
                        }
                        
                        
                   } else {
                        msg = new Response(501); // Not implemented.
                        msg.write(os);
                    }
                    }catch(Exception e){
                        
                        OutputStream os = conn.getOutputStream();
                        Response msg;
                        msg = new Response(500); // Internal server error
                        msg.write(os);
                    }

                } catch (MessageFormatException ex) {
                    OutputStream os = conn.getOutputStream();
                    Response msg;
                    msg = new Response(400); // Bad request
                    msg.write(os);
                }
                if(!conn.isClosed()){
                conn.close();
                }
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
