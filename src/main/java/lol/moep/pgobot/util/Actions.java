package lol.moep.pgobot.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.AttackGymResponseOuterClass.AttackGymResponse;
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.StartGymBattleResponseOuterClass.StartGymBattleResponse.Result;
import POGOProtos.Networking.Responses.UseItemPotionResponseOuterClass.UseItemPotionResponse;
import POGOProtos.Networking.Responses.UseItemReviveResponseOuterClass.UseItemReviveResponse;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.StatsCounter;

public class Actions {
	
	// TODO Version ohne statische Methode, die im Konstruktor go und sc bekommt

	public static void tradeInTrashMobs(final PokemonGo go, final StatsCounter statistics) {
		statistics.logMessage("Verschicke Trashmobs");
		final List<PokemonIdOuterClass.PokemonId> banTypes = Arrays.asList(PokemonIdOuterClass.PokemonId.WEEDLE,
				PokemonIdOuterClass.PokemonId.PIDGEY, PokemonIdOuterClass.PokemonId.RATTATA,
				PokemonIdOuterClass.PokemonId.SPEAROW, PokemonIdOuterClass.PokemonId.ZUBAT,
				PokemonIdOuterClass.PokemonId.DROWZEE, PokemonIdOuterClass.PokemonId.DIGLETT,
				PokemonIdOuterClass.PokemonId.CATERPIE);

		tradeInMobs(go, p -> banTypes.contains(p.getPokemonId()), statistics);
	}

	// TODO aus Datei lesen (die auf .gitignore steht!)
	public static void tradeInWeaklings(final PokemonGo go, final StatsCounter statistics) {
		statistics.logMessage("Verschicke Schwächlinge");
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
		final List<Pokemon> pokemons;
		try {
			pokemons= go.getInventories().getPokebank().getPokemons();
		} catch (LoginFailedException | RemoteServerException e) {
			sc.logError(e);
			return;
		}
		
		final Map<PokemonId, Integer> maxCp = new HashMap<>();
		for (Pokemon p: pokemons) {
			if (!maxCp.containsKey(p.getPokemonId())) {
				maxCp.put(p.getPokemonId(), p.getCp());
			} else {
				if (maxCp.get(p.getPokemonId()) < p.getCp()) {
					maxCp.put(p.getPokemonId(), p.getCp());
				}
			}
		}
		
		// behalte alle Duplikate mit mind. 90% cp vom besten Pokemon derselben Art
		tradeInMobs(go, p -> p.getCp() < maxCp.get(p.getPokemonId()) * 0.9, sc);
	}
	
	public static void tradeInMobs(final PokemonGo go, Predicate<? super Pokemon> predicate, final StatsCounter statistics) {
		final List<Pokemon> pokemons;
		try {
			pokemons= go.getInventories().getPokebank().getPokemons();
		} catch (LoginFailedException | RemoteServerException e) {
			statistics.logError(e);
			return;
		}
		
		final Stream<Pokemon> pokemonStream = pokemons.stream();

		final Stream<Pokemon> filtered = pokemonStream.filter(predicate).filter(p -> !p.isFavorite());

		// TODO exception handling
		// TODO candy statistics
		filtered.forEach(p -> {
			statistics.logMessage(Dictionary.getNameFromPokemonId(p.getPokemonId()) + " - " + p.getCp());
			try {
				transfer(p, statistics);
			} catch (LoginFailedException | RemoteServerException e) {
				statistics.logError(e);
			}
		});
	}

	private static void transfer(Pokemon p, final StatsCounter statistics) throws LoginFailedException, RemoteServerException {
		transfer(p, 3, statistics);
	}

