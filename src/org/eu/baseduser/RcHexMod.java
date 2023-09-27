package org.eu.baseduser;

import org.eu.baseduser.TradePosts.TradePost;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;

public class RcHexMod extends Plugin {
    public TradePosts tradePosts = new TradePosts();

    public static String configPrefix = "rchex-";

    public enum Config{
        allowSpectate("Whether to allow spectating.", true),
        joinCoreScaling("Whether to scale purchasing cost with target team size. Defaults to true.", true),
        joinCost("The resource value needed in order to purchase a team. Default is 25000.", 25000.0f);

        public static final Config[] all = values();

        public final Object defaultValue;
        public String description;

        Config(String description, Object value){
            this.description = description;
            this.defaultValue = value;
            if(!Core.settings.has(getName())) set(value);
        }
        public String getName(){
            return configPrefix + name();
        }
        public float f(){
            return Core.settings.getFloat(getName(), (float)defaultValue);
        }
        public boolean b(){
            return Core.settings.getBool(getName(), (boolean)defaultValue);
        }
        public String s(){
            return Core.settings.get(getName(), defaultValue).toString();
        }
        public void set(Object value){
            Core.settings.put(getName(), value);
        }
    }

    public ObjectFloatMap<Item> values = new ObjectFloatMap<Item>();

    public Seq<PlayerTeam> playerTeams = new Seq<PlayerTeam>();
    public ObjectMap<Team, PlayerTeam> teamDataMap = new ObjectMap<>();
	public ObjectMap<String, Team> playerTeamMap = new ObjectMap<>();
    public boolean randomTeamAssign = true;
  
    public Seq<Player> adminDebuggers = new Seq<Player>(); // + the console user
    
    public RcHexMod() {
    }
    
    public void init(){
        Log.info("Loading routerchain hex mod.");

        Events.on(PlayerJoin.class, e -> {
            String msg = "Use /help for a list of commands.";
            if(randomTeamAssign){
                Team t = getRandTeam();
                if (t == Team.derelict) {
                    msg += "\n\n[stat]There are no available teams[], you were assigned as a spectator.";
                } else if (t.core() != null) {
                    Call.setCameraPosition(e.player.con, t.core().x, t.core().x);
                    e.player.team(t);
                    PlayerTeam pTeam = playerTeams.find(pt -> pt.team == t);
                    if (pTeam == null) {
                        adminDebug("Creating new player team team#" + String.valueOf(t.id) + " for player [#" + e.player.color.toString() + "]" + e.player.name);
                        playerTeams.add(new PlayerTeam(e.player));
                    } else {
                        pTeam.addPlayer(e.player);
                        
                    }
                }
            }
        });
        values.put(Items.copper, 1.0f);
        values.put(Items.lead, 1.0f);
        values.put(Items.metaglass, 3.0f);
        values.put(Items.graphite, 3.0f);
        values.put(Items.sand, 0.5f);
        values.put(Items.coal, 2.0f);
        values.put(Items.titanium, 4.0f);
        values.put(Items.thorium, 8.0f);
        values.put(Items.scrap, 1.5f);
        values.put(Items.silicon, 5.0f);
        values.put(Items.plastanium, 10.0f);
        values.put(Items.phaseFabric, 50.0f);
        values.put(Items.surgeAlloy, 35.0f);
        values.put(Items.blastCompound, 30.0f);
        values.put(Items.pyratite, 15.0f);
        values.put(Items.beryllium, 2.0f);
        values.put(Items.tungsten, 7.5f);
        values.put(Items.oxide, 5.0f);
        values.put(Items.carbide, 40.0f);
        values.put(Items.fissileMatter, 25.0f);
        values.put(Items.dormantCyst, 20.0f);

        Events.run(Trigger.update, () -> {
            tradePosts.posts.each(post -> {
                if(post.shouldTrade()){
                    if(post.leftOutIndicator().block == Blocks.sorter){
                        SorterBuild sorter = (SorterBuild) post.leftOutIndicator();
                        post.leftContainer.items.add(sorter.sortItem, 1);
                        post.rightContainer.items.remove(sorter.sortItem, 1);
                    }
                    
                    if(post.leftInIndicator().block == Blocks.sorter){
                        SorterBuild sorter = (SorterBuild) post.leftInIndicator();
                        post.rightContainer.items.add(sorter.sortItem, 1);
                        post.leftContainer.items.remove(sorter.sortItem, 1);
                    }

                    post.updateInfo(Strings.format("Trade status:\nLeft Traded @, Right Traded @", ++post.leftLifetimeTraded, post.rightLifetimeTraded++));
                }
            });
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            for(TeamData team : Vars.state.teams.active){
                for(Building build : team.buildings){
                    TradePost post = tradePosts.attemptAddTradePost(build);
                    if(post != null){
                        // Call.effect(Fx.explosion, post.x(), post.y(), 10, Pal.orangeSpark);
                    }
                }
            }
        });
    }
  
