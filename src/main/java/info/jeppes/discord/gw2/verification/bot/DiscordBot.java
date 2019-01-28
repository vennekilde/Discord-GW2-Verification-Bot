/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import info.jeppes.discord.gw2.verification.bot.utils.TimeUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordBot extends ListenerAdapter implements Destroyable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DiscordBot.class);

    private static final String HOME_WORLD_ROLE_ID = "182812004631707648";
    private static final String LINKED_WORLD_ROLE_ID = "182812235280678913";
    private static final String TEMP_HOME_WORLD_ROLE_ID = "451360523066540032";
    private static final String TEMP_LINKED_WORLD_ROLE_ID = "451360652875923457";
//    private static final String MUSIC_BOT_ROLE_NAME = "182813018919272448";
    private static final String GUILD_ID = "174512426056810497";
    private static final String WELCOME_CHANNEL = "529635390164959232";
    private static final String SERVER_NAME = "Far Shiverpeaks";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private WebsiteConnector websiteConnector;
    private ResourceBundle config;

    private Role homeWorldRole = null;
    private Role linkedWorldRole = null;
    private Role tempHomeWorldRole = null;
    private Role tempLinkedWorldRole = null;
//    private Role musicBotRole = null;
    private JDA discordAPI;
    private ScheduledFuture<?> refreshSchedule;
    private LinkedList<Long> scheduledRefreshes;
    private Map<Long, User> scheduledRefreshesMap;
    private List<Long> userRefreshingRoles;

    public DiscordBot(ResourceBundle config) {
        scheduledRefreshesMap = new HashMap();
        scheduledRefreshes = new LinkedList();
        userRefreshingRoles = new ArrayList();
        this.config = config;
    }

    public WebsiteConnector getWebsiteConnector() {
        return websiteConnector;
    }

    public ResourceBundle getConfig() {
        return config;
    }

    public void init() throws IOException {
        websiteConnector = new WebsiteConnector(getConfig().getString("base_rest_url"));

        try {
            discordAPI = new JDABuilder(AccountType.BOT)
                    .setToken(getConfig().getString("discord_token"))
                    .addEventListener(this)
                    .buildBlocking();
            discordAPI.setAutoReconnect(true);

            if (refreshSchedule == null) {
                refreshSchedule = scheduler.schedule(() -> {
                    updateRefreshSchedule();
                    int counter = 0;
                    while (true) {
                        try {
                            Long discordId = scheduledRefreshes.removeFirst();
                            if (counter == 100) {
                                counter = 0;
                                updateRefreshSchedule();
                            }

                            User user = scheduledRefreshesMap.remove(discordId);
                            updateUserRoles(user);
                            counter++;

                        } catch (NoSuchElementException ex) {
                            updateRefreshSchedule();
                        } catch (Exception ex) {
                            LOGGER.error(ex.getMessage(), ex);
                        }
                        Thread.sleep(1000);
                    }
                }, 0, TimeUnit.MINUTES);
            }
        } catch (LoginException | IllegalArgumentException | InterruptedException ex) {
            Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("API is ready!");
        Guild guild = event.getJDA().getGuildById(GUILD_ID);

        if (guild != null) {
            guild.getRoles().forEach((role) -> {
                LOGGER.info("Role: " + role.getName() + " ID: " + role.getId());
            });

            homeWorldRole = guild.getRoleById(HOME_WORLD_ROLE_ID);
            linkedWorldRole = guild.getRoleById(LINKED_WORLD_ROLE_ID);
            tempHomeWorldRole = guild.getRoleById(TEMP_HOME_WORLD_ROLE_ID);
            tempLinkedWorldRole = guild.getRoleById(TEMP_LINKED_WORLD_ROLE_ID);
//            musicBotRole = guild.getRoleById(MUSIC_BOT_ROLE_NAME);
        }
    }

    /**
     * Update list of users that needs to be refreshed
     */
    public void updateRefreshSchedule() {
//        LOGGER.info("Update refresh schedule");
        discordAPI.getGuilds().forEach((guild) -> {
            guild.getMembers().forEach((member) -> {
                if (!scheduledRefreshes.contains(member.getUser().getIdLong())) {
                    scheduledRefreshes.add(member.getUser().getIdLong());
                    scheduledRefreshesMap.put(member.getUser().getIdLong(), member.getUser());
                }
            });
        });
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        sendRulesMessage(event.getUser());
        sendVerifyMessageCheckAccess(event.getUser());
        sendWelcomeMessage(event.getUser());
    }

    private void sendWelcomeMessage(User user) {
        String messageString = "Hey <@" + user.getId() + ">, welcome to *" + SERVER_NAME + "* :hugging: :tada: !";
        TextChannel channel = discordAPI.getTextChannelById(WELCOME_CHANNEL);
        if (channel != null) {
            try {
                MessageHistory.MessageRetrieveAction history = channel.getHistoryAfter("529644659262357526", 100);
                RequestFuture<MessageHistory> submit = history.submit();
                submit.get().getRetrievedHistory().forEach((message) -> {
                    if (!message.isPinned()) {
                        message.delete().submit();
                    }
                });
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
            }
            MessageAction message = channel.sendMessage(messageString);
            message.submit();
        }
    }

    @Override
    public void onGenericMessageReaction(GenericMessageReactionEvent event) {
//        if (event instanceof MessageReactionAddEvent && event.getChannel().getIdLong() == 481570753100120095L) {
//            websiteConnector.SetUserServiceLinkAttribute(
//                    event.getUser().getIdLong(),
//                    "poll-" + event.getMessageIdLong(),
//                    String.valueOf(event.getReactionEmote().getName().hashCode()));
//
//            LOGGER.info("User {} voted {}", event.getUser().getId(), event.getReactionEmote().getName().hashCode());
//            Message message = event.getChannel().getMessageById(event.getMessageId()).complete();
//            message.getReactions().forEach((reaction) -> {
////                if (!event.getReaction().getReactionEmote().getName().equals(reaction.getReactionEmote().getName())) {
//                reaction.removeReaction(event.getUser()).complete();
////                }
//            });
//        }
    }

    public void sendVerifyMessageCheckAccess(User user) {
        sendVerifyMessageCheckAccess(user, true);
    }

    public void sendVerifyMessageCheckAccess(User user, boolean hideIfVerified) {
        AbstractMap.SimpleEntry<String, String> userIdEntry = new AbstractMap.SimpleEntry(user.getId(), user.getName());
        Map<String, AccessStatusData> accessStatusMap = getWebsiteConnector().getAccessStatusForUsers(userIdEntry);
        AccessStatusData accessStatusData = accessStatusMap.get(user.getId());
        switch (accessStatusData.getAccessStatus()) {
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_LINKED_WORLD:
                if (!hideIfVerified) {
                    sendAlreadyVerifiedMessage(user);
                }
                break;
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                if (!hideIfVerified) {
                    sendCurrentAccessTypeMessage(user, accessStatusData);
                    sendVerifyMessage(user);
                }
                break;
            case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
            case ACCESS_DENIED_EXPIRED:
            case ACCESS_DENIED_INVALID_WORLD:
                sendVerifyMessage(user);
                break;
            case COULD_NOT_CONNECT:
                sendUnableToConnectMessage(user);
                break;
            case ACCESS_DENIED_UNKNOWN:
                sendUnknownAccessStatusMessage(user);
                break;
            case ACCESS_DENIED_BANNED:
                sendVerificationBannedMessage(user, accessStatusData.getBanReason());
                break;
        }
    }

    public void sendRulesMessage(User user) {
        sendPrivateMessage(user,
                "- If you wish to join TeamSpeak aswell you can do so at: ts.farshiverpeaks.com\n\n"
                + "- #help-desk will contain commands and general help to navigate through Discord. But if you should need help, please feel free to msg @Admin or @Moderator :)\n\n"
                + "_**Discord rules:**_\n"
                + "- Be gentle and respect everyone!\n"
                + "- No racism, no sexual, no political, no religious comments (It’s allowed to discuss about in certain forum part with productive content)\n"
                + "- No pornographic/nsfw/insulting threads and posts\n"
                + "- No pornographic/nsfw/insulting Nicknames\n"
                + "- Personal problems against certain person will be solved ONLY in private messages! (If you need moderator or admin for the help, ask them)\n"
                + "- Spam is not allowed in any way!\n"
                + "- Any malicious activity, either confirmed or suspected, on Discord will result in an immediate action at the discretion of an Administrator.\n"
                + "Violation of the rules shall be judged on an individual basis, though in general there will be a 3 strikes and out policy.");
    }

    public void sendCurrentAccessTypeMessage(User user, AccessStatusData accessStatusData) {
        String message
                = "Your current access level\n"
                + accessStatusData.getAccessStatus().name() + "\n\n";

        if (accessStatusData.getExpires() > 0) {
            message += "Expires in " + TimeUtils.getTimeWWDDHHMMSSStringShort(accessStatusData.getExpires()) + "\n\n";
        }
        if (accessStatusData.getAccessStatus() == AccessStatus.ACCESS_DENIED_BANNED && accessStatusData.getBanReason() != null && !accessStatusData.getBanReason().isEmpty()) {
            message += "Ban reason: " + accessStatusData.getBanReason() + "\n";
        }
        if (accessStatusData.isMusicBot()) {
            message += "Current user is designated as a Music Bot. Primary database user id: " + accessStatusData.getMusicBotOwner() + "\n\n";
        }
        sendPrivateMessage(user, message);
        LOGGER.info("Sent Current Access Type message to user: {}", user.getId());
    }

    public void sendVerifyMessage(User user) {
        Session session = getUniqueLinkURL(user);
        if (session == null) {
            return;
        }
        String message
                = "Link your Discord user with your GuildWars 2 account to gain access\n"
                + session.getSessionURL() + "\n";
        sendPrivateMessage(user, message);
        sendPrivateMessage(user,
                "Link expires in " + TimeUtils.getTimeWWDDHHMMSSStringShort(session.getValidTo() - System.currentTimeMillis() / 1000) + "\n"
                + "Type /verify to get a new one");
        LOGGER.info("Sent authentication message to user: {}", user.getId());
    }

    public void sendAlreadyVerifiedMessage(User user) {
        sendPrivateMessage(user, "You already have access to our discord. If this is not the case, try /verify to get a new link to get access or /refresh to refresh your api data");
    }

    public void sendUnknownAccessStatusMessage(User user) {
        sendPrivateMessage(user,
                "\nFor an unknown reason, your account is not verified\n"
                + "If you just transfered to Far Shiverpeaks, but is still on your old server for the end of the matchup, could be the cause\n"
                + "Please contact an admin if you believe this to be a mistake, or wait a little while until the\n"
                + "API refreshes the persisted data which happens once every 30 minutes and see if that fixed the issue\n");
    }

    public void sendVerificationBannedMessage(User user, String banReason) {
        sendPrivateMessage(user,
                "\nYou have been verification banned from Far Shiverpeaks community\n"
                + "Please contact an admin if you believe this to be a mistake\n"
                + "\nBan reason: " + banReason + "\n\n");
    }

    public void sendUnableToConnectMessage(User user) {
        sendPrivateMessage(user,
                "Could not connect to verification server\n"
                + "If you need access to the restricted channels\n"
                + "Please reconnect and see if you still receive this message\n"
                + "If you still see this message, please contact and admin to make them aware of the issue\n");
    }

    public void sendPrivateMessage(User user, String message) {
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(message).queue();
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String rawContent = event.getMessage().getContentRaw();
        LOGGER.info(event.getAuthor().getId() + ": " + rawContent);
        String content = rawContent.toLowerCase();
        switch (content) {
            case "/verify":
            case "!verify":
            case "/key":
            case "!key":
                sendVerifyMessageCheckAccess(event.getAuthor(), false);
                break;
            case "/status":
            case "!status":
                AbstractMap.SimpleEntry<String, String> entry = new AbstractMap.SimpleEntry(event.getAuthor().getId(), event.getAuthor().getName());
                Map<String, AccessStatusData> accessStatus = getWebsiteConnector().getAccessStatusForUsers(entry);
                sendPrivateMessage(event.getAuthor(), accessStatus.get(event.getAuthor().getId()).toString());
            case "/refresh":
            case "!refresh":
                updateUserRoles(event.getAuthor());
                break;
            case "/rules":
            case "!rules":
                sendRulesMessage(event.getAuthor());
                break;
            case "/help":
            case "!help":
            case "commands":
            case "/commands":
            case "!commands":
                sendPrivateMessage(event.getAuthor(), "Available commands\n"
                        + "/verify  - Provides you with a unique link that will link your Discord account with your GuildWars 2 account\n"
                        + "/status  - Displays your current verification status\n"
                        + "/rules   - Get a list of current discord rules\n"
                        + "/refresh - Forces the Discord bot to refresh your verification status with the verification server (If everything works, this should do absolutely nothing)\n"
                        + "/help    - Shows list of available commands");
                break;
        }
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        onRoleUpdated(event.getMember(), event.getRoles());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        onRoleUpdated(event.getMember(), event.getRoles());
    }

    public void onRoleUpdated(Member member, List<Role> roles) {
        //Do not check for users whose roles are already being updated
        if (userRefreshingRoles.contains(member.getUser().getIdLong())) {
            return;
        }
        roles.forEach((role) -> {
            switch (role.getId()) {
                case HOME_WORLD_ROLE_ID:
                case LINKED_WORLD_ROLE_ID:
                case TEMP_HOME_WORLD_ROLE_ID:
                case TEMP_LINKED_WORLD_ROLE_ID:
                    AbstractMap.SimpleEntry<String, String> entry = new AbstractMap.SimpleEntry(member.getUser().getId(), member.getNickname());
                    Map<String, AccessStatusData> accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);

                    AccessStatusData accessStatus = accessStatuses.get(entry.getKey());

                    boolean givenTempAccess = checkIfReceivedTemporaryAccess(member.getUser(), accessStatus);
                    //Refresh data from website
                    //TODO Consider not doing this, as the website actually informs the teamspeak
                    //bot if a user has recieved temporary access, which causes the bot to
                    //refresh the user data twice
                    if (givenTempAccess) {
                        accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);
                        accessStatus = accessStatuses.get(entry.getKey());
                    }
                    updateUserRoles(member, accessStatus);
            }
        });
    }

    public Session getUniqueLinkURL(User user) {
        return websiteConnector.createSession(user.getIdLong(), null, user.getName(), true);
    }

    private final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void updateUserRoles(long discordId) {
        discordAPI.getGuilds().forEach((guild) -> {
            if (guild != null) {
                Member member = guild.getMemberById(discordId);
                updateUserRoles(member);
            }
        });
    }

    public void updateUserRoles(User user) {
        if (user == null) {
            return;
        }
        user.getMutualGuilds().forEach((guild) -> {
            updateUserRoles(guild.getMember(user));
        });
    }

    public void updateUserRoles(Member member) {
        AbstractMap.SimpleEntry<String, String> userIdEntry = new AbstractMap.SimpleEntry(member.getUser().getId(), member.getUser().getName());
        Map<String, AccessStatusData> accessStatus = getWebsiteConnector().getAccessStatusForUsers(userIdEntry);
        updateUserRoles(member, accessStatus.get(member.getUser().getId()));
    }

    public void updateUserRoles(Member member, AccessStatusData accessStatus) {
        List<Role> rolesForUser = member.getRoles();
        userRefreshingRoles.add(member.getUser().getIdLong());
        try {
            switch (accessStatus.getAccessStatus()) {
                case ACCESS_GRANTED_HOME_WORLD:
                    if (!accessStatus.isMusicBot()) {
                        //Access is primary and not a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser, homeWorldRole);
                        removeRoleFromUserIfOwned(member, rolesForUser, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole/*, musicBotRole*/);
                    } else {
                        //Access is granted trough another user and is a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser/*, musicBotRole*/);
                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole);
                    }
                    break;
                case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
                    //Don't assign temporary groups, just let mods/commanders deal with them
//                    if (!accessStatus.isMusicBot()) {
//                        //Access is primary and not a music bot
//                        addRoleToUserIfNotOwned(member, rolesForUser, tempHomeWorldRole);
//                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempLinkedWorldRole/*, musicBotRole*/);
//                    } else {
//                        //Access is granted trough another user and is a music bot
//                        addRoleToUserIfNotOwned(member, rolesForUser/*, musicBotRole*/);
//                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole);
//                    }
                    break;
                case ACCESS_GRANTED_LINKED_WORLD:
                    if (!accessStatus.isMusicBot()) {
                        //Access is primary and not a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser, linkedWorldRole);
                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, tempHomeWorldRole, tempLinkedWorldRole/*, musicBotRole*/);
                    } else {
                        //Access is granted trough another user and is a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser/*, musicBotRole*/);
                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole);
                    }
                    break;
                case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                    //Don't assign temporary groups, just let mods/commanders deal with them
