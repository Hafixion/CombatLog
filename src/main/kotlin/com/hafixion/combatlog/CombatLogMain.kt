package com.hafixion.combatlog

import com.serializer.java.Serializer
import hazae41.minecraft.kutils.bukkit.*
import hazae41.minecraft.kutils.get
import hazae41.minecraft.kutils.textOf
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

fun main() {
    print(textOf("&6pog").toPlainText())
}

class CombatLogMain : BukkitPlugin() {

    companion object {
        lateinit var instance: CombatLogMain
    }

    var logs = hashMapOf<UUID, ArrayList<CombatData>>()
    private val combat = hashMapOf<Player, CombatCache>()
    private var lastAttack: Long = 0
    private var delay = 200
    private var garbage = true

    override fun onEnable() {
        instance = this
        loadConfig()

        // Tags people
        listen<EntityDamageByEntityEvent> {
            if (it.entity is Player && it.damager is Player) {
                if (!it.entity.hasPermission("combatlog.exempt")) {
                    if (!combat.containsKey(it.entity))
                            (it.entity as Player).spigot()
                                .sendMessage(ChatMessageType.ACTION_BAR, textOf("&cCombat Tagged for ${delay/20} seconds!"))
                    else combat[it.entity as Player]?.task?.cancel()

                    combat[it.entity as Player] = CombatCache(it.damager.uniqueId, it.entity.uniqueId, object : BukkitRunnable() {
                        override fun run() {
                            combat.remove(it.entity)
                            (it.entity as Player).spigot().sendMessage(ChatMessageType.ACTION_BAR, textOf("&aEscaped from Combat!"))
                        }
                    }.runTaskLaterAsynchronously(this, delay.toLong()))
                    lastAttack = System.currentTimeMillis()
                }
            }
        }

        // Actual CombatLog part of this
        listen<PlayerQuitEvent> {
            if (!it.player.hasPermission("combatlog.exempt") && combat.containsKey(it.player)) {
                val cache = combat[it.player]
                if (cache != null && Bukkit.getOfflinePlayer(cache.attacker).isOnline) {
                    if (logs[cache.offender] == null) logs[cache.offender] = ArrayList()
                    logs[cache.offender]!!.add(CombatData(Point3D(it.player.location.x.roundToInt(), it.player.location.y.roundToInt(), it.player.location.z.roundToInt()), cache.attacker))

                    it.player.damage(Int.MAX_VALUE.toDouble(), Bukkit.getOfflinePlayer(cache.attacker).player)
                } else combat.remove(it.player)
            }
        }

        // Garbage Collection
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(
            this,
            object : BukkitRunnable() {
                override fun run() {
                    if (lastAttack - System.currentTimeMillis() >= 300000) combat.clear()
                }
            }
        , 300000, 60000)

        if (garbage) schedule(true, delay = 5, period = 1, TimeUnit.MINUTES) {
            if (System.currentTimeMillis() - lastAttack >= 30000) combat.clear()
        }

        command("combatlog") { args -> kotlin.run {
            if (args.isNotEmpty() && Bukkit.getOfflinePlayer(args[0]).hasPlayedBefore()) {
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                if (logs[uuid] != null) {
                    this.msg(textOf("§6§l${args[0]}'s Combatlogs §m§8─────────".toUpperCase()))
                    val dateFormat = SimpleDateFormat("KK:m a @ dd MMM yyyy")

                    for (combat in logs[Bukkit.getOfflinePlayer(args[0]).uniqueId]!!) {
                        val name = Bukkit.getOfflinePlayer(combat.attacker).name
                        this.msg(textOf("&e${args[0]} combat-logged from &6$name&e at &6${dateFormat.format(combat.time)}&e on " +
                                "&6${combat.location.x}, ${combat.location.y}, ${combat.location.z}").toPlainText())
                        this.msg("§m§8─")
                    }
                } else this.msg("§c${args[0]} has not combatlogged before.")
            } else this.msg("§cNo valid player specified.")
        }
        }
    }

    override fun onDisable() {
        config.set("data", Serializer.serialize(logs))
        config.save(dataFolder["config.yml"])
    }

    private fun loadConfig() {
        saveDefaultConfig()
        try {
            delay = config.getInt("delay") * 20
            garbage = config.getBoolean("trust-garbage-collection")

            if (config.contains("data"))
                logs = Serializer.deserialize(config.getString("data")) as HashMap<UUID, ArrayList<CombatData>>

            info("Config loaded successfully, cheers from Haf.")
        } catch (e: Exception) {
            warning("Invalid config, defaulting to default config.yml")
            warning("Check that all values are entered correctly, first option is an integer, second is a boolean")
        }
    }
}

class CombatCache (val attacker: UUID, val offender: UUID, val task: BukkitTask) : Serializable {
    override fun equals(other: Any?): Boolean {
        return if (other is CombatCache) {
            other.attacker == this.attacker && other.offender == this.offender
        } else false
    }

    override fun hashCode(): Int {
        var result = attacker.hashCode()
        result = 31 * result + offender.hashCode()
        result = 31 * result + task.hashCode()
        return result
    }
}
class CombatData(val location: Point3D, val attacker: UUID, val time: Long = System.currentTimeMillis()) : Serializable
class Point3D(var x: Int, var y: Int, var z: Int) : Serializable