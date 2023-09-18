package geektime.tdd;

import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;

public class Main {
    public static void main(String[] args) throws Exception {

        JettyHttpContainerFactory.createServer(
                UriBuilder.fromUri("http://localhost/").port(8051).build(),
                new Application()
        ).start();

    }
}
