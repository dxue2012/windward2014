package net.windward.Windwardopolis2.api;

import org.dom4j.Element;
import org.dom4j.Attribute;
import java.awt.*;
public class Player
{
	private Player(Element elemPlayer)
	{
		setGuid(elemPlayer.attribute("guid").getValue());
		setName(elemPlayer.attribute("name").getValue());
		setLimo(new Limo(new Point(Integer.parseInt(elemPlayer.attribute("limo-x").getValue()), Integer.parseInt(elemPlayer.attribute("limo-y").getValue())), Integer.parseInt(elemPlayer.attribute("limo-angle").getValue())));
		setPickUp(new java.util.ArrayList<Passenger>());
		setPassengersDelivered(new java.util.ArrayList<Passenger>());
	}

	/**
	 The unique identifier for this player. This will remain constant for the length of the game (while the Player objects passed will
	 change on every call).
	*/
	private String privateGuid;
	public final String getGuid()
	{
		return privateGuid;
	}
	private void setGuid(String value)
	{
		privateGuid = value;
	}

	/**
	 The name of the player.
	*/
	private String privateName;
	public final String getName()
	{
		return privateName;
	}
	private void setName(String value)
	{
		privateName = value;
	}

	/**
	 Who to pick up at the next bus stop. Can be empty and can also only list people not there.
	 This may be wrong after a pick-up occurs as all we get is a count. This is updated with the
	 most recent list sent to the server.
	*/
	private java.util.ArrayList<Passenger> privatePickUp;
	public final java.util.ArrayList<Passenger> getPickUp()
	{
		return privatePickUp;
	}
	private void setPickUp(java.util.ArrayList<Passenger> value)
	{
		privatePickUp = value;
	}

	/**
	 The passengers delivered - this game.
	*/
	private java.util.ArrayList<Passenger> privatePassengersDelivered;
	public final java.util.ArrayList<Passenger> getPassengersDelivered()
	{
		return privatePassengersDelivered;
	}
	private void setPassengersDelivered(java.util.ArrayList<Passenger> value)
	{
		privatePassengersDelivered = value;
	}

	/**
	 The player's limo.
	*/
	private Limo privateLimo;
	public final Limo getLimo()
	{
		return privateLimo;
	}
	private void setLimo(Limo value)
	{
		privateLimo = value;
	}

	/**
	 The score for this player (this game, not across all games so far).
	*/
	private float privateScore;
	public final float getScore()
	{
		return privateScore;
	}
	private void setScore(float value)
	{
		privateScore = value;
	}

    /**
     * The computer language this player's AI is written in.
     */
    private String privateLanguage;
    public final String getLanguage() { return privateLanguage; }
    private void setLangauge(String lang) { privateLanguage = lang; }

    /**
     * The school this player is from
     */
    private String privateSchool;
    public final String getSchool() { return privateSchool; }
    private void setSchool(String school) { privateSchool = school; }

    /**
     * The score for this player across all games (so far).
     */
    private float privateTotalScore;
    public final float getTotalScore() { return privateTotalScore; }
    private void setTotalScore(float score) { privateTotalScore = score; }

    /**
     * The maximum number of cards this player can have in their hand.
     */
    private int privateMaxCardsInHand;
    public final int getMaxCardsInHand() { return privateMaxCardsInHand; }
    private void setMaxCardsInHand(int cards) { privateMaxCardsInHand = cards; }

    /**
     * The power up this player will play at the next bus stop.
     */
    private PowerUp privatePowerUpNextBusStop;
    public final PowerUp getPowerUpNextBusStop() { return privatePowerUpNextBusStop; }
    private void setPowerUpNextBusStop(PowerUp nextBusStop) { privatePowerUpNextBusStop = nextBusStop; }

    /**
     * The power up in effect for the transit this player is presently executing.
     */
    private PowerUp privatePowerUpTransit;
    public final PowerUp getPowerUpTransit() { return privatePowerUpTransit; }
    private void setPowerUpTransit(PowerUp transitPowerUp) { privatePowerUpTransit = transitPowerUp; }

