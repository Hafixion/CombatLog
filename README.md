# CombatLog
***
A simple combat logger for the masses.
Upon getting hit by another player, a player gets combat tagged for a number of seconds specified in the config.yml. If a player leaves the game while they are combat tagged, they automatically get killed.

You can also check a player's history of combat logging using /combatlog <player>
  
### Building & Compiling
***
Easy enough to do, type `gradle shadowJar` into a terminal inside the directory, and use the shadowed jar.

### config.yml
***
```yaml
# In seconds, the amount of time a player is tagged for after they get hit by another player.
# Integer values **ONLY**
delay: 10

# Clears in-combat null objects and players each minute, useful. (DEBUG ONLY)
# Boolean values **ONLY**
trust-garbage-collection: true

# Cheers from Haf
```
