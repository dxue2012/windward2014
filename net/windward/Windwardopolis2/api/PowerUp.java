/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward meet some day, and you think
 * this stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.api;

import org.dom4j.Element;

import java.util.*;

public class PowerUp {
    static final Dictionary<String, PowerUp> statusPowerUps = new java.util.Hashtable<String, PowerUp>();

    /// <summary>
    /// The specific power of this powerUp.
    /// </summary>
    public enum CARD
    {
        /// <summary>Will move all passengers (not in a car) to a random bus stop (can play anytime).</summary>
        MOVE_PASSENGER,

        /// <summary>Change destination for a passenger in an opponent’s car to a random company (can play anytime).</summary>
        CHANGE_DESTINATION,

        /// <summary>Delivery is 1.5X points, but your car travels at 1/4 speed.</summary>
        MULT_DELIVERY_QUARTER_SPEED,

        /// <summary>Drop all other cars to 1/4 speed for 30 seconds (can play anytime).</summary>
        ALL_OTHER_CARS_QUARTER_SPEED,

        /// <summary>Can make a specific car stop for 30 seconds (tacks on road) - (can play anytime).</summary>
        STOP_CAR,

        /// <summary>Relocate all cars (including yours) to random locations (can play anytime).</summary>
        RELOCATE_ALL_CARS,

        /// <summary>Relocate all passengers at bus stops to random locations (can play anytime).</summary>
        RELOCATE_ALL_PASSENGERS,

        /// <summary>1.2X multiplier for delivering a specific person (we have one card for each passenger).</summary>
        MULT_DELIVERING_PASSENGER,

        /// <summary>1.2X multiplier for delivering at a specific company (we have one card for each company).</summary>
        MULT_DELIVER_AT_COMPANY
    }

    private PowerUp(Element elemCompany)
    {
        name = elemCompany.attributeValue("name");
        card = CARD.valueOf(elemCompany.attributeValue("card"));
    }

    private PowerUp(CARD card, Company company, Passenger passenger, Player player)
    {
        this.card = card;
        name = card.toString();
        if (company != null)
        {
            this.company = company;
            name += " - " + company.getName();
        }
        if (passenger != null)
        {
            this.passenger = passenger;
            this.name += " - " + passenger.getName();
        }
        if (player != null)
        {
            this.player = player;
            name += " - " + player.getName();
        }
    }

    public PowerUp(PowerUp src)
    {
        passenger = src.passenger;
        player = src.player;
        name = src.getName();
        card = src.getCard();
        company = src.getCompany();
        okToPlay = src.isOkToPlay();
    }

    /**
     * Class variables
     */

    // The name of the power-up.
    private String name;

    public String getName() {
        return name;
    }

    // The power-up card.
    private CARD card;

    public CARD getCard() {
        return card;
    }

    // The passenger affected for MOVE_PASSENGER, MULT_DELIVERING_PASSENGER.
    private Passenger passenger;

    public Passenger getPassenger() {
        return passenger;
    }
    public void setPassenger (Passenger toSet) {
        passenger = toSet;
    }

    // The player affected for CHANGE_DESTINATION, STOP_CAR
    private Player player;

    public Player getPlayer() {
        return player;
    }
    public void setPlayer(Player toSet) {
        player = toSet;
    }

    // The company affected for MULT_DELIVER_AT_COMPANY.
    private Company company;

    public Company getCompany() {
        return company;
    }
    public void setCompany (Company toSet) {
        company = toSet;
    }

    // It's ok to play this card. This is false until a card is drawn and the limo then visits a stop.
    private boolean okToPlay;

    public boolean isOkToPlay() {
        return okToPlay;
    }
    public void setOkToPlay(boolean toSet) {
        okToPlay = toSet;
    }

    protected boolean equals(PowerUp other) {
        // we do NOT compare Name or OkToPlay - update from server does an equals ignoring that.
        if (card != other.getCard())
            return false;
        if ((company == null) != (other.getCompany() == null))
            return false;
        if ((company != null) && (company.getName() != other.getCompany().getName()))
            return false;
        if ((passenger == null) != (other.getPassenger() == null))
            return false;
        if ((passenger != null) && (passenger.getName() != other.getPassenger().getName()))
            return false;
        if ((player == null) != (other.getPlayer() == null))
            return false;
        if ((player != null) && (player.getName() != other.getPlayer().getName()))
            return false;
        return true;
    }

    public static ArrayList<PowerUp> FromXml(Element elemPowerups, List<Company> companies, List<Passenger> passengers)
    {
        ArrayList<PowerUp> powerups = new ArrayList<PowerUp>();

        List<Element> elemPowerupsList = elemPowerups.elements();

        for (Element elemPuOn : elemPowerupsList)
        {
            PowerUp pu = new PowerUp(elemPuOn);
            switch (pu.getCard())
            {
                case MULT_DELIVERING_PASSENGER:
                    Passenger passToSet = null;
                    for(Passenger pass : passengers) {
                        if(pass.getName().equals(elemPuOn.attributeValue("passenger")))
                        {
                            passToSet = pass;
                            break;
                        }
                    }
                    pu.setPassenger(passToSet);
                    break;
                case MULT_DELIVER_AT_COMPANY:
                    Company compToSet = null;
                    for(Company comp : companies) {
                        if(comp.getName().equals(elemPuOn.attributeValue("company")))
                        {
                            compToSet = comp;
                            break;
                        }
                    }
                    pu.setCompany(compToSet);
                    break;
            }

            powerups.add(pu);
        }

        return powerups;
    }

    /// <summary>
    /// We only create one of each type to avoid memory allocations every time we get an update.
    /// </summary>
    /// <returns></returns>
    public static PowerUp GenerateFlyweight(Element element, List<Company> companies , List<Passenger> passengers, List<Player> players)
    {
        CARD card = CARD.valueOf(element.attributeValue("card"));
        String companyName = element.attribute("company") == null ? null : element.attribute("company").getValue();
        String passengerName = element.attribute("passenger") == null ? null : element.attribute("passenger").getValue();
        String playerName = element.attribute("player") == null ? null : element.attribute("player").getValue();
        String key = card + ":" + companyName + ":" + passengerName + ":" + playerName;
        boolean okToPlay = Boolean.parseBoolean(element.attributeValue("ok-to-play"));

        if (statusPowerUps.get(key) != null)
        {
            statusPowerUps.get(key).setOkToPlay(okToPlay);
            return statusPowerUps.get(key);
        }

        Company company = null;
        for(Company comp : companies) {
            if(comp.getName().equals(companyName)) {
                company = comp;
                break;
            }
        }

        Passenger passenger = null;
        for(Passenger pass : passengers) {
            if(pass.getName().equals(passengerName)) {
                passenger = pass;
                break;
            }
        }

        Player player = null;
        for(Player playuh : players) {
            if(playuh.getName().equals(playerName)) {
                player = playuh;
                break;
            }
        }

        PowerUp pu = new PowerUp(card, company, passenger, player);
        statusPowerUps.put(key, pu);

        pu.setOkToPlay(okToPlay);
        return pu;
    }
}
