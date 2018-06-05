/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordBot extends ListenerAdapter {

    private static final String HOME_WORLD_ROLE_ID = "182812004631707648";
    private static final String LINKED_WORLD_ROLE_NAME = "182812235280678913";
    private static final String MUSIC_BOT_ROLE_NAME = "182813018919272448";
    private static final String GUILD_ID = "174512426056810497";

    private WebsiteConnector websiteConnector;
    private ResourceBundle config; 
    
    private Role homeWorldRole = null;
    private Role linkedWorldRole = null;
    private Role musicBotRole = null;

    public DiscordBot() {
    }

    public WebsiteConnector getWebsiteConnector() {
        return websiteConnector;
    }
    
    public ResourceBundle getConfig() {
        return config;
    }
    
    public void init() {
        config = ResourceBundle.getBundle("config");
        websiteConnector = new WebsiteConnector(this.getConfig().getString("base_rest_url"));
        
        try {
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken("MTgzMDQyOTA0Njc4MjAzMzky.CiAHfQ.DsmeWDLJKi_5ANVMeNFlahG8Nno")
                    .addEventListener(this)
                    .buildBlocking();
            jda.setAutoReconnect(true);
        } catch (LoginException ex) {
            Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("API is ready!");
        Guild guild = event.getJDA().getGuildById(GUILD_ID);
        if (guild != null) {
            homeWorldRole = guild.getRoleById(HOME_WORLD_ROLE_ID);
            linkedWorldRole = guild.getRoleById(LINKED_WORLD_ROLE_NAME);
            musicBotRole = guild.getRoleById(MUSIC_BOT_ROLE_NAME);
        }

        AbstractMap.SimpleEntry<String,String> userIdEntry = new AbstractMap.SimpleEntry("151", "IAMDANSKER!!!!");
        Map<String, AccessStatusData> accessStatusForUsers = getWebsiteConnector().getAccessStatusForUsers(userIdEntry);
        for (Entry<String, AccessStatusData> entry : accessStatusForUsers.entrySet()) {
            System.out.println(entry);

        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String rawContent = event.getMessage().getContentRaw();
        System.out.println(event.getMessage().getContentRaw());
        if (rawContent.equalsIgnoreCase("/verify")) {
            System.out.println("Verify!");
            String session = websiteConnector.createSession(event.getAuthor().getIdLong(), null, event.getAuthor().getName(), true);
            event.getAuthor().openPrivateChannel().queue((channel) ->
            {
                channel.sendMessage(session).queue();
            });
        }
    }

    private SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    

    @Override
    public void onUserOnlineStatusUpdate(UserOnlineStatusUpdateEvent event) {
        List<Role> rolesForUser = event.getJDA().getGuilds().get(0).getMember(event.getUser()).getRoles();
        System.out.println(rolesForUser);
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        super.onPrivateMessageReceived(event);
        String senderId = event.getAuthor().getId();
        String senderName = event.getAuthor().getName();
        if (event.getMessage().getContentRaw().equalsIgnoreCase("/status")) {
            AbstractMap.SimpleEntry<String,String> entry = new AbstractMap.SimpleEntry(senderId, senderName);
            MessageAction mf = event.getChannel().sendMessage(getWebsiteConnector().getAccessStatusForUsers(entry).get(senderId).toString());
            mf.submit();
        }
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        super.onGuildVoiceMove(event); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        super.onGuildVoiceJoin(event); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    
    public void updateUserRoles(Member member, Guild guild) {
        AbstractMap.SimpleEntry<String,String> userIdEntry = new AbstractMap.SimpleEntry(member.getUser().getId(), member.getUser().getName());
        Map<String, AccessStatusData> accessStatus = getWebsiteConnector().getAccessStatusForUsers(userIdEntry);
        updateUserRoles(member, guild, accessStatus.get(member.getUser().getId()));
    }

    public void updateUserRoles(Member member, Guild guild, AccessStatusData accessStatus) {
        List<Role> rolesForUser = member.getRoles();
        switch (accessStatus.getAccessStatus()) {
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
                if (!accessStatus.isMusicBot()) {
                    //Access is primary and not a music bot
                    addRoleToUserIfNotOwned(member, guild, rolesForUser, homeWorldRole);
                    removeRoleFromUserIfOwned(member, guild, rolesForUser, linkedWorldRole, musicBotRole);
                } else {
                    //Access is granted trough another user and is a music bot
                    addRoleToUserIfNotOwned(member, guild, rolesForUser, musicBotRole);
                    removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole);
                }
                break;
            case ACCESS_GRANTED_LINKED_WORLD:
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                if (!accessStatus.isMusicBot()) {
                    //Access is primary and not a music bot
                    addRoleToUserIfNotOwned(member, guild, rolesForUser, linkedWorldRole);
                    removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, musicBotRole);
                } else {
                    //Access is granted trough another user and is a music bot
                    addRoleToUserIfNotOwned(member, guild, rolesForUser, musicBotRole);
                    removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole);
                }
                break;
            case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
                removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole, musicBotRole);
                break;
            case ACCESS_DENIED_BANNED:
                removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole, musicBotRole);
                break;
            case ACCESS_DENIED_EXPIRED:
                removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole, musicBotRole);
                break;
            case ACCESS_DENIED_INVALID_WORLD:
                removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole, musicBotRole);
                break;
            case ACCESS_DENIED_UNKNOWN:
                removeRoleFromUserIfOwned(member, guild, rolesForUser, homeWorldRole, linkedWorldRole, musicBotRole);
                break;
            case COULD_NOT_CONNECT:
                break;
        }
    }

    public boolean removeRoleFromUserIfOwned(Member member, Guild guild, List<Role> givenRoles, Role... roles) {
        boolean result = false;
        List<Role> rolesToRemove = new ArrayList();
        for (Role role : roles) {
            if (givenRoles.contains(role)) {
                rolesToRemove.add(role);
            }
        }
        if (rolesToRemove.size() > 0) {
            AuditableRestAction<Void> action = guild.getController().removeRolesFromMember(member, rolesToRemove);
            action.submit();
            result = true;
        }
        return result;
    }

    public boolean addRoleToUserIfNotOwned(Member member, Guild guild, List<Role> givenRoles, Role... roles) {
        boolean result = false;
        List<Role> rolesToAssign = new ArrayList();
        for (Role role : roles) {
            if (!givenRoles.contains(role)) {
                rolesToAssign.add(role);
            }
        }
        if (rolesToAssign.size() > 0) {
            AuditableRestAction<Void> action = guild.getController().addRolesToMember(member, rolesToAssign.toArray(new Role[rolesToAssign.size()]));
            action.submit();
            result = true;
        }
        return result;
    }
}
