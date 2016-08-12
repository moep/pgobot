package lol.moep.pgobot.util;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.AttackGymResponseOuterClass.AttackGymResponse;
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.StartGymBattleResponseOuterClass.StartGymBattleResponse.Result;
import POGOProtos.Networking.Responses.UseItemPotionResponseOuterClass.UseItemPotionResponse;
import POGOProtos.Networking.Responses.UseItemReviveResponseOuterClass.UseItemReviveResponse;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.PgoBot;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.StatsCounter;
import lol.moep.pgobot.util.logger.Logger;
import lol.moep.pgobot.util.logger.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Actions {

    private static final Logger LOGGER = LoggerFactory.getLoggerInstance();

    private static final String NICKNAME_TEMPLATE = readIvFormatFromProperties();

    private static final String PROPERTIES_FILE_NAME = "pgobot.properties";

    private static String readIvFormatFromProperties() {
        Properties properties = new Properties();
        String format = null;

        try {
            properties.load(new FileInputStream(PROPERTIES_FILE_NAME));
            format = properties.getProperty("nicknameTemplate");
        } catch (IOException e) {
            // Use default format if properties cannot be read
        } finally {
            return format == null ? "A{0} D{1} S{2}" : format;
        }
    }

    // TODO Version ohne statische Methode, die im Konstruktor go und sc bekommt

    public static void tradeInTrashMobs(final PokemonGo go, final StatsCounter statistics) {
        LOGGER.info("Verschicke Trashmobs");
        final List<PokemonIdOuterClass.PokemonId> banTypes = Arrays.asList(PokemonIdOuterClass.PokemonId.WEEDLE,
                PokemonIdOuterClass.PokemonId.PIDGEY, PokemonIdOuterClass.PokemonId.RATTATA,
                PokemonIdOuterClass.PokemonId.SPEAROW, PokemonIdOuterClass.PokemonId.ZUBAT,
                PokemonIdOuterClass.PokemonId.DROWZEE, PokemonIdOuterClass.PokemonId.DIGLETT,
                PokemonIdOuterClass.PokemonId.CATERPIE);

        tradeInMobs(go, p -> banTypes.contains(p.getPokemonId()) && p.getIvRatio() < 0.9d, statistics);
    }

    // TODO aus Datei lesen (die auf .gitignore steht!)
    public static void tradeInWeaklings(final PokemonGo go, final StatsCounter statistics) {
        LOGGER.info("Verschicke Schwächlinge");
        final Map<PokemonIdOuterClass.PokemonId, Integer> minCp = new HashMap<PokemonIdOuterClass.PokemonId, Integer>();
        minCp.put(PokemonIdOuterClass.PokemonId.SCYTHER, 1000); // Sichlor
        minCp.put(PokemonIdOuterClass.PokemonId.JYNX, 900); // Rossane
        minCp.put(PokemonIdOuterClass.PokemonId.HORSEA, 400); // Seeper
        minCp.put(PokemonIdOuterClass.PokemonId.PIDGEOTTO, 500); // Tauboga
        minCp.put(PokemonIdOuterClass.PokemonId.RATICATE, 500); // Rattikarl
        minCp.put(PokemonIdOuterClass.PokemonId.ODDISH, 640); // Myrapla
        minCp.put(PokemonIdOuterClass.PokemonId.KRABBY, 390);
        minCp.put(PokemonIdOuterClass.PokemonId.CHARMANDER, 400); // Glumanda
        minCp.put(PokemonIdOuterClass.PokemonId.GASTLY, 400); // Nebulak
        minCp.put(PokemonIdOuterClass.PokemonId.NIDORAN_FEMALE, 300);
        minCp.put(PokemonIdOuterClass.PokemonId.NIDORAN_MALE, 293);
        minCp.put(PokemonIdOuterClass.PokemonId.JIGGLYPUFF, 400);
        minCp.put(PokemonIdOuterClass.PokemonId.PARAS, 450);
        minCp.put(PokemonIdOuterClass.PokemonId.SHELLDER, 400); // Muschas
        minCp.put(PokemonIdOuterClass.PokemonId.HYPNO, 950);
        minCp.put(PokemonIdOuterClass.PokemonId.MAGIKARP, 90); // Karpador
        minCp.put(PokemonIdOuterClass.PokemonId.SQUIRTLE, 460); // Schiggy
        minCp.put(PokemonIdOuterClass.PokemonId.PSYDUCK, 540); // Enton
        minCp.put(PokemonIdOuterClass.PokemonId.GOLDEEN, 490); // Goldini
        minCp.put(PokemonIdOuterClass.PokemonId.VENONAT, 520); // Bluzuk
        minCp.put(PokemonIdOuterClass.PokemonId.EEVEE, 340); // Evoli
        minCp.put(PokemonIdOuterClass.PokemonId.BULBASAUR, 520); // Bisasam
        minCp.put(PokemonIdOuterClass.PokemonId.BELLSPROUT, 607); // Bellsprout

        tradeInMobs(go, p -> minCp.keySet().contains(p.getPokemonId()) && minCp.get(p.getPokemonId()) > p.getCp(), statistics);
    }

    public static void tradeInDuplicates(final PokemonGo go, final StatsCounter sc) {
        LOGGER.info("Entferne Duplikate");
        final List<Pokemon> pokemons;
        try {
            pokemons = go.getInventories().getPokebank().getPokemons();
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
            return;
        }

        final Map<PokemonId, Double> maxIv = new HashMap<>();
        for (Pokemon p : pokemons) {
            if (!maxIv.containsKey(p.getPokemonId())) {
                maxIv.put(p.getPokemonId(), p.getIvRatio());
            } else {
                if (maxIv.get(p.getPokemonId()) < p.getIvRatio()) {
                    maxIv.put(p.getPokemonId(), p.getIvRatio());
                }
            }
        }

        // behalte alle Duplikate mit mind. 90% IV vom besten Pokemon derselben Art
        tradeInMobs(go, p -> p.getIvRatio() < maxIv.get(p.getPokemonId()) * 0.95, sc);
    }

    /**
     * Verschickt Duplikate. Behält die besten 3 Pokemon, die mind. 95% der IV
     * Ratio des besten Pokemon haben. Favoriten werden immer behalten.
     *
     * @param go
     * @param sc
     */
    public static void tradeInDuplicates2(final PokemonGo go, final StatsCounter sc) {
        LOGGER.info("Entferne Duplikate");
        final List<Pokemon> pokemons;
        try {
            pokemons = go.getInventories().getPokebank().getPokemons();
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
            return;
        }

        final Map<PokemonId, Double> maxIv = new HashMap<>();
        final Map<PokemonId, List<Pokemon>> pokeMap = new HashMap<>();
        for (Pokemon p : pokemons) {
            if (!maxIv.containsKey(p.getPokemonId())) {
                maxIv.put(p.getPokemonId(), p.getIvRatio());
            } else {
                if (maxIv.get(p.getPokemonId()) < p.getIvRatio()) {
                    maxIv.put(p.getPokemonId(), p.getIvRatio());
                }
            }
            if (!pokeMap.containsKey(p.getPokemonId())) {
                pokeMap.put(p.getPokemonId(), new ArrayList<>());
            }
            // Favoriten gar nicht erst betrachten -> die x besten Nicht-Favoriten und die Favoriten werden behalten
            if (!p.isFavorite()) {
                pokeMap.get(p.getPokemonId()).add(p);
            }
        }
        for (List<Pokemon> pokeList : pokeMap.values()) {
            Collections.sort(pokeList, new PokemonComparator());
            Collections.reverse(pokeList);
        }
        // maximal 3 Nicht-Favoriten von der gleichen Sorte
        tradeInMobs(go, p ->
                        p.getIvRatio() < maxIv.get(p.getPokemonId()) * 0.95
                                || pokeMap.get(p.getPokemonId()).indexOf(p) > -1 &&
                                pokeMap.get(p.getPokemonId()).indexOf(p) >= 3
                , sc);
    }

    public static void tradeInMobs(final PokemonGo go, Predicate<? super Pokemon> predicate, final StatsCounter statistics) {
        final List<Pokemon> pokemons;
        try {
            pokemons = go.getInventories().getPokebank().getPokemons();
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
            return;
        }

        final Stream<Pokemon> pokemonStream = pokemons.stream();

        final Stream<Pokemon> filtered = pokemonStream.filter(predicate).filter(p -> !p.isFavorite());

        // TODO exception handling
        // TODO candy statistics
        filtered.forEach(p -> {
            LOGGER.info(Dictionary.getNameFromPokemonId(p.getPokemonId()) + " - " + p.getCp());
            try {
                transfer(p, statistics);
            } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
                LOGGER.logError(e);
            }
        });
    }

    private static void transfer(Pokemon p, final StatsCounter statistics) throws LoginFailedException, RemoteServerException {
        transfer(p, 3, statistics);
    }

    /**
     * @param p   pokemon
     * @param ttl time to live - max amount of retries
     * @throws LoginFailedException
     * @throws RemoteServerException
     */
    private static void transfer(Pokemon p, int ttl, final StatsCounter statistics) {
        if (ttl == 0) {
            LOGGER.info("Höre auf zu versuchen das Pokemon zu verschicken.");
            return;
        }

        ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = null;
        try {
            result = p.transferPokemon();
            LOGGER.info(result.getValueDescriptor());
            sleep(1000);
        } catch (Exception e) {
            result = null;
            LOGGER.logError(e);
        }

        if (result == null || "FAILED".equals(String.valueOf(result.getValueDescriptor()))) {
            LOGGER.info("Retry");
            transfer(p, ttl - 1, statistics);
        }
    }

    /**
     * Ungetestet! Es wird keine Haftung übernommen :D
     *
     * @param go
     * @param gym
     * @param statistics
     */
    public static void attackWithBestTeam(final PokemonGo go, final Gym gym, final StatsCounter statistics) {
        try {
            Stream<Pokemon> sorted = go.getInventories().getPokebank().getPokemons().stream()
                    .sorted((a, b) -> Integer.compare(b.getCp(), a.getCp()))
                    // Annahme: es können immer maximal 6 Pokemon angeifen
                    .limit(6);
            final Pokemon[] battleTeam = sorted.toArray(Pokemon[]::new);
            final Battle battle = gym.battle(battleTeam);

            // Ist das alles? Gibt ja auch andere Methoden...
            final Result result = battle.start();

            switch (result) {
                case SUCCESS:
                    LOGGER.info("Kampf gestartet!");
                    break; // super!
                default:
                    LOGGER.red("Kampf konnte nicht gestartet werden!");
                    return; // nicht so toll!
            }

            AttackGymResponse attack;
            while (!battle.isConcluded()) {
                sleep(1000);
                attack = battle.attack(0);
                switch (attack.getResult()) {
                    case SUCCESS:
                        break;
                    default:
                        break;
                }
            }

            switch (battle.getOutcome()) {
                case VICTORY:
                    LOGGER.green("Gewonnen!");
                    break;
                default:
                    LOGGER.red("Verloren!");
                    break;
            }

            for (Pokemon p : battleTeam) {
                int tries = 3;
                while (p.isFainted()) {
                    if (tries <= 0) {
                        continue;
                    }
                    UseItemReviveResponse.Result reviveResult = p.revive();
                    switch (reviveResult) {
                        case SUCCESS:
                            break;
                        default:
                            tries--;
                            break;
                    }
                }
                tries = 3;
                while (p.isInjured()) {
                    UseItemPotionResponse.Result healResult = p.heal();
                    switch (healResult) {
                        case SUCCESS:
                            break;
                        default:
                            tries--;
                            break;
                    }
                }
            }
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
        }
    }

    public static void tradeInTrashItems(final PokemonGo go, final StatsCounter sc) {
        LOGGER.info("Recycle Trash Items");

        final Map<ItemId, Integer> itemLimits = new HashMap<ItemId, Integer>();
        itemLimits.put(ItemId.ITEM_POTION, 0);
        itemLimits.put(ItemId.ITEM_SUPER_POTION, 0);
        itemLimits.put(ItemId.ITEM_HYPER_POTION, 20);
        itemLimits.put(ItemId.ITEM_REVIVE, 10);

        final ItemBag itemBag;
        try {
            itemBag = go.getInventories().getItemBag();
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
            return;
        }

        Item item;
        for (Map.Entry<ItemId, Integer> entry : itemLimits.entrySet()) {
            item = itemBag.getItem(entry.getKey());
            Integer limit = entry.getValue();
            if (item != null && item.getCount() > limit) {
                LOGGER.info(String.format("Reduziere item %s auf %d Stück", item.getItemId().name(), limit));
                removeItems(go, item, item.getCount() - limit, sc);
            }
        }
    }

    public static void renameToIv(final PokemonGo go, final StatsCounter sc) throws LoginFailedException, RemoteServerException {
        LOGGER.info("Benenne Pokémon um...");
        go.getInventories().getPokebank().getPokemons().stream()
                .filter(p -> p.getNickname() == null || p.getNickname().isEmpty())
                .forEach(p -> {
                    try {
                        String ivNickname = MessageFormat.format(NICKNAME_TEMPLATE, p.getIndividualAttack(), p.getIndividualDefense(), p.getIndividualStamina());
                        LOGGER.info(Dictionary.getNameFromPokemonId(p.getPokemonId()) + " -> " + ivNickname);
                        p.renamePokemon(ivNickname);

                        sleep(1000);

                        if (p.getIndividualAttack() == 15 && p.getIndividualStamina() == 15 && p.getIndividualStamina() == 15) {
                            p.setFavoritePokemon(true);
                            sleep(500);
                        }
                    } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
                        LOGGER.logError("Fehler beim Umbenennen: ", e);
                    }
                });
    }


    private static void removeItems(final PokemonGo go, Item item, Integer amount, final StatsCounter sc) {
        // aktuell keine retries, weil ich mir nicht sicher bin, ob die items nicht auch bei FAILED entfernt werden
        removeItems(go, item, amount, 1, sc);
    }

    private static void removeItems(final PokemonGo go, Item item, Integer amount, final int ttl, final StatsCounter sc) {
        if (ttl == 0) {
            return;
        }

        try {
            final ItemBag itemBag = go.getInventories().getItemBag();

            RecycleInventoryItemResponse.Result result = itemBag.removeItem(item.getItemId(), amount);
            LOGGER.info(result.getValueDescriptor());
            sleep(1000);
            if ("FAILED".equals(String.valueOf(result.getValueDescriptor()))) {
                LOGGER.info("Retry");
                removeItems(go, item, amount, ttl - 1, sc);
            }
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            LOGGER.logError(e);
        }
    }

    public static void sleep(int timeMilis) {
        try {
            Thread.sleep(timeMilis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
