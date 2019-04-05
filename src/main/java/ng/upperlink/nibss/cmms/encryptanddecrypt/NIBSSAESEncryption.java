package ng.upperlink.nibss.cmms.encryptanddecrypt;

import ng.upperlink.nibss.cmms.dto.Response;
import ng.upperlink.nibss.cmms.enums.EncryptionHeader;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 *  Handles Encryption and Decryption
 */
@Service
public class NIBSSAESEncryption {

    //private InstitutionCredentialsRepo institutionCredentialsRepo;

    private static Logger LOG = LoggerFactory.getLogger(NIBSSAESEncryption.class);

    private String password;

    public static final String COMMA = ",", FULL_COLON = ":", ALGORITHM = "AES/CBC/PKCS5Padding", AES = "AES";

    public static String encryptAES(String text, String secretkey, String iv)
    {
        IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());
        SecretKeySpec keyspec = new SecretKeySpec(secretkey.getBytes(), AES);
        try
        {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            return EncyptionUtil.bytesToHex(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            LOG.error("ERROR: Unable to encrypt request >> ", e);
        }
        return null;
    }

    public static String decryptAES(String encryptedRequest, String secretkey, String iv) {
        IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());
        SecretKeySpec keyspec = new SecretKeySpec(secretkey.getBytes(), AES);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            return new String(cipher.doFinal(EncyptionUtil.hexToBytes(encryptedRequest)), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            LOG.error("ERROR: Unable to decrypt request >> ", e);
        }
        return null;
    }

    public Object validateHeaderValue(HttpServletRequest servletRequest, String serviceName){

        //get The Header values
        String authorizationValue = servletRequest.getHeader(EncryptionHeader.AUTHORIZATION.getName());
        String signatureValue = servletRequest.getHeader(EncryptionHeader.SIGNATURE.getName());
        String signatureMethValue = servletRequest.getHeader(EncryptionHeader.SIGNATURE_METH.getName());

        LOG.info("authorizationValue => {}", authorizationValue);
        LOG.info("signatureValue => {}", signatureValue);
        LOG.info("signatureMethodValue => {}", signatureMethValue);

        Object credentialOrErrorCode = validateAuthenticationValue(authorizationValue, serviceName);
        if (credentialOrErrorCode instanceof String){
            LOG.error("Error validating AuthenticationValue => {}",credentialOrErrorCode);
            return String.valueOf(credentialOrErrorCode);
        }

        if(!validateSignatureMethValue(signatureMethValue)){
            LOG.error("SignatureMethValue is NOT sha256");
            return Response.INVALID_DATA_PROVIDED.getCode();
        }

        //return credentials
       Map<String, String> mapCredentials = new HashMap<>();
        return mapCredentials;

    }

    private Object validateOrganisationCodeAndGetCredential(String organisationCode, String serviceName){

        if (organisationCode == null || organisationCode.isEmpty() || "".equals(organisationCode)){
            return Response.INVALID_DATA_PROVIDED.getCode();
        }

        //decode organisationCode
        String decodedOrganisationCode;
        try {
            decodedOrganisationCode = new String(Base64.getDecoder().decode(organisationCode));
        } catch (Exception e) {
            LOG.error("Unable to organisationCode decode ==> {}", organisationCode, e);
            return Response.INVALID_DATA_PROVIDED.getCode();
        }

        return null;
    }

    private Object validateAuthenticationValue(String authorizationValue, String serviceName){

        //using the username, we decode the authentication value and split to get the password.(username:password).replace(username:, "")
        //validate AuthorisationValue
        if (authorizationValue == null || authorizationValue.isEmpty() || "".equals(authorizationValue)){
            return Response.INVALID_DATA_PROVIDED.getCode();
        }
        //decode the auth.
        String decodedAuthValue;
        try {
            decodedAuthValue = new String(Base64.getDecoder().decode(authorizationValue));
            LOG.info("The decodedAuthValue is {}", decodedAuthValue);
        } catch (Exception e) {
            LOG.error("Unable to decode Authentication value => {}", authorizationValue, e);
            return Response.INVALID_DATA_PROVIDED.getCode();
        }

        //Split username:password with ":";
        String[] split = decodedAuthValue.split(FULL_COLON, 2);
        if (split == null || split.length < 2){
            LOG.error("Invalid AuthValue derived After splitting using : , ==> {}", Arrays.toString(split));
            return Response.INVALID_DATA_PROVIDED.getCode();
        }

        String organisationCode = split[0];//organisationCode/institutionCode
        password = split[1];//password

        return null;
    }

    private boolean validateSignatureMethValue(String signatureMeth){

        //confirm that signature meth is 256
        //confirm that the signature meth is the expected sha256
        if (signatureMeth == null || signatureMeth.isEmpty() || "".equals(signatureMeth)){
            return false;
        }

        if (signatureMeth.equalsIgnoreCase("sha256")){
            return true;
        }

        return false;
    }

}
