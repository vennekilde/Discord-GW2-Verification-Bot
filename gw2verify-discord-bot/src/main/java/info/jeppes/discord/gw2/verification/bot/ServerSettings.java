/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot;

import java.util.HashMap;
import java.util.Map;

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
    private final String[] RolesToRemoveWhenInvalid;
    private final Map<String, String> AccessTypeRoles;

    public ServerSettings(boolean addAccountName, String HomeWorldRoleID, String LinkedWorldRoleID, String TempHomeWorldRoleID, String TempLinkedWorldRoleID, String DJRoleID, String CommanderRoleID, String[] RolesToRemoveWhenInvalid) {
        this.addAccountName = addAccountName;
        this.HomeWorldRoleID = HomeWorldRoleID;
        this.LinkedWorldRoleID = LinkedWorldRoleID;
        this.TempHomeWorldRoleID = TempHomeWorldRoleID;
        this.TempLinkedWorldRoleID = TempLinkedWorldRoleID;
        this.DJRoleID = DJRoleID;
        this.CommanderRoleID = CommanderRoleID;
        this.RolesToRemoveWhenInvalid = RolesToRemoveWhenInvalid;
        this.AccessTypeRoles = new HashMap();
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

    public String[] getRolesToRemoveWhenInvalid() {
        return RolesToRemoveWhenInvalid;
    }

    public Map<String, String> getAccessTypeRoles() {
        return AccessTypeRoles;
    }
}