    public Team getRandTeam() {
        Seq<TeamData> activeTeams = Vars.state.teams.active.copy();
        for (PlayerTeam pt : playerTeams) {
            if (pt.locked)
                activeTeams.remove(t -> t.team == pt.team);
        }
        if (activeTeams.size == 0)
            return Team.derelict;
        return activeTeams.random().team;
    }
  
  	public Team getTeam(String uuid) {
    	Player p = Groups.player.find(pl -> pl.uuid() == uuid);
      	if (p != null) {
        	playerTeamMap.put(uuid, p.team());
          	return p.team();
        } else {
        	return playerTeamMap.get(uuid, Team.derelict);
        }
    }

	public enum VoteType {
        none,
    	elect, // elect new leader, if disconnected but not timed out (?)
        join   // join another team, internal vote - if passed without the leader online, runs /join on the team's behalf
    }

    public class PlayerTeam {
        public Team team = Team.derelict;
        public boolean locked = false;

      	public VoteType currentVote = VoteType.none;
        public ObjectMap<Player, Boolean> votes;
    	public Seq<String> bannedPlayers = new Seq<String>(); // for kicks, you can unban with a command
        protected Seq<String> joinedPlayers = new Seq<String>(); // you're not supposed to access this to not break things, use getters/setters

        public PlayerTeam(Player p) {
            this.team = p.team();
            this.locked = false;
            addPlayer(p);
        }
      
      	public Player getLeader() {
        	Seq<Player> teamPlayers = getTeamPlayers(team);
        	while (joinedPlayers.size > 0) {
              	Player p = teamPlayers.find(tp -> tp.uuid() == joinedPlayers.get(0));
            	if (p != null) {
                	return p;
                }
            }
            return null;
        }

      	public void addPlayer(Player p) {
        	if (!joinedPlayers.contains(p.uuid())) {
            	joinedPlayers.add(p.uuid());
            }
        }
    }

	public Seq<Player> getTeamPlayers(Team t) {
    	Seq<Player> teamPlayers = new Seq<>();
		Groups.player.each(p -> p.team() == t, p -> teamPlayers.add(p));
      	return teamPlayers;
    }

    public void adminDebug(String msg) {
        adminDebuggers.each(p->p.sendMessage("[scarlet](ADMIN DEBUG)[] " + msg));
        Log.info(msg);
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("rchexconfig", "[name] [value]", "Configure routerchain hex plugin settings. Run with no arguments to list values.", args -> {
            if(args.length == 0){
                Log.info("All config values:");
                for(Config c : Config.all){
                    Log.info("&lk| @: @", c.name(), "&lc&fi" + c.s());
                    Log.info("&lk| | &lw" + c.description);
                    Log.info("&lk|");
                }
                return;
            }
            try{
                Config c = Config.valueOf(args[0]);
                if(args.length == 1){
                    Log.info("'@' is currently @.", c.name(), c.s());
                }else{
                    if(args[1].equals("default")){
                        c.set(c.defaultValue);
                    }else{
                        try{
                            if(c.defaultValue instanceof Float){
                                c.set(Float.parseFloat(args[1]));
                            }else{
                                c.set(Boolean.parseBoolean(args[1]));
                            }
                        }catch(NumberFormatException e){
                            Log.err("Not a valid number: @", args[1]);
                            return;
                        }
                    }
                    Log.info("@ set to @.", c.name(), c.s());
                    Core.settings.forceSave();
                }
            }catch(IllegalArgumentException e){
                Log.err("Unknown config: '@'. Run the command with no arguments to get a list of valid configs.", args[0]);
            }
        });
    }
}
