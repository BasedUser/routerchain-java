package org.eu.baseduser;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Groups;

public class TradePosts {
        public Seq<TradePost> posts = new Seq<>();

        public TradePosts(){
            
        }

        public void update(){

        }

        public TradePost tradePost(Building container){
            if(!isTradePost(container) && isInTradePosts(container)) return null;

            return new TradePost(){{
                this.leftContainer = container;
                rightContainer = container.nearby(Blocks.container.size, 0);
            }};
        }

        public TradePost attemptAddTradePost(Building container){
            TradePost post = tradePost(container);

            if(post != null) {
                posts.add(post);
                Log.info("Trade post added at {}", post.x(), post.y());
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
            if(leftContainer != null && rightContainer != null && leftContainer.y == rightContainer.y && leftContainer.block == Blocks.container && rightContainer.block == Blocks.container && rightContainer.team != leftContainer.team){

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
            float leftLifetimeTraded;
            float rightLifetimeTraded;

            public float x(){
                return (leftContainer.x + rightContainer.x) / 2f;
            }

            public float y(){
                return (leftContainer.y + rightContainer.y) / 2f;
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