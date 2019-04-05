package ng.upperlink.nibss.cmms.mandates.utils;


import java.util.Random;

/*
* Generates a random string
* */
public class RandomString {

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@$&";
    static Random rnd = new Random();

    String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }
}
