
package com.novorobo.market;

import com.novorobo.tracker.app.MetricList; // really just here for hte characters

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.PendingIntent;
import android.content.pm.ResolveInfo;
import android.util.Log;

import android.os.RemoteException;
import org.json.JSONException;

import com.android.vending.billing.IInAppBillingService;




import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;




public class MarketInterface {
    public static final int BILLING_API_VERSION = 3;
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final int MAX_ITEMS_RETRIEVED_PER_CALL = 700;

    // generalized fuckups
    public static final String ERROR_UNINITIALIZED = "This isn't initialized yet. Don't touch it until the callback fires.";
    public static final int ERROR_CODE_BAD_RESPONSE = -347;
    public static final int ERROR_CODE_VERIFICATION_FAILURE = -348;
    public static final int ERROR_CODE_DISPOSED_UNEXPECTEDLY = -349;
    public static final int ERROR_CODE_REMOTE_EXCEPTION = -350;
    public static final int ERROR_CODE_MAX_ITEMS_EXCEEDED = -351;

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

    // Keys for the responses from InAppBillingService
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

    public static final int PURCHASE_STATE_PURCHASED = 0;
    public static final int PURCHASE_STATE_CANCELED = 1;
    public static final int PURCHASE_STATE_REFUNDED = 2;

    public static final String PREMIUM_SKU = "track_and_graph_premium_styling";
    public static final String TEST_PURCHASE_SKU = "android.test.purchased";
    public static final String TEST_CANCELED_SKU = "android.test.canceled";
    public static final String TEST_REFUNDED_SKU = "android.test.refunded";
    public static final String TEST_UNAVAILABLE_SKU = "android.test.item_unavailable";




    private Context context;
    private ServiceConnection service;
    private IInAppBillingService play;

    private ArrayList<String> inventory = new ArrayList<String>();
    
    private boolean initializationComplete = false;
    private boolean initializationFailed = false;
    private boolean serviceBound = false;
    private boolean disposed = false;

    private ArrayList<InitListener> initListeners = new ArrayList<InitListener>();

    public static class InitListener {
        public void onInit () {};
        public void onFail () {};
    }


