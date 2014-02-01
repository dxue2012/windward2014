/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2;

import net.windward.Windwardopolis2.AI.MyPlayerBrain;
import net.windward.Windwardopolis2.AI.PlayerAIBase;
import net.windward.Windwardopolis2.api.*;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.*;
import java.util.Iterator;

import org.apache.log4j.*;


public class Framework implements IPlayerCallback {
    private TcpClient tcpClient;
    private MyPlayerBrain brain;
    private String ipAddress = "127.0.0.1";

    private String myGuid;

    // we play a card and remove it from our hand. But at the same time the server is sending us a status with our
    // hand as it sees it. So we ignore updates to our hand if it's been less than a second and we're not seeing the
    // card we played in the incoming status.
    private PowerUp cardLastPlayed;
    private long cardLastSendTime = System.currentTimeMillis() - 2000;

    // this is used to make sure we don't have multiple threads updating the Player/Passenger lists, sending
    // back multiple orders, etc. This is a lousy way to handle this - but it keeps the example simple and
    // leaves room for easy improvement.
    private int signal;

    private static Logger log = Logger.getLogger(IPlayerCallback.class);

    /**
     * Run the A.I. player. All parameters are optional.
     *
     * @param args I.P. address of server, name
     */
    public static void main(String[] args) throws IOException {
        log.debug("***** Windwardopolis II starting *****");

        Framework framework = new Framework(Arrays.asList(args));
        framework.Run();
    }

    private Framework(java.util.List<String> args) {
        brain = new MyPlayerBrain(args.size() >= 2 ? args.get(1) : null);
        if (args.size() >= 1) {
            ipAddress = args.get(0);
        }
        String msg = String.format("Connecting to server %1$s for user: %2$s", ipAddress, brain.getName());

        System.out.println(msg);
    }

