/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

/**
 *
 * @author venne
 */
public class ServerSettings {

    private final boolean addAccountName;
    private final String HomeWorldRoleID;
    private final String LinkedWorldRoleID;
    private final String TempHomeWorldRoleID;
    private final String TempLinkedWorldRoleID;
    private final String DJRoleID;
    private final String CommanderRoleID;

    public ServerSettings(boolean addAccountName, String HomeWorldRoleID, String LinkedWorldRoleID, String TempHomeWorldRoleID, String TempLinkedWorldRoleID, String DJRoleID, String CommanderRoleID) {
        this.addAccountName = addAccountName;
        this.HomeWorldRoleID = HomeWorldRoleID;
        this.LinkedWorldRoleID = LinkedWorldRoleID;
        this.TempHomeWorldRoleID = TempHomeWorldRoleID;
        this.TempLinkedWorldRoleID = TempLinkedWorldRoleID;
        this.DJRoleID = DJRoleID;
        this.CommanderRoleID = CommanderRoleID;
    }

    public boolean isAddAccountName() {
        return addAccountName;
    }

    public String getHomeWorldRoleID() {
        return HomeWorldRoleID;
    }

    public String getLinkedWorldRoleID() {
        return LinkedWorldRoleID;
    }

    public String getTempHomeWorldRoleID() {
        return TempHomeWorldRoleID;
    }

    public String getTempLinkedWorldRoleID() {
        return TempLinkedWorldRoleID;
    }

    public String getDJRoleID() {
        return DJRoleID;
    }

    public String getCommanderRoleID() {
        return CommanderRoleID;
    }
}
