package net.windward.Windwardopolis2.AI;

import net.windward.Windwardopolis2.api.Passenger;
import net.windward.Windwardopolis2.api.PowerUp;

import java.awt.*;

public class PlayerAIBase
{
	public static interface PlayerOrdersEvent
	{
		void invoke(String order, java.util.ArrayList<Point> path, java.util.ArrayList<Passenger> pickUp);
	}

    public static interface PlayerCardEvent
    {
        void invoke(PlayerAIBase.CARD_ACTION action, PowerUp powerup);
    }

    // playing a card
    public enum CARD_ACTION
    {
        DRAW,
        DISCARD,
        PLAY
    }

	public enum STATUS
	{
		/** 
		 Called ever N ticks to update the AI with the game status.
		*/
		UPDATE,
		/** 
		 The car has no path.
		*/
		NO_PATH,
		/** 
		 The passenger was abandoned, no passenger was picked up.
		*/
		PASSENGER_ABANDONED,
		/** 
		 The passenger was delivered, no passenger was picked up.
		*/
		PASSENGER_DELIVERED,
		/** 
		 The passenger was delivered or abandoned, a new passenger was picked up.
		*/
		PASSENGER_DELIVERED_AND_PICKED_UP,
		/** 
		 The passenger refused to exit at the bus stop because an enemy was there.
		*/
		PASSENGER_REFUSED_ENEMY,
		/** 
		 A passenger was picked up. There was no passenger to deliver.
		*/
		PASSENGER_PICKED_UP,
		/** 
		 At a bus stop, nothing happened (no drop off, no pick up).
		*/
		PASSENGER_NO_ACTION,
        /**
         * The passenger refused to board due to lack of coffee.
         */
        PASSENGER_REFUSED_NO_COFFEE,
        /**
         * The passenger was delivered or abandoned, the new passenger refused to board due to lack of coffee.
         */
        PASSENGER_DELIVERED_AND_PICK_UP_REFUSED,
        /**
         * Coffee stop did not stock up car. You cannot stock up when you have a passenger.
         */
        COFFEE_STORE_NO_STOCK_UP,
        /**
         * Coffee stop stocked up car.
         */
        COFFEE_STORE_CAR_RESTOCKED,
        /**
         * A draw request was refused as too many powerups are already in hand.
         */
        POWER_UP_DRAW_TOO_MANY,
        /**
         * A play request for a card not in hand.
         */
        POWER_UP_PLAY_NOT_EXIST,
        /**
         * A play request for a card drawn but haven't visited a stop yet.
         */
        POWER_UP_PLAY_NOT_READY,
        /**
         * It's illegal to play this card at this time.
         */
        POWER_UP_ILLEGAL_TO_PLAY,
        /**
         * The power up was played. For one that impacts a transit, the passenger is delivered.
         */
        POWER_UP_PLAYED,
        /**
         * The number of power-ups in the hand were too many. Randome one(s) discarded to reduce to the correct amount.
         */
        POWER_UP_HAND_TOO_MANY;


		public int getValue()
		{
			return this.ordinal();
		}

		public static STATUS forValue(int value)
		{
			return values()[value];
		}
	}
}