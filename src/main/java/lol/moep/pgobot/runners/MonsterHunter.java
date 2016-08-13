package lol.moep.pgobot.runners;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Inventory.Item.ItemIdOuterClass;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.Haversine;
import lol.moep.pgobot.util.Actions;
import lol.moep.pgobot.util.logger.Logger;
import lol.moep.pgobot.util.logger.LoggerFactory;
import lol.moep.pgobot.waypoints.MartinWaypoints;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by moep on 13.08.16.
 */
public class MonsterHunter extends AbstractPgoBotRunner {
    private static final Logger LOG = LoggerFactory.getLoggerInstance();

    private static final String PGO_URL = "https://pokemap.berlin/raw_data?pokemon=true&pokestops=true&gyms=false&scanned=false&swLat=52.50970385827099&swLng=13.387260619915764&neLat=52.5266521055571&neLng=13.420498554028313&_=";

    private static final List<PokemonIdOuterClass.PokemonId> BLACKLISTED_POKEMONS = Arrays.asList(
            PokemonIdOuterClass.PokemonId.CATERPIE, // Raupy
            PokemonIdOuterClass.PokemonId.METAPOD, // Safcon
            PokemonIdOuterClass.PokemonId.BUTTERFREE, // Smetbo
            PokemonIdOuterClass.PokemonId.WEEDLE, // Hornliu
            PokemonIdOuterClass.PokemonId.KAKUNA, // Kokuna
            PokemonIdOuterClass.PokemonId.BEEDRILL, // Bibor
            PokemonIdOuterClass.PokemonId.PIDGEY, // Taubsi
            PokemonIdOuterClass.PokemonId.PIDGEOTTO, // Tauboga
            PokemonIdOuterClass.PokemonId.PIDGEOT, // Tauboss
            PokemonIdOuterClass.PokemonId.RATTATA, // Rattfratz
            PokemonIdOuterClass.PokemonId.RATICATE, // Rattikarl
            PokemonIdOuterClass.PokemonId.SPEAROW, // Habitak
            PokemonIdOuterClass.PokemonId.FEAROW, // Ibitak
            PokemonIdOuterClass.PokemonId.EKANS, // Rettan
            PokemonIdOuterClass.PokemonId.ARBOK,
            PokemonIdOuterClass.PokemonId.ZUBAT,
            PokemonIdOuterClass.PokemonId.GOLBAT,
            PokemonIdOuterClass.PokemonId.ODDISH, // Myrapla
            PokemonIdOuterClass.PokemonId.GLOOM, // Duflor
            PokemonIdOuterClass.PokemonId.VILEPLUME, // Giflor
            PokemonIdOuterClass.PokemonId.PARAS,
            PokemonIdOuterClass.PokemonId.PARASECT, // Parasek
            PokemonIdOuterClass.PokemonId.VENONAT, // Bluzuk
            PokemonIdOuterClass.PokemonId.VENOMOTH, // Omot
            PokemonIdOuterClass.PokemonId.POLIWAG, // Quapsel
            PokemonIdOuterClass.PokemonId.POLIWHIRL, // Quaputzi
            PokemonIdOuterClass.PokemonId.POLIWRATH, // Quappo
            PokemonIdOuterClass.PokemonId.SHELLDER, // Muschas
            PokemonIdOuterClass.PokemonId.CLOYSTER, // Austos
            PokemonIdOuterClass.PokemonId.GASTLY, // Nebulak
            PokemonIdOuterClass.PokemonId.DROWZEE, // Traumato
            PokemonIdOuterClass.PokemonId.HYPNO,
            PokemonIdOuterClass.PokemonId.KRABBY,
            PokemonIdOuterClass.PokemonId.KINGLER,
            PokemonIdOuterClass.PokemonId.KOFFING, // Smogon
            PokemonIdOuterClass.PokemonId.WEEZING, // Smogmog
            PokemonIdOuterClass.PokemonId.GOLDEEN, // Goldini
            PokemonIdOuterClass.PokemonId.SEAKING, // Golking
            PokemonIdOuterClass.PokemonId.STARYU, // Sterndu
            PokemonIdOuterClass.PokemonId.STARMIE,
            PokemonIdOuterClass.PokemonId.JYNX, // Rossana
            PokemonIdOuterClass.PokemonId.MAGIKARP // Karpador
    );

