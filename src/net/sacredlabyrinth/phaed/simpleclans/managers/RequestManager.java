package net.sacredlabyrinth.phaed.simpleclans.managers;

import net.sacredlabyrinth.phaed.simpleclans.ClanRequest;
import net.sacredlabyrinth.phaed.simpleclans.VoteResult;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.Request;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author phaed
 */
public final class RequestManager
{
    private SimpleClans plugin;
    private HashMap<String, Request> requests = new HashMap<String, Request>();

    /**
     *
     */
    public RequestManager()
    {
        plugin = SimpleClans.getInstance();
        askerTask();
    }

    /**
     * Check whether the clan has a pending request
     * @param tag
     * @return
     */
    public boolean hasRequest(String tag)
    {
        return requests.containsKey(tag);
    }

    /**
     * Add a demotion request
     * @param plugin
     * @param requester
     * @param demotedName
     * @param clan
     */
    public void addDemoteRequest(SimpleClans plugin, ClanPlayer requester, String demotedName, Clan clan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("asking.for.the.demotion"), Helper.capitalize(requester.getName()), demotedName);

        ClanPlayer demotedTp = plugin.getClanManager().getClanPlayer(demotedName.toLowerCase());

        List<ClanPlayer> acceptors = Helper.stripOffLinePlayers(clan.getLeaders());
        acceptors.remove(demotedTp);

