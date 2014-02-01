/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

// Last year's pathfinder sucked (on purpose). The one this year is good (again on purpose).
// While this can be improved some, it is unlikely to be worth the effort within the 8 hours you have.


package net.windward.Windwardopolis2.AI;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.windward.Windwardopolis2.api.Map;
import net.windward.Windwardopolis2.api.MapSquare;
import net.windward.Windwardopolis2.TRAP;

public class SimpleAStar {

    private static final HashMap<Long, java.util.ArrayList<Point>> cachePaths = new HashMap<Long, java.util.ArrayList<Point>>();

    private static final Point[] offsets = {
            new Point(-1, 0),
            new Point(1, 0),
            new Point(0, -1),
            new Point(0, 1)
    };

    private static final int DEAD_END = 10000;

    private static final Point ptOffMap = new Point(-1, -1);

    public static void Flush() {
        cachePaths.clear();
    }

    /**
     * Calculate a path from start to end. No comments about how this is the world's worst A* implementation. It is purposely
     * simplistic to leave the teams the opportunity to improve greatly upon this. (I was yelled at last year for making the
     * sample A.I.'s too good.)
     *
     * @param map   The game map.
     * @param start The tile units of the start point (inclusive).
     * @param end   The tile units of the end point (inclusive).
     * @return The path from start to end.
     */
    public static java.util.ArrayList<Point> CalculatePath(Map map, Point start, Point end) {

        // should never happen but just to be sure
        if (start == end) {
            return new java.util.ArrayList<Point>(java.util.Arrays.asList(new Point[]{start}));
        }

        Long startEnd = (long)start.getX() | ((long)start.getY() << 16) | ((long)end.getX() << 32) | ((long)end.getY() << 48);
        List<Point> savedPath;
        if(cachePaths.get(startEnd) != null)
            return cachePaths.get(startEnd);

        // nodes are points we have walked to
        java.util.HashMap<Point, TrailPoint> nodes = new java.util.HashMap<Point, TrailPoint>();
        // points we have in a TrailPoint, but not yet evaluated.
        HashMap<Point, TrailPoint> notEvaluated = new HashMap<Point, TrailPoint>();

        TrailPoint tpOn = new TrailPoint(start, end, 0);
        while (true) {
            nodes.put(tpOn.getMapTile(), tpOn);

            // get the neighbors
            TrailPoint tpClosest = null;
            for (Point ptOffset : offsets) {
                Point pt = new Point(tpOn.getMapTile().x + ptOffset.x, tpOn.getMapTile().y + ptOffset.y);
                MapSquare square = map.SquareOrDefault(pt);
                // off the map or not a road/bus stop
                if ((square == null) || (!square.getIsDriveable())) {
                    continue;
                }

                // already evaluated - add it in
                if (nodes.containsKey(pt)) {
                    TrailPoint tpAlreadyEvaluated = nodes.get(pt);
                    //  may have a shorter or longer path back to the start
                    TrailPoint tpRecalc;
                    Point ptIgnore;
                    if(tpAlreadyEvaluated.getCostFromStart() + 1 < tpOn.getCostFromStart()) {
                        tpRecalc = tpOn;
                        ptIgnore = tpAlreadyEvaluated.getMapTile();
                    } else if(tpOn.getCostFromStart() + 1 < tpAlreadyEvaluated.getCostFromStart()) {
                        tpRecalc = tpAlreadyEvaluated;
                        ptIgnore = tpOn.getMapTile();
                    } else {
                        tpRecalc = null;
                        ptIgnore = ptOffMap;
                    }
                    tpOn.setCostFromStart(Math.min(tpOn.getCostFromStart(), tpAlreadyEvaluated.getCostFromStart() + 1));
                    tpAlreadyEvaluated.setCostFromStart(Math.min(tpAlreadyEvaluated.getCostFromStart(), tpOn.getCostFromStart() + 1));
                    ArrayList<TrailPoint> tempNeighbors = tpOn.getNeighbors();
                    tempNeighbors.add(tpAlreadyEvaluated);
                    tpOn.setNeighbors(tempNeighbors);
                    if(tpRecalc != null)
                        tpRecalc.RecalculateDistance(ptIgnore, (map.getWidth() * map.getHeight()) / 4);
                    continue;
                }

                // add this one in
                TrailPoint tpNeighbor = new TrailPoint(pt, end, tpOn.getCostFromStart() + 1);
                tpOn.getNeighbors().add(tpNeighbor);

                // may already be in notEvaluated. If so remove it as this is a more recent cost estimate
/*
                int indTp = -1;

                for (int i = 0; i < notEvaluated.size(); i++) {
                    TrailPoint tp = notEvaluated.get(i);
                    if (tp.getMapTile() == tpNeighbor.getMapTile()) {
                        indTp = i;
                        break;
                    }

                }

                if (indTp != -1)
                    notEvaluated.remove(indTp);
                */
                notEvaluated.remove(tpNeighbor.getMapTile());


                // we only assign to tpClosest if it is closer to the destination. If it's further away, then we
                // use notEvaluated below to find the one closest to the dest that we have not walked yet.
                if (tpClosest == null) {
                    if (tpNeighbor.getCostCompletePath() < tpOn.getCostCompletePath())
                    // new neighbor is closer - work from this next.
                    {
                        tpClosest = tpNeighbor;
                    } else
                    // this is further away - put in the list to try if a better route is not found
                    {
                        notEvaluated.put(tpNeighbor.getMapTile(), tpNeighbor);
                    }
                } else {
                    if (tpClosest.getCostCompletePath() <= tpNeighbor.getCostCompletePath())
                    // this is further away - put in the list to try if a better route is not found
                    {
                        notEvaluated.put(tpNeighbor.getMapTile(), tpNeighbor);
                    } else {
                        // this is closer than tpOn and another neighbor - use it next.
                        notEvaluated.put(tpClosest.getMapTile(), tpClosest);
                        tpClosest = tpNeighbor;
                    }
                }
            }

            // re-calc based on neighbors
            tpOn.RecalculateDistance(ptOffMap, map.getWidth() + map.getHeight());

            // if no closest, then get from notEvaluated. This is where it guarantees that we are getting the shortest
            // route - we go in here if the above did not move a step closer. This may not either as the best choice
            // may be the neighbor we didn't go with above - but we drop into this to find the closest based on what we know.
            if (tpClosest == null) {
                // We need the closest one as that's how we find the shortest path.
                Iterator<Point> keyIt = notEvaluated.keySet().iterator();
                tpClosest = null;
                while(keyIt.hasNext()) {
                    Point key = keyIt.next();
                    if(tpClosest == null) {
                        tpClosest = notEvaluated.get(key);
                    } else {
                        if(notEvaluated.get(key).getCostCompletePath() < tpClosest.getCostCompletePath())
                            tpClosest = notEvaluated.get(key);
                    }
                }
                // if nothing left to check - should never happen.
                if (tpClosest == null)
                    break;
                notEvaluated.remove(tpClosest.getMapTile());
            }

            // if we're at end - we're done!
            if (tpClosest.getMapTile() == end) {
                tpClosest.getNeighbors().add(tpOn);
                nodes.put(tpClosest.getMapTile(), tpClosest);
                break;
            }

            // try this one
            tpOn = tpClosest;
        }

        ArrayList<Point> path = new ArrayList<Point>();
        if(!nodes.containsKey(end))
            return path;

        // Create the return path - from end back to beginning.
        tpOn = nodes.get(end);
        path.add(tpOn.getMapTile());
        while (tpOn.getMapTile() != start) {
            java.util.ArrayList<TrailPoint> neighbors = tpOn.getNeighbors();
            int cost = tpOn.getCostFromStart();

            tpOn = tpOn.getNeighbors().get(0);
            for (int ind = 1; ind < neighbors.size(); ind++) {
                if (neighbors.get(ind).getCostFromStart() < tpOn.getCostFromStart()) {
                    tpOn = neighbors.get(ind);
                }
            }

            // we didn't get to the start.
            if (tpOn.getCostFromStart() >= cost) {
                TRAP.trap();
                return path;
            }
            path.add(0, tpOn.getMapTile());
        }
        cachePaths.put(startEnd, path);
        return path;
    }

