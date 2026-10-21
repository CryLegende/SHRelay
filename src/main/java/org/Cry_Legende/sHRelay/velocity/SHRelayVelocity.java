package org.Cry_Legende.sHRelay.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.Cry_Legende.sHRelay.core.RelayProtocol;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cote proxy : recoit les trames de SHRelay et execute la commande, soit en
 * console, soit au nom du joueur qui a porte le message.
 */
@Plugin(
    id = "shrelay",
    name = "SHRelay",
    version = "1.0",
    description = "Relais de commandes depuis les serveurs de jeu",
    authors = {"Cry_Legende"}
)
public final class SHRelayVelocity {

    private static final MinecraftChannelIdentifier CHANNEL =
        MinecraftChannelIdentifier.create(RelayProtocol.NAMESPACE, RelayProtocol.NAME);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataFolder;

    private Set<String> allowedCommands = Set.of();
    private boolean logRelayed = true;

    @Inject
    public SHRelayVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadConfig();
        proxy.getChannelRegistrar().register(CHANNEL);
        logger.info("SHRelay ecoute sur {} ({}).", CHANNEL.getId(),
            allowedCommands.isEmpty() ? "toutes commandes autorisees"
                : allowedCommands.size() + " commande(s) en liste blanche");
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Path file = dataFolder.resolve("config.yml");
        try {
            Files.createDirectories(dataFolder);
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Impossible de preparer config.yml", e);
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> root = (Map<String, Object>) map;
                Object log = root.get("log-relayed");
                if (log instanceof Boolean b) {
                    logRelayed = b;
                }
                Object allowed = root.get("allowed-commands");
                if (allowed instanceof List<?> list) {
                    Set<String> set = new HashSet<>();
                    for (Object o : list) {
                        if (o != null) {
                            set.add(String.valueOf(o).trim().toLowerCase(Locale.ROOT));
                        }
                    }
                    allowedCommands = Set.copyOf(set);
                }
            }
        } catch (Exception e) {
            logger.error("config.yml illisible, liste blanche desactivee", e);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        // Le message s'arrete ici : il ne doit jamais repartir vers un client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection backend)) {
            return;
        }

        String mode;
        String command;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            mode = in.readUTF();
            command = in.readUTF();
        } catch (Exception e) {
            logger.warn("Trame SHRelay illisible depuis {}", backend.getServerInfo().getName(), e);
            return;
        }

        String origin = backend.getServerInfo().getName();
        if (!isAllowed(command)) {
            logger.warn("Commande refusee (hors liste blanche) depuis {} : {}", origin, command);
            return;
        }

        if (RelayProtocol.MODE_PLAYER.equals(mode)) {
            Player player = backend.getPlayer();
            if (logRelayed) {
                logger.info("[{}] {} > {}", origin, player.getUsername(), command);
            }
            proxy.getCommandManager().executeAsync(player, command);
        } else if (RelayProtocol.MODE_CONSOLE.equals(mode)) {
            if (logRelayed) {
                logger.info("[{}] console > {}", origin, command);
            }
            proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
        } else {
            logger.warn("Mode SHRelay inconnu \"{}\" depuis {}", mode, origin);
        }
    }

    /**
     * Liste blanche vide = tout autorise (comportement de CommandBrothers).
     */
    private boolean isAllowed(String command) {
        if (allowedCommands.isEmpty()) {
            return true;
        }
        String root = command.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return allowedCommands.contains(root);
    }
}
