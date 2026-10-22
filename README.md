<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHRelay

*Relais de commandes vers le proxy (remplace CommandBrothers)*

<img src="https://img.shields.io/badge/Paper%20%C2%B7%20Bungee%20%C2%B7%20Velocity-26.3-f2a900?style=for-the-badge" alt="Paper · Bungee · Velocity 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale%20%C2%B7%20Lobby%20%C2%B7%20Login%20%C2%B7%20Proxy-3aa655?style=for-the-badge" alt="SkyHand Opale · Lobby · Login · Proxy">

</div>

---

## Présentation

Relais de commandes vers le proxy (remplace CommandBrothers)

Utilisé sur : **Opale · Lobby · Login · Proxy**.

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/proxycommand` | `/bungeecommand`, `/bungeec`, `/bungeee`, `/pcmd` | Execute une commande sur la console du proxy | `shrelay.console` |
| `/proxyplayer` | `/bungeeplayer`, `/bungeep`, `/pplayer` | Execute une commande sur le proxy en tant que joueur | `shrelay.player` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shrelay.console` | Relayer une commande vers la console du proxy | `op` |
| `shrelay.player` | Relayer une commande executee en son propre nom sur le proxy | `op` |

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHRelay/
└── <a href="src/main/resources/config.yml">config.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
