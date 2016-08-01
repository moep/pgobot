package lol.moep.pgobot.auth;

import java.util.Scanner;

import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

public class GetRefreshToken {

	public static void main(String[] args) throws LoginFailedException, RemoteServerException {
		final OkHttpClient httpClient = new OkHttpClient();
		final GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(httpClient);
		
    	System.out.println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
    	System.out.println("Enter authorisation code:");
    	
    	final Scanner sc = new Scanner(System.in);
    	final String access = sc.nextLine();
    	sc.close();
    	
    	provider.login(access);
    	System.out.println(String.format("refresh token: %s", provider.getRefreshToken()));
	}

}
