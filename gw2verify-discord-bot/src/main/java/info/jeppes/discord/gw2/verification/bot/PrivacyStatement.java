package info.jeppes.discord.gw2.verification.bot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author venne
 */
public class PrivacyStatement {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DiscordMain.class);
    private static String[] PRIVACY_STATEMENT;

    static {
        try {
            InputStream is = PrivacyStatement.class.getClassLoader().getResourceAsStream("privacy-statement.md");
            String result = IOUtils.toString(is, StandardCharsets.UTF_8);
            PRIVACY_STATEMENT = result.split("\n\\*\\* ");
            for (int i = 1; i < PRIVACY_STATEMENT.length; i++) {
                PRIVACY_STATEMENT[i] = "‏‏‎ ‎\n**" + PRIVACY_STATEMENT[i];
            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            System.exit(-1);
        }
    }

    public static void sendPrivacyStatement(MessageChannel channel) {
        if (DiscordBot.ENABLE_PRIVACY_STATEMENT != null && DiscordBot.ENABLE_PRIVACY_STATEMENT.equalsIgnoreCase("true")) {
            Arrays.asList(PRIVACY_STATEMENT).forEach(section -> channel.sendMessage(section).queue());
        }
    }
}
