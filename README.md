# YOH-DropMagnet 🧲

**YOH-DropMagnet** est un puissant plugin Minecraft (Spigot/Paper) d'aimant à items pensé pour la performance, la sécurité anti-duplication et la traçabilité.

Compatible de la version **1.20 à la 26.2** (Arclight, Paper, Spigot, Bukkit).

## 🧭 Concept & Modes d'Utilisation

Ce plugin ajoute un aimant (visuellement une Shulker Box, personnalisable) capable d'aspirer les items au sol selon deux modes :

1. **Mode Portatif (Inventaire) :** 
   Le joueur garde l'aimant dans son inventaire. S'il est activé, l'aimant aspire de manière passive tous les items autour du joueur (rayon paramétrable).
   
2. **Mode Fixe (Bloc posé) :** 
   Le joueur pose l'aimant au sol. Il aspirera automatiquement les items dans son rayon d'action ou dans tout son chunk de manière autonome, ce qui est idéal pour l'automatisation et les usines !

## ✨ Fonctionnalités Principales

- **Inventaire virtuel intégré :** Chaque aimant possède son propre inventaire de 27 slots pour stocker les items aspirés.
- **Filtres (Whitelist) :** Une interface intuitive permet de définir jusqu'à 9 filtres d'items. Si le filtre est vide, l'aimant aspire tout.
- **Hologrammes & Particules :** Un hologramme au-dessus de l'aimant posé indique son état (Actif/Inactif) et son remplissage (%). Des particules délimitent sa zone d'action.
- **Alertes de Saturation :** Notification visuelle (ActionBar, Title, Chat ou BossBar) lorsque l'aimant est plein.

## 🛡️ Sécurité & Anti-Duplication

Ce plugin a été conçu avec une approche "zéro faille" :

- **Force-Close Anti-FakeLag :** Si le bloc de l'aimant est détruit (explosion, joueur, piston) pendant qu'un utilisateur consulte son interface, celle-ci est fermée instantanément côté serveur. Cela rend toute duplication via des logiciels de *FakeLag* ou des latences réseau mathématiquement impossible.
- **Lock Multi-Joueurs :** L'interface d'un aimant posé au sol ne peut être consultée que par un seul joueur à la fois pour éviter les conflits d'items.
- **Anti-Inception :** Impossible de ranger un aimant à l'intérieur d'un autre aimant.
- **Support WorldGuard :** L'aimant n'aspirera aucun objet dans une zone protégée s'il n'en a pas les droits.
- **Sauvegarde Thread-Safe :** Le système asynchrone est totalement protégé contre la corruption de fichiers lors d'écritures massives.

## 📜 Système de Logs & Restitution

**Chaque aimant possède un identifiant unique.** Toutes les actions sont tracées en direct : les items aspirés, les retraits manuels, et les ajouts manuels.

- **Interface Administrateur :** La commande `/magnetlog <ID>` ouvre une interface graphique.
- **Restitution en 1 clic :** L'équipe de modération peut visualiser l'historique et cliquer sur un item du log pour le redonner directement dans l'inventaire du joueur spolié.

## 🛠️ Configuration (ItemAdder & Oraxen)

Le fichier `config.yml` permet une personnalisation totale :
- Remplacement du `material` et définition d'un **`custom-model-data`** pour utiliser vos propres textures/modèles 3D via **ItemAdder** ou **Oraxen**.
- Réglage indépendant des rayons d'aspiration (joueur vs posé).
- Fréquence de calcul (en ticks) pour optimiser les performances serveur.
- Configuration des protections (résistance aux explosions, pistons).

## ⚙️ Commandes & Permissions

Le plugin utilise les permissions classiques (compatibilité totale avec LuckPerms) :

|                Commande                        |       Permission      |             Description                    |
|----------------------------------------|---------------------|-----------------------------------|
| `/magnet give <joueur> [quantité]` | `yoh.magnet.give` | Donner un aimant à un joueur. |
| `/magnetlog <ID>`                           | `yoh.magnet.log`   | Ouvrir les logs d'un aimant.      |
|----------------------------------------|---------------------|-----------------------------------|
