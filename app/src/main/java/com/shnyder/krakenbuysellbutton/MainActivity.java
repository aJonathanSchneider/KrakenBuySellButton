package com.shnyder.krakenbuysellbutton;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
/**
 * Created by Jonathan Schneider on 08.11.2017.
 * https://github.com/aJonathanSchneider
 */
public class MainActivity extends AppCompatActivity {

    protected final HashMap<String, Double> balance = new HashMap<>();

    protected JSONObject tradeAnalysis = null;

    protected Double xbtLastTradePrice = null;
    protected Double ethLastTradePrice = null;
    protected Double bchLastTradePrice = null;

    protected void updateBalanceInfo() {
        if (balance.containsKey("EUR") && tradeAnalysis != null && xbtLastTradePrice != null) {
            TextView accBalTxt = (TextView) this.findViewById(R.id.accountBalanceTxt);
            TextView buyCalcTxt = (TextView) this.findViewById(R.id.buyCalcTxt);

            NumberFormat formatter = new DecimalFormat("#0.0000");
            Double total = balance.get("EUR")
                    + balance.get("XBT") * xbtLastTradePrice
                    + balance.get("ETH") * ethLastTradePrice
                    + balance.get("BCH") * bchLastTradePrice;
            String balanceStr = "EUR:" + balance.get("EUR") + "\n"
                    + "XBT: " + formatter.format(balance.get("XBT")) + "   " + calcValuation("XXBT", xbtLastTradePrice) + "\n"
                    + "ETH: " + formatter.format(balance.get("ETH")) + "   " + calcValuation("XETH", ethLastTradePrice) + "\n"
                    + "BCH: " + formatter.format(balance.get("BCH")) + "   " + calcValuation("BCH", bchLastTradePrice) + "\n"
                    + "TOTAL: " + formatter.format(total);
            Double eurAvailable = balance.get("EUR");
            if (eurAvailable >= 10) {
                Double possXBT = eurAvailable / xbtLastTradePrice;
                Double possETH = eurAvailable / ethLastTradePrice;
                Double possBCH = eurAvailable / bchLastTradePrice;
                String newBuyPoss = "XBT: " + formatter.format(possXBT)
                        + "   ETH: " + formatter.format(possETH)
                        + "   BCH: " + formatter.format(possBCH);
                buyCalcTxt.setText(newBuyPoss);
            } else {
                buyCalcTxt.setText("EUR balance too low");
            }
            accBalTxt.setText(balanceStr);
        }
    }

