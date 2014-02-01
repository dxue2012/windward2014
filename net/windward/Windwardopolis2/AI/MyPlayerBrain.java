/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.AI;

import net.windward.Windwardopolis2.api.*;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/**
 * The sample C# AI. Start with this project but write your own code as this is a very simplistic implementation of the AI.
 */
public class MyPlayerBrain implements net.windward.Windwardopolis2.AI.IPlayerAI {
    // bugbug - put your team name here.
    private static String NAME = "ICanChooseTheBestPassenger";

    // bugbug - put your school name here. Must be 11 letters or less (ie use MIT, not Massachussets Institute of Technology).
    public static String SCHOOL = "Princeton U.";

    private static Logger log = Logger.getLogger(IPlayerAI.class);

    /**
     * The name of the player.
     */
    private String privateName;

    public final String getName() {
        return privateName;
    }

    private void setName(String value) {
        privateName = value;
    }

    /**
     * The game map.
     */
    private Map privateGameMap;

    public final Map getGameMap() {
        return privateGameMap;
    }

    private void setGameMap(Map value) {
        privateGameMap = value;
    }

    /**
     * All of the players, including myself.
     */
    private java.util.ArrayList<Player> privatePlayers;

    public final java.util.ArrayList<Player> getPlayers() {
        return privatePlayers;
    }

    private void setPlayers(java.util.ArrayList<Player> value) {
        privatePlayers = value;
    }

    /**
     * All of the companies.
     */
    private java.util.ArrayList<Company> privateCompanies;

    public final java.util.ArrayList<Company> getCompanies() {
        return privateCompanies;
    }

    private void setCompanies(java.util.ArrayList<Company> value) {
        privateCompanies = value;
    }

    /**
     * All of the passengers.
     */
    private java.util.ArrayList<Passenger> privatePassengers;

    private Passenger currPassenger;

    private Passenger abandonedPassenger;

    public final java.util.ArrayList<Passenger> getPassengers() {
        return privatePassengers;
    }

    private void setPassengers(java.util.ArrayList<Passenger> value) {
        privatePassengers = value;
    }

    /**
     * All of the coffee stores.
     */
    private java.util.ArrayList<CoffeeStore> privateStores;

    public final ArrayList<CoffeeStore> getCoffeeStores() { return privateStores; }

    private void setCoffeeStores(ArrayList<CoffeeStore> value) { privateStores = value; }

    /**
     * The power up deck
     */
    private ArrayList<PowerUp> privatePowerUpDeck;

    public final ArrayList<PowerUp> getPowerUpDeck() { return privatePowerUpDeck; }

    private void setPowerUpDeck(ArrayList<PowerUp> value) { privatePowerUpDeck = value; }


    /**
     * My power up hand
     */
    private ArrayList<PowerUp> privatePowerUpHand;

    public final ArrayList<PowerUp> getPowerUpHand() { return privatePowerUpHand; }

    private void setPowerUpHand(ArrayList<PowerUp> value) { privatePowerUpHand = value; }

    /**
     * Abandonment Factor
     */
    public final double ABANDONMENT_FACTOR = 2;

    /**
     * Me (my player object).
     */
    private Player privateMe;

    public final Player getMe() {
        return privateMe;
    }

    private void setMe(Player value) {
        privateMe = value;
    }

    /**
     * My current passenger
     */
    private Passenger privateMyPassenger;

    public final Passenger getMyPassenger() { return privateMyPassenger; }

    private void setMyPassenger(Passenger value) { privateMyPassenger = value; }


    private PlayerAIBase.PlayerOrdersEvent sendOrders;

    private PlayerAIBase.PlayerCardEvent playCards;

    private HashSet<Passenger> passengersDelivered;

    /**
     * The maximum number of trips allowed before a refill is required.
     */
    private static final int MAX_TRIPS_BEFORE_REFILL = 3;

    private static final java.util.Random rand = new java.util.Random();