    //  TODO TrailPoint is done

    private static class TrailPoint {
        /**
         * The Map tile for this point in the trail.
         */
        private Point privateMapTile;

        public final Point getMapTile() {
            return privateMapTile;
        }

        private void setMapTile(Point value) {
            privateMapTile = value;
        }

        /**
         * The neighboring tiles (up to 4). If 0 then this point has been added as a neighbor to another tile but
         * is in the notEvaluated List because it has not yet been tried and therefore does not have its neighbors set.
         */
        private java.util.ArrayList<TrailPoint> privateNeighbors;

        public final java.util.ArrayList<TrailPoint> getNeighbors() {
            return privateNeighbors;
        }

        private void setNeighbors(java.util.ArrayList<TrailPoint> value) {
            privateNeighbors = value;
        }

        /**
         * Estimate of the distance to the end. Direct line if have no neighbors. Best neighbor.Distance + 1
         * if have neighbors. This value is bad if it's along a trail that failed.
         */
        private int privateCostToEnd;

        public final int getCostToEnd() {
            return privateCostToEnd;
        }

        private void setCostToEnd(int value) {
            privateCostToEnd = value;
        }

        /**
         * The number of steps from the start to this tile.
         */
        private int privateCostFromStart;

        public final int getCostFromStart() {
            return privateCostFromStart;
        }

