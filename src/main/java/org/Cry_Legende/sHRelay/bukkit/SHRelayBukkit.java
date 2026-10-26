package org.Cry_Legende.sHRelay.bukkit;

import org.Cry_Legende.sHRelay.core.RelayProtocol;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cote serveur de jeu : expose les commandes de relais et pousse la trame vers
 * le proxy via la connexion d'un joueur.
 * <p>
 * Remplace CommandBrothers (/bungeecommand et /bungeeplayer), dont les alias
 * sont conserves pour ne rien casser dans les configs existantes.
 */
public final class SHRelayBukkit extends JavaPlugin implements Listener, CommandExecutor {

    /**
     * Trames en attente quand aucun joueur n'est connecte pour les porter.
     */
    private final Deque<byte[]> queue = new ArrayDeque<>();
    private boolean enableQueue;
    private int queueLimit;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.enableQueue = getConfig().getBoolean("enable-queue", true);
        // au moins 1 : avec 0 ou une valeur négative, la file vide lèverait une exception à chaque commande
        this.queueLimit = Math.max(1, getConfig().getInt("queue-limit", 100));

        getServer().getMessenger().registerOutgoingPluginChannel(this, RelayProtocol.CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);

        for (String name : new String[]{"proxycommand", "proxyplayer"}) {
            var command = getCommand(name);
            if (command != null) {
                command.setExecutor(this);
            }
        }
        getLogger().info("SHRelay actif sur le canal " + RelayProtocol.CHANNEL
            + (enableQueue ? " (file d'attente activee)" : ""));
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    private static void send(CommandSender to, String message) {
        to.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean asPlayer = command.getName().equalsIgnoreCase("proxyplayer");
        String permission = asPlayer ? "shrelay.player" : "shrelay.console";

        if (!sender.hasPermission(permission)) {
            send(sender, "&8&l« &c&lSH &8&l» &7Commande inconnue, essayer autre chose.");
            return true;
        }
        if (args.length == 0) {
            send(sender, "&8&l« &b&lSH &8&l» &7Syntaxe : &b/" + label + " <commande proxy>");
            return true;
        }

        String proxyCommand = String.join(" ", args);
        if (proxyCommand.startsWith("/")) {
            proxyCommand = proxyCommand.substring(1);
        }

        boolean sent;
        if (asPlayer) {
            // Le proxy execute au nom du joueur qui porte le message : il faut
            // donc que ce soit ce joueur-la qui l'envoie.
            if (!(sender instanceof Player player)) {
                send(sender, "&8&l« &c&lSH &8&l» &7Cette commande doit etre lancee par un joueur.");
                return true;
            }
            sent = dispatch(player, RelayProtocol.MODE_PLAYER, proxyCommand);
        } else {
            sent = dispatch(null, RelayProtocol.MODE_CONSOLE, proxyCommand);
        }
        if (!sent) {
            return true; // déjà signalé en console : on ne confirme pas une transmission qui n'a pas eu lieu
        }

        send(sender, "&8&l« &b&lSH &8&l» &7Commande transmise au proxy : &b" + proxyCommand);
        return true;
    }

    /**
     * @return faux si la trame n'a pu être ni envoyée ni mise en file
     */
    private boolean dispatch(Player carrier, String mode, String proxyCommand) {
        byte[] payload = encode(mode, proxyCommand);
        if (payload == null) {
            return false;
        }

        Player sender = carrier;
        if (sender == null) {
            sender = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        }

        if (sender == null) {
            if (enableQueue) {
                if (queue.size() >= queueLimit) {
                    queue.removeFirst();
                    getLogger().warning("File de relais pleine, la plus ancienne commande est abandonnee.");
                }
                queue.addLast(payload);
                getLogger().info("Aucun joueur connecte : commande mise en file (" + queue.size() + ").");
            } else {
                getLogger().warning("Aucun joueur connecte, commande perdue : " + proxyCommand);
            }
            return true;
        }
        try {
            sender.sendPluginMessage(this, RelayProtocol.CHANNEL, payload);
        } catch (RuntimeException e) {
            // trame trop grande pour le canal de plugin : refus propre plutôt qu'une erreur interne pour l'émetteur
            getLogger().warning("Commande de relais non transmise (" + e.getMessage() + ") : " + proxyCommand);
            return false;
        }
        return true;
    }

    private byte[] encode(String mode, String proxyCommand) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF(mode);
            out.writeUTF(proxyCommand);
        } catch (Exception e) {
            getLogger().severe("Impossible d'encoder la commande de relais : " + e.getMessage());
            return null;
        }
        return buffer.toByteArray();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (queue.isEmpty()) {
            return;
        }
        // Un tick de marge : le canal doit etre pret cote joueur.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) {
                return;
            }
            int sent = 0;
            while (!queue.isEmpty()) {
                player.sendPluginMessage(this, RelayProtocol.CHANNEL, queue.removeFirst());
                sent++;
            }
            if (sent > 0) {
                getLogger().info(sent + " commande(s) en attente transmise(s) au proxy.");
            }
        }, 20L);
    }
}
