package net.windward.Windwardopolis2.AI;


import net.windward.Windwardopolis2.api.*;

import java.util.ArrayList;

public interface IPlayerAI
{

	/** 
	 Called when your robot must be placed on the board. This is called at the start of the game.
	 
	 @param map The game map.
	 @param me My Player object. This is also in the players list.
	 @param players All players (including you).
	 @param companies The companies on the map.
	 @param passengers The passengers that need a lift.
	 @param ordersEvent Method to call to send orders to the server.
	*/
	void Setup(Map map, Player me, java.util.ArrayList<Player> players, java.util.ArrayList<Company> companies, ArrayList<CoffeeStore> stores, java.util.ArrayList<Passenger> passengers, ArrayList<PowerUp> powerUps, PlayerAIBase.PlayerOrdersEvent ordersEvent, PlayerAIBase.PlayerCardEvent cardEvent);

	/** 
	 Called to send an update message to this A.I. We do NOT have to reply to it.
	 
	 @param status The status message.
	 @param plyrStatus The status of my player.
	*/
	void GameStatus(PlayerAIBase.STATUS status, Player plyrStatus);
}