        /**
         * The cost from beginning to end if using this tile in the final path.
         */
        public final int getCostCompletePath() { return privateCostFromStart + privateCostToEnd; }


        public final void setCostFromStart(int value) {
            privateCostFromStart = value;
        }

        public TrailPoint(Point pt, Point end, int costFromStart) {
            setMapTile(pt);
            setNeighbors(new java.util.ArrayList<TrailPoint>());
            setCostToEnd(Math.abs(getMapTile().x - end.x) + Math.abs(getMapTile().y - end.y));
            setCostFromStart(costFromStart);
        }

        /**
         * Recalculate the CostFromStart. We check our (new) neighbors as it may be faster through us.
         * @param ptIgnore - Do not recalculate this map point (where we started this).
         * @param remainingSteps - Stop infinite recursion
         */
        public void RecalculateCostFromStart(Point ptIgnore, int remainingSteps) {
            // if we're at the start, we're done.
            if (privateCostFromStart == 0)
                return;
            // if gone too far, no more recalculate
            if (remainingSteps-- < 0)
                return;

            //  Need to update our neighbors - except the one that called us. They have a valid value, but
            // it may now be faster to get there from the start via the point we're on
            for (TrailPoint neighborOn : privateNeighbors)
            {
                if(neighborOn.getMapTile() == ptIgnore)
                    continue;

                if (neighborOn.getCostFromStart() <= privateCostFromStart + 1)
                    continue;
                neighborOn.setCostFromStart(privateCostFromStart + 1);
                neighborOn.RecalculateCostFromStart(privateMapTile, remainingSteps);
            }
        }

        public final void RecalculateDistance(Point mapTileCaller, int remainingSteps) {

            // if no neighbors then this is in notEvaluated and so can't recalculate.
            if (getNeighbors().isEmpty()) {
                return;
            }

            // if just 1 neighbor, then it's a dead end
            if (getNeighbors().size() == 1) {
                privateCostToEnd = DEAD_END;
                return;
            }

            int shortestDistance = Integer.MAX_VALUE;
            for(TrailPoint tpOn : privateNeighbors) {
                if(tpOn.getCostToEnd() < shortestDistance)
                    shortestDistance = tpOn.getCostToEnd();
            }
            if(shortestDistance != DEAD_END)
                shortestDistance++;

            // no change, no need to recalc neighbors
            if (shortestDistance == getCostToEnd() || privateCostFromStart == 0) {
                return;
            }

            // new value (could be longer or shorter)
            setCostToEnd(shortestDistance);

            // if gone too far, no more recalculate
            if (remainingSteps-- < 0) {
                return;
            }

            //  Need to tell our neighbors - except the one that called us
            for (TrailPoint neighborOn : getNeighbors()) {
                if (neighborOn.getMapTile() != mapTileCaller)
                    neighborOn.RecalculateDistance(getMapTile(), remainingSteps);
            }

            // and we re-calc again because that could have changed our neighbor's values
            for (TrailPoint neighborOn : getNeighbors()) {
                if (shortestDistance > neighborOn.getCostToEnd())
                    shortestDistance = neighborOn.getCostToEnd();
            }
            // it's 1+ lowest neighbor value (unless a dead end)
            if (shortestDistance != DEAD_END) {
                shortestDistance++;
            }
            setCostToEnd(shortestDistance);
        }

        @Override
        public String toString() {
            return String.format("Map:%1$s, Cost:%2$s+%3$s=%4$s, Neighbors:%5$s", getMapTile(), getCostFromStart(), getCostToEnd(), getCostCompletePath(), getNeighbors().size());
        }
    }
}