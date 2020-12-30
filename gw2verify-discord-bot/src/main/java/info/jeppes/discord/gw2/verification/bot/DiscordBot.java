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
import com.farshiverpeaks.gw2verifyclient.model.BanData;
import com.farshiverpeaks.gw2verifyclient.model.ChannelMetadata;
import com.farshiverpeaks.gw2verifyclient.model.ChannelUserMetadata;
import com.farshiverpeaks.gw2verifyclient.model.Error;
import com.farshiverpeaks.gw2verifyclient.model.TemporaryData;
import com.farshiverpeaks.gw2verifyclient.model.VerificationStatus;
import com.farshiverpeaks.gw2verifyclient.resource.v1.channels.service_id.channel.statistics.model.StatisticsPOSTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.apikey.model.ApikeyPUTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.apikey.model.ApikeyPUTQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.apikey.name.model.NameGETHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.ban.model.BanPUTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.properties.model.PropertiesPUTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.properties.model.PropertiesPUTQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.verification.refresh.model.RefreshPOSTHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.verification.status.model.StatusGETHeader;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.verification.status.model.StatusGETQueryParam;
import com.farshiverpeaks.gw2verifyclient.resource.v1.users.service_id.service_user_id.verification.temporary.model.TemporaryPUTHeader;
import info.jeppes.discord.gw2.verification.bot.utils.TimeUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.login.LoginException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import me.xhsun.guildwars2wrapper.GuildWars2;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class DiscordBot extends ListenerAdapter implements Destroyable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DiscordBot.class);

    private final Map<Long, ServerSettings> serverSettings = new HashMap();

//    private static final String MUSIC_BOT_ROLE_NAME = "182813018919272448";
    private static final String WELCOME_CHANNEL = "529635390164959232";
    private static final String SERVER_NAME = "Far Shiverpeaks";
    private static final String SERVICE_ID = "2";

//    private static final String TEMP_HOME_WORLD = "HOME_WORLD";
//    private static final String TEMP_LINKED_WORLD = "LINKED_WORLD";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private static final Pattern MENTION_ID_PATTERN = Pattern.compile("<@!(\\d*)>", Pattern.CASE_INSENSITIVE);

    private GuildWars2 gw2api;
    private GuildWars2VerificationAPIClient apiClient;
    private String apiAuthToken;
    private ResourceBundle config;

    private Map<String, Role> roleCache = new HashMap();
