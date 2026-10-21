package org.Cry_Legende.sHRelay.core;

/**
 * Protocole du canal de plugin messaging entre le serveur Paper/Purpur et le proxy.
 * <p>
 * Trame : UTF(mode) puis UTF(commande).
 * CONSOLE : la commande est executee par la console du proxy.
 * PLAYER  : la commande est executee au nom du joueur qui a transporte le message.
 * <p>
 * Le canal porte un namespace propre : le canal "BungeeCord" historique ne connait
 * qu'une liste fixe de sous-canaux et n'accepte pas l'execution de commandes.
 */
public final class RelayProtocol {

    public static final String NAMESPACE = "shrelay";
    public static final String NAME = "main";
    public static final String CHANNEL = NAMESPACE + ":" + NAME;

    public static final String MODE_CONSOLE = "CONSOLE";
    public static final String MODE_PLAYER = "PLAYER";

    private RelayProtocol() {
    }
}