    {
        try {
            installCertificate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public MonsterHunter(PokemonGo go) {
        super(go);
    }

    @Override
    public void startTour() throws LoginFailedException, RemoteServerException {

        LOG.gray("=== JAGD ===");
        Actions.tradeInDuplicates2(go, sc);
        Actions.renameToIv(go, sc);

        teleportTo(MartinWaypoints.FERNSEHTURM);

        List<String> encounterIds = new ArrayList<>();

        while (hasPokeballsLeft()) {
            List<PokemonDto> reachablePokemons = getReachablePokemons();
            List<PokemonDto> filtered = reachablePokemons.stream()
                    .filter(p -> !BLACKLISTED_POKEMONS.contains(p.getId()))
                    .filter(p1 -> !encounterIds.contains(p1.getEncounterId()))
                    .collect(Collectors.toList());

            LOG.gray("Erreichbar: " + reachablePokemons.size());
            LOG.gray("Interessant: " + filtered.size());
//            filtered.forEach(p2 -> LOG.gray("  " + Dictionary.getNameFromPokemonId(p2.getId())));

            filtered.sort((p1, p2) ->
                    (int) (Haversine.getDistanceInMeters(this.getCurrentPosition(), p1.getPosition()) - Haversine.getDistanceInMeters(this.getCurrentPosition(), p2.getPosition())));

            if (filtered.isEmpty()) {
                LOG.yellow("Kein interessantes Pokémon in der Nähe.");
                Actions.sleep(30000);
            } else {

                PokemonDto p = filtered.stream()
                        .collect(Collectors.toList())
                        .get(0);

                LOG.blue("Jage: " + Dictionary.getNameFromPokemonId(p.getId()) + " - " + (int) Haversine.getDistanceInMeters(this.getCurrentPosition(), p.getPosition()) + "m");
                driveTo(p.getPosition());
                findAndCatchPokemon();
                encounterIds.add(p.getEncounterId());
            }
        }

        Actions.tradeInDuplicates2(go, sc);
        Actions.renameToIv(go, sc);
        LOG.red("=== ENDE (keine Pokébälle mehr im Inventar) ===");
    }

    private boolean hasPokeballsLeft() {
        try {
            return !this.go.getInventories().getItemBag().getItems().stream()
                    .filter(i -> i.getItemId().equals(ItemIdOuterClass.ItemId.ITEM_POKE_BALL)
                            || i.getItemId().equals(ItemIdOuterClass.ItemId.ITEM_GREAT_BALL)
                            || i.getItemId().equals(ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL)
                            || i.getItemId().equals(ItemIdOuterClass.ItemId.ITEM_MASTER_BALL))
                    .collect(Collectors.toList())
                    .isEmpty();
        } catch (LoginFailedException | RemoteServerException e) {
            LOG.logError(e);
            return false;
        }

    }

    private List<PokemonDto> getReachablePokemons() {
        String json = getNearbyPokemonsAsJsonString();

        JSONParser parser = new JSONParser();
        List<PokemonDto> reachablePokemons = new ArrayList<>();
        try {
            JSONObject result = (JSONObject) parser.parse(json);
            JSONArray pokemons = (JSONArray) result.get("pokemons");

            pokemons.stream().forEach(obj -> {
                PokemonDto p = convertToPokemon((JSONObject) obj);

                if (p.isReachableInTime(this.getCurrentPosition(), MovementSpeed.DRIVE)) {
                    reachablePokemons.add(p);
                }
            });

        } catch (ParseException e) {
            LOG.logError(e);
        }

        return reachablePokemons;
    }

    private PokemonDto convertToPokemon(JSONObject obj) {
        PokemonIdOuterClass.PokemonId id = PokemonIdOuterClass.PokemonId.forNumber(Integer.valueOf(obj.get("pokemon_id").toString()));
        GeoCoordinate pos = new GeoCoordinate(Double.valueOf(obj.get("latitude").toString()).doubleValue(), Double.valueOf(obj.get("longitude").toString()).doubleValue());
        long disappearTime = Long.valueOf(obj.get("disappear_time").toString());
        String encounterId = obj.get("encounter_id").toString();

        return new PokemonDto(id, pos, disappearTime, encounterId);
    }

    private String getNearbyPokemonsAsJsonString() {
        StringBuilder sb = new StringBuilder();
        String httpsUrl = PGO_URL + System.currentTimeMillis();
        URL url;
        try {

            url = new URL(httpsUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

//            LOG.info("Response code: " + con.getResponseCode());
//            LOG.info("Cipher Suite: " + con.getCipherSuite());

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String input;

            while ((input = br.readLine()) != null) {
                sb.append(input);
            }
            br.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return sb.toString();
        }
    }

    private static void installCertificate() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        LOG.info("Installiere Zertifikat");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path ksPath = Paths.get(System.getProperty("java.home"),
                "lib", "security", "cacerts");
        keyStore.load(Files.newInputStream(ksPath),
                "changeit".toCharArray());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");


        // java.io.InputStream caInput = new BufferedInputStream(new FileInputStream("pokemapberlin.cer"));
        ClassLoader classLoader = MonsterHunter.class.getClassLoader();
        java.io.InputStream caInput = new BufferedInputStream(classLoader.getResource("pokemapberlin.cer").openStream());
        Certificate crt = cf.generateCertificate(caInput);
        LOG.info("Zertifikat hinzugefügt: " + ((X509Certificate) crt)
                .getSubjectDN());

        keyStore.setCertificateEntry("DSTRootCAX3", crt);


//        LOG.info("Vertraue: ");
//        PKIXParameters params = new PKIXParameters(keyStore);
//        params.getTrustAnchors().stream()
//                .map(TrustAnchor::getTrustedCert)
//                .map(X509Certificate::getSubjectDN)
//                .forEach(System.out::println);

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
    }

    private static class PokemonDto {
        private PokemonIdOuterClass.PokemonId id;
        private GeoCoordinate position;
        private long disappearTime;

        private String encounterId;

        public PokemonDto(PokemonIdOuterClass.PokemonId id, GeoCoordinate position, long disappearTime, String encounterId) {
            this.id = id;
            this.position = position;
            this.disappearTime = disappearTime;
            this.encounterId = encounterId;
        }

        public PokemonIdOuterClass.PokemonId getId() {
            return id;
        }

        public void setId(PokemonIdOuterClass.PokemonId id) {
            this.id = id;
        }

        public GeoCoordinate getPosition() {
            return position;
        }

        public void setPosition(GeoCoordinate position) {
            this.position = position;
        }

        public long getDisappearTime() {
            return disappearTime;
        }

        public void setDisappearTime(long disappearTime) {
            this.disappearTime = disappearTime;
        }

        public String getEncounterId() {
            return encounterId;
        }

        public void setEncounterId(String encounterId) {
            this.encounterId = encounterId;
        }

        public boolean isReachableInTime(GeoCoordinate start, MovementSpeed speed) {
            // TODO create constant or config value for 30s?
            long remainingTimeMillis = this.getDisappearTime() - System.currentTimeMillis() - 30000;

            // Normalize bugged results
            if (remainingTimeMillis > 60 * 15 * 1000) {
                remainingTimeMillis = 60 * 15 * 1000;
            }

            return remainingTimeMillis > Math.round(MovementSpeed.DRIVE.getVal() * Haversine.getDistanceInMeters(start, this.getPosition()));


        }

    }

    @Override
    protected void onMove() throws LoginFailedException, RemoteServerException {
        // Do nothing
    }
}
