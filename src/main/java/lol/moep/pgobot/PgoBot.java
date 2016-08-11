package lol.moep.pgobot;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.runners.InteractiveRunner;
import lol.moep.pgobot.runners.PgoBotRunner;
import okhttp3.OkHttpClient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

/**
 * The one and only main class.
 * <p>
 * It first tries to authentificate. If the authentification was successful the bot run(s) is / are started.
 */
public class PgoBot {

    private static final String TOKEN_PROPERTIES_FILE_NAME = "token.properties";

    public static void main(String[] args) {
        final OkHttpClient httpClient = new OkHttpClient();
        final GoogleUserCredentialProvider provider;

        // Auth part

        try {
            provider = getCredentialProvider(httpClient);
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }

        // Fun part

        try {
            PokemonGo go = new PokemonGo(provider, httpClient);

            PgoBotRunner r = new InteractiveRunner(go);
            r.startTour();
            r.getStatistics().print();


        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static GoogleUserCredentialProvider getCredentialProvider(OkHttpClient httpClient) throws LoginFailedException, RemoteServerException {
        GoogleUserCredentialProvider provider;

        if (getRefreshToken() == null) {
            provider = new GoogleUserCredentialProvider(httpClient);
            System.out.println("Bitte folgende URL im Browser aufrufen: " + GoogleUserCredentialProvider.LOGIN_URL);
            System.out.println("Bitte den Code eingeben: ");

            Scanner sc = new Scanner(System.in);
            String refreshToken = sc.nextLine();

            provider.login(refreshToken);
            saveRefreshToken(provider.getRefreshToken());
        } else {
            provider = new GoogleUserCredentialProvider(httpClient, getRefreshToken());
        }

        return provider;
    }

    private static String getRefreshToken() {
        Properties properties = new Properties();
        String refreshToken = null;

        try {
            properties.load(new FileInputStream(TOKEN_PROPERTIES_FILE_NAME));
            refreshToken = properties.getProperty("refreshToken");
        } catch (IOException e) {
            // It's ok if the file cannot be accessed
        } finally {
            return refreshToken;
        }

    }

    private static void saveRefreshToken(String token) {
        Properties properties = new Properties();
        properties.setProperty("refreshToken", token);
        try {
            properties.store(new FileOutputStream(TOKEN_PROPERTIES_FILE_NAME), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