	/**
	 * 
	 * @param p
	 *            pokemon
	 * @param ttl
	 *            time to live - max amount of retries
	 * @throws LoginFailedException
	 * @throws RemoteServerException
	 */
	private static void transfer(Pokemon p, int ttl, final StatsCounter statistics) {
		if (ttl == 0) {
			statistics.logMessage("Höre auf zu versuchen das Pokemon zu verschicken.");
			return;
		}

		ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = null;
		try {
			result = p.transferPokemon();
			statistics.logMessage(result.getValueDescriptor());
			sleep(1000);
		} catch (Exception e) {
			result = null;
			statistics.logError(e);
		}

		if (result == null || "FAILED".equals(String.valueOf(result.getValueDescriptor()))) {
			statistics.logMessage("Retry");
			transfer(p, ttl - 1, statistics);
		}
	}

	/**
	 * Ungetestet! Es wird keine Haftung übernommen :D
	 * @param go
	 * @param gym
	 * @param statistics
	 */
	public static void attackWithBestTeam(final PokemonGo go, final Gym gym, final StatsCounter statistics) {
		try {
			Stream<Pokemon> sorted = go.getInventories().getPokebank().getPokemons().stream()
					.sorted((a,b) -> Integer.compare(b.getCp(), a.getCp()) )
					// Annahme: es können immer maximal 6 Pokemon angeifen
					.limit(6);
			final Pokemon[] battleTeam = sorted.toArray(Pokemon[]::new);
			final Battle battle = gym.battle(battleTeam);
			
			// Ist das alles? Gibt ja auch andere Methoden...
			final Result result = battle.start();
			
			switch (result) {
				case SUCCESS: statistics.logMessage("Kampf gestartet!"); break; // super!
				default: statistics.logMessage("Kampf konnte nicht gestartet werden!"); return; // nicht so toll!
			}
			
			AttackGymResponse attack;
			while (!battle.isConcluded()) {
				sleep(1000);
				attack = battle.attack(0);
				switch (attack.getResult()) {
					case SUCCESS: break;
					default: break;
				}
			}

			switch (battle.getOutcome()) {
				case VICTORY: statistics.logMessage("Gewonnen!"); break;
				default: statistics.logMessage("Verloren!"); break;
			}
			
			for (Pokemon p: battleTeam) {
				int tries = 3;
				while (p.isFainted()) {
					if (tries <= 0) {
						continue;
					}
					UseItemReviveResponse.Result reviveResult = p.revive();
					switch (reviveResult) {
						case SUCCESS: break;
						default: tries--; break;
					}
				}
				tries = 3;
				while (p.isInjured()) {
					UseItemPotionResponse.Result healResult = p.heal();
					switch (healResult) {
						case SUCCESS: break;
						default: tries--; break;
					}
				}
			}
		} catch (LoginFailedException | RemoteServerException e) {
			statistics.logError(e);
		}
	}
	
	public static void tradeInTrashItems(final PokemonGo go, final StatsCounter sc) {
		sc.logMessage("Recycle Trash Items");
		
		final Map<ItemId, Integer> itemLimits = new HashMap<ItemId, Integer>();
		itemLimits.put(ItemId.ITEM_POTION, 0);
		itemLimits.put(ItemId.ITEM_SUPER_POTION, 0);
		itemLimits.put(ItemId.ITEM_HYPER_POTION, 20);
		itemLimits.put(ItemId.ITEM_REVIVE, 10);
		
		final ItemBag itemBag;
		try {
			itemBag = go.getInventories().getItemBag();
		} catch (LoginFailedException | RemoteServerException e) {
			sc.logError(e);
			return;
		}
		
		Item item;
		for (Map.Entry<ItemId, Integer> entry: itemLimits.entrySet()) {
			item = itemBag.getItem(entry.getKey());
			Integer limit = entry.getValue();
			if (item != null && item.getCount() > limit) {
				sc.logMessage(String.format("Reduziere item %s auf %d Stück", item.getItemId().name(), limit));
				removeItems(go, item, item.getCount() - limit, sc);
			}
		}
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
			sc.logMessage(result.getValueDescriptor());
			sleep(1000);
			if ("FAILED".equals(String.valueOf(result.getValueDescriptor()))) {
				sc.logMessage("Retry");
				removeItems(go, item, amount, ttl-1, sc);
			}
		} catch (LoginFailedException | RemoteServerException e) {
			sc.logError(e);
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