//    private Role musicBotRole = null;
    private JDA discordAPI;
    private ScheduledFuture<?> refreshSchedule;
    private LinkedList<Long> scheduledRefreshes;
    private Map<Long, User> scheduledRefreshesMap;
    private List<Long> userRefreshingRoles;

    private Map<String, String> guildNameCache;
    private Map<String, String> guildIdCache;

    public DiscordBot(ResourceBundle config) {
        scheduledRefreshesMap = new HashMap();
        scheduledRefreshes = new LinkedList();
        userRefreshingRoles = new ArrayList();
        guildNameCache = new HashMap();
        guildIdCache = new HashMap();
        this.config = config;
        this.apiAuthToken = config.getString("rest_access_token");
        gw2api = GuildWars2.getInstance();

        // Far shiverpeaks discord
        ServerSettings fspSettings = new ServerSettings(
                false,
                "182812004631707648",
                "182812235280678913",
                "451360523066540032",
                "451360652875923457",
                "482569992613920788",
                "182891788846104577",
                new String[]{"530768604497575950"}
        );
        fspSettings.getAccessTypeRoles().put("PlayForFree", "792147206656360468");
        serverSettings.put(174512426056810497L, fspSettings);

        // FSP Fighters discord
        serverSettings.put(722126272335052810L, new ServerSettings(
                true,
                "722175144986017813",
                "725633671486242827",
                "722449716415037471",
                "722449716415037471",
                null,
                "722127153038098502",
                null
        ));
    }

    public GuildWars2VerificationAPIClient getAPIClient() {
        return apiClient;
    }

    public JDA getDiscordAPI() {
        return discordAPI;
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
                    .addEventListeners(this)
                    .build();
            discordAPI.setAutoReconnect(true);
            if (refreshSchedule == null) {
                refreshSchedule = scheduler.schedule(() -> {
                    updateRefreshSchedule();
                    int counter = 0;
                    while (true) {
                        try {
                            Long discordId = scheduledRefreshes.removeFirst();
                            if (counter == 1000) {
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
        } catch (LoginException | IllegalArgumentException ex) {
            Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("API is ready!");
        for (Map.Entry<Long, ServerSettings> entry : serverSettings.entrySet()) {
            Guild guild = event.getJDA().getGuildById(entry.getKey());
            if (guild == null) {
                LOGGER.warn("Not a member of guild with ID: " + entry.getKey());
                continue;
            }
            guild.getRoles().forEach((role) -> {
                LOGGER.info("Guild: " + guild.getName() + ":" + guild.getIdLong() + " Role: " + role.getName() + " ID: " + role.getId());
            });

            ServerSettings settings = entry.getValue();
            roleCache.put(settings.getHomeWorldRoleID(), guild.getRoleById(settings.getHomeWorldRoleID()));
            roleCache.put(settings.getLinkedWorldRoleID(), guild.getRoleById(settings.getLinkedWorldRoleID()));
            roleCache.put(settings.getTempHomeWorldRoleID(), guild.getRoleById(settings.getTempHomeWorldRoleID()));
            roleCache.put(settings.getTempLinkedWorldRoleID(), guild.getRoleById(settings.getTempLinkedWorldRoleID()));
            if (settings.getDJRoleID() != null) {
                roleCache.put(settings.getDJRoleID(), guild.getRoleById(settings.getDJRoleID()));
            }
            if (settings.getRolesToRemoveWhenInvalid() != null) {
                Arrays.stream(settings.getRolesToRemoveWhenInvalid()).forEach((roleID) -> {
                    roleCache.put(roleID, guild.getRoleById(roleID));
                });
            }
            settings.getAccessTypeRoles().values().forEach((roleID) -> {
                roleCache.put(roleID, guild.getRoleById(roleID));
            });
//            musicBotRole = guild.getRoleById(MUSIC_BOT_ROLE_NAME);
        }
        scheduler.scheduleAtFixedRate(() -> {
            try {
                StatisticsPOSTHeader headers = new StatisticsPOSTHeader(apiAuthToken);
                getDiscordAPI().getVoiceChannels().forEach((channel) -> {
                    try {
                        ChannelMetadata metadata = new ChannelMetadata();
                        metadata.setUsers(new ArrayList());
                        metadata.setName(channel.getName());
                        channel.getMembers().forEach((member) -> {
                            GuildVoiceState state = member.getVoiceState();
                            if (state != null) {
                                ChannelUserMetadata userMetadata = new ChannelUserMetadata();
                                userMetadata.setDeafened(state.isDeafened());
                                userMetadata.setMuted(state.isMuted());
                                userMetadata.setId(member.getId());
                                userMetadata.setStreaming(state.isStream());
                                userMetadata.setName(member.getEffectiveName());
                                metadata.getUsers().add(userMetadata);
                            }
                        });
                        if (!metadata.getUsers().isEmpty()) {
                            apiClient.v1.channels.serviceId(SERVICE_ID).channel(channel.getId()).statistics.post(metadata, headers);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                });
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * Update list of users that needs to be refreshed
     */
    public void updateRefreshSchedule() {
        LOGGER.info("Updating refresh schedule. Current users to refresh: " + scheduledRefreshes.size());
        discordAPI.getGuilds().forEach((guild) -> {
            guild.getMembers().forEach((member) -> {
                if (!scheduledRefreshes.contains(member.getUser().getIdLong())) {
                    scheduledRefreshes.add(member.getUser().getIdLong());
                    scheduledRefreshesMap.put(member.getUser().getIdLong(), member.getUser());
                }
            });
        });
        LOGGER.info("Updated refresh schedule. New users to refresh: " + scheduledRefreshes.size());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        //sendRulesMessage(event.getUser());
        try {
            sendVerifyMessageCheckAccess(event.getUser());
        } catch (GuildWars2VerificationAPIException ex) {
            LOGGER.error(ex.getMessage(), ex);
            Error error = getErrorFromGW2VerificationAPIException(ex);
            if (error != null && error.getSafeDisplayError() != null) {
                sendPrivateMessage(event.getUser(), error.getSafeDisplayError());
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
                history.submit().get().getRetrievedHistory().forEach((message) -> {
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
            case ACCESS_GRANTED_LINKED_WORLD_TEMPORARY:
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
                "- #help-desk will contain commands and general help to navigate through Discord. But if you should need help, please feel free to msg @Admin or @Moderator :)\n\n"
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
            message += "\n\nBan reason: " + accessStatusData.getBanReason() + "\n";
        }
        if (accessStatusData.getIsPrimary() != null && !accessStatusData.getIsPrimary()) {
            message += "Current user is designated as a Music Bot. Primary database user id: " + accessStatusData.getPrimaryUserId() + "\n\n";
        }
        sendPrivateMessage(user, message);
        LOGGER.info("Sent Current Access Type message to user: {}", user.getId());
    }

    public void sendVerifyMessage(User user, String apikeyName) {
        String message = "Go to https://account.arena.net/applications and create an API key \n"
                + "**Requirement:** You have to name your API Key **" + apikeyName + "**\n"
                + "**Required Permissions:** Characters & Progression \n\n"
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
                case "!glistm":
                    if (event.getAuthor().getId() == "187402729696526336") {
                        StringBuilder sb = new StringBuilder();
                        Role role = getDiscordAPI().getRoleById(content[1]);
                        if (role == null) {
                            sendPrivateMessage(event.getAuthor(), "You need to use this command on the Discord server");
                            break;
                        }
                    }
                    break;
                case "!add":
                try {
                    event.getMessage().delete().queue();
                } finally {
                    if (event.getMember() == null) {
                        event.getChannel().sendMessage("You need to use this command on the Discord server").queue();
                        break;
                    }
                    if (content.length < 2) {
                        sendPrivateMessage(event.getAuthor(), "Missing guild tag. Command syntax: !add <GuildTag>");
                        break;
                    }
                    addGuildRole(event.getMessage().getMember(), content[1], true);
                    break;
                }
                case "!addsec":
                try {
                    event.getMessage().delete().queue();
                } finally {
                    if (event.getMember() == null) {
                        event.getChannel().sendMessage("You need to use this command on the Discord server").queue();
                        break;
                    }
                    if (content.length < 2) {
                        sendPrivateMessage(event.getAuthor(), "Missing guild tag. Command syntax: !addsec <GuildTag>");
                        break;
                    }
                    addGuildRole(event.getMessage().getMember(), content[1], false);
                    break;
                }
                case "!rm":
                try {
                    event.getMessage().delete().queue();
                } finally {
                    if (event.getMember() == null) {
                        event.getChannel().sendMessage("You need to use this command on the Discord server").queue();
                        break;
                    }
                    if (content.length < 2) {
                        sendPrivateMessage(event.getAuthor(), "Missing guild tag. Command syntax: !rm <GuildTag>");
                        break;
                    }
                    removeGuildRole(event.getMessage().getMember(), content[1]);
                    break;
                }
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
                //case "/rules":
                //case "!rules":
                //    sendRulesMessage(event.getAuthor());
                //    break;
                case "/help":
                case "!help":
                case "commands":
                case "/commands":
                case "!commands":
                    handleHelp(event);
                    break;
                case "/gw2ban":
                case "!gw2ban":
                    if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                        return;
                    }
                    if (content.length >= 4) {
                        String targetID = content[1];
                        String durationString = content[2];
                        String[] reasonArray = Arrays.copyOfRange(content, 3, content.length);
                        StringBuilder builder = new StringBuilder();
                        for (String s : reasonArray) {
                            builder.append(s).append(" ");
                        }
                        String reason = builder.toString();
                        long duration = 0;
                        try {
                            if ("p".equals(durationString)) {
                                duration = Long.MAX_VALUE;
                            } else {
                                duration = TimeUtils.getDurationMillisFromString(durationString);
                            }
                        } catch (IllegalArgumentException ex) {
                            event.getChannel().sendMessage("Could not parse provided duration string: " + durationString).queue();
                        }
                        Matcher matcher = MENTION_ID_PATTERN.matcher(targetID);
                        if (matcher.matches()) {
                            targetID = matcher.group(1);
                        }
                        if (duration > 0) {
                            banUser(targetID, duration, reason);
                            event.getChannel().sendMessage(content[1] + " has been banned for " + TimeUtils.getTimeWWDDHHMMSSStringShort(duration / 1000) + "\nReason: " + reason).queue();
                        }
                    } else {
                        event.getChannel().sendMessage("Missing argumnents. Command format: /gw2ban <user-id> <duration> <reason>\nDuration examples:\n - 1d2h3m4s = 1 Days, 2 Hours, 3 Minutes and 4 seconds\n - p = Permanent").queue();
                    }
                    break;
                default:
                    if (event.getChannelType() == ChannelType.PRIVATE) {
                        // check if message is an apikey
                        if (content[0].toUpperCase().matches("^(?:[A-F\\d]{4,20}-?){8,}$")) {
                            handleSetAPIKey(event, content[0]);
                        } else {
                            handleHelp(event);
                        }
                    }
                    break;
            }
        } catch (GuildWars2VerificationAPIException ex) {
            Error error = getErrorFromGW2VerificationAPIException(ex);
            if (error != null && error.getSafeDisplayError() != null) {
                sendPrivateMessage(event.getAuthor(), error.getSafeDisplayError());
            } else {
                sendPrivateMessage(event.getAuthor(), "Unable to communicate with verification backend");
            }
        } catch (Throwable ex) {
            LOGGER.error(ex.getMessage(), ex);
            sendPrivateMessage(event.getAuthor(), "Unable to communicate with verification backend");
        }
    }

    public void handleSetAPIKey(MessageReceivedEvent event, String apikey) throws GuildWars2VerificationAPIException {
        setAPIKey(event.getAuthor().getId(), apikey, true);
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
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        handlePlebKicking(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        handlePlebKicking(event);
    }

    /**
     * Kick a pleb if the user limit is reached to ensure pro's have space to
     * join
     *
     * @param event
     */
    public void handlePlebKicking(GenericGuildVoiceEvent event) {
        VoiceChannel vChannel = event.getVoiceState().getChannel();
        if (vChannel == null) {
            return;
        }

        List<Member> members = vChannel.getMembers();
        int userCount = members.size();
        int userLimit = vChannel.getUserLimit();
        if (userLimit > 0 && userCount >= userLimit) {
            //Sucks to be this guy
            Member leastImportantMember = null;
            //Arbitrary reasons for picking some member over another for kick
            int leastImportantImportance = 0;
            //Attempt to kick someone, limit reached

            // Only pro's allowed when channel is full
            if (isPleb(event.getMember())) {
                leastImportantMember = event.getMember();
                LOGGER.info("Pleb [{}] {} tried to rejoin full voice channel {}", leastImportantMember.getId(), leastImportantMember.getEffectiveName(), vChannel.getName());
            } else {
                // Alright, lets find our least important pleb and kick the sucker
                for (Member member : members) {
                    if (isPleb(member)) {
                        int importance = getPlebImportanceLevel(member);
                        if (importance < leastImportantImportance) {
                            LOGGER.info("[{}] {} was deafened, marked for kick. Importance: {}", member.getId(), member.getEffectiveName(), importance);
                            leastImportantMember = member;
                            leastImportantImportance = importance;
                        }
                    }
                }
            }
            if (leastImportantMember != null) {
                LOGGER.info("Kicked pleb [{}] {} to make space in channel {}", leastImportantMember.getId(), leastImportantMember.getEffectiveName(), vChannel.getName());

                leastImportantMember.getUser().openPrivateChannel().queue((channel) -> {
                    channel.sendMessage(
                            "You have been disconnected from voice channel \"" + vChannel.getName() + "\" to make space for other users\n"
                            + "Reasons for being kicked:\n"
                            + "- You were deafened\n"
                            + "- You are not verified"
                    ).queue();
                });
                vChannel.getGuild().kickVoiceMember(leastImportantMember).queue();
            } else {
                LOGGER.info("Could not find any plebs to kick from voice channel {}", vChannel.getName());
            }
        }
    }

    public boolean isPleb(Member member) {
        ServerSettings settings = serverSettings.get(member.getGuild().getIdLong());
        boolean isCommander = member.getRoles().stream().anyMatch((role) -> {
            return role.getId().equals(settings.getCommanderRoleID());
        });

        // Soo to avoid stuff... we just won't kick commanders
        if (isCommander) {
            return false;
        }

        // Only plebs are deaf
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState != null && voiceState.isDeafened()) {
            return true;
        }

        boolean isVerified = member.getRoles().stream().anyMatch((role) -> {
            return role.getId().equals(settings.getHomeWorldRoleID())
                    || role.getId().equals(settings.getLinkedWorldRoleID())
                    || role.getId().equals(settings.getTempHomeWorldRoleID())
                    || role.getId().equals(settings.getTempLinkedWorldRoleID());
        });

        return !isVerified;
    }

    public int getPlebImportanceLevel(Member member) {
        // The more roles you have, the more important you are... sure lets say that
        // Lets just sprinkle some randomness in there also
        int importance = (int) (member.getRoles().size() + (Math.random() * 3));

        //Check if the member is deaf
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState != null && !voiceState.isDeafened()) {
            //At least they aren't muted, so lets make them important... ish
            importance += 10;
        }
        return importance;
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

        ServerSettings settings = serverSettings.get(member.getGuild().getIdLong());

        for (Role role : roles) {
            if (role.getId().equals(settings.getTempHomeWorldRoleID())
                    || role.getId().equals(settings.getTempLinkedWorldRoleID())) {
                if (added) {
                    VerificationStatus accessStatusData = getStatus(member.getUser().getId(), member.getEffectiveName());

                    boolean givenTempAccess = grantTemporaryAccess(member.getUser(), accessStatusData);
                    //Refresh data from website
                    //TODO Consider not doing this, as the website actually informs the teamspeak
                    //bot if a user has recieved temporary access, which causes the bot to
                    //refresh the user data twice
                    if (givenTempAccess) {
                        accessStatusData = getStatus(member.getUser().getId(), member.getEffectiveName());
                    }
                    updateUserRoles(member, accessStatusData);
                }
            } else {
                boolean isPrimaryGuild = role.getName().matches("\\[.*?\\].*");
                boolean isSecondaryGuild = role.getName().matches("\\{.*?\\}.*");
                if (isPrimaryGuild || isSecondaryGuild) {
                    if (added) {
                        String guildName = role.getName().replaceFirst("(\\[|\\{).*?(\\]|\\}) ", "");
                        try {
                            String guildId = getGuildIdFromName(guildName);
                            VerificationStatus status = getStatus(member.getUser().getId(), member.getEffectiveName());
                            Map<String, Object> accountData = (Map<String, Object>) status.getAdditionalProperties().get("AccountData");
                            if (accountData != null) {
                                List<String> guilds = (List<String>) accountData.get("guilds");
                                if (!guilds.contains(guildId)) {
                                    member.getGuild().removeRoleFromMember(member, role).queue();
                                } else {
                                    if (isPrimaryGuild) {
                                        // Rename user
                                        Pattern p = Pattern.compile("\\[.*\\]");
                                        Matcher roleTagMatch = p.matcher(role.getName());
                                        if (!roleTagMatch.find()) {
                                            throw new RuntimeException();
                                        }
                                        String roleTag = roleTagMatch.group();
                                        Matcher match = p.matcher(member.getEffectiveName());
                                        if (match.find()) {
                                            // Replace existing tag
                                            member.modifyNickname(match.replaceAll(roleTag)).queue();
                                        } else {
                                            member.modifyNickname(roleTag + " " + member.getEffectiveName()).queue();
                                        }

                                        // remove other primary roles
                                        List<Role> rolesToRemove = new ArrayList();
                                        member.getRoles().forEach((r) -> {
                                            if (!r.getId().equals(role.getId()) && r.getName().matches("\\[.*?\\].*")) {
                                                rolesToRemove.add(r);
                                            }
                                        });
                                        rolesToRemove.forEach(roleToRemove -> {
                                            member.getGuild().removeRoleFromMember(member, roleToRemove).queue();
                                        });
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            LOGGER.error(ex.getMessage(), ex);
                        }
                    } else {
                        String nickname = member.getEffectiveName().replaceFirst("\\[.*?\\] ", "");
                        member.modifyNickname(nickname).queue();
                    }
                }
                break;
            }
        }
    }

//    public Session getUniqueLinkURL(User user) {
//        return apiClient.v1.createSession(user.getIdLong(), null, user.getName(), true);
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
        Long serverId = member.getGuild().getIdLong();
        ServerSettings settings = serverSettings.get(serverId);
        try {
            AccessStatus verificationStatus = AccessStatus.valueOf(accessStatus.getStatus());
            switch (verificationStatus) {
                case ACCESS_GRANTED_HOME_WORLD:
                    if (accessStatus.getIsPrimary() == null || accessStatus.getIsPrimary()) {
                        //Access is primary and not a music bot
                        addRoleToUserIfNotOwned(member, false, rolesForUser,
                                roleCache.get(settings.getHomeWorldRoleID()),
                                roleCache.get(settings.getDJRoleID()));
                        boolean removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser,
                                roleCache.get(settings.getLinkedWorldRoleID()),
                                roleCache.get(settings.getTempHomeWorldRoleID()),
                                roleCache.get(settings.getTempLinkedWorldRoleID())/*, musicBotRole*/);
                        if (removedAny) {
                            LOGGER.info("Removed roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                        }
                    } else {
                        //Access is granted trough another user and is a music bot
                        addRoleToUserIfNotOwned(member, false, rolesForUser/*, musicBotRole*/);
                        boolean removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser,
                                roleCache.get(settings.getHomeWorldRoleID()),
                                roleCache.get(settings.getLinkedWorldRoleID()),
                                roleCache.get(settings.getTempHomeWorldRoleID()),
                                roleCache.get(settings.getTempLinkedWorldRoleID()));
                        if (removedAny) {
                            LOGGER.info("Removed roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                        }
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
                        addRoleToUserIfNotOwned(member, false, rolesForUser,
                                roleCache.get(settings.getLinkedWorldRoleID()),
                                roleCache.get(settings.getDJRoleID()));
                        boolean removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser,
                                roleCache.get(settings.getHomeWorldRoleID()),
                                roleCache.get(settings.getTempHomeWorldRoleID()),
                                roleCache.get(settings.getTempLinkedWorldRoleID())/*, musicBotRole*/);
                        if (removedAny) {
                            LOGGER.info("Removed roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                        }
                    } else {
                        //Access is granted trough another user and is a music bot
                        addRoleToUserIfNotOwned(member, false, rolesForUser/*, musicBotRole*/);
                        boolean removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser,
                                roleCache.get(settings.getHomeWorldRoleID()),
                                roleCache.get(settings.getLinkedWorldRoleID()),
                                roleCache.get(settings.getTempHomeWorldRoleID()),
                                roleCache.get(settings.getTempLinkedWorldRoleID()));
                        if (removedAny) {
                            LOGGER.info("Removed roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                        }
                    }
                    break;
                case ACCESS_GRANTED_LINKED_WORLD_TEMPORARY:
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
                    // Do not uncomment this unless you have fixed the issue!!!!
                    // YOU ALREADY DID THIS ONCE!!!!
//                    Role[] rolesForUserArray = new Role[rolesForUser.size()];
//                    removeRoleFromUserIfOwned(member, rolesForUser, (Role[]) rolesForUser.toArray(rolesForUserArray));
                    boolean removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser,
                            roleCache.get(settings.getHomeWorldRoleID()),
                            roleCache.get(settings.getLinkedWorldRoleID()),
                            roleCache.get(settings.getTempHomeWorldRoleID()),
                            roleCache.get(settings.getTempLinkedWorldRoleID()),
                            roleCache.get(settings.getDJRoleID())/*, musicBotRole*/);
                    if (removedAny) {
                        LOGGER.info("Removed roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                    }
                    if (settings.getRolesToRemoveWhenInvalid() != null) {
                        List<Role> additionalRolesList = Arrays.stream(settings.getRolesToRemoveWhenInvalid()).map(roleCache::get).collect(Collectors.toList());
                        removedAny = removeRoleFromUserIfOwned(member, false, rolesForUser, additionalRolesList.toArray(new Role[additionalRolesList.size()]));
                        if (removedAny) {
                            LOGGER.info("Removed additional roles from " + member.getEffectiveName() + " due to " + verificationStatus);
                        }
                    }
                    break;

                case COULD_NOT_CONNECT:
                    break;
            }
        } finally {
            userRefreshingRoles.remove(member.getUser().getIdLong());
        }
        Map<String, Object> accountData = (Map<String, Object>) accessStatus.getAdditionalProperties().get("AccountData");

        //Add account name as display name
        if (settings.isAddAccountName() && member.getNickname() == null) {
            if (accountData != null) {
                String accountName = (String) accountData.get("name");
                if (accountName != null && accountName != "") {
                    String newName = member.getUser().getName() + " - " + accountName;
                    int lenght = newName.length();
                    if (lenght > 32) {
                        LOGGER.info("Name " + newName + " is too long");
                        int nameLength = member.getUser().getName().length();
                        newName = member.getUser().getName().substring(0, nameLength - (lenght - 32) - 1) + " - " + accountName;
                        LOGGER.info("Shorted to " + newName);
                    }
                    member.modifyNickname(newName).queue();
                };
            }
        }

        // add free to play role
        if (accountData != null) {
            settings.getAccessTypeRoles().forEach((accessName, roleID) -> {
                List<String> accessList = (List) accountData.get("access");
                if (accessList != null) {
                    boolean hasAccessType = accessList.stream().anyMatch(a -> accessName.equals(a));
                    if (hasAccessType) {
                        // If a user has been PlayForFree, they will always have the access type, even after purchasing gw2
                        if ("PlayForFree".equals(accessName)) {
                            boolean hasNormalAccess = accessList.stream().anyMatch(a -> "GuildWars2".equals(a));
                            if (hasNormalAccess) {
                                // User is not free to play, but was in the past.
                                removeRoleFromUserIfOwned(member, true, rolesForUser, roleCache.get(roleID));
                                return;
                            }
                        }
                        addRoleToUserIfNotOwned(member, true, rolesForUser, roleCache.get(roleID));
                    }
                }
            });
        }

        rolesForUser.forEach(role -> {
            String guildName = getGuildNameFromRole(role.getName());
            if (guildName != null) {
                try {
                    String guildId = getGuildIdFromName(guildName);
                    if (guildId != null) {
                        if (accountData != null) {
                            List<String> guilds = (List<String>) accountData.get("guilds");
                            if (!guilds.contains(guildId)) {
                                removeGuildRole(member, role);
                                LOGGER.info("Removing " + member.getEffectiveName() + " from guild role: " + role.getName());
                            }
                        } else {
                            removeGuildRole(member, role);
                            LOGGER.info("Removing " + member.getEffectiveName() + " from guild role: " + role.getName());
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DiscordBot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public boolean removeRoleFromUserIfOwned(Member member, boolean silent, List<Role> givenRoles, Role... roles) {
        ServerSettings settings = serverSettings.get(member.getGuild().getId());
        boolean result = false;
        List<Role> rolesToRemove = new ArrayList();
        for (Role role : roles) {
            if (role != null && givenRoles.contains(role)) {
                rolesToRemove.add(role);
                LOGGER.info("Removing " + member.getEffectiveName() + " from " + role.getName());
            }
        }
        if (rolesToRemove.size() > 0) {
            rolesToRemove.forEach(roleToRemove -> {
                member.getGuild().removeRoleFromMember(member, roleToRemove).queue();
            });
            result = true;

            if (!silent) {
                //Send message to user about removed roles
                final StringBuilder rolesStr = new StringBuilder();
                member.getUser().openPrivateChannel().queue((channel) -> {
                    rolesToRemove.forEach((role) -> {
                        if (role != null && !role.getId().equals(settings.getDJRoleID())) {
                            rolesStr.append("\n - ").append(role.getName());
                        }
                    });
                    MessageAction message = channel.sendMessage("You have been removed from the following roles on Discord server \"" + member.getGuild().getName() + "\"" + rolesStr.toString());
                    message.submit();
                });
            }
        }
        return result;
    }

    public boolean addRoleToUserIfNotOwned(Member member, boolean silent, List<Role> givenRoles, Role... roles) {
        final ServerSettings settings = serverSettings.get(member.getGuild().getIdLong());
        boolean result = false;
        List<Role> rolesToAssign = new ArrayList();
        for (Role role : roles) {
            if (role != null && !givenRoles.contains(role)) {
                rolesToAssign.add(role);
                LOGGER.info("Adding " + member.getEffectiveName() + " to " + role.getName());
            }
        }
        if (rolesToAssign.size() > 0) {
            rolesToAssign.forEach(roleToAssign -> {
                member.getGuild().addRoleToMember(member, roleToAssign).queue();
            });
            result = true;
            if (!silent) {
                //Send message to user about added roles
                final StringBuilder rolesStr = new StringBuilder();
                member.getUser().openPrivateChannel().queue((channel) -> {
                    rolesToAssign.forEach((role) -> {
                        if (!role.getId().equals(settings.getDJRoleID())) {
                            rolesStr.append("\n - ").append(role.getName());
                        }
                    });
                    if (rolesStr.length() != 0) {
                        MessageAction message = channel.sendMessage("You have been added to the following roles on Discord server \"" + member.getGuild().getName() + "\"" + rolesStr.toString());
                        message.submit();
                    }
                });
            }
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
            ServerSettings settings = serverSettings.get(guild.getIdLong());
            // Member is never null, as it only checks for mutual servers
            for (Role role : guild.getMember(user).getRoles()) {
                if (role.getId().equals(settings.getTempHomeWorldRoleID())) {
                    //if(!shadowMode){
                    grantTemporary(user.getId(), user.getName(), AccessType.HOME_WORLD.name());
                    //}
                    accessGiven = true;
                    LOGGER.info("User " + user.getId() + " has been granted temporary access [Home World]");
                    if (role.getId().equals(settings.getTempLinkedWorldRoleID())) {
                        //if(!shadowMode){
                        grantTemporary(user.getId(), user.getName(), AccessType.LINKED_WORLD.name());
                        //}
                        accessGiven = true;
                        LOGGER.info("User " + user.getId() + " has been granted temporary access [Linked World]");
                    }
                }
            }
        }
        return accessGiven;
    }

    public VerificationStatus getStatus(String userId, String displayName) throws GuildWars2VerificationAPIException {
        StatusGETQueryParam qParams = new StatusGETQueryParam().withDisplayName(displayName);
        StatusGETHeader headers = new StatusGETHeader(getAPIAuthToken());
        VerificationStatus accessStatusData = getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.status.get(qParams, headers).getBody();
        return accessStatusData;
    }

    public long grantTemporary(String userId, String displayName, String accessType) throws GuildWars2VerificationAPIException {
        TemporaryData body = new TemporaryData()
                .withAccessType(accessType).withDisplayName(displayName);
        TemporaryPUTHeader headers = new TemporaryPUTHeader(getAPIAuthToken());
        long expiresIn = getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.temporary.put(body, headers).getBody();
        return expiresIn;
    }

    public void banUser(String userId, long duration, String reason) throws GuildWars2VerificationAPIException {
        BanData body = new BanData().withDuration(duration).withReason(reason);
        BanPUTHeader headers = new BanPUTHeader(getAPIAuthToken());
        getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).ban.put(body, headers).getBody();
    }

    public void setUserProperty(String userId, String name, String value) throws GuildWars2VerificationAPIException {
        PropertiesPUTQueryParam qParams = new PropertiesPUTQueryParam(name, value);
        PropertiesPUTHeader headers = new PropertiesPUTHeader(getAPIAuthToken());
        getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).properties.put(qParams, headers).getBody();
    }

    public VerificationStatus refreshAccess(String userId) throws GuildWars2VerificationAPIException {
        RefreshPOSTHeader headers = new RefreshPOSTHeader(getAPIAuthToken());
        VerificationStatus response = getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).verification.refresh.post(headers).getBody();
        return response;
    }

    public void setAPIKey(String userId, String apikey, boolean primary) throws GuildWars2VerificationAPIException {
        APIKeyData body = new APIKeyData(apikey, primary);
        ApikeyPUTHeader headers = new ApikeyPUTHeader(getAPIAuthToken());
        ApikeyPUTQueryParam qParams = new ApikeyPUTQueryParam(false);
        getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).apikey.put(body, qParams, headers);
    }

    public String getAPIKeyName(String userId) throws GuildWars2VerificationAPIException {
        NameGETHeader headers = new NameGETHeader(getAPIAuthToken());
        APIKeyName response = getAPIClient().v1.users
                .serviceId(SERVICE_ID)
                .serviceUserId(userId).apikey.name.get(headers).getBody();
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

    private void addGuildRole(Member member, String tag, boolean primary) {
        String tagWithBrackets;
        if (primary) {
            tagWithBrackets = "^\\[" + tag + "\\]";
        } else {
            tagWithBrackets = "^\\{" + tag + "\\}";
        }
        Pattern tagMatcher = Pattern.compile(tagWithBrackets, Pattern.CASE_INSENSITIVE);
        Role guildRole = null;
        for (Role role : member.getGuild().getRoles()) {
            if (tagMatcher.matcher(role.getName()).find()) {
                guildRole = role;
                break;
            }
        }
        if (guildRole != null) {
            final String guildRoleName = guildRole.getName();
            final String guildName = guildRoleName.replaceFirst("^(\\[|\\{).*?(\\]|\\}) ", "");
            try {
                String guildId = getGuildIdFromName(guildName);
                VerificationStatus status = getStatus(member.getUser().getId(), member.getEffectiveName());
                Map<String, Object> accountData = (Map<String, Object>) status.getAdditionalProperties().get("AccountData");
                if (accountData != null) {
                    List<String> guilds = (List<String>) accountData.get("guilds");
                    if (!guilds.contains(guildId)) {
                        member.getGuild().removeRoleFromMember(member, guildRole).queue();
                        member.getUser().openPrivateChannel().queue((channel) -> {
                            channel.sendMessage("\"" + accountData.get("name") + "\" is not a member of guild \"" + guildRoleName + "\"").queue();
                        });
                    } else {
                        member.getGuild().addRoleToMember(member, guildRole).queue();
                        if (primary) {
                            // Rename user
                            Pattern p = Pattern.compile("^\\[.*\\]");
                            Matcher roleTagMatch = p.matcher(guildRoleName);
                            if (!roleTagMatch.find()) {
                                throw new RuntimeException();
                            }
                            String roleTag = roleTagMatch.group();
                            Matcher match = p.matcher(member.getEffectiveName());
                            if (match.find()) {
                                // Replace existing tag
                                member.modifyNickname(match.replaceAll(roleTag)).queue();
                            } else {
                                member.modifyNickname(roleTag + " " + member.getEffectiveName()).queue();
                            }

                            // remove other primary roles
                            List<Role> rolesToRemove = new ArrayList();
                            for (Role r : member.getRoles()) {
                                if (!r.getId().equals(guildRole.getId()) && r.getName().matches("\\[.*?\\].*")) {
                                    rolesToRemove.add(r);
                                }
                            }
                            rolesToRemove.forEach(role -> {
                                member.getGuild().removeRoleFromMember(member, role).queue();
                            });

                            member.getUser().openPrivateChannel().queue((channel) -> {
                                channel.sendMessage((primary ? "Primary" : "Secondary") + " guild set to \"" + guildRoleName + "\"").queue();
                            });
                        }
                    }
                } else {
                    member.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("You need to be verified to join guild role \"" + guildRoleName + "\"\nType !verify for more info").queue();
                    });
                }
            } catch (IOException | GuildWars2VerificationAPIException ex) {
                LOGGER.error(ex.getMessage(), ex);
                member.getUser().openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Error while trying to set " + (primary ? "primary" : "secondary") + " guild with tag \"" + tag + "\"").queue();
                });
            }
        } else {
            member.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Could not find any " + (primary ? "primary" : "secondary") + " guild role with tag \"" + tag + "\"").queue();
            });
        }
    }

    private void removeGuildRole(Member member, String tag) {
        Pattern tagMatcher = Pattern.compile("^(\\[|\\{)" + tag + "(\\]|\\})", Pattern.CASE_INSENSITIVE);
        List<Role> guildRoles = new ArrayList();
        for (Role role : member.getRoles()) {
            if (tagMatcher.matcher(role.getName()).find()) {
                guildRoles.add(role);
                break;
            }
        }
        if (guildRoles.size() > 0) {
            guildRoles.forEach(role -> {
                member.getGuild().removeRoleFromMember(member, role).queue();
            });

            if (tagMatcher.matcher(member.getEffectiveName()).find()) {
                String nickname = member.getEffectiveName().replaceFirst("^\\[.*?\\] ", "");
                member.modifyNickname(nickname).queue();
            }

            member.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("You have been removed from  \"" + guildRoles.get(0).getName() + "\"").queue();
            });
        } else {
            member.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("You do not currently have a guild role with tag \"" + tag + "\"").queue();
            });
        }
    }

    private void removeGuildRole(Member member, Role role) {
        member.getGuild().removeRoleFromMember(member, role).queue();

        Pattern p = Pattern.compile("\\[.*\\]");
        Matcher roleTagMatch = p.matcher(role.getName());
        if (roleTagMatch.find()) {
            String roleTag = roleTagMatch.group();
            String nickname = member.getEffectiveName().replaceFirst("^\\[" + roleTag + "\\] ", "");
            member.modifyNickname(nickname).queue();
        }
    }

    public String getGuildNameFromRole(String role) {
        Pattern tagMatcher = Pattern.compile("^(\\[|\\{).*?(\\]|\\}) ", Pattern.CASE_INSENSITIVE);
        Matcher match = tagMatcher.matcher(role);
        if (match.find()) {
            return match.replaceAll("");
        }
        return null;
    }

    public String getGuildNameFromId(String id) throws MalformedURLException, IOException {
        String name = guildIdCache.get(id);
        if (name == null) {
            JSONObject json = new JSONObject(IOUtils.toString(new URL("https://api.guildwars2.com/v1/guild_details.json?guild_id=" + id), Charset.forName("UTF-8")));
            name = json.getString("guild_name");
            if (name != null) {
                guildNameCache.put(name, id);
                guildIdCache.put(id, name);
            }
        }
        return name;
    }

    public String getGuildIdFromName(String name) throws MalformedURLException, IOException {
        String id = guildNameCache.get(name);
        if (id == null) {
            JSONObject json = new JSONObject(IOUtils.toString(new URL("https://api.guildwars2.com/v1/guild_details.json?guild_name=" + name.replaceAll(" ", "%20")), Charset.forName("UTF-8")));
            id = json.getString("guild_id");
            if (id != null) {
                guildNameCache.put(name, id);
                guildIdCache.put(id, name);
            }
        }
        return id;
    }

    public Error getErrorFromGW2VerificationAPIException(GuildWars2VerificationAPIException ex) {
        if (ex.getResponse().hasEntity()) {
            return ex.getResponse().readEntity(Error.class);
        }
        return null;
    }
}
