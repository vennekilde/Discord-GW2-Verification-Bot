/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import com.farshiverpeaks.gw2verifyclient.api.GuildWars2VerificationAPIClient;
import com.farshiverpeaks.gw2verifyclient.exceptions.GuildWars2VerificationAPIException;
import com.farshiverpeaks.gw2verifyclient.model.APIKeyData;
import com.farshiverpeaks.gw2verifyclient.model.APIKeyName;
import com.farshiverpeaks.gw2verifyclient.model.TemporaryData;
import com.farshiverpeaks.gw2verifyclient.model.VerificationStatus;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.apikey.model.ApikeyPUTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.apikey.model.ApikeyPUTQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.apikey.name.model.NameGETHeader;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.properties.model.PropertiesPUTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.properties.model.PropertiesPUTQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.verification.refresh.model.RefreshPOSTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.verification.status.model.StatusGETHeader;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.verification.status.model.StatusGETQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.users.service_id.service_user_id.verification.temporary.model.TemporaryPUTHeader;
import info.jeppes.discord.gw2.verification.bot.utils.TimeUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
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
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.glassfish.jersey.client.ClientProperties;
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
    private static final String DJ_ROLE_ID = "482569992613920788";

//    private static final String MUSIC_BOT_ROLE_NAME = "182813018919272448";
    private static final String GUILD_ID = "174512426056810497";
    private static final String WELCOME_CHANNEL = "529635390164959232";
    private static final String SERVER_NAME = "Far Shiverpeaks";
    private static final String SERVICE_ID = "2";

    private static final String TEMP_HOME_WORLD = "HOME_WORLD";
    private static final String TEMP_LINKED_WORLD = "LINKED_WORLD";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private GuildWars2VerificationAPIClient apiClient;
    private String apiAuthToken;
    private ResourceBundle config;

    private Role homeWorldRole = null;
    private Role linkedWorldRole = null;
    private Role tempHomeWorldRole = null;
    private Role tempLinkedWorldRole = null;
    private Role djRole = null;
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
        this.apiAuthToken = config.getString("rest_access_token");
    }

    public GuildWars2VerificationAPIClient getAPIClient() {
        return apiClient;
    }

    public JDA getDiscordAPI() {
        return discordAPI;
    }

    public String getGuildId() {
        return GUILD_ID;
    }

    public String getAPIAuthToken() {
        return apiAuthToken;
    }

    public ResourceBundle getConfig() {
        return config;
    }

    public void init() throws IOException {
        apiClient = new GuildWars2VerificationAPIClient(config.getString("base_rest_url")) {
            @Override
            protected Client getClient() {
                final Client client = ClientBuilder.newClient();
                client.property(ClientProperties.CONNECT_TIMEOUT, 5000);
                client.property(ClientProperties.READ_TIMEOUT, 5000);
                return client;
            }
        };

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
            djRole = guild.getRoleById(DJ_ROLE_ID);
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
        try {
            sendVerifyMessageCheckAccess(event.getUser());
        } catch (GuildWars2VerificationAPIException ex) {
            LOGGER.error(ex.getMessage(), ex);
            if (ex.getError() != null && ex.getError().getSafeDisplayError() != null) {
                sendPrivateMessage(event.getUser(), ex.getError().getSafeDisplayError());
            }
        }
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

    public void sendVerifyMessageCheckAccess(User user) throws GuildWars2VerificationAPIException {
        sendVerifyMessageCheckAccess(user, true);
    }

    public void sendVerifyMessageCheckAccess(User user, boolean hideIfVerified) throws GuildWars2VerificationAPIException {
        VerificationStatus accessStatusData = getStatus(user.getId(), user.getName());
        AccessStatus accessStatus = AccessStatus.valueOf(accessStatusData.getStatus());
        switch (accessStatus) {
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
                    sendVerifyMessage(user, getAPIKeyName(user.getId()));
                }
                break;
            case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
            case ACCESS_DENIED_EXPIRED:
            case ACCESS_DENIED_INVALID_WORLD:
                sendVerifyMessage(user, getAPIKeyName(user.getId()));
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

    public void sendCurrentAccessTypeMessage(User user, VerificationStatus accessStatusData) {
        String message
                = "Your current access level\n"
                + accessStatusData.getStatus() + "\n\n";

        if (accessStatusData.getExpires() > 0) {
            message += "Expires in " + TimeUtils.getTimeWWDDHHMMSSStringShort(accessStatusData.getExpires()) + "\n\n";
        }
        if (AccessStatus.ACCESS_DENIED_BANNED.name().equals(accessStatusData.getStatus()) && accessStatusData.getBanReason() != null && !accessStatusData.getBanReason().isEmpty()) {
            message += "Ban reason: " + accessStatusData.getBanReason() + "\n";
        }
        if (accessStatusData.getIsPrimary() != null && !accessStatusData.getIsPrimary()) {
            message += "Current user is designated as a Music Bot. Primary database user id: " + accessStatusData.getPrimaryUserId() + "\n\n";
        }
        sendPrivateMessage(user, message);
        LOGGER.info("Sent Current Access Type message to user: {}", user.getId());
    }

    public void sendVerifyMessage(User user, String apikeyName) {
        String message = "Go to https://account.arena.net/applications and create an API key \n"
                + "**Requirement:** You have to name your API Key **" + apikeyName + "**\n\n"
                + "Once you have your api key, type */verify <apikey>* to verify yourself";
        sendPrivateMessage(user, message);
        LOGGER.info("Sent authentication message to user: {}", user.getId());
    }

    public void sendAlreadyVerifiedMessage(User user) {
        sendPrivateMessage(user, "You already have access to our discord. If this is not the case, try /refresh to refresh your api data");
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
        if (event.getAuthor().getIdLong() == getDiscordAPI().getSelfUser().getIdLong()) {
            return;
        }
        try {
            String rawContent = event.getMessage().getContentRaw();
            String name = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
            LOGGER.info(event.getChannel().getName() + " - " + event.getAuthor().getId() + ":" + name + ": " + rawContent);
            String[] content = rawContent.split(" ");
            switch (content[0].toLowerCase()) {
                case "/verify":
                case "!verify":
                case "/apikey":
                case "!apikey":
                case "/key":
                case "!key":
                    if (content.length < 2) {
                        sendPrivateMessage(event.getAuthor(), "**Missing api key**");
                        sendVerifyMessage(event.getAuthor(), getAPIKeyName(event.getAuthor().getId()));
                    } else {
                        handleSetAPIKey(event, content[1]);
                    }
                    break;
                case "/status":
                case "!status":
                    VerificationStatus status = getStatus(event.getAuthor().getId(), event.getAuthor().getName());
                    StringBuilder sb = new StringBuilder();
                    sb.append("Status: ").append(status.getStatus());
                    if (status.getExpires() != null && status.getExpires() > 0) {
                        sb.append("Expires in : ").append(TimeUtils.getTimeWWDDHHMMSSStringShort(status.getExpires() * 1000));
                    }
                    if (status.getBanReason() != null) {
                        sb.append("Ban reason : ").append(status.getBanReason());
                    }
                    sendPrivateMessage(event.getAuthor(), sb.toString());
                    break;
                case "/refresh":
                case "!refresh":
                    refreshAccess(event.getAuthor().getId());
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
                    handleHelp(event);
                    break;
                default:
                    if (event.getChannelType() == ChannelType.PRIVATE) {
                        if (content[0].toUpperCase().matches("^(?:[A-F\\d]{4,20}-?){8,}$")) {
                            handleSetAPIKey(event, content[0]);
                        } else {
                            handleHelp(event);
                        }
                    }
                    break;
            }
        } catch (GuildWars2VerificationAPIException ex) {
            if (ex.getError() != null) {
                LOGGER.error(ex.getError().getError());
                if (ex.getError().getSafeDisplayError() != null) {
                    sendPrivateMessage(event.getAuthor(), ex.getError().getSafeDisplayError());
                } else {
                    sendPrivateMessage(event.getAuthor(), "Unable to communicate with verification backend");
                }
            } else {
                LOGGER.error(ex.getMessage(), ex);
                sendPrivateMessage(event.getAuthor(), "Unable to communicate with verification backend");
            }
        } catch (Throwable ex) {
            LOGGER.error(ex.getMessage(), ex);
            sendPrivateMessage(event.getAuthor(), "Unable to communicate with verification backend");
        }
    }

    public void handleSetAPIKey(MessageReceivedEvent event, String apikey) throws GuildWars2VerificationAPIException {
        setAPIKey(event.getAuthor().getId(), apikey, false);
        sendPrivateMessage(event.getAuthor(), "APIKey changed to: " + apikey);
        updateUserRoles(event.getAuthor());
    }

    public void handleHelp(MessageReceivedEvent event) {
        sendPrivateMessage(event.getAuthor(), "Available commands\n"
                + "/verify **apikey** - Verify yourself with an API key\n"
                + "/status          - Displays your current verification status\n"
                + "/rules           - Get a list of current discord rules\n"
                + "/refresh         - Forces the Discord bot to refresh your verification status with the verification server (If everything works, this should do absolutely nothing)\n"
                + "/help            - Shows list of available commands");
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        try {
            onRoleUpdated(event.getMember(), event.getRoles(), true);
        } catch (GuildWars2VerificationAPIException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        try {
            onRoleUpdated(event.getMember(), event.getRoles(), false);
        } catch (GuildWars2VerificationAPIException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    public void onRoleUpdated(Member member, List<Role> roles, boolean added) throws GuildWars2VerificationAPIException {
        //Do not check for users whose roles are already being updated
        if (userRefreshingRoles.contains(member.getUser().getIdLong())) {
            return;
        }
        for (Role role : roles) {
            switch (role.getId()) {
                case TEMP_HOME_WORLD_ROLE_ID:
                case TEMP_LINKED_WORLD_ROLE_ID:
                    if (added) {
                        VerificationStatus accessStatusData = getStatus(member.getUser().getId(), member.getNickname());

                        boolean givenTempAccess = grantTemporaryAccess(member.getUser(), accessStatusData);
                        //Refresh data from website
                        //TODO Consider not doing this, as the website actually informs the teamspeak
                        //bot if a user has recieved temporary access, which causes the bot to
                        //refresh the user data twice
                        if (givenTempAccess) {
                            accessStatusData = getStatus(member.getUser().getId(), member.getNickname());
                        }
                        updateUserRoles(member, accessStatusData);
                    }
                    break;
            }
        }
    }

//    public Session getUniqueLinkURL(User user) {
//        return apiClient.createSession(user.getIdLong(), null, user.getName(), true);
//    }
    private final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void updateUserRoles(long discordId) throws GuildWars2VerificationAPIException {
        for (Guild guild : discordAPI.getGuilds()) {
            if (guild != null) {
                Member member = guild.getMemberById(discordId);
                updateUserRoles(member);
            }
        }
    }

    public void updateUserRoles(User user) throws GuildWars2VerificationAPIException {
        if (user == null) {
            return;
        }
        for (Guild guild : user.getMutualGuilds()) {
            updateUserRoles(guild.getMember(user));
        }
    }

    public void updateUserRoles(Member member) throws GuildWars2VerificationAPIException {
        VerificationStatus accessStatusData = getStatus(member.getUser().getId(), member.getUser().getName());
        updateUserRoles(member, accessStatusData);
    }

    public void updateUserRoles(Member member, VerificationStatus accessStatus) {
        List<Role> rolesForUser = member.getRoles();
        userRefreshingRoles.add(member.getUser().getIdLong());
        try {
            switch (AccessStatus.valueOf(accessStatus.getStatus())) {
                case ACCESS_GRANTED_HOME_WORLD:
                    if (accessStatus.getIsPrimary() == null || accessStatus.getIsPrimary()) {
                        //Access is primary and not a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser, homeWorldRole, djRole);
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
                    if (accessStatus.getIsPrimary() == null || accessStatus.getIsPrimary()) {
                        //Access is primary and not a music bot
                        addRoleToUserIfNotOwned(member, rolesForUser, linkedWorldRole, djRole);
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
                    removeRoleFromUserIfOwned(member, rolesForUser, homeWorldRole, linkedWorldRole, tempHomeWorldRole, tempLinkedWorldRole, djRole/*, musicBotRole*/);
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
                    if (role != djRole) {
                        rolesStr.append("\n - ").append(role.getName());
                    }
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
                    if (role != djRole) {
                        rolesStr.append("\n - ").append(role.getName());
                    }
                });
                if (rolesStr.length() != 0) {
                    MessageAction message = channel.sendMessage("You have been added to the following roles on Discord server \"" + member.getGuild().getName() + "\"" + rolesStr.toString());
                    message.submit();
                }
            });
        }
        return result;
    }

    public boolean grantTemporaryAccess(User user, VerificationStatus accessStatusData) throws GuildWars2VerificationAPIException {
        boolean accessGiven = false;
        //Check if user already has been given access
        switch (AccessStatus.valueOf(accessStatusData.getStatus())) {
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_LINKED_WORLD:
            case ACCESS_DENIED_BANNED:
                return false;
        }

        for (Guild guild : user.getMutualGuilds()) {
            for (Role role : guild.getMember(user).getRoles()) {
                switch (role.getId()) {
                    case TEMP_HOME_WORLD_ROLE_ID:
                        //if(!shadowMode){
                        grantTemporary(user.getId(), user.getName(), AccessType.HOME_WORLD.name());
                        //}
                        accessGiven = true;
                        LOGGER.info("User " + user.getId() + " has been granted temporary access [Home World]");
                        break;
                    case TEMP_LINKED_WORLD_ROLE_ID:
                        //if(!shadowMode){
                        grantTemporary(user.getId(), user.getName(), AccessType.LINKED_WORLD.name());
                        //}
                        accessGiven = true;
                        LOGGER.info("User " + user.getId() + " has been granted temporary access [Linked World]");
                        break;
                }
            }
        }
        return accessGiven;
    }

    public VerificationStatus getStatus(String userId, String displayName) throws GuildWars2VerificationAPIException {
        StatusGETQueryParam qParams = new StatusGETQueryParam().withDisplayName(displayName);
        StatusGETHeader headers = new StatusGETHeader(getAPIAuthToken());
        VerificationStatus accessStatusData = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.status.get(qParams, headers);
        return accessStatusData;
    }

    public long grantTemporary(String userId, String displayName, String accessType) throws GuildWars2VerificationAPIException {
        TemporaryData body = new TemporaryData()
                .withAccessType(accessType).withDisplayName(displayName);
        TemporaryPUTHeader headers = new TemporaryPUTHeader(getAPIAuthToken());
        long expiresIn = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.temporary.put(body, headers);
        return expiresIn;
    }

    public void setUserProperty(String userId, String name, String value) throws GuildWars2VerificationAPIException {
        PropertiesPUTQueryParam qParams = new PropertiesPUTQueryParam(name, value);
        PropertiesPUTHeader headers = new PropertiesPUTHeader(getAPIAuthToken());
        String response = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).properties.put(qParams, headers);
    }

    public VerificationStatus refreshAccess(String userId) throws GuildWars2VerificationAPIException {
        RefreshPOSTHeader headers = new RefreshPOSTHeader(getAPIAuthToken());
        VerificationStatus response = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.refresh.post(headers);
        return response;
    }

    public void setAPIKey(String userId, String apikey, boolean primary) throws GuildWars2VerificationAPIException {
        APIKeyData body = new APIKeyData(apikey, primary);
        ApikeyPUTHeader headers = new ApikeyPUTHeader(getAPIAuthToken());
        ApikeyPUTQueryParam qParams = new ApikeyPUTQueryParam(false);
        String response = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).apikey.put(body, qParams, headers);
    }

    public String getAPIKeyName(String userId) throws GuildWars2VerificationAPIException {
        NameGETHeader headers = new NameGETHeader(getAPIAuthToken());
        APIKeyName response = getAPIClient().users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).apikey.name.get(headers);
        return response.getName();
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
