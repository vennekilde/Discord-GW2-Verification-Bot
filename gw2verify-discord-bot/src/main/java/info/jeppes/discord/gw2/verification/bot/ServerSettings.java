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

    private String HomeWorldRoleID;
    private String LinkedWorldRoleID;
    private String TempHomeWorldRoleID;
    private String TempLinkedWorldRoleID;
    private String DJRoleID;
    private String CommanderRoleID;

    public ServerSettings(String HomeWorldRoleID, String LinkedWorldRoleID, String TempHomeWorldRoleID, String TempLinkedWorldRoleID, String DJRoleID, String CommanderRoleID) {
        this.HomeWorldRoleID = HomeWorldRoleID;
        this.LinkedWorldRoleID = LinkedWorldRoleID;
        this.TempHomeWorldRoleID = TempHomeWorldRoleID;
        this.TempLinkedWorldRoleID = TempLinkedWorldRoleID;
        this.DJRoleID = DJRoleID;
        this.CommanderRoleID = CommanderRoleID;
    }

    public String getHomeWorldRoleID() {
        return HomeWorldRoleID;
    }

    public void setHomeWorldRoleID(String HomeWorldRoleID) {
        this.HomeWorldRoleID = HomeWorldRoleID;
    }

    public String getLinkedWorldRoleID() {
        return LinkedWorldRoleID;
    }

    public void setLinkedWorldRoleID(String LinkedWorldRoleID) {
        this.LinkedWorldRoleID = LinkedWorldRoleID;
    }

    public String getTempHomeWorldRoleID() {
        return TempHomeWorldRoleID;
    }

    public void setTempHomeWorldRoleID(String TempHomeWorldRoleID) {
        this.TempHomeWorldRoleID = TempHomeWorldRoleID;
    }

    public String getTempLinkedWorldRoleID() {
        return TempLinkedWorldRoleID;
    }

    public void setTempLinkedWorldRoleID(String TempLinkedWorldRoleID) {
        this.TempLinkedWorldRoleID = TempLinkedWorldRoleID;
    }

    public String getDJRoleID() {
        return DJRoleID;
    }

    public void setDJRoleID(String DJRoleID) {
        this.DJRoleID = DJRoleID;
    }

    public String getCommanderRoleID() {
        return CommanderRoleID;
    }

    public void setCommanderRoleID(String CommanderRoleID) {
        this.CommanderRoleID = CommanderRoleID;
    }

}
