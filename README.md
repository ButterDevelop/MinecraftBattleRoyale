# Minecraft Battle Royale Spigot Plugin

Battle Royale Plugin is a Minecraft Spigot plugin that brings a fast-paced, last-team-standing game mode to your server. With features like dynamic world creation, team selection, kit selection, dynamic world border, and in-game statistics displayed on the TAB list and via commands, this plugin offers an immersive and customizable Battle Royale experience.

## Features

- **Lobby & Game Worlds**  
  - Wait in a dedicated lobby until the game starts.
  - Automatically create and reset arena, Nether, and End worlds for each game round.
  - Configurable world settings (spawn protection, world border, etc.).

- **Team Selection**  
  - Choose your team via an in-game GUI opened by a compass.
  - Teams are color-coded and managed via Scoreboard Teams.
  - Automatically assign players without a team to balance team sizes.

- **Kit Selection**  
  - Choose a kit (a predefined set of items) from an in-game GUI.
  - Kits are saved in an external file (`kits.yml`), ensuring persistence between server restarts.
  - Receive your selected kit automatically at the start of the game.

- **In-Game Statistics**  
  - Display player statistics (Kills, Deaths, Wins, Games Played) in the TAB list in a neatly formatted, colorful table.
  - Use the `/stats` command to view detailed statistics in chat.
  - A temporary Scoreboard is displayed for a few seconds when `/stats` is executed.

- **Vote to Start**  
  - Vote for the game start using an in-game item (e.g., an Amethyst Shard) that triggers the `/votestart` command.
  
- **Vote to End**  
  - Vote for the game end using the `/voteend` command.

- **Dynamic Arena Mechanics**  
  - World borders that shrink over time.
  - Environmental hazards such as rising air or lava to force encounters.
  - Firework effects for celebration when a team wins.

## Installation

1. **Requirements:**
   - Spigot (or Paper) server running Minecraft 1.21.4+.
   - Java 21 or later.

2. **Setup:**
   - Clone or download this repository.
   - Import the project into your favorite IDE (e.g., IntelliJ IDEA).
   - Build the project using Maven/Gradle (or your preferred build tool) to generate the plugin JAR file.
   - Place the generated JAR file into your server's `plugins` folder.
   - Start or restart your server.
   - On first run, the plugin will create default configuration files (`config.yml` and `kits.yml`) in the plugin's data folder.

## Usage

### In-Game Items
- **Team Selector (Compass):**  
  Right-click the compass to open the team selection GUI.

- **Stats Viewer (Paper):**  
  Right-click the paper to execute `/stats`, which displays your statistics in chat and shows a temporary Scoreboard.

- **Vote Start (Amethyst Shard):**  
  Right-click the amethyst shard to vote for starting the game via `/votestart`.

- **Kit Selector (Chest):**  
  A separate chest item can be used to open the kit selection GUI and choose your kit.

These items are given automatically when you join the lobby.

### Commands

- `/teamselect`  
  Opens the team selection GUI.

- `/stats`  
  Displays the statistics of all players in the chat in a tabular, colorful format, and shows a temporary Scoreboard for a short duration.

- `/votestart`  
  Registers your vote to start the game.

- `/voteend`  
  Registers your vote to end the game.

- `/startgame`  
  Starts your game (needs admin rights).

- `/endgame`  
  Ends your game (needs admin rights).

- `/kitselect`  
  Opens the kit selection GUI so you can choose your kit.  
  *(Alternatively, this functionality is integrated with the plugin items.)*

## Configuration

All major settings can be adjusted via the configuration files:

- **config.yml**  
  - Worlds (lobby, arena, Nether, End)
  - World border settings (minimum size, maximum size, shrink times)
  - Timings (seconds before/after the arena)
  - Teams (names, colors, etc.)

- **kits.yml**  
  - Contains the kit selection for each player.
  - Player kit choices are saved here and persist between server restarts.

Feel free to modify these files to suit your server's needs.

## Contributing

Contributions are welcome! Please follow these guidelines:
- Fork the repository and create a new branch for your feature or bug fix.
- Ensure your code follows the project's style conventions.
- Test your changes thoroughly.
- Submit a pull request with a detailed description of your changes.

## License

This project is licensed under the [MIT License](LICENSE).

## Acknowledgements

- Thanks to the Spigot community for providing great APIs and support.
- Inspired by various Battle Royale game modes and server plugins.

---

Enjoy the Battle Royale experience on your server!
