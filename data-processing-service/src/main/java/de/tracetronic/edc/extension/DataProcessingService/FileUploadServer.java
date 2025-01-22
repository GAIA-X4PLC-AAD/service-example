// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import jakarta.servlet.MultipartConfigElement;

/**
 * Base server for receiving file uploads
 */
public class FileUploadServer implements Runnable {

    private static final int DEFAULT_PORT = 29200;

    private Monitor monitor;
    private int port;

    public FileUploadServer(Monitor monitor) {
        this.monitor = monitor;
        try {
            this.port = Integer.parseInt(DataStore.getPort());
        } catch (NumberFormatException exception) {
            monitor.severe("Couldn't parse port from config, using default port of " + DEFAULT_PORT);
            this.port = DEFAULT_PORT;
        }
    }

    /**
     * Run the server: Configures and adds the servlet
     */
    @Override
    public void run() {
        Server server;
        if (DataStore.USE_HTTPS) {
            server = setupHttpsServer();
        } else {
            server = new Server(port); 
        }
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder servletHolder = new ServletHolder(new FileUploadServlet(monitor));
        //Configure the multipart element which is used for transferring the files
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
            null, // location 
            50 * 1024 * 1024, // maxFileSize 
            100 * 1024 * 1024, // maxRequestSize 
            1 * 1024 * 1024 // fileSizeThreshold
        );
        //Add the servlet
        servletHolder.getRegistration().setMultipartConfig(multipartConfigElement);
        context.addServlet(servletHolder, "/upload");

        HandlerList handlers = new HandlerList();
        handlers.addHandler(context);
        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            monitor.severe("Failed to start FileReceiverServer");
            return;
        }
        monitor.info("FileReceiverServer started");
        try {
            server.join();
        } catch (InterruptedException e) {
            monitor.severe("Exception when invoking .join() on the server thread");
            return;
        }
    }

    /**
     * Setup https connection for server if needed. *Currently NOT TESTED*
     * @return the configured server
     */
    private Server setupHttpsServer() {
        Server server = new Server();

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("data-processing-service/resources/certs/cert.pfx");
        sslContextFactory.setKeyStorePassword("123456");
        sslContextFactory.setKeyManagerPassword("123456");

        ServerConnector sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new org.eclipse.jetty.server.HttpConnectionFactory()
        );
        sslConnector.setPort(port);

        server.addConnector(sslConnector);
        return server;
    }

}