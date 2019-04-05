package ng.upperlink.nibss.cmms.mandates.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;

public class Utilities {

    private static Log logger = LogFactory.getLog( Utilities.class );

    public static void assertNotNullArgument( Object obj ) {
        if( obj == null ) {
            logger.error( "Null Object specified as argument" );
            throw new IllegalArgumentException( "Null Object specified as argument" );
        }
    }

    /**
     * Validates email address.
     *
     * @param emailID:
     *            email address to be verified
     * @return boolean: true if varified correctly false if email address is
     *         incorrect
     */
    public static boolean validateEmailID( String emailID ) {

        if( emailID.indexOf( '@' ) == -1 || emailID.indexOf( '@' ) != emailID.lastIndexOf( '@' )
                || emailID.lastIndexOf( '@' ) == ( emailID.length() - 1 ) || emailID.indexOf( '@' ) == 0 ) {
            return false;
        }
        else if( emailID.lastIndexOf( '.' ) == -1 || ( emailID.length() - emailID.lastIndexOf( '.' ) - 1 ) < 2 ) {
            return false;
        }
        else if( emailID.indexOf( '@' ) > emailID.lastIndexOf( '.' )
                || ( emailID.lastIndexOf( '.' ) - emailID.indexOf( '@' ) ) == 1 ) {
            return false;
        }
        else if( emailID.indexOf( ' ' ) != -1 ) {
            return false;
        }
        else {
            String spclChars = ";:?<>!~`!#$%^&*()+=|\\/'\"";

            for( int i = 0; i < spclChars.length(); i++ ) {
                if( emailID.indexOf( spclChars.charAt( i ) ) != -1 ) {
                    return false;
                }
            }

            return true;

        }
    }


    /** converts millisecond to time
     * @param millisecond
     * @return hours,minutes and seconds int ime format
     */
    public static String convertMillis(Object millisecond){
        long milliseconds=0;
        if (millisecond instanceof String){
            milliseconds= Long.valueOf((String)millisecond);
        }else milliseconds= Long.valueOf((Integer)millisecond);

        java.text.DecimalFormat formatter = new  java.text.DecimalFormat("#00.###");
        formatter.setDecimalSeparatorAlwaysShown(false);
        int seconds = (int) (milliseconds / 1000) % 60 ;
        int minutes = (int) ((milliseconds / (1000*60)) % 60);
        int hours   = (int) ((milliseconds / (1000*60*60)) % 24);


        return(hours + ":" + formatter.format(minutes) + ":" + formatter.format(seconds));
    }

    public static Long reflectToLong(String item){
        Class<?> cls;
        Class<?>[] paramString = new Class[1];
        paramString[0] = String.class;
        Long theLong= null;

        try {
            cls = Class.forName(" ng.upperlink.nibss.cmms.mandates.utils.Utilities");
            Object obj = cls.newInstance();
            Method method = cls.getDeclaredMethod("getLong", paramString);
            theLong=(Long) (method.invoke(obj, new String(item)));
        } catch (Exception e) {
            logger.error(e);
        }
        return theLong;
    }

    public static Long reflectToByte(String item){
        Class<?> cls;
        Class<?>[] paramString = new Class[1];
        paramString[0] = String.class;
        Long theLong= null;

        try {
            cls = Class.forName(" ng.upperlink.nibss.cmms.mandates.utils.Utilities");
            Object obj = cls.newInstance();
            Method method = cls.getDeclaredMethod("getByte", paramString);
            theLong=(Long) (method.invoke(obj, new String(item)));
        } catch (Exception e) {
            logger.error(e);
        }
        return theLong;
    }

    public static String generatePassword(){
        return new RandomString().randomString(8);
    }


    public Long getLong(String Item){
        return Long.valueOf(Item);
    }

    public Byte getByte(String Item){
        return Byte.valueOf(Item);
    }

}
