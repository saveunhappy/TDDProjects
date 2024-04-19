package com.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class ServletTest {
    private Server server;

    @BeforeEach
    public void start() throws Exception {

        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        //刚开始这个TestApplication中是存储Controller，使用java自带的流去输出，现在要增加使用哪种方式去写出去
        //TestProviders里面有各种可以提供的，context的，messageBodyWriter的，还有Exception的，但是这些对象
        //放在哪呢?目前是放在TestApplication里面，所以new TestProviders(application)需要把application传送进去
        handler.addServlet(new ServletHolder(getServlet()), "/");
        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();
    }

    protected abstract Servlet getServlet();

    protected URI path(String path) throws Exception {
        return new URL(new URL("http://localhost:8080/"), path).toURI();
    }

    protected HttpResponse<String> get(String path) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(path(path)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
