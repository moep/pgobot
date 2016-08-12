import lol.moep.pgobot.util.logger.Logger;
import lol.moep.pgobot.util.logger.LoggerFactory;
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
import java.security.KeyStore;
import java.security.cert.*;
import java.util.Iterator;

/**
 * Created by moep on 12.08.2016.
 */
public class PokemapParser {
    private static final String PGO_URL = "https://pokemap.berlin/raw_data?pokemon=true&pokestops=true&gyms=false&scanned=false&swLat=52.50970385827099&swLng=13.387260619915764&neLat=52.5266521055571&neLng=13.420498554028313&_=";

    private static final Logger log = LoggerFactory.getLoggerInstance();

    public static void main(String[] args) throws IOException {
        log.blue(PGO_URL + System.currentTimeMillis());

        installCertificate();
        String json = getNearbyPokemonsAsJsonString();
        log.green(json);

        JSONParser parser = new JSONParser();
        try {
            JSONObject result = (JSONObject) parser.parse(json);
            JSONArray pokemons = (JSONArray) result.get("pokemons");

            Iterator<JSONObject> it = pokemons.iterator();
            while (it.hasNext()) {
                JSONObject pokemon = (JSONObject) it.next();
                //log.red(pokemon.get("pokemon_id"));
                log.red(pokemon.get("pokemon_name"));
                log.yellow(pokemon.get("latitude") + ", " + pokemon.get("longitude"));

                long disappearTime = Long.valueOf(pokemon.get("disappear_time").toString());
                log.yellow(System.currentTimeMillis() - disappearTime + "ms");
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

//        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                .tlsVersions(TlsVersion.TLS_1_2)
//                .cipherSuites(
//                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA)
//                .build();
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectionSpecs(Collections.singletonList(spec))
//                .build();
//
//        Request req = new Request.Builder()
//                .url(PGO_URL + System.currentTimeMillis())
//                .build();
//
//        Response resp = client.newCall(req).execute();

    }

    private static String getNearbyPokemonsAsJsonString() {
        StringBuilder sb = new StringBuilder();
        String https_url = PGO_URL + System.currentTimeMillis();
        URL url;
        try {

            url = new URL(https_url);
            log.green(url);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            //dumpl all cert info
            log.blue("Response code: " + con.getResponseCode());
            log.mangenta("Cipher Suite: " + con.getCipherSuite());

            // print response
            log.info("****** Content of the URL ********");
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

    // TODO cleanup
    private static void installCertificate() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Path ksPath = Paths.get(System.getProperty("java.home"),
                    "lib", "security", "cacerts");
            keyStore.load(Files.newInputStream(ksPath),
                    "changeit".toCharArray());

            CertificateFactory cf = CertificateFactory.getInstance("X.509");


            // java.io.InputStream caInput = new BufferedInputStream(new FileInputStream("pokemapberlin.cer"));
            ClassLoader classLoader = PokemapParser.class.getClassLoader();
            java.io.InputStream caInput = new BufferedInputStream(classLoader.getResource("pokemapberlin.cer").openStream());
            Certificate crt = cf.generateCertificate(caInput);
            log.info("Added Cert for " + ((X509Certificate) crt)
                    .getSubjectDN());

            keyStore.setCertificateEntry("DSTRootCAX3", crt);


            log.info("Truststore now trusting: ");
            PKIXParameters params = new PKIXParameters(keyStore);
            params.getTrustAnchors().stream()
                    .map(TrustAnchor::getTrustedCert)
                    .map(X509Certificate::getSubjectDN)
                    .forEach(System.out::println);

            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
