package com.bayviewglen.zork;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;
class Game implements Serializable {
    private Parser parser;
    private Room currentRoom;
    private Player player = new Player("empty");
    private boolean dead;
    private boolean playerwin;
    private Scanner keyboard = new Scanner(System.in);
    private Troll troll = new Troll(20, 50);
    private BarbarianKing king=new BarbarianKing(100, 20);
    private HashMap < String, Equipable > equipables;
    private HashMap < String, Food > foods;
    // This is a MASTER object that contains all of the rooms and is easily accessible.
    // The key will be the name of the room -> no spaces (Use all caps and underscore -> Great Room would have a key of GREAT_ROOM
    // In a hashmap keys are case sensitive.
    private HashMap < String, Room > masterRoomMap;
    private void initRooms(String fileName) throws Exception {
        masterRoomMap = new HashMap < String, Room > ();
        Scanner roomScanner;
        try {
            HashMap < String, HashMap < String, String >> exits = new HashMap < String, HashMap < String,
                String >> ();
            roomScanner = new Scanner(new File(fileName));
            while (roomScanner.hasNext()) {
                Room room = new Room();
                // Read the Name
                String roomName = roomScanner.nextLine();
                room.setRoomName(roomName.split(":")[1].trim());
                // Read the Description
                String roomDescription = roomScanner.nextLine();
                room.setDescription(roomDescription.split(":")[1].replaceAll("<br>", "\n").trim());
                // Read the Exits
                String roomExits = roomScanner.nextLine();
                // An array of strings in the format E-RoomName
                String[] rooms = roomExits.split(":")[1].split(",");
                HashMap < String, String > temp = new HashMap < String, String > ();
                for (String s: rooms) {
                    temp.put(s.split("-")[0].trim(), s.split("-")[1]);
                }
                exits.put(roomName.substring(roomName.indexOf(" ")).trim().toUpperCase().replaceAll(" ",
                    "_"), temp);
                masterRoomMap.put(roomName.toUpperCase().substring(roomName.indexOf(":") + 1).trim().replaceAll(
                    " ", "_"), room);
                // This puts the room we created (Without the exits in the masterMap)
                String itemsline = roomScanner.nextLine();
                String[] itemnames = itemsline.split(":")[1].trim().split(",");
                Inventory tempinv = new Inventory();
                for (String s: itemnames) {
                    Item temp2 = new Item(s.trim());
                    tempinv.addItem(temp2);
                }
                masterRoomMap.get(roomName.toUpperCase().substring(roomName.indexOf(":") + 1).trim().replaceAll(
                    " ", "_")).setInventory(tempinv);
                String lockedline = roomScanner.nextLine();
                String locked = lockedline.split(":")[1].trim();
                if (locked.toUpperCase().equals("YES")) {
                    masterRoomMap.get(roomName.toUpperCase().substring(roomName.indexOf(":") + 1).trim().replaceAll(
                        " ", "_")).setLocked(true);
                    String keyname = roomScanner.nextLine().split(":")[1].trim();
                    masterRoomMap.get(roomName.toUpperCase().substring(roomName.indexOf(":") + 1).trim().replaceAll(
                        " ", "_")).setKeyname(keyname);
                }
                String enemyline = roomScanner.nextLine();
                String enemy = enemyline.split(":")[1].trim();
                masterRoomMap.get(roomName.toUpperCase().substring(roomName.indexOf(":") + 1).trim().replaceAll(
                    " ", "_")).setEnemytype(enemy);
                // Now we better set the exits.
            }
            for (String key: masterRoomMap.keySet()) {
                Room roomTemp = masterRoomMap.get(key);
                HashMap < String, String > tempExits = exits.get(key);
                for (String s: tempExits.keySet()) {
                    // s = direction
                    // value is the room.
                    String roomName2 = tempExits.get(s.trim());
                    Room exitRoom = masterRoomMap.get(roomName2.toUpperCase().replaceAll(" ", "_"));
                    roomTemp.setExit(s.trim().charAt(0), exitRoom);
                }
            }
            roomScanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void initEquipables(String fileName) {
        equipables = new HashMap < String, Equipable > ();
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(new File(fileName));
            while (fileScanner.hasNext()) {
                String[] temp = fileScanner.nextLine().split("~");
                Equipable temp2 = new Equipable(temp[0], Double.parseDouble(temp[1]), Double.parseDouble(
                        temp[2]),
                    Double.parseDouble(temp[3]), Double.parseDouble(temp[4]));
                equipables.put(temp[0], temp2);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void initFoods(String fileName) {
            foods = new HashMap < String, Food > ();
            Scanner fileScanner;
            try {
                fileScanner = new Scanner(new File(fileName));
                while (fileScanner.hasNext()) {
                    String[] temp = fileScanner.nextLine().split("~");
                    Food temp2 = new Food(temp[0], Integer.parseInt(temp[1]));
                    foods.put(temp[0], temp2);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        /**
         * Create the game and initialise its internal map.
         */
    public Game() {
            try {
                initRooms("data/Rooms.dat");
                currentRoom = masterRoomMap.get("COURTYARD_SOUTH");
                initEquipables("data/equipable.dat");
                initFoods("data/foods.dat");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            parser = new Parser();
        }
        /**
         *  Main play routine.  Loops until end of play.
         * @throws InterruptedException 
         */
    public void play() throws InterruptedException {
            System.out.println(
                "If you'd like to use a savefile type YES and press enter, if not press literally anything else"
            );
            if (keyboard.nextLine().equalsIgnoreCase("yes")) {
                try {
                    FileInputStream f_in = new FileInputStream("data/savefile.dat");
                    ObjectInputStream ois = new ObjectInputStream(f_in);
                    Object[] laziness = (Object[]) ois.readObject();
                    currentRoom = (Room) laziness[0];
                    player = (Player) laziness[1];
                    masterRoomMap = (HashMap < String, Room > ) laziness[2];
                } catch (Exception ex) {
                    // TODO Auto-generated catch block
                    ex.printStackTrace();
                }
            } else {
                System.out.println("Please enter your name:");
                String name = keyboard.nextLine();
                player.setName(name);
            }
            printWelcome();
            // Enter the main command loop.  Here we repeatedly read commands and
            // execute them until the game is over.
            boolean finished = false;
            while (!finished) {
                if (player.getPlayerhealth() == 0)
                    break;
                Command command = parser.getCommand();
                finished = processCommand(command);
                System.out.println();
                if (!(currentRoom.isBeenhere()) && !(player.getPlayerhealth() == 0)) {
                    System.out.println(currentRoom.longDescription());
                } else if (!(player.getPlayerhealth() == 0)) {
                    System.out.println(currentRoom.getRoomName());
                } else {
                    break;
                }
            }
            
            if(playerwin)
            	System.out.println("Congratulations!  You have defeated the king and have won!.");
            System.out.println("Thank you for playing.");
        }
        /**
         * Print out the opening message for the player.
         */
    private void printWelcome() {
            System.out.println();
            System.out.println("Hello " + player.getName() +
                " and welcome to Zork!!!  Zork is an incredibly fun adventure game programmed by Shon and Justin."
            );
            System.out.println(
                "If you ever need help while playing the game, type in 'help'.  Now, press Enter to continue."
            );
            String useless = keyboard.nextLine();
            System.out.println();
            if (!(currentRoom.isBeenhere()))
                System.out.println(currentRoom.longDescription());
            else
                System.out.println(currentRoom.getRoomName());
        }
        /**
         * Given a command, process (that is: execute) the command.
         * If this command ends the game, true is returned, otherwise false is
         * returned.
         */
    private boolean processCommand(Command command) throws InterruptedException {
            if (command.isUnknown()) {
                System.out.println("I don't know what you mean...");
                return false;
            }
            String commandWord = command.getCommandWord();
            String secondWord = command.getSecondWord();
            if (commandWord.equals("help")) {
                printHelp();
                currentRoom.setBeenhere(true);
            } else if (commandWord.equals("go")) {
                goRoom(command);
                currentRoom.setBeenhere(true);
            } else if (commandWord.equals("unequip")) {
                if (secondWord != null && player.getEquipped().contains(secondWord)) {
                    Inventory temp = player.getPlayerInv();
                    Inventory temp2 = player.getEquipped();
                    temp.addItem(player.getEquipped().getItem(secondWord));
                    temp2.removeItem(player.getEquipped().getItem(secondWord));
                    player.setEquipped(temp2);
                    player.setPlayerInv(temp);
                    player.setACCURACY((int)(player.getACCURACY() * equipables.get(secondWord).getACCURACYmultiplier()));
                    player.setATTACK((int)(player.getATTACK() * equipables.get(secondWord).getATTACKmultiplier()));
                    player.setPlayerhealth((int)(player.getPlayerhealth() * equipables.get(secondWord).getPlayerhealthmultiplier()));
                    player.setPlayerSpeed((int)(player.getPlayerSpeed() * equipables.get(secondWord).getPLAYER_SPEEDmultiplier()));
                    player.displayPlayerStats();
                }
                currentRoom.setBeenhere(true);
            } else if (commandWord.equals("equip")) {
                if (secondWord != null && player.getPlayerInv().contains(secondWord) && equipables.containsKey(
                        secondWord)) {
                    Inventory temp = player.getPlayerInv();
                    Inventory temp2 = player.getEquipped();
                    temp2.addItem(player.getPlayerInv().getItem(secondWord));
                    temp.removeItem(player.getPlayerInv().getItem(secondWord));
                    player.setEquipped(temp2);
                    player.setPlayerInv(temp);
                    player.setACCURACY((int)(player.getACCURACY() / equipables.get(secondWord).getACCURACYmultiplier()));
                    player.setATTACK((int)(player.getATTACK() / equipables.get(secondWord).getATTACKmultiplier()));
                    player.setPlayerhealth((int)(player.getPlayerhealth() / equipables.get(secondWord).getPlayerhealthmultiplier()));
                    player.setPlayerSpeed((int)(player.getPlayerSpeed() / equipables.get(secondWord).getPLAYER_SPEEDmultiplier()));
                    player.displayPlayerStats();
                }
                currentRoom.setBeenhere(true);
            } else if (commandWord.equals("take")) {
                if (currentRoom.getInventory().contains(secondWord)) {
                    Inventory tempinventory = player.getPlayerInv();
                    Inventory currentroominventory = currentRoom.getInventory();
                    Item toTake = currentroominventory.getItem(secondWord);
                    currentroominventory.removeItem(toTake);
                    tempinventory.addItem(toTake);
                    player.setPlayerInv(tempinventory);
                    System.out.println("Taken!");
                    currentRoom.setBeenhere(true);
                } else if (commandWord.equals("drop")) {
                    if (secondWord != null && player.getPlayerInv().contains(secondWord)) {
                        Inventory tempinventory = player.getPlayerInv();
                        Inventory currentroominventory = currentRoom.getInventory();
                        Item toTake = tempinventory.getItem(secondWord);
                        currentroominventory.addItem(toTake);
                        tempinventory.removeItem(toTake);
                        player.setPlayerInv(tempinventory);
                        System.out.println("Dropped!");
                        currentRoom.setBeenhere(true);
                    } else {
                        System.out.println("You don't have a single " + secondWord +
                            " in your inventory or unequipped...");
                        currentRoom.setBeenhere(true);
                    }
                } else {
                    System.out.println("There are no " + secondWord + "s at this location!");
                    currentRoom.setBeenhere(true);
                }
            } else if (commandWord.equals("inv")) {
                //PrintInventory
                System.out.println("Your inventory:");
                player.getPlayerInv().print();
                System.out.println("Current Health:" + player.getPlayerhealth());
            } else if (commandWord.equals("attack")) {
                if (secondWord.equals(null)) {
                    System.out.println("Attack what?");
                } else if (secondWord.compareTo("troll") == 0 && currentRoom.getEnemytype().compareTo("troll") ==
                    0) {
                    //Battle Troll
                    boolean trolldead = trollBattleMove();
                    currentRoom.setBeenhere(true);
                    if (trolldead) {
                        masterRoomMap.get("BEDROOM").setBeenhere(false);
                        masterRoomMap.get("BEDROOM").setDescription(masterRoomMap.get("BEDROOM_WITHOUT_TROLL")
                            .getDescription());
                        masterRoomMap.get("BEDROOM").setEnemytype("");
                        currentRoom = masterRoomMap.get("BEDROOM_WITHOUT_TROLL");
                    }
                } else if (secondWord.toUpperCase().equals("BARBARIAN KING") || secondWord.toUpperCase().equals(
                        "KING") || secondWord.toUpperCase().equals("BARBARIAN") && currentRoom.getEnemytype()
                    .toUpperCase().equals("BARBARIAN KING")) {
                    
                	//Battle King
                    boolean kingdead = kingBattleMove();
                    if (kingdead) {
                       playerwin=true;
                    } 	
                	currentRoom.setBeenhere(true);
                } else {
                    System.out.println("There are no " + secondWord + "s here!!!");
                    currentRoom.setBeenhere(true);
                }
            } else if (commandWord.equals("quit")) {
                if (command.hasSecondWord()) {
                    System.out.println("Quit what?");
                    currentRoom.setBeenhere(true);
                } else {
                    try {
                        FileOutputStream f_out = new FileOutputStream("data/savefile.dat");
                        ObjectOutputStream gamestate = new ObjectOutputStream(f_out);
                        Object[] laziness = {
                            currentRoom, player, masterRoomMap
                        };
                        gamestate.writeObject(laziness);
                        gamestate.close();
                        System.out.println("Game Saved.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                }
            } else if (commandWord.equals("eat")) {
                if (secondWord != null && player.getPlayerInv().contains(secondWord) && foods.containsKey(
                        secondWord)) {
                    player.setPlayerhealth(player.getPlayerhealth() + foods.get(secondWord).getHeal());
                    Inventory temp = player.getPlayerInv();
                    temp.removeItem(player.getPlayerInv().getItem(secondWord));
                    player.setPlayerInv(temp);
                    System.out.println("Eaten! HP: " + player.getPlayerhealth());
                } else
                    System.out.println("What are you trying to eat???");
            } else if ((commandWord.equals("east") || commandWord.equals("north") || commandWord.equals(
                        "south") ||
                    commandWord.equals("west") || commandWord.equals("up") || commandWord.equals("down"))) {
                if (commandWord.equals("east") && (currentRoom.nextRoom("east") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("east");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That door is locked!!!");
                    }
                } else if (commandWord.equals("north") && (currentRoom.nextRoom("north") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("north");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That room is locked!!!  You cannot go there!!!");
                    }
                } else if (commandWord.equals("west") && (currentRoom.nextRoom("west") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("west");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That room is locked!!!  You cannot go there!!!");
                    }
                } else if (commandWord.equals("south") && (currentRoom.nextRoom("south") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("south");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That room is locked!!!  You cannot go there!!!");
                    }
                } else if (commandWord.equals("up") && (currentRoom.nextRoom("up") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("up");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That room is locked!!!  You cannot go there!!!");
                    }
                } else if (commandWord.equals("down") && (currentRoom.nextRoom("down") != null)) {
                    Room oldroom = currentRoom;
                    currentRoom.setBeenhere(true);
                    currentRoom.unLock();
                    currentRoom = currentRoom.nextRoom("down");
                    if (currentRoom.isLocked() && !HasKey(player.getPlayerInv(), currentRoom.getKeyname())) {
                        currentRoom = oldroom;
                        System.out.println("That room is locked!!!  You cannot go there!!!");
                    }
                } else {
                    System.out.println("There is nothing in this direction!!!");
                }
            }
            return false;
        }
        private boolean kingBattleMove() {
        	int originalplayerhealth = player.getPlayerhealth();
            int playeraccuracy = player.getACCURACY();
            int originalenemyhealth = king.getKinghealth();
            int enemyaccuracy = king.getKingaccuracy();
            king.setKinghealth(playerAttack(playeraccuracy, originalenemyhealth));
            player.setPlayerhealth(kingAttack(enemyaccuracy, originalplayerhealth));
            if (king.getKinghealth() == 0) {
                System.out.println("You have slain the king.");
                return true;
            } else if (player.getPlayerhealth() <= 0) {
                System.out.println("You have died");
            }
            return false;
			
		}
        private int kingAttack(int enemyaccuracy, int playerhealth) {
            int randomized = (int)(Math.random() * 100);
            boolean success = (randomized <= enemyaccuracy);
            if (success) {
                playerhealth -= 5;
                System.out.println(
                    "The king's attack was successful and you have lost 5 health points.  You now have " +
                    playerhealth + " health points");
            } else {
                System.out.println("The king's attack was unsuccessful and you still have " + playerhealth +
                    " health points");
            }
            return playerhealth;
        }
		// implementations of user commands:
    private boolean HasKey(Inventory playerInv, String string) {
            if (playerInv != null)
                return playerInv.contains(string);
            return false;
        }
        /**
         * Print out some help information.
         * Here we print some stupid, cryptic message and a list of the 
         * command words.
         */
    private void printHelp() {
        System.out.println(
            "You are trapped in a apartment complex.  Your goal is to kill the Barbarian King.");
        System.out.println("Your command words are:");
        parser.showCommands();
        System.out.println("This game was created by Justin Borromeo and Shon Czinner.");
    }
    private void goRoom(Command command) {
        if (!command.hasSecondWord()) {
            // if there is no second word, we don't know where to go...
            System.out.println("Go where?");
            return;
        }
        String direction = command.getSecondWord();
        // Try to leave current room.
        Room nextRoom = currentRoom.nextRoom(direction);
        if (nextRoom == null)
            System.out.println("There is no door!");
        else {
            currentRoom = nextRoom;
            System.out.println();
            System.out.println(currentRoom.longDescription());
        }
    }
    public boolean trollBattleMove() throws InterruptedException {
        int originalplayerhealth = player.getPlayerhealth();
        int playeraccuracy = player.getACCURACY();
        int originalenemyhealth = troll.getTrollhealth();
        int enemyaccuracy = troll.getTrollaccuracy();
        troll.setTrollhealth(playerAttack(playeraccuracy, originalenemyhealth));
        player.setPlayerhealth(trollAttack(enemyaccuracy, originalplayerhealth));
        if (troll.getTrollhealth() == 0) {
            System.out.println("Congratulations!  You have slain a troll.");
            return true;
        } else if (player.getPlayerhealth() <= 0) {
            System.out.println("You have died");
        }
        return false;
    }
    private int playerAttack(int playeraccuracy, int enemyhealth) {
        int randomized = (int)(Math.random() * 100);
        boolean success = (randomized <= playeraccuracy);
        if (success) {
            enemyhealth -= player.getATTACK();
            System.out.println(
                "Your attack was successful and the enemy has lost "+player.getATTACK()+" health points.  It now has " +
                enemyhealth + " health points");
        } else {
            System.out.println("Your attack was unsuccessful and the enemy still has " + enemyhealth +
                " health points");
        }
        return enemyhealth;
    }
    private int trollAttack(int enemyaccuracy, int playerhealth) {
        int randomized = (int)(Math.random() * 100);
        boolean success = (randomized <= enemyaccuracy);
        if (success) {
            playerhealth -= 5;
            System.out.println(
                "The troll's attack was successful and you have lost 5 health points.  You now have " +
                playerhealth + " health points");
        } else {
            System.out.println("The troll's attack was unsuccessful and you still have " + playerhealth +
                " health points");
        }
        return playerhealth;
    }
}