import java.util.*;
import javax.sound.sampled.*;
import java.io.*;

class SoundManager {
    private static Map<String, Clip> soundCache = new HashMap<>();
    
    public static void playSound(String soundName) {
        try {
            if (!soundCache.containsKey(soundName)) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(generateTone(soundName))
                );
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                soundCache.put(soundName, clip);
            }
            
            Clip clip = soundCache.get(soundName);
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) {
            System.err.println("Sound playback error: " + e.getMessage());
        }
    }
    
    // Generate simple tones for different actions since we can't include actual sound files
    private static byte[] generateTone(String soundName) {
        int sampleRate = 44100;
        float duration = 0.3f; // seconds
        
        // Different frequencies for different actions
        float frequency = switch (soundName) {
            case "move" -> 440.0f;     // A4 note
            case "pickup" -> 660.0f;    // E5 note
            case "victory" -> 880.0f;   // A5 note
            default -> 440.0f;
        };
        
        // Longer duration for victory sound
        if (soundName.equals("victory")) {
            duration = 1.0f;
        }
        
        int numSamples = (int) (duration * sampleRate);
        byte[] buffer = new byte[2 * numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            short sample = (short) (32767.0 * Math.sin(angle));
            buffer[2*i] = (byte) (sample & 0xFF);
            buffer[2*i+1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return buffer;
    }
    
    public static void cleanup() {
        for (Clip clip : soundCache.values()) {
            clip.close();
        }
        soundCache.clear();
    }
}

class Location {
    private String name;
    private String description;
    private List<Location> connections;
    private List<String> items;
    private boolean isEndLocation;

    public Location(String name, String description, boolean isEndLocation) {
        this.name = name;
        this.description = description;
        this.isEndLocation = isEndLocation;
        this.connections = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public void addConnection(Location location) {
        connections.add(location);
    }

    public void addItem(String item) {
        items.add(item);
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Location> getConnections() { return connections; }
    public List<String> getItems() { return items; }
    public boolean isEndLocation() { return isEndLocation; }
}

class Player {
    private List<String> inventory;

    public Player() {
        inventory = new ArrayList<>();
    }

    public void addItem(String item) {
        inventory.add(item);
        System.out.println("You picked up: " + item);
        SoundManager.playSound("pickup");
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public void showInventory() {
        System.out.println("\nInventory:");
        if (inventory.isEmpty()) {
            System.out.println("Empty");
            return;
        }
        for (String item : inventory) {
            System.out.println("- " + item);
        }
    }
}

class Game {
    private Location currentLocation;
    private Player player;
    private Scanner scanner;

    public Game(Location startLocation) {
        this.currentLocation = startLocation;
        this.player = new Player();
        this.scanner = new Scanner(System.in);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void displayLocation() {
        clearScreen();
        System.out.println("\n=== " + currentLocation.getName() + " ===");
        System.out.println(currentLocation.getDescription());

        if (!currentLocation.getItems().isEmpty()) {
            System.out.println("\nYou see:");
            for (String item : currentLocation.getItems()) {
                System.out.println("- " + item);
            }
        }

        System.out.println("\nPossible exits:");
        List<Location> connections = currentLocation.getConnections();
        for (int i = 0; i < connections.size(); i++) {
            System.out.println((i + 1) + ". Go to " + connections.get(i).getName());
        }
    }

    private void handleItems() {
        if (!currentLocation.getItems().isEmpty()) {
            System.out.print("\nWould you like to pick up any items? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("y")) {
                List<String> items = currentLocation.getItems();
                for (String item : new ArrayList<>(items)) {
                    player.addItem(item);
                }
                items.clear();
            }
        }
    }

    private void exploreLocation() {
        if (currentLocation.isEndLocation()) {
            displayLocation();
            System.out.println("\nCongratulations! You've reached the end of your adventure!");
            SoundManager.playSound("victory");
            return;
        }

        while (true) {
            displayLocation();
            handleItems();

            System.out.println("\nWhat would you like to do?");
            System.out.println("1-" + currentLocation.getConnections().size() + ". Move to a new location");
            System.out.println("i. Check inventory");
            System.out.println("q. Quit game");

            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("q")) {
                System.out.println("Thanks for playing!");
                return;
            }

            if (choice.equals("i")) {
                player.showInventory();
                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();
                continue;
            }

            try {
                int index = Integer.parseInt(choice) - 1;
                List<Location> connections = currentLocation.getConnections();
                if (index >= 0 && index < connections.size()) {
                    currentLocation = connections.get(index);
                    SoundManager.playSound("move");
                    exploreLocation(); // Recursive call
                    return;
                }
            } catch (NumberFormatException e) {
                // Invalid input, will show menu again
            }

            System.out.print("Invalid choice. Press Enter to continue...");
            scanner.nextLine();
        }
    }

    public void start() {
        System.out.println("Welcome to the Adventure Game!");
        System.out.print("Press Enter to begin...");
        scanner.nextLine();
        exploreLocation();
        scanner.close();
        SoundManager.cleanup();
    }
}

public class AdventureGame {
    public static void main(String[] args) {
        // Create locations
        Location cave = new Location("Cave", 
            "You're in a dimly lit cave. Water drips from the ceiling.", false);
        Location forest = new Location("Forest", 
            "You're in a dense forest. Sunlight filters through the leaves.", false);
        Location ruins = new Location("Ancient Ruins", 
            "You stand before crumbling stone walls covered in mysterious symbols.", false);
        Location temple = new Location("Temple", 
            "You've reached a magnificent temple atop a mountain.", true);

        // Add connections
        cave.addConnection(forest);
        forest.addConnection(cave);
        forest.addConnection(ruins);
        ruins.addConnection(forest);
        ruins.addConnection(temple);

        // Add items
        cave.addItem("Torch");
        forest.addItem("Magic Stone");
        ruins.addItem("Ancient Key");

        // Create and start game
        Game game = new Game(cave);
        game.start();
    }
}