    public MyPlayerBrain(String name) {
        setName(!net.windward.Windwardopolis2.DotNetToJavaStringHelper.isNullOrEmpty(name) ? name : NAME);
        currPassenger = null;
        abandonedPassenger = null;
        privatePowerUpHand = new ArrayList<PowerUp>();
        passengersDelivered = new HashSet<Passenger>();
    }

    /**
     * The avatar of the player. Must be 32 x 32.
     */
    public final byte[] getAvatar() {
        try {
            // open image
            InputStream stream = getClass().getResourceAsStream("/net/windward/Windwardopolis2/res/MyAvatar.png");

            byte [] avatar = new byte[stream.available()];
            stream.read(avatar, 0, avatar.length);
            return avatar;

        } catch (IOException e) {
            System.out.println("error reading image");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Called at the start of the game.
     *
     * @param map         The game map.
     * @param me          You. This is also in the players list.
     * @param players     All players (including you).
     * @param companies   The companies on the map.
     * @param passengers  The passengers that need a lift.
     * @param ordersEvent Method to call to send orders to the server.
     */
    public final void Setup(Map map, Player me, java.util.ArrayList<Player> players, java.util.ArrayList<Company> companies, ArrayList<CoffeeStore> stores,
                            java.util.ArrayList<Passenger> passengers, ArrayList<PowerUp> powerUps, PlayerAIBase.PlayerOrdersEvent ordersEvent, PlayerAIBase.PlayerCardEvent cardEvent) {

        try {
            setGameMap(map);
            setPlayers(players);
            setMe(me);
            setCompanies(companies);
            setPassengers(passengers);
            setCoffeeStores(stores);
            setPowerUpDeck(powerUps);
            sendOrders = ordersEvent;
            playCards = cardEvent;

            java.util.ArrayList<Passenger> pickup = AllPickups(me, passengers);

            // get the path from where we are to the dest.
            java.util.ArrayList<Point> path = CalculatePathPlus1(me, chooseBestPassenger(getAvailablePassengers(pickup)).getLobby().getBusStop());
            sendOrders.invoke("ready", path, pickup);
        } catch (RuntimeException ex) {
            log.fatal("setup(" + me == null ? "NULL" : me.getName() + ") Exception: " + ex.getMessage());
            ex.printStackTrace();

        }
    }

    /**
     * Called to send an update message to this A.I. We do NOT have to send orders in response.
     *
     * @param status     The status message.
     * @param plyrStatus The player this status is about. THIS MAY NOT BE YOU.
     */
    public final void GameStatus(PlayerAIBase.STATUS status, Player plyrStatus) {

        // bugbug - Framework.cs updates the object's in this object's Players, Passengers, and Companies lists. This works fine as long
        // as this app is single threaded. However, if you create worker thread(s) or respond to multiple status messages simultaneously
        // then you need to split these out and synchronize access to the saved list objects.

        try {
            // bugbug - we return if not us because the below code is only for when we need a new path or our limo hit a bus stop.
            // if you want to act on other players arriving at bus stops, you need to remove this. But make sure you use Me, not
            // plyrStatus for the Player you are updatiing (particularly to determine what tile to start your path from).
            if (plyrStatus != getMe() && status != PlayerAIBase.STATUS.POWER_UP_PLAYED) {
                return;
            }

            if(status == PlayerAIBase.STATUS.UPDATE) {
                MaybePlayPowerUp();
                return;
            }

            DisplayStatus(status, plyrStatus);

            if(log.isDebugEnabled())
                log.info("gameStatus( " + status + " )");

            Point ptDest = null;
            java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

            if (getMe().getLimo().getCoffeeServings() <= 0 && getMe().getLimo().getPassenger() == null) {
                ptDest = getCoffeeDest();
            }
            else
            {
                switch (status) {
                    case NO_PATH:
                    case PASSENGER_NO_ACTION:
                        if (getMe().getLimo().getPassenger() == null) {
                            pickup = AllPickups(plyrStatus, getPassengers());
                            ptDest = chooseBestPassenger(getAvailablePassengers(pickup)).getLobby().getBusStop();
                        } else {
                            ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                        }
                        break;
                    case PASSENGER_DELIVERED:
                        currPassenger = null;
                        abandonedPassenger = null;
                        break;
                    case PASSENGER_ABANDONED:
                        abandonedPassenger = currPassenger;
                        currPassenger = null;
                        System.out.println("abandoned passenger is " + abandonedPassenger);
                        /*
                        if (log.isInfoEnabled())
                            log.info(msg);
                            */

                        pickup = AllPickups(getMe(), getPassengers(), abandonedPassenger);
                        ptDest = chooseBestPassenger(getAvailablePassengers(pickup)).getLobby().getBusStop();
                        break;
                    case PASSENGER_REFUSED_ENEMY:
                        abandonedPassenger = currPassenger;
                        System.out.println("refused passenger is " + abandonedPassenger);

                        // override algorithm for choosing the company to abandon the passenger
                        java.util.List<Company> comps = getCompanies();
                        Company abandonCompany = chooseNearestCompanyWithoutEnemy(comps, getMyPassenger());
                        ptDest = abandonCompany.getBusStop();
                        break;
                    case PASSENGER_DELIVERED_AND_PICKED_UP:
                        currPassenger = getMyPassenger();
                        abandonedPassenger = null;
                        ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                        break;
                    case PASSENGER_PICKED_UP:
                        currPassenger = getMyPassenger();
                        abandonedPassenger = null;

                        //pickup = AllPickups(getMe(), getPassengers());
                        ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
                        break;
                    case COFFEE_STORE_CAR_RESTOCKED:
                        pickup = AllPickups(getMe(), getPassengers());
                        ptDest = chooseBestPassenger(getAvailablePassengers(pickup)).getLobby().getBusStop();
                        break;
                }

                if (plyrStatus != getMe() && status == PlayerAIBase.STATUS.POWER_UP_PLAYED)
                {
                    if (getMyPassenger() != null)
                    {
                        double abandon = evaluateAbandonment(getMyPassenger());
                        double cont = evaluateCurrentDelivery(getMyPassenger());
                        if (cont < (abandon * ABANDONMENT_FACTOR))
                        {
                            java.util.List<Company> comps = getCompanies();
                            ptDest = chooseNearestCompanyWithoutEnemy(comps, getMyPassenger()).getBusStop();
                        }
                    }
                    else {
                        // we don't have a passenger
                        pickup = AllPickups(getMe(), getPassengers());
                        ptDest = chooseBestPassenger(getAvailablePassengers(pickup)).getLobby().getBusStop();
                    }
                }

                // may be another status
                if(ptDest == null)
                    return;
            }

            DisplayOrders(ptDest);

            // get the path from where we are to the dest.
            java.util.ArrayList<Point> path = CalculatePathPlus1(getMe(), ptDest);

            if (log.isDebugEnabled())
            {
                log.debug(status + "; Path:" + (path.size() > 0 ? path.get(0).toString() : "{n/a}") + "-" + (path.size() > 0 ? path.get(path.size()-1).toString() : "{n/a}") + ", " + path.size() + " steps; Pickup:" + (pickup.size() == 0 ? "{none}" : chooseBestPassenger(getAvailablePassengers(pickup)).getName()) + ", " + pickup.size() + " total");
            }

            // update our saved Player to match new settings
            if (path.size() > 0) {
                getMe().getLimo().getPath().clear();
                getMe().getLimo().getPath().addAll(path);
            }
            if (pickup.size() > 0) {
                getMe().getPickUp().clear();
                getMe().getPickUp().addAll(pickup);
            }

            sendOrders.invoke("move", path, pickup);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    private Point getCoffeeDest() {
        java.util.List<CoffeeStore> cof = getCoffeeStores();
        return Collections.min(cof, new Comparator<CoffeeStore>() {
            public int compare (CoffeeStore a, CoffeeStore b) {
                Integer distA = SimpleAStar.CalculatePath(getGameMap(), getMe().getLimo().getMapPosition(), a.getBusStop()).size();
                Integer distB = SimpleAStar.CalculatePath(getGameMap(), getMe().getLimo().getMapPosition(), b.getBusStop()).size();
                return Integer.compare(distA, distB);
            }
        }).getBusStop();
    }


    private void MaybePlayPowerUp() {
        if ((getPowerUpHand().size() != 0) && (rand.nextInt(50) < 30))
            return;
        // not enough, draw
        if (getPowerUpHand().size() < getMe().getMaxCardsInHand() && getPowerUpDeck().size() > 0)
        {
            for (int index = 0; index < getMe().getMaxCardsInHand() - getPowerUpHand().size() && getPowerUpDeck().size() > 0; index++)
            {
                // select a card
                PowerUp pu = getPowerUpDeck().get(0);
                privatePowerUpDeck.remove(pu);
                privatePowerUpHand.add(pu);
                playCards.invoke(PlayerAIBase.CARD_ACTION.DRAW, pu);
            }
            return;
        }

        // which cards can we play?
        ArrayList<PowerUp> canPlay = new ArrayList<PowerUp>();
        for(PowerUp current : getPowerUpHand()) {
            //System.out.println(current.getCard().name());
            // always discard MULT_DELIVERY QUANT_SPEED
            if(current.getCard() == PowerUp.CARD.MULT_DELIVERY_QUARTER_SPEED) playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, current);
            else if(current.isOkToPlay()) canPlay.add(current);
        }

        if (canPlay.isEmpty()) {
            // discard cards
            if (Math.random() < .66) {
                if (!getPowerUpHand().isEmpty()) System.out.println("Discarding");
                for (PowerUp current : getPowerUpHand()) {
                    if (current.getCard().equals(PowerUp.CARD.MULT_DELIVER_AT_COMPANY) || current.getCard().equals(PowerUp.CARD.MULT_DELIVERING_PASSENGER)) {
                        System.out.println("Discarded");
                        playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, current);
                        privatePowerUpHand.remove(current);
                        break;
                    }
                    else if (Math.random() < .43) {
                        System.out.println("Discarded");
                        playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, current);
                        privatePowerUpHand.remove(current);
                        break;
                    }
                }
            }
            return;
        }

        // Evaluate value of cards
        float highValue = 0;
        PowerUp toPlay = null;
        Player[] playOn = new Player[1];
        for (PowerUp pu : canPlay) {
            if (evaluatePowerUp(pu, playOn) > highValue) {
                highValue = evaluatePowerUp(pu,playOn);
                toPlay = pu;
            }
        }
        if (toPlay == null)  {
            // discard cards
            if (Math.random() < .2) {
                if (!getPowerUpHand().isEmpty()) System.out.println("Discarding");
                for (PowerUp current : getPowerUpHand()) {
                    if (current.getCard().equals(PowerUp.CARD.MULT_DELIVER_AT_COMPANY) || current.getCard().equals(PowerUp.CARD.MULT_DELIVERING_PASSENGER)) {
                        System.out.println("Discarded");
                        playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, current);
                        privatePowerUpHand.remove(current);
                        break;
                    }
                    else if (Math.random() < .4) {
                        System.out.println("Discarded");
                        playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, current);
                        privatePowerUpHand.remove(current);
                        break;
                    }
                }
            }
            return;
        }

