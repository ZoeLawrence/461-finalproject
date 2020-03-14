import java.net.*;
import java.io.*;

/**
 * This program demonstrates a client socket application that connects to
 * a web server and send a HTTP HEAD request.
 *
 * @author www.codejava.net
 */
public class RequestTest {

    public static void main(String[] args) throws UnknownHostException {
        String hostname = "www.google.com";
        InetAddress addr = InetAddress.getByName(hostname);
        int port = 80;

        try (Socket socket = new Socket(addr, port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            writer.println("CONNECT www.google.com:443 HTTP/1.1");
            writer.println("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:70.0) Gecko/20100101 Firefox/70.0");
            writer.println("Connection: close");
            writer.println("Host: " + "www.google.com:443");
            writer.println();

            InputStream input = socket.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}