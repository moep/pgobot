package lol.moep.pgobot.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.pokegoapi.api.pokemon.Pokemon;

import POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;

/**
 * Sortiert aufsteigend nach IV Ratio, Angriffen (DPS) und CP.
 * 
 * @author Hicks
 *
 */
public class PokemonComparator implements Comparator<Pokemon> {

	// TODO volle Liste von https://thesilphroad.com/research übernehmen
	// http://www.techtimes.com/articles/173239/20160810/pokemon-go-best-30-moves-use-battle.htm
	// @formatter:off
	private static final List<PokemonMove> BEST_QUICK_MOVES = Arrays.asList(
			PokemonMove.POUND, PokemonMove.POUND_FAST,
			PokemonMove.METAL_CLAW, PokemonMove.METAL_CLAW_FAST,
			PokemonMove.PSYCHO_CUT, PokemonMove.PSYCHO_CUT_FAST,
			PokemonMove.SCRATCH, PokemonMove.SCRATCH_FAST,
			PokemonMove.WATER_GUN, PokemonMove.WATER_GUN_FAST, PokemonMove.WATER_GUN_FAST_BLASTOISE,
			PokemonMove.WING_ATTACK, PokemonMove.WING_ATTACK_FAST,
			PokemonMove.BITE, PokemonMove.BITE_FAST,
			PokemonMove.DRAGON_BREATH, PokemonMove.DRAGON_BREATH_FAST,
			PokemonMove.FIRE_FANG_FAST,
			PokemonMove.SHADOW_CLAW, PokemonMove.SHADOW_CLAW_FAST,
			PokemonMove.FEINT_ATTACK_FAST,
			PokemonMove.POISON_JAB, PokemonMove.POISON_JAB_FAST,
			PokemonMove.ZEN_HEADBUTT_FAST,
			PokemonMove.STEEL_WING, PokemonMove.STEEL_WING_FAST,
			PokemonMove.FROST_BREATH, PokemonMove.FROST_BREATH_FAST
	);
	// @formatter:on
	// @formatter:off
	private static final List<PokemonMove> BEST_CHARGE_MOVES = Arrays.asList(
			PokemonMove.CROSS_CHOP,
			PokemonMove.STONE_EDGE,
			PokemonMove.BLIZZARD,
			PokemonMove.BODY_SLAM,
			PokemonMove.POWER_WHIP,
			PokemonMove.HURRICANE,
			PokemonMove.MEGAHORN,
			PokemonMove.SOLAR_BEAM,
			PokemonMove.FIRE_BLAST,
			PokemonMove.HYPER_BEAM,
			PokemonMove.EARTHQUAKE,
			PokemonMove.HYDRO_PUMP, PokemonMove.HYDRO_PUMP_BLASTOISE,
			PokemonMove.DRAGON_CLAW,
			PokemonMove.THUNDERBOLT,
			PokemonMove.GUNK_SHOT
	);
	// @formatter:on

	@Override
	public int compare(Pokemon o1, Pokemon o2) {
		int result = Double.compare(o1.getIvRatio(), o2.getIvRatio());

		if (result == 0) {
			result = compareQuickMoves(o1, o2);
		}
		if (result == 0) {
			result = compareChargeMoves(o1, o2);
		}
		if (result == 0) {
			result = Integer.compare(o1.getCp(), o2.getCp());
		}

		return result;
	}

	private static int compareQuickMoves(final Pokemon p1, final Pokemon p2) {
		return compareMoves(p1.getMove1(), p2.getMove1(), BEST_QUICK_MOVES);
	}

	private static int compareChargeMoves(final Pokemon p1, final Pokemon p2) {
		return compareMoves(p1.getMove2(), p2.getMove2(), BEST_CHARGE_MOVES);
	}

	private static int compareMoves(final PokemonMove pm1, final PokemonMove pm2, final List<PokemonMove> bestMoves) {
		if (!bestMoves.contains(pm1)) {
			if (bestMoves.contains(pm2)) {
				return -1;
			} else {
				return 0;
			}
		} else {
			if (!bestMoves.contains(pm2)) {
				return 1;
			} else {
				return -Integer.compare(bestMoves.indexOf(pm1), bestMoves.indexOf(pm2));
			}
		}
	}

}
