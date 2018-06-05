/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordMain {
    
    public DiscordMain() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DiscordBot discordBot = new DiscordBot();
        discordBot.init();
    }
    
}