        Request req = new Request(plugin, ClanRequest.DEMOTE, acceptors, requester, demotedName, clan, msg);
        requests.put(clan.getTag(), req);
        ask(req);
    }

    /**
     * Add a promotion request
     * @param plugin
     * @param requester
     * @param promotedName
     * @param clan
     */
    public void addPromoteRequest(SimpleClans plugin, ClanPlayer requester, String promotedName, Clan clan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("asking.for.the.promotion"), Helper.capitalize(requester.getName()), promotedName);

        List<ClanPlayer> acceptors = Helper.stripOffLinePlayers(clan.getLeaders());
        acceptors.remove(requester);

        Request req = new Request(plugin, ClanRequest.PROMOTE, acceptors, requester, promotedName, clan, msg);
        requests.put(clan.getTag(), req);
        ask(req);
    }

    /**
     * Add a clan disband request
     * @param plugin
     * @param requester
     * @param clan
     */
    public void addDisbandRequest(SimpleClans plugin, ClanPlayer requester, Clan clan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("asking.for.the.deletion"), Helper.capitalize(requester.getName()));

        List<ClanPlayer> acceptors = Helper.stripOffLinePlayers(clan.getLeaders());
        acceptors.remove(requester);

        Request req = new Request(plugin, ClanRequest.DISBAND, acceptors, requester, null, clan, msg);
        requests.put(clan.getTag(), req);
        ask(req);
    }

    /**
     * Add a member invite request
     * @param plugin
     * @param requester
     * @param invitedName
     * @param clan
     */
    public void addInviteRequest(SimpleClans plugin, ClanPlayer requester, String invitedName, Clan clan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("inviting.you.to.join"), Helper.capitalize(requester.getName()), clan.getName());
        Request req = new Request(plugin, ClanRequest.INVITE, null, requester, invitedName, clan, msg);
        requests.put(invitedName.toLowerCase(), req);
        ask(req);
    }

    /**
     * Add an clan alliance request
     * @param plugin
     * @param requester
     * @param allyClan
     * @param requestingClan
     */
    public void addAllyRequest(SimpleClans plugin, ClanPlayer requester, Clan allyClan, Clan requestingClan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("proposing.an.alliance"), Helper.capitalize(requestingClan.getName()), Helper.stripColors(allyClan.getColorTag()));

        List<ClanPlayer> acceptors = Helper.stripOffLinePlayers(allyClan.getLeaders());
        acceptors.remove(requester);

        Request req = new Request(plugin, ClanRequest.CREATE_ALLY, acceptors, requester, allyClan.getTag(), requestingClan, msg);
        requests.put(allyClan.getTag(), req);
        ask(req);
    }

    /**
     * Add an clan rivalry break request
     * @param plugin
     * @param requester
     * @param rivalClan
     * @param requestingClan
     */
    public void addRivalryBreakRequest(SimpleClans plugin, ClanPlayer requester, Clan rivalClan, Clan requestingClan)
    {
        String msg = MessageFormat.format(plugin.getLang().getString("proposing.to.end.the.rivalry"), Helper.capitalize(requestingClan.getName()), Helper.stripColors(rivalClan.getColorTag()));

        List<ClanPlayer> acceptors = Helper.stripOffLinePlayers(rivalClan.getLeaders());
        acceptors.remove(requester);

        Request req = new Request(plugin, ClanRequest.BREAK_RIVALRY, acceptors, requester, rivalClan.getTag(), requestingClan, msg);
        requests.put(rivalClan.getTag(), req);
        ask(req);
    }

    /**
     * Record one player's accept vote
     * @param cp
     */
    public void accept(ClanPlayer cp)
    {
        Request req = requests.get(cp.getTag());

        if (req != null)
        {
            req.vote(cp.getName(), VoteResult.ACCEPT);
            processResults(req);
        }
        else
        {
            req = requests.get(cp.getCleanName());

            if (req != null)
            {
                processInvite(req, VoteResult.ACCEPT);
            }
        }
    }

    /**
     * Record one player's deny vote
     * @param cp
     */
    public void deny(ClanPlayer cp)
    {
        Request req = requests.get(cp.getTag());

        if (req != null)
        {
            req.vote(cp.getName(), VoteResult.DENY);
            processResults(req);
        }
        else
        {
            req = requests.get(cp.getCleanName());

            if (req != null)
            {
                processInvite(req, VoteResult.DENY);
            }
        }
    }

    /**
     * Process the answer from an invite and add the player to the clan if accepted
     * @param req
     * @param vote
     */
    public void processInvite(Request req, VoteResult vote)
    {
        Clan clan = req.getClan();
        String invited = req.getTarget();

        if (vote.equals(VoteResult.ACCEPT))
        {
            ClanPlayer cp = plugin.getClanManager().getCreateClanPlayer(invited);

            clan.addBb(ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("joined.the.clan"), Helper.capitalize(invited)));
            plugin.getClanManager().serverAnnounce(MessageFormat.format(plugin.getLang().getString("has.joined"), Helper.capitalize(invited), clan.getName()));
            clan.addPlayerToClan(cp);
        }
        else
        {
            clan.leaderAnnounce(plugin.getLang().getString("leaders"), ChatColor.RED + MessageFormat.format(plugin.getLang().getString("membership.invitation"), Helper.capitalize(invited)));
        }

        requests.remove(req.getTarget().toLowerCase());
    }

    /**
     * Check to see if votes are complete and process the result
     * @param req
     */
    public void processResults(Request req)
    {
        if (req.getType().equals(ClanRequest.CREATE_ALLY))
        {
            Clan clan = req.getClan();
            Clan ally = plugin.getClanManager().getClan(req.getTarget());
            ClanPlayer cp = req.getRequester();

            if (ally != null && clan != null)
            {
                List<String> accepts = req.getAccepts();
                List<String> denies = req.getDenies();

                if (!accepts.isEmpty())
                {
                    clan.addAlly(ally);

                    ally.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("accepted.an.alliance"), Helper.capitalize(accepts.get(0)), clan.getName()));
                    clan.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("created.an.alliance"), Helper.capitalize(cp.getName()), Helper.capitalize(ally.getName())));
                }
                else
                {
                    ally.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("denied.an.alliance"), Helper.capitalize(denies.get(0)), clan.getName()));
                    clan.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("the.alliance.was.denied"), Helper.capitalize(ally.getName())));
                }
            }
        }
        else if (req.getType().equals(ClanRequest.BREAK_RIVALRY))
        {
            Clan clan = req.getClan();
            Clan rival = plugin.getClanManager().getClan(req.getTarget());
            ClanPlayer cp = req.getRequester();

            if (rival != null && clan != null)
            {
                List<String> accepts = req.getAccepts();
                List<String> denies = req.getDenies();

                if (!accepts.isEmpty())
                {
                    clan.removeRival(rival);
                    rival.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("broken.the.rivalry"), Helper.capitalize(accepts.get(0)), clan.getName()));
                    clan.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("broken.the.rivalry.with"), Helper.capitalize(cp.getName()), Helper.capitalize(rival.getName())));
                }
                else
                {
                    rival.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("denied.to.make.peace"), Helper.capitalize(denies.get(0)), clan.getName()));
                    clan.addBb(cp.getName(), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("peace.agreement.denied"), Helper.capitalize(rival.getName())));
                }
            }
        }
        else if (req.votingFinished())
        {
            List<String> denies = req.getDenies();

            if (req.getType().equals(ClanRequest.DEMOTE))
            {
                Clan clan = req.getClan();
                String demoted = req.getTarget();

                if (denies.isEmpty())
                {
                    clan.addBb(plugin.getLang().getString("leaders"), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("demoted.back.to.member"), Helper.capitalize(demoted)));
                    clan.demote(demoted);
                }
                else
                {
                    String deniers = Helper.capitalize(Helper.toMessage(Helper.toArray(denies), ", "));
                    clan.leaderAnnounce(plugin.getLang().getString("leaders"), ChatColor.RED + MessageFormat.format(plugin.getLang().getString("denied.demotion"), deniers, demoted));
                }
            }
            else if (req.getType().equals(ClanRequest.PROMOTE))
            {
                Clan clan = req.getClan();
                String promoted = req.getTarget();

                if (denies.isEmpty())
                {
                    clan.addBb(plugin.getLang().getString("leaders"), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("promoted.to.leader"), Helper.capitalize(promoted)));
                    clan.promote(promoted);
                }
                else
                {
                    String deniers = Helper.capitalize(Helper.toMessage(Helper.toArray(denies), ", "));
                    clan.leaderAnnounce(plugin.getLang().getString("leaders"), ChatColor.RED + MessageFormat.format(plugin.getLang().getString("denied.the.promotion"), deniers, promoted));
                }
            }
            else if (req.getType().equals(ClanRequest.DISBAND))
            {
                Clan clan = req.getClan();

                if (denies.isEmpty())
                {
                    clan.addBb(plugin.getLang().getString("leaders"), ChatColor.AQUA + MessageFormat.format(plugin.getLang().getString("has.been.disbanded"), clan.getName()));
                    clan.disband();
                }
                else
                {
                    String deniers = Helper.capitalize(Helper.toMessage(Helper.toArray(denies), ", "));
                    clan.leaderAnnounce(plugin.getLang().getString("leaders"), ChatColor.RED + MessageFormat.format(plugin.getLang().getString("clan.deletion"), deniers));
                }
            }

            req.cleanVotes();
            requests.remove(req.getClan().getTag());
        }
    }

    /**
     * End a pending request prematurely
     * @param playerName
     * @return
     */
    public boolean endPendingRequest(String playerName)
    {
        for (Request req : requests.values())
        {
            for (ClanPlayer cp : req.getAcceptors())
            {
                if (cp.getName().equalsIgnoreCase(playerName))
                {
                    req.getClan().leaderAnnounce(plugin.getLang().getString("leaders"), MessageFormat.format(plugin.getLang().getString("signed.off.request.cancelled"), ChatColor.RED + Helper.capitalize(playerName), req.getType()));
                    requests.remove(req.getClan().getTag());
                    break;
                }
            }
        }

        return false;
    }

    /**
     * Starts the task that asks for the votes of all requests
     */
    public void askerTask()
    {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                for (Request req : requests.values())
                {
                    ask(req);
                }
            }
        }, 0, plugin.getSettingsManager().getRequestFreqencySecs() * 20L);
    }

    /**
     * Asks a request to players for votes
     * @param req
     */
    public void ask(Request req)
    {
        final String tag = plugin.getSettingsManager().getClanChatBracketColor() + plugin.getSettingsManager().getClanChatTagBracketLeft() + plugin.getSettingsManager().getTagDefaultColor() + req.getClan().getColorTag() + plugin.getSettingsManager().getClanChatBracketColor() + plugin.getSettingsManager().getClanChatTagBracketRight();
        final String message = tag + " " + plugin.getSettingsManager().getRequestMessageColor() + req.getMsg();
        final String options = MessageFormat.format(plugin.getLang().getString("accept.or.deny"), ChatBlock.makeEmpty(Helper.stripColors(tag)) + " " + ChatColor.DARK_GREEN + "/" + plugin.getSettingsManager().getCommandAccept() + plugin.getSettingsManager().getPageHeadingsColor(), ChatColor.DARK_RED + "/" + plugin.getSettingsManager().getCommandDeny());

        if (req.getType().equals(ClanRequest.INVITE))
        {
            Player player = plugin.getServer().getPlayer(req.getTarget());

            if (player != null)
            {
                ChatBlock.sendBlank(player);
                ChatBlock.sendMessage(player, message);
                ChatBlock.sendMessage(player, options);
                ChatBlock.sendBlank(player);
            }
        }
        else
        {
            for (ClanPlayer cp : req.getAcceptors())
            {
                if (cp.getVote() == null)
                {
                    Player player = plugin.getServer().getPlayer(cp.getName());

                    if (player != null)
                    {
                        ChatBlock.sendBlank(player);
                        ChatBlock.sendMessage(player, message);
                        ChatBlock.sendMessage(player, options);
                        ChatBlock.sendBlank(player);
                    }
                }
            }
        }
    }
}