    public String calcValuation(String symbol, Double compVal) {
        Double buyPrice = 0.0;
        try {
            JSONArray trades = tradeAnalysis.getJSONArray(symbol);
            for (int i = 0; i < trades.length(); i++) {
                JSONObject trade = trades.getJSONObject(i);
                if (!trade.getBoolean("isSell")) {
                    Double eurValue = trade.getDouble("EUR");
                    Double otherValue = trade.getDouble("OTHER");
                    buyPrice = (-eurValue) / otherValue;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (buyPrice == 0.0) return "N/A";
        Double rvDouble = compVal / buyPrice - 0.0024;
        NumberFormat formatter = new DecimalFormat("#0.0000");
        return formatter.format(rvDouble);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnTickerInfoClick(View v) {
        new TickerInfoTask(this).execute();
    }

    public void onBtnBalanceClick(View v) {
        new BalanceTask(this).execute();
    }

    public void onBtnOpenOrdersClick(View v) {
        new OpenOrdersTask(this).execute();
    }

    public void onBtnSellMarket(View v) {
        final String pair = ((Spinner) this.findViewById(R.id.currencyMrktSpinner)).getSelectedItem().toString() + "EUR";
        final String orderType = "sell";
        final String volume = ((EditText) this.findViewById(R.id.volumeTxt)).getText().toString();
        new SellTask(this, pair, orderType, volume).execute();
    }

    public void onBtnBuyMarket(View v) {
        final TextView BuySellRtrnTxt = (TextView) this.findViewById(R.id.BuySellRtrnTxt);
        final String pair = ((Spinner) this.findViewById(R.id.currencyMrktSpinner)).getSelectedItem().toString() + "EUR";
        final String orderType = "buy";
        final String volume = ((EditText) this.findViewById(R.id.volumeTxt)).getText().toString();
        new BuyTask(this, pair, orderType, volume).execute();
    }

    public void onBtnXBTChart(View v) {
        String url = "https://dwq4do82y8xi7.cloudfront.net/widgetembed/?symbol=KRAKEN%3AXBTEUR&interval=1&symboledit=1&toolbarbg=f1f3f6&hideideas=1&studies=&theme=White&style=1&timezone=exchange";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void onBtnETHChart(View v) {
        String url = "https://dwq4do82y8xi7.cloudfront.net/widgetembed/?symbol=KRAKEN%3AETHEUR&interval=1&symboledit=1&toolbarbg=f1f3f6&hideideas=1&studies=&theme=White&style=1&timezone=exchange";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void onBtnBCHChart(View v) {
        String url = "https://dwq4do82y8xi7.cloudfront.net/widgetembed/?symbol=KRAKEN%3ABCHEUR&interval=1&symboledit=1&toolbarbg=f1f3f6&hideideas=1&studies=&theme=White&style=1&timezone=exchange";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private static class TickerInfoTask extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        TickerInfoTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        private Exception exception;

        protected String doInBackground(String... params) {
            try {
                KrakenAPICaller kac = new KrakenAPICaller();
                return kac.tickerInfo();
            } catch (Exception e) {
                this.exception = e;

                return null;
            }
        }

        protected void onPostExecute(String commandRV) {
            if (this.exception != null) commandRV = this.exception.getMessage();
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            try {
                JSONObject myRV = new JSONObject(commandRV);
                String errStr = myRV.getString("error");
                if (errStr != null && errStr.length() > 3) {
                    throw new JSONException(errStr);
                }
                myRV = (JSONObject) myRV.get("result");
                JSONObject xbtObj = myRV.getJSONObject("XXBTZEUR");
                JSONObject ethObj = myRV.getJSONObject("XETHZEUR");
                JSONObject bchObj = myRV.getJSONObject("BCHEUR");
                activity.xbtLastTradePrice = xbtObj.getJSONArray("c").getDouble(0);
                activity.ethLastTradePrice = ethObj.getJSONArray("c").getDouble(0);
                activity.bchLastTradePrice = bchObj.getJSONArray("c").getDouble(0);
                commandRV = "XBT:" + activity.xbtLastTradePrice +
                        "   ETH:" + activity.ethLastTradePrice + "   BCH:" + activity.bchLastTradePrice;
            } catch (JSONException e) {
                commandRV = "JSON parsing error" + e.getMessage();
            }
            Date date = new Date();
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(activity.getApplicationContext());
            commandRV = dateFormat.format(date) + "\n" + commandRV;
            final TextView tickerTxt = (TextView) activity.findViewById(R.id.tickerTxt);
            tickerTxt.setText(commandRV);
            activity.updateBalanceInfo();
        }
    }

    private static class BalanceTask extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        BalanceTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        private Exception exception;

        private String balanceReturn;
        private String tradeInfo;

        protected String doInBackground(String... params) {
            try {
                KrakenAPICaller kac = new KrakenAPICaller();
                balanceReturn = kac.account_balance();
                tradeInfo = kac.tradeInfo();
                return tradeInfo + balanceReturn;
            } catch (Exception e) {
                this.exception = e;

                return null;
            }
        }

        protected void onPostExecute(String commandRV) {
            if (this.exception != null) commandRV = this.exception.getMessage();
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            try {
                JSONObject myRV = new JSONObject(balanceReturn);
                myRV = (JSONObject) myRV.get("result");
                HashMap<String, Double> balance = activity.balance;
                balance.put("EUR", Double.valueOf(myRV.getString("ZEUR")));
                balance.put("XBT", Double.valueOf(myRV.getString("XXBT")));
                balance.put("ETH", Double.valueOf(myRV.getString("XETH")));
                balance.put("BCH", Double.valueOf(myRV.getString("BCH")));
                commandRV = "EUR:" + balance.get("EUR") + "\n"
                        + "XBT:" + balance.get("XBT") + "\n"
                        + "ETH:" + balance.get("ETH") + "\n"
                        + "BCH:" + balance.get("BCH");
                JSONObject myLedger = new JSONObject(tradeInfo);
                myLedger = (JSONObject) myLedger.get("result");
                myLedger = (JSONObject) myLedger.get("ledger");
                JSONArray ledgerNames = myLedger.names();
                JSONObject tradeCollection = new JSONObject();
                for (int i = 0; i < ledgerNames.length(); i++) {
                    JSONObject curLedgerItm = myLedger.getJSONObject(ledgerNames.getString(i));
                    JSONObject curLedgerWrp = new JSONObject();
                    curLedgerWrp.put(ledgerNames.getString(i), curLedgerItm);
                    String tradeRef = curLedgerItm.getString("refid");
                    tradeCollection.accumulate(tradeRef, curLedgerWrp);
                }
                JSONArray tCollNames = tradeCollection.names();
                JSONObject analysisRV = new JSONObject();
                analysisRV.put("XXBT", new JSONArray());
                analysisRV.put("XETH", new JSONArray());
                analysisRV.put("BCH", new JSONArray());
                for (int i = 0; i < tCollNames.length(); i++) {
                    boolean isSell = false;
                    JSONArray curTCitmArray = tradeCollection.getJSONArray(tCollNames.getString(i));
                    Double amountEUR = 0.0;
                    Double amountOTHER = 0.0;
                    String assetOTHER = "NONE";
                    for (int j = 0; j < curTCitmArray.length(); j++) {
                        JSONObject subLedgerEntry = curTCitmArray.getJSONObject(j);
                        JSONObject subSub = subLedgerEntry.getJSONObject(subLedgerEntry.names().getString(0));
                        String asset = subSub.getString("asset");
                        Double amount = subSub.getDouble("amount");
                        Double fee = subSub.getDouble("fee");
                        Double balancesub = subSub.getDouble("balance");
                        if (asset.equals("ZEUR")) {
                            if (amount > 0) isSell = true;
                            amountEUR = amount;
                        } else {
                            amountOTHER = amount;
                            assetOTHER = asset;
                        }
                    }
                    JSONObject analysisItm = new JSONObject();
                    analysisItm.put("EUR", amountEUR);
                    analysisItm.put("OTHER", amountOTHER);
                    analysisItm.put("isSell", isSell);
                    if(analysisRV.has(assetOTHER)) analysisRV.getJSONArray(assetOTHER).put(analysisItm);
                }
                activity.tradeAnalysis = analysisRV;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Date date = new Date();
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(activity.getApplicationContext());
            commandRV = dateFormat.format(date) + "\n" + commandRV;
            final TextView accBalTxt = (TextView) activity.findViewById(R.id.accountBalanceTxt);
            accBalTxt.setText(commandRV);
            activity.updateBalanceInfo();
        }
    }

    private static class OpenOrdersTask extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        OpenOrdersTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        private Exception exception;

        protected String doInBackground(String... params) {
            try {
                KrakenAPICaller kac = new KrakenAPICaller();
                return kac.openOrders();
            } catch (Exception e) {
                this.exception = e;

                return null;
            }
        }

        protected void onPostExecute(String commandRV) {
            if (this.exception != null) commandRV = this.exception.getMessage();
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            try {
                JSONObject myRV = new JSONObject(commandRV);
                myRV = (JSONObject) myRV.get("result");
                myRV = (JSONObject) myRV.get("open");
                JSONArray orderNames = myRV.names();
                if (orderNames != null) {
                    commandRV = "Orders:\n";
                    for (int i = 0; i < orderNames.length(); i++) {
                        JSONObject singleOrder = (JSONObject) myRV.get((String) orderNames.get(i));
                        JSONObject singleOrderDesc = (JSONObject) singleOrder.getJSONObject("descr");
                        commandRV += singleOrderDesc.getString("order") + "\n";
                    }
                } else {
                    commandRV = "no open orders";
                }
            } catch (JSONException e) {
                commandRV = "JSON parsing error";
            }
            Date date = new Date();
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(activity.getApplicationContext());
            commandRV = dateFormat.format(date) + "\n" + commandRV;
            final TextView openOrdersTxt = (TextView) activity.findViewById(R.id.openOrdersTxt);
            openOrdersTxt.setText(commandRV);
        }
    }

    private static class SellTask extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;
        protected String pair = "";
        protected String orderType = "";
        protected String volume = "";

        SellTask(MainActivity context, String pair, String ordertype, String volume) {
            this.pair = pair;
            this.orderType = ordertype;
            this.volume = volume;
            activityReference = new WeakReference<>(context);
        }

        private Exception exception;

        protected String doInBackground(String... params) {
            try {
                KrakenAPICaller kac = new KrakenAPICaller();
                String newOrder = kac.addOrderType_002(pair, orderType, null, volume.toString(), false);
                return newOrder;
            } catch (Exception e) {
                this.exception = e;

                return null;
            }
        }

        protected void onPostExecute(String commandRV) {
            if (this.exception != null) commandRV = this.exception.getMessage();
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            //{"error":[],"result":{"descr":{"order":"sell 0.10000000 XBTEUR @ market"}}}
            try {
                JSONObject myRV = new JSONObject(commandRV);
                myRV = (JSONObject) myRV.get("result");
                myRV = (JSONObject) myRV.get("descr");
                commandRV = myRV.getString("order");

            } catch (JSONException e) {
                commandRV = commandRV + "\nJSON parsing error";
            }
            Date date = new Date();
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(activity.getApplicationContext());
            commandRV = dateFormat.format(date) + "\n" + commandRV;
            final TextView BuySellRtrnTxt = (TextView) activity.findViewById(R.id.BuySellRtrnTxt);
            BuySellRtrnTxt.setText(commandRV);
        }
    }

    private static class BuyTask extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;
        private Exception exception;
        protected String pair = "";
        protected String orderType = "";
        protected String volume = "";

        BuyTask(MainActivity context, String pair, String ordertype, String volume) {
            this.pair = pair;
            this.orderType = ordertype;
            this.volume = volume;
            activityReference = new WeakReference<>(context);
        }

        protected String doInBackground(String... params) {
            try {
                KrakenAPICaller kac = new KrakenAPICaller();
                String newOrder = kac.addOrderType_002(pair, orderType, null, volume.toString(), false);
                return newOrder;
            } catch (Exception e) {
                this.exception = e;

                return null;
            }
        }

        protected void onPostExecute(String commandRV) {
            if (this.exception != null) commandRV = this.exception.getMessage();
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            //{"error":[],"result":{"descr":{"order":"sell 0.10000000 XBTEUR @ market"}}}
            try {
                JSONObject myRV = new JSONObject(commandRV);
                myRV = (JSONObject) myRV.get("result");
                myRV = (JSONObject) myRV.get("descr");
                commandRV = myRV.getString("order");

            } catch (JSONException e) {
                commandRV = commandRV + "\nJSON parsing error";
            }
            Date date = new Date();
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(activity.getApplicationContext());
            commandRV = dateFormat.format(date) + "\n" + commandRV;
            final TextView BuySellRtrnTxt = (TextView) activity.findViewById(R.id.BuySellRtrnTxt);
            BuySellRtrnTxt.setText(commandRV);
        }
    }
}