	/**
	 Called on setup to create initial list of players.

	 @param elemPlayers The xml with all the players.
	 @return The created list of players.
	*/
	public static java.util.ArrayList<Player> FromXml(Element elemPlayers)
	{
		java.util.ArrayList<Player> players = new java.util.ArrayList<Player>();
		for (Object elemPlyrOn : elemPlayers.selectNodes("player"))
		{
			players.add(new Player((Element)elemPlyrOn));
		}
		return players;
	}

	public static void UpdateFromXml(java.util.ArrayList<Company> companies, java.util.ArrayList<Player> players, java.util.ArrayList<Passenger> passengers, Element elemPlayers)
	{
		for (Object objPlyrOn : elemPlayers.selectNodes("player"))
		{
            Element elemPlyrOn = (Element)objPlyrOn;
            Player plyrOn = null;
            for(Player pl : players)
            {
                if(pl.getGuid().equals(elemPlyrOn.attribute("guid").getValue()))
                    plyrOn = pl;
            }

            if (plyrOn != null) {
                plyrOn.setScore(Float.parseFloat(elemPlyrOn.attribute("score").getValue()));
                if(elemPlyrOn.attribute("total-score") != null)
                    plyrOn.setTotalScore(Float.parseFloat(elemPlyrOn.attribute("total-score").getValue()));
                plyrOn.setMaxCardsInHand(Integer.parseInt(elemPlyrOn.attribute("cards-max").getValue()));
                if(elemPlyrOn.attribute("coffee-servings") != null)
                    plyrOn.getLimo().setCoffeeServings(Integer.parseInt(elemPlyrOn.attribute("coffee-servings").getValue()));
            }

            // car location
			plyrOn.getLimo().setMapPosition(new Point(Integer.parseInt(elemPlyrOn.attribute("limo-x").getValue()), Integer.parseInt(elemPlyrOn.attribute("limo-y").getValue())));
			plyrOn.getLimo().setAngle(Integer.parseInt(elemPlyrOn.attribute("limo-angle").getValue()));

			// see if we now have a passenger.
			Attribute attrPassenger = elemPlyrOn.attribute("passenger");
			if (attrPassenger != null)
			{
                Passenger passenger = null;
                for(Passenger psngr : passengers)
                {
                    if(psngr.getName().equals(attrPassenger.getValue()))
                    {
                        passenger = psngr;
                    }
                }
				plyrOn.getLimo().setPassenger(passenger);
                if (passenger != null) {
                    passenger.setCar(plyrOn.getLimo());
                }
            }
			else
			{
				plyrOn.getLimo().setPassenger(null);
			}

			// add most recent delivery if we this is the first time we're told.
			attrPassenger = elemPlyrOn.attribute("last-delivered");
			if (attrPassenger != null)
			{
                Passenger passenger = null;
                for(Passenger psngr : passengers)
                {
                    if(psngr.getName().equals(attrPassenger.getValue()))
                    {
                        passenger = psngr;
                    }
                }


				if (!plyrOn.getPassengersDelivered().contains(passenger))
				{
					plyrOn.getPassengersDelivered().add(passenger);
				}
			}

            //  powerups in action
            Element elemCards = elemPlyrOn.element("next-bus-stop");
            if(elemCards != null) {
                plyrOn.setPowerUpNextBusStop(PowerUp.GenerateFlyweight(elemCards, companies, passengers, players));
            }
            elemCards = elemPlyrOn.element("transit");
            if(elemCards != null) {
                plyrOn.setPowerUpTransit(PowerUp.GenerateFlyweight(elemCards, companies, passengers, players));
            }
		}
	}

	@Override
	public String toString()
	{
		return String.format("%1$s; NumDelivered:%2$s", getName(), getPassengersDelivered().size());
	}
}