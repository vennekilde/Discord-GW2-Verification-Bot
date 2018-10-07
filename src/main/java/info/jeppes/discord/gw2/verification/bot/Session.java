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
public class Session {

    private String session;
    private long validTo;

    public Session(String session, long validTo) {
        this.session = session;
        this.validTo = validTo;
    }

    public String getSession() {
        return session;
    }

    public String getSessionURL() {
        return getSessionURL(session);
    }

    public static String getSessionURL(String session) {
        return "https://farshiverpeaks.com/index.php?page=gw2integration&ls-sessions=" + session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public long getValidTo() {
        return validTo;
    }

    public void setValidTo(long validTo) {
        this.validTo = validTo;
    }

}
