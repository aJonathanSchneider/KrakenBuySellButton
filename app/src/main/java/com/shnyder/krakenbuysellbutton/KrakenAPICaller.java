package com.shnyder.krakenbuysellbutton;

/**
 * Created by Jonathan Schneider on 08.11.2017.
 * https://github.com/aJonathanSchneider
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;

public class KrakenAPICaller {
    static String domain = "https://api.kraken.com";
    static String OT_LIMIT = "limit";
    static String OT_MARKET = "market";

    public HashMap<String,Long> lastRequestId = new HashMap<>();
    public HashMap<String,JSONArray> ohlcArrays = new HashMap<>();

    String account_balance() {
        String answer = newRequest(null,"/0/private/Balance");
        return answer;
        // on empty accounts, returns {"error":[],"result":{}}
        // this is a known Kraken bug
        //...
    }

    String ohlc(String assetpair) {
        //calculateSignature();
        String last = "";
        if(lastRequestId.containsKey(assetpair)) {
            last = "&since="+lastRequestId.get(assetpair);
        }
        String addData = "pair="+assetpair+last;
        String answer = newRequest(addData,"/0/public/OHLC");
        return answer;
    }

    String cancelOpenOrder(String txid) {
        //calculateSignature();
        String addData = "txid="+txid;
        String answer = newRequest(addData,"/0/private/CancelOrder");
        return answer;
    }
    /**
     *
     * @param pair e.g. ETHEUR or ETHUSD
     * @param type
     * @param price
     * @param volume
     * @param isValidate
     * @return
     */
    String addOrderType_002(String pair, String type, String price,
                            String volume,boolean isValidate) {
        //calculateSignature();
        String ordertype = price == null ? OT_MARKET : OT_LIMIT;
        String leverage = "none";
        price = price == null ? "" : "&price="+price;
        String validate = isValidate ? "&validate="+isValidate : "";
        String addData = "pair="+pair
                +"&type="+type
                +"&ordertype="+ordertype
                +price
                +"&volume="+volume
                +"&leverage="+leverage
                +"&trading_agreement=agree"
                +validate;
        String answer = newRequest(addData,"/0/private/AddOrder");
        return answer;
    }

    String realizationOrderType_002(String pair, String type, String price,
                                    String volume,boolean isValidate) {
        //calculateSignature();
        String ordertype = OT_MARKET;
        String leverage = "0";
        String addData = "pair="+pair
                +"&type="+type
                +"&ordertype="+ordertype
                +"&volume="+volume
                +"&leverage="+leverage
                +"&validate="+isValidate;
        String answer = newRequest(addData,"/0/private/AddOrder");
        return answer;
    }


    String assets() {
        //calculateSignature();
        String addData = "";
        String answer = newRequest(addData,"/0/public/Assets");
        return answer;
    }

    String assetPairs() {
        //calculateSignature();
        String addData = "";
        String answer = newRequest(addData,"/0/public/AssetPairs");
        return answer;
    }

    String openOrders() {
        //calculateSignature();
        String addData = "";
        String answer = newRequest(addData,"/0/private/OpenOrders");
        return answer;
    }

    String tickerInfo() {
        //calculateSignature();
        String addData = "pair=XXBTZEUR,ETHEUR,BCHEUR";
        String answer = newRequest(addData,"/0/public/Ticker");
        return answer;
    }

    String closedOrders(int offset) {
        //calculateSignature();
        String addData = "ofs="+offset;
        String answer = newRequest(addData,"/0/private/ClosedOrders");
        return answer;
    }

    String tradeInfo() {
        //calculateSignature();
        String addData = "type=trade&ofs=0";
        String answer = newRequest(addData,"/0/private/Ledgers");
        return answer;
    }

    String openPositions() {
        String addData = "docalcs=true";
        String answer = newRequest(addData,"/0/private/OpenPositions");
        return answer;
    }

    public String newRequest(String additionalData, String path) {
        String nonce, signature;
        nonce = String.valueOf(System.currentTimeMillis());
        String data = "nonce=" + nonce;
        if(additionalData != null && !additionalData.equals("")) data += "&"+additionalData;
        String domainPath = domain + path;
        signature = calculateSignature(nonce, data,path);
        String answer = post(domainPath, data, signature);
        return answer;
    }

    private String post(String address, String output, String signature) {
        String answer = "";
        HttpsURLConnection c = null;
        try {
            URL u = new URL(address);
            c = (HttpsURLConnection)u.openConnection();
            if(StatString.key == null || StatString.secret == null) throw new Exception(" kraken API key and/or secret are null, please fill the static values first");
            c.setRequestMethod("POST");
            c.setRequestProperty("API-Key", StatString.key);
            c.setRequestProperty("API-Sign", signature);
            c.setDoOutput(true);
            DataOutputStream os = new DataOutputStream(c.getOutputStream());
            os.writeBytes(output);
            os.flush();
            os.close();
            BufferedReader br = null;
            if(c.getResponseCode() >= 400) {
                //System.exit(1);
                answer = "{error:'accesssing Kraken error is "+c.getResponseCode()+"'}";
                return answer;
            }
            br = new BufferedReader(new InputStreamReader((c.getInputStream())));
            String line;
            while ((line = br.readLine()) != null)
                answer += line;
        } catch (Exception x) {
            //System.exit(1);
            answer = "{error: 'accesssing Kraken error"+x.getMessage()+"'}";
            return answer;
        } finally {
            c.disconnect();
        }
        return answer;
    }

    private String calculateSignature(String nonce, String data, String path) {
        String signature = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((nonce + data).getBytes());
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(Base64.decodeBase64(StatString.secret.getBytes()), "HmacSHA512"));
            mac.update(path.getBytes());
            signature = new String(Base64.encodeBase64(mac.doFinal(md.digest())));
        } catch(Exception e) {}
        return signature;
    }
}
