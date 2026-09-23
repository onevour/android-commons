package com.onevour.core.rest.components;

/**
 * Created by Zuliadin on 06/06/2017.
 */

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  * This utility class provides an abstraction layer for sending multipart HTTP
 *  * POST requests to a web server.
 *  * @author www.codejava.net
 *  *
 *
 */
public class HttpMultipart {

    private static final String TAG = "Upload";
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset = "UTF-8";
    private OutputStream outputStream;
    private PrintWriter writer;
    private static final int MIN_TIMEOUT = 6000;
    private int timeout = 0;
    private int responseCode = 0;
    private Context context;

    private Map<String, String> formHeader = new HashMap<>();
    private Map<String, String> formField = new HashMap<>();
    private Map<String, File> formFile = new HashMap<>();

    /**
     *      * This constructor initializes a new HTTP POST request with content type
     *      * is set to multipart/form-data
     *      * @param requestURL
     *      * @param charset
     *      * @throws IOException
     *
     */
    public HttpMultipart(Context context, String requestURL) throws IOException {
        // creates a unique boundary based on time stamp
        boundary = "===".concat(String.valueOf(System.currentTimeMillis())).concat("===");
        initializeHttp(context, requestURL, 0);
    }

    public HttpMultipart(Context context, String requestURL, int timeout) throws IOException {
        // creates a unique boundary based on time stamp
        boundary = "===".concat(String.valueOf(System.currentTimeMillis())).concat("===");
        initializeHttp(context, requestURL, timeout);
    }

    public HttpMultipart(Context context, String requestURL, String charset) throws IOException {
        this.charset = charset;
        // creates a unique boundary based on time stamp
        boundary = "===".concat(String.valueOf(System.currentTimeMillis())).concat("===");
        initializeHttp(context, requestURL, 0);
    }

    public HttpMultipart(Context context, String requestURL, String charset, int timeout) throws IOException {
        this.charset = charset;
        // creates a unique boundary based on time stamp
        boundary = "===".concat(String.valueOf(System.currentTimeMillis())).concat("===");
        initializeHttp(context, requestURL, timeout);
    }

    private void initializeHttp(Context context, String requestURL, int timeout) throws IOException {
        Log.d(TAG, "upload url : ".concat(requestURL));
        this.context = context;
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=".concat(boundary));
        httpConn.setRequestProperty("User-Agent", "Evo-Agent/1.0");
        httpConn.setConnectTimeout(timeout < MIN_TIMEOUT ? MIN_TIMEOUT : timeout);
        httpConn.setRequestProperty("connection", "close");
    }


    public void request() throws IOException {
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
        for (Map.Entry<String, String> entry : formHeader.entrySet()) {
//            Log.d(TAG, "Key : " + entry.getKey() + " Value : " + entry.getValue());
            addHeader(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : formField.entrySet()) {
//            Log.d(TAG, "Key : " + entry.getKey() + " Value : " + entry.getValue());
            addField(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, File> entry : formFile.entrySet()) {
//            Log.d(TAG, "Key : " + entry.getKey() + " Value : " + entry.getValue());
            addFilePart(entry.getKey(), entry.getValue());
        }
    }

    /**
     * add form header
     */
    public void setParamHeader(String name, String value) {
        formHeader.put(name, value);
    }

    /**
     * add form field
     */
    public void setParam(String name, String value) {
        formField.put(name, value);
    }


    /**
     * add form file
     */
    public void setParamFile(String name, File value) {
        formFile.put(name, value);
    }

    /**
     *      * Adds a form field to the request
     *      * @param name field name
     *      * @param value field value
     *
     */

    private void addField(String name, String value) {
        /*
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value);
        writer.append(LINE_FEED);
        */

        StringBuilder sb = new StringBuilder("--");
        sb.append(boundary).append(LINE_FEED);
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_FEED);
        sb.append("Content-Type: text/plain; charset=").append(charset).append(LINE_FEED);
        sb.append(LINE_FEED);
        sb.append(value);
        sb.append(LINE_FEED);
        writer.append(sb.toString());
        writer.flush();
    }

    /**
     *      * Adds a upload file section to the request
     *      * @param fieldName name attribute in <input type="file" name="..." />
     *      * @param uploadFile a File to be uploaded
     *      * @throws IOException
     *
     */

    private void addFilePart(String fieldName, File uploadFile) throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--");
        writer.append(boundary);
        writer.append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"".concat(fieldName).concat("\"; filename=\"").concat(fileName).concat("\"")).append(LINE_FEED);
        writer.append("Content-Type: ".concat(URLConnection.guessContentTypeFromName(fileName)).concat(LINE_FEED));
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     *      * Adds a header field to the request.
     *      * @param name - name of the header field
     *      * @param value - value of the header field
     *
     */

    private void addHeader(String name, String value) {
        writer.append(name.concat(": ").concat(value)).append(LINE_FEED);
        writer.flush();
    }

    /**
     *      * Completes the request and receives response from the server.
     *      * @return a list of Strings as response in case the server returned
     *      * status OK, otherwise an exception is thrown.
     *      * @throws IOException
     *
     */

    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();
        writer.append(LINE_FEED).flush();
        writer.append("--".concat(boundary).concat("--")).append(LINE_FEED);
        writer.close();
        // checks server's status code first
        int status = httpConn.getResponseCode();
        responseCode = status;
        if (responseCode == 204) {
            return response;
        }
        if (responseCode >= 200 && responseCode < 300) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned ".concat(String.valueOf(status)));
        }
        return response;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public Map<String, List<String>> getHeaderFields() {
        return httpConn.getHeaderFields();
    }
}