    public MarketInterface (Context ctx) {
        context = ctx.getApplicationContext();

        service = new ServiceConnection () {
            public void onServiceConnected (ComponentName name, IBinder binder) {
                initPlay(binder);
            }
            public void onServiceDisconnected (ComponentName name) {
                service = null;
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");

        // if there's no-one to answer the phone, we fail
        List<ResolveInfo> markets = context.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (markets == null || markets.isEmpty()) {
            onInitFailure();
            return;
        }

        serviceBound = context.bindService(serviceIntent, service, Context.BIND_AUTO_CREATE);
        if (!serviceBound)
            onInitFailure();
    }


    public void dispose () {
        if (service != null && context != null && serviceBound)
            context.unbindService(service);

        context = null;
        service = null;
        play = null;
        disposed = true;
        inventory = null;
    }


    public void addInitListener (InitListener listener) {
        if (disposed) return;

        if (initializationFailed)
            listener.onFail();

        else if (initializationComplete)
            listener.onInit();

        else
            initListeners.add( listener );
    }


    private void initPlay (IBinder binder) {
        if (disposed) return;

        play = IInAppBillingService.Stub.asInterface(binder);

        // are we even able to do billing?
        try {
            int response = play.isBillingSupported(3, context.getPackageName(), ITEM_TYPE_INAPP);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                onInitFailure();
                return;
            }
        } 
        // I don't know what this represents. Cribbed from TrivialDrive.
        catch (RemoteException e) {
            onInitFailure();
            e.printStackTrace();
            return;
        }

        if (queryInventory() != BILLING_RESPONSE_RESULT_OK) {
            onInitFailure();
            return;
        }

        onInitSuccess();           
    }



    private void onInitFailure () {
        if (disposed) return;
        
        initializationFailed = true;
        initializationComplete = true;
        
        for (InitListener listener : initListeners)
            listener.onFail();
    }

    private void onInitSuccess () {
        if (disposed) return;

        initializationFailed = false;
        initializationComplete = true;
        
        for (InitListener listener : initListeners)
            listener.onInit();
    }



    private int queryInventory () {
        if (disposed)
            return ERROR_CODE_DISPOSED_UNEXPECTEDLY;

        Bundle items;
        try {
            items = play.getPurchases(BILLING_API_VERSION, context.getPackageName(), ITEM_TYPE_INAPP, null);
        } 
        catch (RemoteException e) { return ERROR_CODE_REMOTE_EXCEPTION; }

        int response = getResponseCodeFromBundle( items );
        if (response != BILLING_RESPONSE_RESULT_OK)
            return response;

        if (!items.containsKey(RESPONSE_INAPP_ITEM_LIST) ||
            !items.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST) ||
            !items.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) 
        {
            return ERROR_CODE_BAD_RESPONSE;
        }

        ArrayList<String> ownedSkus = items.getStringArrayList( RESPONSE_INAPP_ITEM_LIST );
        ArrayList<String> purchaseDataList = items.getStringArrayList( RESPONSE_INAPP_PURCHASE_DATA_LIST );
        ArrayList<String> signatureList = items.getStringArrayList( RESPONSE_INAPP_SIGNATURE_LIST );

        // THIS CODE ASSUMES THAT NO MORE THAN 699 IN-APP ITEMS WILL BE BOUGHT
        // I THINK THAT'S OKAY, BECAUSE I ONLY INTEND TO HAVE ONE AVAILABLE
        if (ownedSkus.size() == MAX_ITEMS_RETRIEVED_PER_CALL)
            return ERROR_CODE_MAX_ITEMS_EXCEEDED;

        for (int i = 0; i < purchaseDataList.size(); i++) {
            String purchaseDataStr = purchaseDataList.get(i);
            String signature = signatureList.get(i);
            String sku = ownedSkus.get(i);

            try {
                if (!verifyPurchase(purchaseDataStr, signature)) {
                    //consume(sku);
                    inventory.clear();
                    return ERROR_CODE_VERIFICATION_FAILURE;
                }
            } catch (IllegalArgumentException e) {
                //consume(sku);
                continue;
            }

            inventory.add(sku);
        }

        return BILLING_RESPONSE_RESULT_OK;
    }


    private void consume (String sku) {
        try {
            String packageName = context.getPackageName();
            int result = play.consumePurchase(3, packageName, "inapp:"+packageName+":"+sku );
        }
        catch (RemoteException e) {}
    }


    public PendingIntent getBuyPremiumIntent () {
        if (disposed || initializationFailed || !initializationComplete)
            return null;

        String packageName = context.getPackageName();
        Bundle buyIntentBundle = null;

        try {
            buyIntentBundle = play.getBuyIntent(BILLING_API_VERSION, packageName, PREMIUM_SKU, ITEM_TYPE_INAPP, "");
        } catch (RemoteException e) {
            return null;
        }

        int response = getResponseCodeFromBundle(buyIntentBundle);

        // There are a variety of reasons that this might not work.
        // I'm not really interested in discriminating between them.
        if (response != BILLING_RESPONSE_RESULT_OK)
            return null;
        
        return buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
    }


    public boolean ownsPremium () {
        if (disposed || initializationFailed || !initializationComplete)
            return false;

        for (String sku : inventory)
            if (sku.equals(PREMIUM_SKU))
                return true;

        return false;
    }

    public void refreshInventory () {
        if (disposed || initializationFailed || !initializationComplete)
            return;

        inventory.clear();
        queryInventory();
    }









