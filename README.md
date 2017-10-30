# Pacman-multiAgent
A multi agent platform simulating an IA running a pacman-like game.

## Architecture
Our platform runs a Multi Agent system wich involves differents agents asynchronously in order to simulate a complete environment for a traveler (the pacman) to move and survive through the gid using artificial intelligence.

![alt text](https://github.com/AlexisDrch/Pacman-multiAgent/blob/master/Untitled%20Diagram.jpg)


### Agents
#### Engine
Engine agent can be considered as a ticker that will trigger continously the environment to act. It sets up a bunch of setting (subscription, initialisation) before starting the ticker.

#### Environment
Environment agent takes care of linking all the agents involves in the game. Its main role consist in reacting to each tick so it can ask all the entities to move accordingly. Each of the entities move corresponds to an update in the grid encapsulated in environment agent.

#### Monsters
Monsters agents are independant and random steppers in the grids. They react to the tick sent by environment and responds with a random move (new position in the grid) regarding their initial position.

#### Traveler
Traveler is the player. Here its the entity that moves across the grid and try to survive from the monsters. It moves (new position) regarding a bunch of analyses made by ArtificialIntelligence agent. If a traveler cross the same step as a monster, he dies.

#### ArtificialIntelligence (AI)
AI agents are called by the traveler. This agent uses different analyser agents to analyse the environment, compares their response and find the best move for the traveler.

### AnalyserAgent
Thoses agents propose differents move to the AI agent regarding the environment's state. pour commit
"# MultiAgentProgramming" 
