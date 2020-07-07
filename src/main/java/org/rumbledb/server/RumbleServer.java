package org.rumbledb.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.rumbledb.config.RumbleRuntimeConfiguration;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.OurBadException;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class RumbleServer {

    private RumbleRuntimeConfiguration rumbleRuntimeConfiguration;

    public RumbleServer(RumbleRuntimeConfiguration rumbleRuntimeConfiguration) {
        this.rumbleRuntimeConfiguration = rumbleRuntimeConfiguration;
    }

    public void start() {
        try {
            System.out.println(
                "Starting Rumble in server mode on port " + this.rumbleRuntimeConfiguration.getPort() + "..."
            );
            HttpServer server = HttpServer.create(
                new InetSocketAddress(
                        this.rumbleRuntimeConfiguration.getHost(),
                        this.rumbleRuntimeConfiguration.getPort()
                ),
                0
            );
            HttpContext context = server.createContext("/jsoniq");
            context.setHandler(new RumbleHttpHandler());
            server.start();
            System.out.println("Server running. Press Control+C to stop.");
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10000);
            }
        } catch (IOException e) {
            throw new OurBadException(e.getMessage(), ExceptionMetadata.EMPTY_METADATA);
        } catch (InterruptedException e) {
            System.out.println("Interrupted.");
        }
    }

}