    private void Run() throws IOException {
        System.out.println("starting...");

        tcpClient = new TcpClient(this, ipAddress);
        tcpClient.Start();
        ConnectToServer();

        // It's all messages to us now.
        System.out.println("enter \"exit\" to exit program");
        while (true) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String line = in.readLine();
                if (line.equals("exit")) {
                    System.out.println("Exiting program...");
                    tcpClient.abort();
                    break;
                }
            } catch (Exception e) {
                System.out.println("ERROR restarting app (Exception: " + e.getMessage() + " )");
                log.error("restarting run(), Exception: " + e.getMessage());
            }

        }
    }

    public final void StatusMessage(String message) {
        System.out.println(message);
    }

    public final void IncomingMessage(String message) throws DocumentException {
        try {
            long startTime = System.currentTimeMillis();
            // get the xml - we assume we always get a valid message from the server.
            SAXReader reader = new SAXReader();
            Document xml = reader.read(new StringReader(message));

            String rootName = xml.getRootElement().getName();

            if (rootName.equals("setup")) {
                System.out.println("Received setup message");
                if(log.isInfoEnabled())
                    log.info("Recieved setup message");

                java.util.ArrayList<Player> players = Player.FromXml(xml.getRootElement().element("players"));
                java.util.ArrayList<Company> companies = Company.FromXml(xml.getRootElement().element("companies"));
                java.util.ArrayList<CoffeeStore> stores = CoffeeStore.FromXml(xml.getRootElement().element("stores"));
                java.util.ArrayList<Passenger> passengers = Passenger.FromXml(xml.getRootElement().element("passengers"), companies);
                java.util.ArrayList<PowerUp> powerups = PowerUp.FromXml(xml.getRootElement().element("powerups"), companies, passengers);

                Map map = new Map(xml.getRootElement().element("map"), companies);
                myGuid = xml.getRootElement().attribute("my-guid").getValue();

                Player me2 = null;
                for(Player plyr : players)
                {
                    if (myGuid.equals(plyr.getGuid()))
                        me2 = plyr;
                }

                brain.Setup(map, me2, players, companies, stores, passengers, powerups,
                        new PlayerAIBase.PlayerOrdersEvent() {public void invoke(String order, ArrayList<Point> path, ArrayList<Passenger> pickUp) {PlayerOrdersEvent(order, path, pickUp);}},
                        new PlayerAIBase.PlayerCardEvent() { public void invoke(PlayerAIBase.CARD_ACTION action, PowerUp powerup) { PlayerPowerSend(action, powerup); } });


            }
            else if (rootName.equals("powerup-status")) {
                // may be here because re-started and got this message before the re-send of setup.
                if(myGuid == null || myGuid.length() == 0)
                    return;

                synchronized (this) {
                    // bad news - we're throwing this message away.
                    if (signal > 0)
                        return;
                    signal++;
                }

                try {
                    // get what was played
                    PlayerAIBase.STATUS puStatus = PlayerAIBase.STATUS.valueOf(xml.getRootElement().attribute("status").getValue());
                    String puGuid = xml.getRootElement().attribute("played-by") != null ? xml.getRootElement().attribute("played-by").getValue() : myGuid;

                    Player plyrPowerUp = null;
                    for(Player plyr :brain.getPlayers())
                    {
                        if(puGuid.equals(plyr.getGuid()))
                            plyrPowerUp = plyr;
                    }

                    PowerUp cardPlayed = PowerUp.GenerateFlyweight(xml.getRootElement().element("card"), brain.getCompanies(), brain.getPassengers(), brain.getPlayers());

                    if (log.isInfoEnabled())
                        log.info(plyrPowerUp.getName() + " " + puStatus + " on " + cardPlayed);

                    // do we update the card deck?
                    if (cardPlayed.equals(cardLastPlayed) || (System.currentTimeMillis() - cardLastSendTime) > 1000)
                    {
                        // move any not in deck to hand
                        UpdateCards(xml.getRootElement().element("cards-deck").elements("card"), brain.getPowerUpDeck(), brain.getPowerUpHand());
                        // delete any not in drawn
                        UpdateCards(xml.getRootElement().element("cards-hand").elements("card"), brain.getPowerUpHand(), null);
                    }

                    // pass in to play cards.
                    brain.PowerupStatus(puStatus, plyrPowerUp, cardPlayed);
                } finally {
                    synchronized (this) { signal--; }
                }
            }
            else if (rootName.equals("status")) {
                // may be here because re-started and got this message before the re-send of setup.
                if (net.windward.Windwardopolis2.DotNetToJavaStringHelper.isNullOrEmpty(myGuid)) {
                    return;
                }

                synchronized (this) {
                    if (signal > 0) {
                        // bad news - we're throwing this message away.
                        TRAP.trap();
                        return;
                    }
                    signal++;
                }
                try {

                    PlayerAIBase.STATUS status = PlayerAIBase.STATUS.valueOf(xml.getRootElement().attribute("status").getValue());
                    Attribute attr = xml.getRootElement().attribute("player-guid");
                    String guid = attr != null ? attr.getValue() : myGuid;

                    Player.UpdateFromXml(brain.getCompanies(), brain.getPlayers(), brain.getPassengers(),xml.getRootElement().element("players"));
                    Passenger.UpdateFromXml(brain.getCompanies(), brain.getPlayers(), brain.getPassengers(), xml.getRootElement().element("passengers"));


                    // update my path & pick-up.
                    Player plyrStatus = null;
                    for(Player plyr :brain.getPlayers())
                    {
                        if(guid.equals(plyr.getGuid()))
                            plyrStatus = plyr;
                    }
                    Element elem = xml.getRootElement().element("path");
                    if (elem != null) {
                        String[] path = elem.getText().split(";", 0);
                        plyrStatus.getLimo().getPath().clear();
                        for (String stepOn : path) {
                            int pos = stepOn.indexOf(',');
                            if(pos>0)
                            plyrStatus.getLimo().getPath().add(new Point(Integer.parseInt(stepOn.substring(0, pos)), Integer.parseInt(stepOn.substring(pos+1))));
                        }
                    }

                    elem = xml.getRootElement().element("pick-up");
                    if (elem != null) {
                        String[] names = elem.getText().split(";", 0);
                        plyrStatus.getPickUp().clear();

                        ArrayList<Passenger> newPsngrList = new ArrayList<Passenger>();

                        for(String name : names)
                        {
                            for(Passenger ps : brain.getPassengers())
                            {
                                if(ps.getName().equals(name))
                                {
                                    newPsngrList.add(ps);
                                }
                            }

                        }

                        for (Passenger psngrOn : newPsngrList)
                        {
                            plyrStatus.getPickUp().add(psngrOn);
                        }
                    }

                    // pass in to generate new orders
                    brain.GameStatus(status, plyrStatus);
                } finally {
                    synchronized (this) {
                        signal--;
                    }
                }


            }

            else if (xml.getRootElement().getName().equals("exit")) {
                System.out.println("Received exit message");
                if (log.isInfoEnabled()) {
                    log.info("Received exit message");
                }
                System.exit(0);

            } else {
                TRAP.trap();
                String msg = String.format("ERROR: bad message (XML) from server - root node %1$s", xml.getRootElement().getName());
                log.warn(msg);
                //Trace.WriteLine(msg);
            }

            long turnTime = System.currentTimeMillis() - startTime;
            if (turnTime > 800) {
                System.out.println("WARNING - turn took " + turnTime / 1000 + " seconds");

            }
        } catch (RuntimeException ex) {
            System.out.println(String.format("Error on incoming message. Exception: %1$s", ex));
            ex.printStackTrace();
            log.error("Error on incoming message.", ex);
        }
    }

    private void UpdateCards(java.util.List<Element> elements, java.util.List<PowerUp> cardList, java.util.List<PowerUp> hand)
    {

        ArrayList<PowerUp> deck = new ArrayList<PowerUp>();
        Iterator<Element> it = elements.iterator();

        while(it.hasNext()) {
            Element current = it.next();
            deck.add(PowerUp.GenerateFlyweight(current, brain.getCompanies(), brain.getPassengers(), brain.getPlayers()));
        }

        for (int ind = 0; ind < cardList.size(); )
        {
            PowerUp pu = cardList.get(ind);
            if (deck.contains(pu))
            {
                int thisCardInd = deck.indexOf(pu);
                pu.setOkToPlay(deck.get(thisCardInd).isOkToPlay());
                deck.remove(pu);
                ind++;
                continue;
            }
            // moving from deck to hand
            if (hand != null)
                hand.add(pu);

            cardList.remove(ind);
        }

        // did some get added back to the deck/hand?
        ArrayList<PowerUp> toAdd = new ArrayList<PowerUp>();
        for(PowerUp p : deck) {
            toAdd.add(new PowerUp(p));
        }
        cardList.addAll(toAdd);
    }

    private void PlayerOrdersEvent(String order, java.util.ArrayList<Point> path, java.util.ArrayList<Passenger> pickUp) {
        try {

            // update our info
            if (path.size() > 0) {
                brain.getMe().getLimo().getPath().clear();
                brain.getMe().getLimo().getPath().addAll(path);
            }
            if (pickUp.size() > 0) {
                brain.getMe().getPickUp().clear();
                brain.getMe().getPickUp().addAll(pickUp);
            }
            Document xml = DocumentHelper.createDocument();
            Element elem = DocumentHelper.createElement(order);
            xml.add(elem);
            if (path.size() > 0) {
                StringBuilder buf = new StringBuilder();
                for (Point ptOn : path) {
                    buf.append(String.valueOf(ptOn.x) + ',' + String.valueOf(ptOn.y) + ';');
                }
                Element newElem = DocumentHelper.createElement("path");
                newElem.setText(buf.toString());
                elem.add(newElem);
            }
            if (pickUp.size() > 0) {
                StringBuilder buf = new StringBuilder();
                for (Passenger psngrOn : pickUp) {
                    buf.append(psngrOn.getName() + ';');
                }
                Element newElem = DocumentHelper.createElement("pick-up");
                newElem.setText(buf.toString());
                elem.add(newElem);
            }
            try {
                String toSend = xml.asXML();
                tcpClient.SendMessage(toSend);
            } catch (IOException e) {
                System.out.println("bad sent orders event");
                e.printStackTrace();
            }
        } catch (Exception e) {
            log.error("PlayerOrderEvent( " + order + ", ...) threw Exception: " + e.getMessage());
        }
    }

    private void PlayerPowerSend(PlayerAIBase.CARD_ACTION action, PowerUp powerup) {

        if (log.isInfoEnabled())
            log.info("Request " + action + " " + powerup);

        cardLastPlayed = powerup;
        cardLastSendTime = System.currentTimeMillis();

        Document xml = DocumentHelper.createDocument();
        Element elem = DocumentHelper.createElement("order");
        elem.add(DocumentHelper.createAttribute(elem, "action", action.name()));
        Element elemCard = DocumentHelper.createElement("powerup");
        elemCard.add(DocumentHelper.createAttribute(elemCard, "card", powerup.getCard().name()));

        if(powerup.getCompany() != null)
            elemCard.add(DocumentHelper.createAttribute(elemCard, "company", powerup.getCompany().getName()));
        if(powerup.getPassenger() != null)
            elemCard.add(DocumentHelper.createAttribute(elemCard, "passenger", powerup.getPassenger().getName()));
        if(powerup.getPlayer() != null)
            elemCard.add(DocumentHelper.createAttribute(elemCard, "player", powerup.getPlayer().getName()));

        elem.add(elemCard);
        xml.add(elem);
        try {
            String toSend = xml.asXML();
            tcpClient.SendMessage(toSend);
        } catch (IOException e) {
            System.out.println("bad sent orders event");
            e.printStackTrace();
        }
    }

    public final void ConnectionLost(Exception ex) throws IOException, InterruptedException {

        System.out.println("Lost our connection! Exception: " + ex.getMessage());

        int delay = 500;
        while (true) {
            try {
                if (tcpClient != null) {
                    tcpClient.Close();
                }
                tcpClient = new TcpClient(this,ipAddress);
                tcpClient.Start();

                ConnectToServer();
                System.out.println("Re-connected");

                return;
            } catch (RuntimeException e) {

                System.out.println("Re-connection fails! Exception: " + e.getMessage());
                Thread.sleep(delay);
                delay += 500;
            }
        }
    }

    private void ConnectToServer() throws IOException {
        try {
            Document doc = DocumentHelper.createDocument();
            Element root = DocumentHelper.createElement("join");
            root.addAttribute("name",brain.getName());
            root.addAttribute("school",MyPlayerBrain.SCHOOL);
            root.addAttribute("language","Java");

            byte[] data = brain.getAvatar();
            if (data != null) {
                Element avatarElement = DocumentHelper.createElement("avatar");
                BASE64Encoder encoder = new BASE64Encoder();
                avatarElement.setText(encoder.encode(data));
                root.add(avatarElement);
            }

            doc.add(root);

            tcpClient.SendMessage(doc.asXML());
        } catch (Exception e) {
            log.warn("ConnectToServer() threw Exception: " + e.getMessage());
        }
    }
}