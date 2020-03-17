import java.net.*;
import java.util.*;
import java.io.*;

public class Proxy {
    private Set<Thread> thread_pool;
    private final int BUF_SIZE = 128;
    private final int MAX_POOL_SIZE = 250;

    public Proxy(int port) {
        // Print Writers will use the right newline character for HTTP requests
        System.setProperty("line.separator", "\r\n");
        thread_pool = new HashSet<>();
        try {
            ServerSocket tcp_socket_client = new ServerSocket(port);
            System.out.println("Proxy listening on " + InetAddress.getLocalHost().toString() + ":" + port);

            while (true) {
                // Generate a new thread per connection
                Socket connected = tcp_socket_client.accept();
                Thread thread = new Thread(new ProxyThread(connected));
                thread_pool.add(thread);
                thread.start();
                System.out.println("Pool size: " + thread_pool.size());
                if (thread_pool.size() >= MAX_POOL_SIZE) {
                    Iterator<Thread> iter = thread_pool.iterator();
                    while (iter.hasNext()) {
                        Thread t = iter.next();
                        if (!t.isAlive()) {
                            t.join();
                            iter.remove();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ProxyThread implements Runnable {
        private Socket connected;

        public ProxyThread(Socket connected) {
            this.connected = connected;
        }

        public void run() {
            try {
                InputStream is_browser = connected.getInputStream();
                OutputStream os_browser = connected.getOutputStream();
                PrintWriter writer_os_browser = new PrintWriter(os_browser, true);
                BufferedReader br_browser = new BufferedReader(new InputStreamReader(is_browser));

                // Variables to store server port, server url, and the request itself
                // from browser
                String server_url = "";
                int server_port = -1;
                List<String> request = new ArrayList<>();

                // Wait for browser to send a request
                while (!br_browser.ready()) {
                    Thread.sleep(500);
                }

                // Parse request line by line
                while (br_browser.ready()) {
                    String line = br_browser.readLine();
                    String[] split = line.split(" ");

                    // Check if this line configures connection & set to close
                    if (split[0].equalsIgnoreCase("proxy-connection:") ||
                            split[0].equalsIgnoreCase("connection:")) {
                        request.add(split[0] + " close");
                        continue;
                    } else {
                        // Add this line to our request array without processing
                        request.add(line);
                    }

                    // Check if this line has the host & save the information
                    if (split[0].equalsIgnoreCase("host:")) {
                        if (split[1].contains(":")) {
                            String[] host_split = split[1].split(":");
                            server_url = host_split[0];
                            server_port = Integer.parseInt(host_split[1]);
                        } else {
                            server_url = split[1];
                        }
                    }
                }

                // extract protocol and port number
                String protocol = get_protocol(request.get(0));
                if (server_port == -1) {
                    server_port = get_port(request.get(0));
                }


                // default port numbers if it hasn't been extracted
                if (server_port == -1) {
                    if (protocol.equalsIgnoreCase(("http"))) {
                        server_port = 80;
                    } else if (protocol.equalsIgnoreCase("https")){
                        server_port = 443;
                    }
                }

                // Print the first line of the request
                System.out.println(">>> " + request.get(0));

                // Connect to the server using TCP socket
                Socket tcp_socket_server = null;
                try {
                    tcp_socket_server = new Socket(server_url, server_port);
                } catch (Exception e) {
                    // issue setting up the socket
                    writer_os_browser.print("HTTP/1.1 502 Bad Gateway\r\n" + "Connection: close\r\n");
                    writer_os_browser.println();
                    return;
                }

                InputStream is_server = tcp_socket_server.getInputStream();
                OutputStream os_server = tcp_socket_server.getOutputStream();
                BufferedReader br_server = new BufferedReader(new InputStreamReader(is_server));
                PrintWriter writer_os_server = new PrintWriter(os_server, true);

                if (protocol.equalsIgnoreCase(("http"))) {
                    // HTTP
                    for (String req_line : request) {
                        writer_os_server.println(req_line);
                    }

                    // Wait for server to be ready
                    while (!br_server.ready()) {
                        Thread.sleep(500);
                    }

                    // Read in the server response using buffer
                    byte[] buf = new byte[BUF_SIZE];
                    int bytes = is_server.read(buf);
                    while (bytes > 0) {
                        os_browser.write(buf, 0, bytes);
                        bytes = is_server.read(buf);
                    }
                } else {
                    // HTTPS. Requires tunneling
                    writer_os_browser.print("HTTP/1.1 200 OK\r\n" + "Connection: keep-alive\r\n");
                    writer_os_browser.println();

                    // Set timeout
                    connected.setSoTimeout(5000);
                    tcp_socket_server.setSoTimeout(5000);

                    // Spawn threads to handle Server --> Browser and Browser --> Server communications
                    Thread browser_to_server = new Thread(new TCPThread(connected, tcp_socket_server, "browser to server"));
                    Thread server_to_browser = new Thread(new TCPThread(tcp_socket_server, connected, "server to browser"));
                    browser_to_server.start();
                    server_to_browser.start();
                }
            } catch (Exception e) {
                // exception occurred so just let thread die
            }
        }
    }

    private int get_port(String s) {
        URL url;
        try {
            url = new URL(s.split(" ")[0]);
        } catch (MalformedURLException e) {
            return -1;
        }
        return url.getPort();
    }

    private String get_protocol(String s) {
        String req_type = s.split(" ")[0];
        if (req_type.equalsIgnoreCase("connect")) {
            return "https";
        }
        return "http";
    }

    public class TCPThread implements Runnable {
        private Socket input;
        private Socket output;
        private String name;

        public TCPThread(Socket input, Socket output, String name) {
            this.input = input;
            this.output = output;
            this.name = name;
        }

        public void run() {
            byte[] buf = new byte[BUF_SIZE];
            try {
                InputStream is = input.getInputStream();
                OutputStream os = output.getOutputStream();
                int bytes = is.read(buf);
                while (bytes > 0) {
                    os.write(buf, 0, bytes);
                    os.flush();
                    bytes = is.read(buf);
                }
            } catch (Exception e) {
                // One socket has closed so make sure both are disconnected
                try {
                    input.close();
                    output.close();
                } catch (IOException ex) {
                    // socket was already closed so do nothing
                }
            }
        }
    }

    public static void main(String[] args) {
        new Proxy(1234);
    }
}