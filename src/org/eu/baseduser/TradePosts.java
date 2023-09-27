package org.eu.baseduser;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.WorldLabel;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.power.Battery.BatteryBuild;

public class TradePosts {
        public Seq<TradePost> posts = new Seq<>();

        public TradePosts(){
            
        }

        public TradePost tradePost(Building container){
            if(isInTradePosts(container) || !isTradePost(container)) return null;

            return new TradePost(container, container.nearby(2, 0));
        }

        public TradePost attemptAddTradePost(Building container){
            TradePost post = tradePost(container);

            if(post != null) {
                posts.add(post);
                Log.info("Trade post added at @, @", post.x(), post.y());
                return post;
            };
            return null;
        }

        public void reset(){
            posts.clear();
        }
        
        public boolean isInTradePosts(Building container){
            return posts.contains(post -> post.leftContainer == container || post.rightContainer == container);
        }

        public static boolean isTradePost(Building container){
            if (container.block != Blocks.container)
                return false;
            Building right = container.nearby(Blocks.container.size, 0);

            if (right != null && right.block == Blocks.container && right.team != container.team) {
                if (TradePosts.hasIndicators(container, right))
                    return true;
            }
            return false;
        }

        public static boolean hasIndicators(Building leftContainer, Building rightContainer){
            if(leftContainer != null && rightContainer != null &&
                    leftContainer.y == rightContainer.y &&
                    leftContainer.block == Blocks.container && rightContainer.block == Blocks.container &&
                    rightContainer.team != leftContainer.team){
                Building topA, bottomA, topB, bottomB;
                topA = leftContainer.nearby(1, 2);
                topB = rightContainer.nearby(0, 2);
                bottomA = leftContainer.nearby(1, -1);
                bottomB = rightContainer.nearby(0, -1);

                return 
                        topA != null && (topA.block == Blocks.sorter || topA.block == Blocks.battery) && topA.team == leftContainer.team &&
                        bottomA != null && (bottomA.block == Blocks.sorter || bottomA.block == Blocks.battery) && bottomA.team == leftContainer.team &&
                        topB != null && (topB.block == Blocks.sorter || topB.block == Blocks.battery) && topB.team == rightContainer.team &&
                        bottomB != null && (bottomB.block == Blocks.sorter || bottomB.block == Blocks.battery) && bottomB.team == rightContainer.team;
            }
            return false;
        }

        public class TradePost {
            Building leftContainer;
            Building rightContainer;

            // TODO: use Item equivalent values on this
            float leftLifetimeTraded;
            float rightLifetimeTraded;

            WorldLabel info;

            public TradePost(Building leftContainer, Building rightContainer){
                this.leftContainer = leftContainer;
                this.rightContainer = rightContainer;

                info = WorldLabel.create();
                info.set(x(), y());
                info.add();

                updateInfo("Trade Post created!");
            }

            public float x(){
                return (leftContainer.x + rightContainer.x) / 2f;
            }

            public float y(){
                return (leftContainer.y + rightContainer.y) / 2f;
            }

            public boolean isValidTradePost(){
                return TradePosts.isTradePost(leftContainer);
            }

            public boolean canTrade(){
                if(leftOutIndicator().block == rightInIndicator().block && leftInIndicator().block == rightOutIndicator().block){
                    boolean leftTrade = true, rightTrade = true;
                    if(leftOutIndicator().block == Blocks.sorter){
                        SorterBuild leftSorter = (SorterBuild) leftOutIndicator(), rightSorter = (SorterBuild) rightInIndicator();

                        rightTrade = leftSorter.sortItem != null && leftSorter.sortItem == rightSorter.sortItem ;
                    }
                    if(leftInIndicator().block == Blocks.sorter){
                        SorterBuild leftSorter = (SorterBuild) leftInIndicator(), rightSorter = (SorterBuild) rightOutIndicator();
                        leftTrade = leftSorter.sortItem != null && leftSorter.sortItem == rightSorter.sortItem;
                    }
                    if(!(leftTrade && rightTrade)){
                        updateInfo("Trade Post:\nPossible misconfiguration?");
                    }
                    return leftTrade && rightTrade;
                }
                return false;
            }
            
            public boolean shouldTrade(){
                if(canTrade()){
                    boolean rightSufficient = true, leftSufficient = true;
                    if(leftOutIndicator().block == Blocks.sorter){
                        SorterBuild sorter = (SorterBuild) leftOutIndicator();
                        rightSufficient = rightContainer.items.has(sorter.sortItem) && leftContainer.acceptItem(leftContainer, sorter.sortItem);
                    } else {
                        // BatteryBuild battery = (BatteryBuild) leftOutIndicator();
                    }
                    
                    if(leftInIndicator().block == Blocks.sorter){
                        
                        SorterBuild sorter = (SorterBuild) leftInIndicator();
                        leftSufficient = leftContainer.items.has(sorter.sortItem) && rightContainer.acceptItem(leftContainer, sorter.sortItem);
                    } else {
                        // BatteryBuild battery = (BatteryBuild) leftOutIndicator();
                    }
                    return rightSufficient && leftSufficient;
                }
                return false;
            }

            public void updateInfo(String msg){
                info.text = msg;
            }

            public Building leftOutIndicator(){
                return leftContainer.nearby(1, 2);
            }

            public Building leftInIndicator(){
                return leftContainer.nearby(1, -1);
            }

            public Building rightInIndicator(){
                return rightContainer.nearby(0, 2);
            }

            public Building rightOutIndicator(){
                return rightContainer.nearby(0, -1);
            }
        }
    }