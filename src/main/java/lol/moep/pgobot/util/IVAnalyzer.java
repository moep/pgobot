package lol.moep.pgobot.util;

import java.util.List;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

public class IVAnalyzer {

	public static void analyze(final PokemonGo go) {
		System.out.println("Analysiere Pokemon");
		List<Pokemon> pokemons;
		try {
			pokemons = go.getInventories().getPokebank().getPokemons();
		} catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
			// TODO
			e.printStackTrace();
			return;
		}

		for (Pokemon p : pokemons) {
			String nickname = p.getNickname();
			if (nickname == null || "".equals(nickname)) {
				int atk = p.getIndividualAttack();
				int def = p.getIndividualDefense();
				int sta = p.getIndividualStamina();
				
				try {
					p.renamePokemon("A" + atk + " D" + def + " S" + sta);
				} catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Actions.sleep(1000);
				
				if (atk == 15 && def == 15 && sta == 15) {
					try {
						p.setFavoritePokemon(true);
					} catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Actions.sleep(1000);
				}
			}
		}
	}

}
