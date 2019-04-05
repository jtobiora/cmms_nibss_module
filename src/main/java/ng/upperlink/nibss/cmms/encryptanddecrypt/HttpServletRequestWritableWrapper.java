package ng.upperlink.nibss.cmms.encryptanddecrypt;

import com.google.gson.Gson;
import ng.upperlink.nibss.cmms.config.dto.DataExchangeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

/*
* HttpServlet Request writer
**/
public class HttpServletRequestWritableWrapper extends HttpServletRequestWrapper {

    private String payload = "";

    private Logger LOG = LoggerFactory.getLogger(HttpServletRequestWritableWrapper.class);

    HttpServletRequestWritableWrapper(HttpServletRequest request, String url, String secretKey, String iv) {
        super(request);

        // read the original payload into the payload variable
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            // read the payload into the StringBuilder
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;

                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                // make an empty string since there is no payload
                stringBuilder.append("");
            }
        } catch (IOException ex) {
//            LOG.error("Error reading the request payload", ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException iox) {
                    // ignore
                }
            }
        }
        String content = stringBuilder.toString();

        if (ContentFilter.isToBeEncrypted(url)){
            DataExchangeObject exchangeObject = new Gson().fromJson(content, DataExchangeObject.class);
            if (exchangeObject != null) {
                payload = NIBSSAESEncryption.decryptAES(exchangeObject.getData(), secretKey, iv);
            }
        }else {
            payload = content;
        }
    }


    @Override
    public ServletInputStream getInputStream() throws IOException {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload.getBytes());
        ServletInputStream inputStream = new ServletInputStream() {
            public int read ()
                    throws IOException {
                return byteArrayInputStream.read();
            }
        };
        return inputStream;

    }

    public String getPayload() {
        return payload;
    }
}
