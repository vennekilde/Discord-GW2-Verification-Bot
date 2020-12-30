/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import com.farshiverpeaks.gw2verifyclient.api.GuildWars2VerificationAPIClient;
import com.farshiverpeaks.gw2verifyclient.exceptions.GuildWars2VerificationAPIException;
import com.farshiverpeaks.gw2verifyclient.model.ServiceLink;
import com.farshiverpeaks.gw2verifyclient.model.VerificationStatus;
import com.farshiverpeaks.gw2verifyclient.resource.v1.updates.service_id.subscribe.model.SubscribeGETHeader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import javax.security.auth.DestroyFailedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMain.class);

    private static DiscordBot discordBot;
    private static PropertyResourceBundle config;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, DestroyFailedException {
        String configPathEnv = System.getenv("BOT_CONFIG_PATH");
        File configFile;
        if (configPathEnv != null) {
            configFile = new File(configPathEnv);
        } else {
            configFile = new File(System.getProperty("user.dir"), "config.properties");
        }
        try (FileInputStream fis = new FileInputStream(configFile)) {
            config = new PropertyResourceBundle(fis);
        }

        createDiscordBot();

        new Thread(() -> {
            GuildWars2VerificationAPIClient apiClient = new GuildWars2VerificationAPIClient(config.getString("base_rest_url")) {
                @Override
                protected Client getClient() {
                    final Client client = ClientBuilder.newClient();
                    client.property(ClientProperties.CONNECT_TIMEOUT, 5000);
                    //High timeout for polling client
                    client.property(ClientProperties.READ_TIMEOUT, 150000);
                    return client;
                }
            };
            while (true) {
                try {
                    SubscribeGETHeader headers = new SubscribeGETHeader(discordBot.getAPIAuthToken());
                    VerificationStatus status = apiClient.v1.updates.serviceId("2").subscribe.get(headers).getBody();
                    LOGGER.info("Received verication update from server {}", status.toString());
                    getDiscordBot().getDiscordAPI().getGuilds().forEach((guild) -> {
                        for (ServiceLink link : status.getServiceLinks()) {
                            if (link.getServiceId() == 2) {
                                Member member = guild.getMemberById(link.getServiceUserId());
                                if (member != null) {
                                    getDiscordBot().updateUserRoles(member, status);
                                }
                            }
                        }
                    });
                } catch (ProcessingException ex) {
                    //Ignore, as it just means there was no update
                } catch (GuildWars2VerificationAPIException ex) {
                    // Ignore timeout exceptions
                    if (ex.getStatusCode() != 408) {
                        try {
                            LOGGER.error(ex.getMessage(), ex);
                            Thread.sleep(1000);
                        } catch (InterruptedException ex1) {
                        }
                    }
                }
            }
        }).start();
    }

    public static PropertyResourceBundle getConfig() {
        return config;
    }

    public static DiscordBot getDiscordBot() {
        return discordBot;
    }

    public static void destroyDiscordBot() throws DestroyFailedException {
        if (discordBot != null) {
            LOGGER.info("Destroying Discord bot instance");
            discordBot.destroy();
            discordBot = null;
        }
    }

    public static void createDiscordBot() throws DestroyFailedException, IOException {
        if (discordBot != null) {
            destroyDiscordBot();
        }
        LOGGER.info("Initiating Discord bot instance");
        discordBot = new DiscordBot(getConfig());
        discordBot.init();
    }

}
