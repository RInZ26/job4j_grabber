package ru.job4j.grabber;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

public class HttpPostsServer {
    private int port;
    private List<Post> posts;

    HttpPostsServer(int port, List<Post> posts) throws Exception {
        this.port = port;
        this.posts = posts;
    }

    public void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/posts", new MyHandler(posts));
        server.setExecutor(null);
        server.start();
    }

    static class MyHandler implements HttpHandler {
        private List<Post> posts;

        MyHandler(List<Post> posts) {
            this.posts = posts;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, 0);
            try (var out = new PrintWriter(t.getResponseBody(), true,
                                           Charset.forName("cp866"))) {
                for (Post post : posts) {
                    out.printf("%s\n\n", post.toString());
                }
            } catch (Exception io) {
                io.printStackTrace();
            }
        }
    }
}