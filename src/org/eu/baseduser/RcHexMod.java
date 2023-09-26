package org.eu.baseduser;

import arc.*;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
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
import mindustry.world.Edges;
import mindustry.world.Tile;

public class RcHexMod extends Plugin {
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

        Events.on(BlockBuildEndEvent.class, e -> {
            for(TeamData team : Vars.state.teams.active){
                for(Building build : team.buildings){
                    checkStation(build);
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

    public void checkStation(Building build){
        if(build.block != Blocks.container) return;
        Building right = build.nearby(Blocks.container.size, 0);
                    
        if(right != null && build.y == right.y && right.block == Blocks.container && right.team != build.team){
            Building topA, bottomA, topB, bottomB;
            topA = build.nearby(1, 2);
            bottomA = build.nearby(1, -1);
            topB = right.nearby(0, 2);
            bottomB = right.nearby(0, -1);

            if(
                topA != null && (topA.block == Blocks.sorter || topA.block == Blocks.battery) && topA.team == build.team &&
                bottomA != null && (bottomA.block == Blocks.sorter || bottomA.block == Blocks.battery) && bottomA.team == build.team &&
                topB != null && (topB.block == Blocks.sorter || topB.block == Blocks.battery) && topB.team == right.team &&
                bottomB != null && (bottomB.block == Blocks.sorter || bottomB.block == Blocks.battery) && bottomB.team == right.team
            ){
                Call.effect(Fx.coreBuildBlock, build.x, build.y, 0, Pal.accent, build.block);
                Call.effect(Fx.coreBuildBlock, right.x, right.y, 0, Pal.accent, build.block);
                Call.effect(Fx.coreBuildBlock, topA.x, topA.y, 0, Pal.accent, topA.block);
                Call.effect(Fx.coreBuildBlock, bottomA.x, bottomA.y, 0, Pal.accent, bottomA.block);
                Call.effect(Fx.coreBuildBlock, topB.x, topB.y, 0, Pal.accent, topB.block);
                Call.effect(Fx.coreBuildBlock, bottomB.x, bottomB.y, 0, Pal.accent, bottomB.block);
            }
        }
    }
}