//                    if (!accessStatus.isMusicBot()) {
//                        //Access is primary and not a music bot
//                        addRoleToUserIfNotOwned(member, rolesForUser, tempLinkedWorldRole);
//                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole/*, musicBotRole*/);
//                    } else {
//                        //Access is granted trough another user and is a music bot
//                        addRoleToUserIfNotOwned(member, rolesForUser/*, musicBotRole*/);
//                        removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole);
//                    }
                    break;
                case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
                case ACCESS_DENIED_UNKNOWN:
                case ACCESS_DENIED_BANNED:
                case ACCESS_DENIED_EXPIRED:
                case ACCESS_DENIED_INVALID_WORLD:
                    removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole/*, musicBotRole*/);
                    break;
                case COULD_NOT_CONNECT:
                    break;
            }
        } finally {
            userRefreshingRoles.remove(member.getUser().getIdLong());
        }
    }

    public boolean removeRoleFromUserIfOwned(Member member, List<Role> givenRoles, Role... roles) {
        boolean result = false;
        List<Role> rolesToRemove = new ArrayList();
        for (Role role : roles) {
            if (givenRoles.contains(role)) {
                rolesToRemove.add(role);
            }
        }
        if (rolesToRemove.size() > 0) {
            AuditableRestAction<Void> action = member.getGuild().getController().removeRolesFromMember(member, rolesToRemove);
            action.submit();
            result = true;
            //Send message to user about removed roles
            final StringBuilder rolesStr = new StringBuilder();
            member.getUser().openPrivateChannel().queue((channel) -> {
                rolesToRemove.forEach((role) -> {
                    rolesStr.append("\n - ").append(role.getName());
                });
                MessageAction message = channel.sendMessage("You have been removed from the following roles on Discord server \"" + member.getGuild().getName() + "\"" + rolesStr.toString());
                message.submit();
            });
        }
        return result;
    }

    public boolean addRoleToUserIfNotOwned(Member member, List<Role> givenRoles, Role... roles) {
        boolean result = false;
        List<Role> rolesToAssign = new ArrayList();
        for (Role role : roles) {
            if (!givenRoles.contains(role)) {
                rolesToAssign.add(role);
            }
        }
        if (rolesToAssign.size() > 0) {
            AuditableRestAction<Void> action = member.getGuild().getController().addRolesToMember(member, rolesToAssign.toArray(new Role[rolesToAssign.size()]));
            action.submit();
            result = true;
            //Send message to user about added roles
            final StringBuilder rolesStr = new StringBuilder();
            member.getUser().openPrivateChannel().queue((channel) -> {
                rolesToAssign.forEach((role) -> {
                    rolesStr.append("\n - ").append(role.getName());
                });
                MessageAction message = channel.sendMessage("You have been added to the following roles on Discord server \"" + member.getGuild().getName() + "\"" + rolesStr.toString());
                message.submit();
            });
        }
        return result;
    }

    public boolean checkIfReceivedTemporaryAccess(User user, AccessStatusData accessStatusData) {
        boolean accessGiven = false;
        //Check if user already has been given access
        int expires = accessStatusData.getExpires();
        switch (accessStatusData.getAccessStatus()) {
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
            case ACCESS_GRANTED_LINKED_WORLD:
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                return false;
            case ACCESS_DENIED_EXPIRED:
                JSONObject attributes = accessStatusData.getAttributes();
                if (attributes == null || !attributes.has("tempExpired") || attributes.getString("tempExpired").equals("false")) {
                    getWebsiteConnector().SetUserServiceLinkAttribute(user.getIdLong(), "tempExpired", "true");
                    return false;
                }
        }

        if (expires <= (System.currentTimeMillis() / 1000)) {
            for (Guild guild : user.getMutualGuilds()) {
                for (Role role : guild.getMember(user).getRoles()) {
                    switch (role.getId()) {
                        case TEMP_HOME_WORLD_ROLE_ID:
                            //if(!shadowMode){
                            getWebsiteConnector().grantTemporaryAccess(user.getIdLong(), user.getName(), WebsiteConnector.AccessType.HOME_SERVER);
                            //}
                            accessGiven = true;
                            LOGGER.info("User " + user.getId() + " has been granted temporary access [Home World]");
                            break;
                        case TEMP_LINKED_WORLD_ROLE_ID:
                            //if(!shadowMode){
                            getWebsiteConnector().grantTemporaryAccess(user.getIdLong(), user.getName(), WebsiteConnector.AccessType.LINKED_SERVER);
                            //}
                            accessGiven = true;
                            LOGGER.info("User " + user.getId() + " has been granted temporary access [Linked World]");
                            break;
                    }
                }
            }
        }
        return accessGiven;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        discordAPI.shutdownNow();
        if (refreshSchedule != null) {
            refreshSchedule.cancel(true);
            refreshSchedule = null;
        }
    }
}
