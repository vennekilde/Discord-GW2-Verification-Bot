/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.security.auth.DestroyFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMain.class);

    private static DiscordBot discordBot;

    public DiscordMain() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, DestroyFailedException {
        createDiscordBot();

        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
        server.createContext("/discord", new RESTService());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public static DiscordBot getDiscordBot() {
        return discordBot;
    }

    public static void destroyDiscordBot() throws DestroyFailedException {
        if (discordBot != null) {
            LOGGER.info("Destroying Teamspeak bot instance");
            discordBot.destroy();
            discordBot = null;
        }
    }

    public static void createDiscordBot() throws DestroyFailedException, IOException {
        if (discordBot != null) {
            destroyDiscordBot();
        }
        LOGGER.info("Initiating Teamspeak bot instance");
        discordBot = new DiscordBot();
        discordBot.init();
    }

}