    // Workaround to bug where sometimes response codes come as Long instead of Integer
    public static int getResponseCodeFromBundle (Bundle bundle) {
        Object code = bundle.get(RESPONSE_CODE);

        // bundle with a null response code is assumed to be ok (apparently, a known issue)
        if (code == null)
            return BILLING_RESPONSE_RESULT_OK;

        if (code instanceof Integer)
            return ((Integer) code).intValue();

        if (code instanceof Long)
            return (int) ((Long) code).longValue();

        throw new RuntimeException("Unexpected type for bundle response code: " + code.getClass().getName());
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    public static int getResponseCodeFromIntent (Intent intent) {
        Object code = intent.getExtras().get(RESPONSE_CODE);

        // intent with no response code is assumed to be ok (apparently, a known issue)
        if (code == null)
            return BILLING_RESPONSE_RESULT_OK;

        if (code instanceof Integer)
            return ((Integer) code).intValue();

        if (code instanceof Long)
            return (int) ((Long) code).longValue();

        throw new RuntimeException("Unexpected type for intent response code: " + code.getClass().getName());
    }









    // good e-fucking-nough. Peeps want to crack my app, 
    // I'm not gonna be able to stop 'em like this anyhow.
    private static String PUBLIC_KEY;
    static {
        char[] c = MetricList.CHARACTERS.toCharArray();
        PUBLIC_KEY = "" +
            c[12]+c[8 ]+c[8 ]+c[1 ]+c[8 ]+c[35]+c[0 ]+c[13]+c[1 ]+c[32]+
            c[36]+c[42]+c[33]+c[36]+c[34]+c[6 ]+c[60]+c[48]+c[61]+c[1 ]+
            c[0 ]+c[16]+c[4 ]+c[5 ]+c[0 ]+c[0 ]+c[14]+c[2 ]+c[0 ]+c[16]+
            c[59]+c[0 ]+c[12]+c[8 ]+c[8 ]+c[1 ]+c[2 ]+c[32]+c[10]+c[2 ]+
            c[0 ]+c[16]+c[4 ]+c[0 ]+c[61]+c[54]+c[34]+c[27]+c[4 ]+c[37]+
            c[39]+c[46]+c[54]+c[17]+c[42]+c[61]+c[19]+c[16]+c[55]+c[48]+
            c[40]+c[37]+c[54]+c[22]+c[22]+c[31]+c[39]+c[27]+c[26]+c[2 ]+
            c[39]+c[61]+c[55]+c[20]+c[21]+c[4 ]+c[2 ]+c[15]+c[37]+c[47]+
            c[5 ]+c[28]+c[18]+c[4 ]+c[7 ]+c[46]+c[50]+c[39]+c[20]+c[6 ]+
            c[60]+c[49]+c[17]+c[32]+c[22]+c[0 ]+c[61]+c[42]+c[30]+c[6 ]+
            c[16]+c[1 ]+c[10]+c[56]+c[56]+c[52]+c[10]+c[23]+c[45]+c[10]+
            c[42]+c[47]+c[30]+c[29]+c[41]+c[26]+c[14]+c[76]+c[16]+c[9 ]+
            c[31]+c[13]+c[41]+c[23]+c[76]+c[11]+c[20]+c[23]+c[6 ]+c[13]+
            c[9 ]+c[10]+c[61]+c[20]+c[18]+c[44]+c[42]+c[11]+c[28]+c[39]+
            c[9 ]+c[4 ]+c[35]+c[30]+c[40]+c[61]+c[40]+c[1 ]+c[21]+c[2 ]+
            c[18]+c[46]+c[20]+c[13]+c[52]+c[57]+c[32]+c[31]+c[19]+c[55]+
            c[56]+c[11]+c[58]+c[8 ]+c[59]+c[17]+c[1 ]+c[43]+c[30]+c[0 ]+
            c[34]+c[13]+c[19]+c[40]+c[3 ]+c[12]+c[14]+c[55]+c[33]+c[49]+
            c[14]+c[12]+c[42]+c[93]+c[43]+c[18]+c[30]+c[44]+c[39]+c[40]+
            c[18]+c[3 ]+c[41]+c[29]+c[51]+c[37]+c[19]+c[15]+c[39]+c[14]+
            c[50]+c[17]+c[31]+c[9 ]+c[28]+c[30]+c[31]+c[26]+c[19]+c[3 ]+
            c[40]+c[47]+c[41]+c[44]+c[20]+c[55]+c[6 ]+c[22]+c[30]+c[16]+
            c[11]+c[55]+c[31]+c[54]+c[21]+c[28]+c[51]+c[18]+c[34]+c[49]+
            c[93]+c[34]+c[1 ]+c[4 ]+c[35]+c[26]+c[26]+c[76]+c[21]+c[5 ]+
            c[25]+c[37]+c[23]+c[44]+c[20]+c[21]+c[93]+c[18]+c[26]+c[22]+
            c[33]+c[35]+c[42]+c[55]+c[37]+c[7 ]+c[41]+c[38]+c[2 ]+c[51]+
            c[12]+c[27]+c[60]+c[44]+c[13]+c[45]+c[31]+c[23]+c[5 ]+c[52]+
            c[55]+c[15]+c[29]+c[57]+c[20]+c[4 ]+c[20]+c[24]+c[10]+c[10]+
            c[57]+c[7 ]+c[45]+c[58]+c[52]+c[25]+c[16]+c[6 ]+c[13]+c[29]+
            c[19]+c[6 ]+c[16]+c[15]+c[41]+c[58]+c[59]+c[2 ]+c[34]+c[59]+
            c[24]+c[41]+c[27]+c[48]+c[10]+c[59]+c[13]+c[35]+c[45]+c[36]+
            c[9 ]+c[40]+c[23]+c[55]+c[29]+c[32]+c[60]+c[76]+c[41]+c[17]+
            c[38]+c[46]+c[76]+c[9 ]+c[37]+c[43]+c[57]+c[16]+c[37]+c[20]+
            c[0 ]+c[13]+c[30]+c[57]+c[6 ]+c[10]+c[9 ]+c[93]+c[34]+c[31]+
            c[54]+c[15]+c[37]+c[15]+c[38]+c[4 ]+c[4 ]+c[18]+c[47]+c[28]+
            c[47]+c[3 ]+c[60]+c[12]+c[33]+c[33]+c[44]+c[39]+c[24]+c[22]+
            c[46]+c[14]+c[41]+c[26]+c[9 ]+c[30]+c[21]+c[40]+c[32]+c[14]+
            c[13]+c[25]+c[60]+c[9 ]+c[5 ]+c[6 ]+c[27]+c[58]+c[18]+c[53]+
            c[0 ]+c[54]+c[57]+c[32]+c[12]+c[48]+c[8 ]+c[3 ]+c[0 ]+c[16]+
            c[0 ]+c[1 ];
    }

    
    public static boolean verifyPurchase (String signedData, String signature) {

        if (signedData == null || signature == null || signedData.isEmpty() || signature.isEmpty())
            return false;
        
        try {
            byte[] decodedKey = Base64.decode(PUBLIC_KEY);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key =  keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(key);
            sig.update(signedData.getBytes());
            
            return sig.verify(Base64.decode(signature));
        }
        catch (SignatureException e) { Log.e("gotcha", "Signature exception."); } 
        catch (InvalidKeyException e) { Log.e("gotcha", "Invalid key specification."); }
        catch (NoSuchAlgorithmException e) { Log.e("gotcha", "NoSuchAlgorithmException.");  throw new RuntimeException(e); } 
        catch (InvalidKeySpecException e)  { Log.e("gotcha", "Invalid key specification."); throw new IllegalArgumentException(e); } 
        catch (Base64.DecoderException e)  { Log.e("gotcha", "Base64 decoding failed: " + e.getMessage());    throw new IllegalArgumentException(e); }
        return false;
    }









}