        if (toPlay.getCard() == PowerUp.CARD.MOVE_PASSENGER) {
            // Passenger toUseCardOn = playOn[0];
            if (highValue == 10) {
                for (Passenger CEO : getPassengers()) {
                    if (CEO.getPointsDelivered() == 3) toPlay.setPassenger(CEO);
                }
            } else {
                java.util.List<Passenger> overlap = getMyPassenger().getEnemies();
                overlap.retainAll(getMyPassenger().getDestination().getPassengers());
                toPlay.setPassenger(overlap.get(0));
            }
        }
        if (toPlay.getCard() == PowerUp.CARD.CHANGE_DESTINATION || toPlay.getCard() == PowerUp.CARD.STOP_CAR) {
            toPlay.setPlayer(playOn[0]);
        }
        if (log.isInfoEnabled())
            log.info("Request play card " + toPlay);
        System.out.println("Request play card " + toPlay.getCard().name());
        playCards.invoke(PlayerAIBase.CARD_ACTION.PLAY, toPlay);

        privatePowerUpHand.remove(toPlay);
    }

    private float evaluatePowerUp(PowerUp pu, Player[] playOn) {
        float score = 0;
        float hiscore = 0;
        Player high = getMe();
        for (Player player : getPlayers()) {
            float playerScore = player.getScore();
            if (playerScore > hiscore && player != getMe()) {
                hiscore = playerScore;
                high = player;
            }
        }
        Passenger nextPsngr = getMe().getPickUp().get(0);

        switch (pu.getCard()) {
            case MOVE_PASSENGER:
                // if we're not about to pick up the 3pt CEO, move her
                if (nextPsngr.getPointsDelivered() != 3) {
                    score = 10;
                    if (getMyPassenger() != null && getMyPassenger().getPointsDelivered() == 3) {
                        // +100 if one of her enemies is there ...
                        java.util.List<Passenger> overlaps = getMyPassenger().getDestination().getPassengers();
                        overlaps.retainAll(getMyPassenger().getEnemies());
                        if (overlaps.size() == 1) score += 100;
                    }
                }
                break;
            case CHANGE_DESTINATION:
                for (Player player : getPlayers()) {
                    // if 3pt CEO in car,  + 20
                    if (player.getLimo().getPassenger() != null && player.getLimo().getPassenger().getPointsDelivered() == 3) {
                        score += 20;
                        playOn[0] = player;
                        break;
                    }
                }
                // if leading car: +10 * (CEO pt)
                if (high.getLimo().getPassenger() != null) {
                    score += (10 * high.getLimo().getPassenger().getPointsDelivered());
                    playOn[0] = high;
                }
                break;
            /*case MULT_DELIVERY_QUARTER_SPEED:
                score = 0; // never play, always discard
                break;*/
            case ALL_OTHER_CARS_QUARTER_SPEED:
                score = 50;
                break;
            case STOP_CAR:
                if (getMe().getScore() < hiscore) {
                    score = 15 * (hiscore - getMe().getScore());
                    playOn[0] = high;
                }
                break;
            case RELOCATE_ALL_CARS:
                if (getMe().getLimo().getPassenger() == null) score = 20;
                break;
            case RELOCATE_ALL_PASSENGERS:
                if (getMe().getLimo().getPassenger() != null) {
                    int noRelocCEOs = 0;
                    for (Player player : getPlayers()) {
                        if (player.getLimo().getPassenger() != null) noRelocCEOs += 1;
                    }
                    score = 5 * noRelocCEOs;
                }
                break;
            case MULT_DELIVERING_PASSENGER:
                if (nextPsngr == pu.getPassenger()) {
                    score = 50 * nextPsngr.getPointsDelivered();
                }
                break;
            case MULT_DELIVER_AT_COMPANY:
                if (nextPsngr.getDestination() == pu.getCompany()) {
                    score = 50 * nextPsngr.getPointsDelivered();
                }
                break;
        }
        return score;
    }

    /**
     * A power-up was played. It may be an error message, or success.
     * @param puStatus - The status of the played card.
     * @param plyrPowerUp - The player who played the card.
     * @param cardPlayed - The card played.
     */
    public void PowerupStatus(PlayerAIBase.STATUS puStatus, Player plyrPowerUp, PowerUp cardPlayed)
    {
        // redo the path if we got relocated
        if ((puStatus == PlayerAIBase.STATUS.POWER_UP_PLAYED) && ((cardPlayed.getCard() == PowerUp.CARD.RELOCATE_ALL_CARS) ||
                ((cardPlayed.getCard() == PowerUp.CARD.CHANGE_DESTINATION) && (cardPlayed.getPlayer() != null ? cardPlayed.getPlayer().getGuid() : null) == getMe().getGuid())))
            GameStatus(PlayerAIBase.STATUS.NO_PATH, getMe());
    }

    private void DisplayStatus(PlayerAIBase.STATUS status, Player plyrStatus)
    {
        String msg = null;
        switch (status)
        {
            case PASSENGER_DELIVERED:
                msg = getMyPassenger().getName() + " delivered to " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                passengersDelivered.add(getMyPassenger());
                break;
            case PASSENGER_ABANDONED:
                msg = getMyPassenger().getName() + " abandoned at " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                break;
            case PASSENGER_REFUSED_ENEMY:
                msg = plyrStatus.getLimo().getPassenger().getName() + " refused to exit at " +
                        plyrStatus.getLimo().getPassenger().getDestination().getName() + " - enemy there";
                break;
            case PASSENGER_DELIVERED_AND_PICKED_UP:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() + " and " +
                        plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                passengersDelivered.add(getMyPassenger());
                break;
            case PASSENGER_PICKED_UP:
                msg = plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                break;
            case PASSENGER_REFUSED_NO_COFFEE:
                msg = "Passenger refused to board limo, no coffee";
                break;
            case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() +
                        ", new passenger refused to board limo, no coffee";
                passengersDelivered.add(getMyPassenger());
                break;
            case COFFEE_STORE_CAR_RESTOCKED:
                msg = "Coffee restocked!";
                break;
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }

    private void DisplayOrders(Point ptDest)
    {
        String msg = null;
        CoffeeStore store = null;
        for(CoffeeStore s : getCoffeeStores()) {
            if(s.getBusStop() == ptDest) {
                store = s;
                break;
            }
        }

        if (store != null)
            msg = "Heading toward " + store.getName() + " at " + ptDest.toString();
        else
        {
            Company company = null;
            for(Company c : getCompanies()) {
                if(c.getBusStop() == ptDest) {
                    company = c;
                    break;
                }
            }

            if (company != null)
                msg = "Heading toward " + company.getName() + " at " + ptDest.toString();
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }

    private java.util.ArrayList<Point> CalculatePathPlus1(Player me, Point ptDest) {
        java.util.ArrayList<Point> path = SimpleAStar.CalculatePath(getGameMap(), me.getLimo().getMapPosition(), ptDest);
        // add in leaving the bus stop so it has orders while we get the message saying it got there and are deciding what to do next.
        if (path.size() > 1) {
            path.add(path.get(path.size() - 2));
        }
        return path;
    }

    private java.util.ArrayList<Passenger> AllPickups(Player me, Iterable<Passenger> passengers) {
        java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

        for (Passenger psngr : passengers) {
            if (psngr.equals(abandonedPassenger) || passengersDelivered.contains(psngr))
                continue;

            if ((!me.getPassengersDelivered().contains(psngr)) && (psngr != me.getLimo().getPassenger()) && (psngr.getCar() == null) && (psngr.getLobby() != null) && (psngr.getDestination() != null))
                pickup.add(psngr);
        }

        //add sort by random so no loops for can't pickup
        return pickup;
    }


    /**
     *
     * @param me
     * @param passengers
     * @param exclude We exclude this passenger
     * @return
     */
    private java.util.ArrayList<Passenger> AllPickups(Player me, Iterable<Passenger> passengers, Passenger exclude) {
        java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

        for (Passenger psngr : passengers) {
            if (psngr.equals(abandonedPassenger) || psngr.equals(exclude))
                continue;

            if ((!me.getPassengersDelivered().contains(psngr)) && (psngr != me.getLimo().getPassenger()) && (psngr.getCar() == null) && (psngr.getLobby() != null) && (psngr.getDestination() != null))
                pickup.add(psngr);
        }

        //add sort by random so no loops for can't pickup
        return pickup;
    }

    /*
    Below is our own code

     */
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Passenger chooseBestPassenger(BinarySearchST<Double, Passenger> passengersByValue)
    {
        // default best CEO
        Passenger bestPsngr = passengersByValue.get(passengersByValue.max());

        // get array of empty cars
        java.util.ArrayList<Limo> emptyLimos = new ArrayList<Limo>();
        for (Player player: getPlayers())
        {
            if (player.getLimo().getPassenger() == null)
                emptyLimos.add(player.getLimo());
        }

        // compare our distance to each passenger with that of other limos
        // returns the nearest passenger with the highest value if no other cars are in front of us
        while (!passengersByValue.isEmpty())
        {
            Passenger psngr = passengersByValue.get(passengersByValue.max());
            int distToPassenger = CalculatePathPlus1(getMe(), psngr.getLobby().getBusStop()).size();
            for (Limo opp: emptyLimos)
            {
                int oppDistToPassenger = SimpleAStar.CalculatePath(getGameMap(), opp.getMapPosition(), psngr.getLobby().getBusStop()).size();
                if (oppDistToPassenger < distToPassenger)
                {
                    passengersByValue.deleteMax();
                    break;
                }
            }

            return psngr;
        }

        // if, unfortunately, we cannot locate such a passenger, we just choose the "best" passenger with the highest
        // point-to-dist ratio
        return bestPsngr;
    }

    public BinarySearchST<Double, Passenger> getAvailablePassengers(ArrayList<Passenger> pickup)
    {
        if (pickup == null)
            return null;

        BinarySearchST<Double, Passenger> passengersByValue = new BinarySearchST<Double, Passenger>();

        double currMaxPoint = Double.MIN_VALUE;
        Passenger bestPsngr = null;
        for (Passenger psngr: pickup)
        {
            int distToPassenger = CalculatePathPlus1(getMe(), psngr.getLobby().getBusStop()).size();
            int distToDest = SimpleAStar.CalculatePath(
                    getGameMap(), psngr.getLobby().getBusStop(), psngr.getDestination().getBusStop()).size();
            int totalDist = distToDest + distToPassenger;
            double currPoint = (double) Math.sqrt(psngr.getPointsDelivered()) / totalDist;
            passengersByValue.put(currPoint, psngr);
        }

        return passengersByValue;
    }

    public Company chooseNearestCompanyWithoutEnemy(java.util.List<Company> comps, Passenger currPsngr)
    {
        int shortestDist = Integer.MAX_VALUE;
        Company nearestComp = null;

        // get locations of enemies
        ArrayList<Point> enemyLocations = new ArrayList<Point>();
        for (Passenger enemy: currPsngr.getEnemies())
        {
            if (enemy.getLobby() != null)
                enemyLocations.add(enemy.getLobby().getBusStop());
        }

        for (Company comp : comps)
        {
            // if this company is the location of an enemy, we ignore this company
            if (enemyLocations.contains(comp.getBusStop()))
                continue;

            int distToComp = CalculatePathPlus1(getMe(), comp.getBusStop()).size();
            if (distToComp < shortestDist)
            {
                shortestDist = distToComp;
                nearestComp = comp;
            }
        }

        return nearestComp;
    }

    public double evaluateAbandonment(Passenger currPsngr)
    {
        // we choose the best passenger who's not our current passenger
        Passenger psngr = chooseBestPassenger(getAvailablePassengers(AllPickups(getMe(), getPassengers(), currPsngr)));
        int distToPassenger = CalculatePathPlus1(getMe(), psngr.getLobby().getBusStop()).size();
        int distToDest = SimpleAStar.CalculatePath(
                getGameMap(), psngr.getLobby().getBusStop(), psngr.getDestination().getBusStop()).size();
        int totalDist = distToDest + distToPassenger;
        return (double) Math.sqrt(psngr.getPointsDelivered()) / totalDist;
    }

    public double evaluateCurrentDelivery(Passenger currPsngr)
    {
        int distToDest = CalculatePathPlus1(getMe(), currPsngr.getDestination().getBusStop()).size();
        double valueOfCurrentDelivery = (double) Math.sqrt(currPsngr.getPointsDelivered()) / distToDest;
        return valueOfCurrentDelivery;
    }
}