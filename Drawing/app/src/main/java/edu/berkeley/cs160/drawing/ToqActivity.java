package edu.berkeley.cs160.drawing;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.Runnable;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.ResourceStoreException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

//for location
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class ToqActivity extends Activity {

    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    static DeckOfCardsManager mDeckOfCardsManager;
    static RemoteDeckOfCards mRemoteDeckOfCards;
    static RemoteResourceStore mRemoteResourceStore;
    ListCard listCard;
    SimpleTextCard simpleTextCard;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;
    HashMap<String,String> fsmCardImages;
    private DeckOfCardsManagerListener deckOfCardsManagerListener;
    private DeckOfCardsEventListener deckOfCardsEventListener = new DeckOfCardsEventListenerImpl();
    private ToqAppStateBroadcastReceiver toqAppStateReceiver;
    private ViewGroup notificationPanel;
    private ViewGroup deckOfCardsPanel;
    View installDeckOfCardsButton;
    View uninstallDeckOfCardsButton;
    private TextView statusTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toq);
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();
        init();
        setupUI();
    }

    /**
     * @see android.app.Activity#onStart()
     * This is called after onCreate(Bundle) or after onRestart() if the activity has been stopped
     */
    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "ToqApiDemo.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toq, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupUI() {

        findViewById(R.id.send_notif_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // sendNotification();
                trackLocation();
            }
        });

        findViewById(R.id.install_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                install();

            }
        });

        findViewById(R.id.uninstall_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uninstall();
                removeDeckOfCards();

            }
        });
        //trackLocation();

    }

    private void sendNotification() {

        //First find the location
        Set<String> keys = fsmCardImages.keySet();
        String[] keysArray = new String[0];
        keysArray = keys.toArray(keysArray);

        // get a random key to get a random value
        Random rand = new Random();
        String randomKey = keysArray[rand.nextInt(keysArray.length)];
        String myRandomValues = fsmCardImages.get(randomKey);
        System.out.println(myRandomValues);

        //then send notification
        String[] message = new String[2];

        message[0] = randomKey + " welcomes you at Sproul!";
        message[1] = "Find me on your watch's FSM Poster app and see further instructions.";
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "FSM Poster App", message);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send Notification", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Installs applet to Toq watch if app is not yet installed
     */
    private void install() {
        boolean isInstalled = true;

        try {

            isInstalled = mDeckOfCardsManager.isInstalled();
            // Get the launcher icons
            DeckOfCardsLauncherIcon whiteIcon = null;
            DeckOfCardsLauncherIcon colorIcon = null;
            try{
                whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
                colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("Can't get launcher icon");
                return;
            }

            mCardImages = new CardImage[6];
            try{
                //mCardImages[0]= new CardImage("card.image.1", getBitmap("image1.png"));
                mCardImages[0]= new CardImage("card.image.1", getBitmap("baez.png"));
                mCardImages[1]= new CardImage("card.image.2", getBitmap("goldberg.png"));
                mCardImages[2]= new CardImage("card.image.3", getBitmap("jgoldberg.png"));
                mCardImages[3]= new CardImage("card.image.4", getBitmap("rossman.png"));
                mCardImages[4]= new CardImage("card.image.5", getBitmap("savio.png"));
                mCardImages[5]= new CardImage("card.image.6", getBitmap("weinberg.png"));
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("Can't get picture icon");
                return;
            }
            // Try to retrieve a stored deck of cards
            try {
                // If there is no stored deck of cards or it is unusable, then create new and store
                /**  if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
                 mRemoteDeckOfCards = createDeckOfCards();
                 storeDeckOfCards();
                 }**/
            }
            catch (Throwable th){
                th.printStackTrace();
                mRemoteDeckOfCards = null; // Reset to force recreate
            }

            // Make sure in usable state
            if (mRemoteDeckOfCards == null){
                mRemoteDeckOfCards = createDeckOfCards();
            }

            // Set the custom launcher icons, adding them to the resource store
            mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});


        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }


        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
                Log.v("RR", "I am here!");
             /*   int i = 0;

                while (i<6) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    addSimpleTextCard();
                    Toast.makeText(this, "Im here",Toast.LENGTH_SHORT).show();
                    i=i+1;
                }
//                addSimpleTextCard(); --- RR: images not showing up in card*/


            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uninstall() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
           // sendNotification();
        }

        if (isInstalled) {
            try{
                mDeckOfCardsManager.uninstallDeckOfCards();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, getString(R.string.error_uninstalling_deck_of_cards), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.already_uninstalled), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a deck of cards to the applet
     */
    private void addSimpleTextCard() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        int currSize = listCard.size();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

        simpleTextCard.setHeaderText("Header: " + Integer.toString(currSize+1));
        simpleTextCard.setTitleText("Title: " + Integer.toString(currSize+1));
        String[] messages = {"Message: " + Integer.toString(currSize+1)};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        try{
            CardImage cardImage= simpleTextCard.getCardImage(mRemoteResourceStore);

        }
        catch (ResourceStoreException e){
            Log.w(Constants.TAG, "ToqApiDemo.initUI - an error occurred retrieving the card image", e);
        }
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[2]);

        simpleTextCard.setShowDivider(true);

        listCard.add(simpleTextCard);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
        }
    }


    private void removeDeckOfCards() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        if (listCard.size() == 0) {
            return;
        }

        listCard.remove(0);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to delete Card from ListCard", Toast.LENGTH_SHORT).show();
        }

    }

    //
    private CardImage parseCardImageFromUI(int selectedItemPos){

        switch (selectedItemPos){

            case 1:
                return mCardImages[0];

            case 2:
                return mCardImages[1];

            case 3:
                return mCardImages[2];
            case 4:
                return mCardImages[3];

            default:
                return null;

        }

    }
    // Initialise
    private void init() {

        fsmCardImages = new HashMap<String, String>();
        Log.v("RR", "init function!");

        fsmCardImages.put("Mario Savio", "Express your view of free speech in a drawing");
        fsmCardImages.put("Joan Baez", "Draw a megaphone");
        fsmCardImages.put("Art Goldberg", "Draw Now");
        fsmCardImages.put("Michael Rossman", "Draw Free Speech");
        fsmCardImages.put("Jack Weinberg", "Draw FSM");
        fsmCardImages.put("Jackie Goldberg", "Draw SLATE");

        // Create the resource store for icons and images
        mRemoteResourceStore = new RemoteResourceStore();


        // Panels
        notificationPanel= (ViewGroup)findViewById(R.id.notification_panel);
        deckOfCardsPanel= (ViewGroup)findViewById(R.id.doc_panel);

        setChildrenEnabled(deckOfCardsPanel, false);
        setChildrenEnabled(notificationPanel, false);
        installDeckOfCardsButton = findViewById(R.id.install_button);
        uninstallDeckOfCardsButton = findViewById(R.id.uninstall_button);
        // Create the state receiver
        toqAppStateReceiver= new ToqAppStateBroadcastReceiver();
        // Register toq app state receiver
        deckOfCardsManagerListener= new DeckOfCardsManagerListenerImpl();
        deckOfCardsEventListener= new DeckOfCardsEventListenerImpl();
        mDeckOfCardsManager.addDeckOfCardsManagerListener(deckOfCardsManagerListener);
        mDeckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);


        // Status
        Log.v("RR", "get status!");
        statusTextView= (TextView)findViewById(R.id.status_text);
        statusTextView.setText("Initialised");

        registerToqAppStateReceiver();

        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){

            setStatus(getString(R.string.status_connecting));

            Log.d(Constants.TAG, "ToqApiDemo.onStart - not connected, connecting...");

            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, getString(R.string.error_connecting_to_service), Toast.LENGTH_SHORT).show();
                Log.e(Constants.TAG, "ToqApiDemo.onStart - error connecting to Toq app service", e);
            }

        }
        else{
            Log.d(Constants.TAG, "ToqApiDemo.onStart - already connected");
            setStatus(getString(R.string.status_connected));
            refreshUI();
        }



    }
    // Set status bar message
    private void setStatus(String msg){
        statusTextView.setText(msg);
    }

    // Register state receiver
    private void registerToqAppStateReceiver(){
        IntentFilter intentFilter= new IntentFilter();
        intentFilter.addAction(Constants.BLUETOOTH_ENABLED_INTENT);
        intentFilter.addAction(Constants.BLUETOOTH_DISABLED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_PAIRED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_UNPAIRED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_CONNECTED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_DISCONNECTED_INTENT);
        getApplicationContext().registerReceiver(toqAppStateReceiver, intentFilter);
    }
    // Connected to Toq app service, so refresh the UI
    private void refreshUI(){

        try{

            // If Toq watch is connected
            if (mDeckOfCardsManager.isToqWatchConnected()){

                // If the deck of cards applet is already installed
                if (mDeckOfCardsManager.isInstalled()){
                    Log.d(Constants.TAG, "ToqApiDemo.refreshUI - already installed");
                    updateUIInstalled();
                }
                // Else not installed
                else{
                    Log.d(Constants.TAG, "ToqApiDemo.refreshUI - not installed");
                    updateUINotInstalled();
                }

            }
            // Else not connected to the Toq app
            else{
                Log.d(Constants.TAG, "ToqApiDemo.refreshUI - Toq watch is disconnected");
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_disconnected), Toast.LENGTH_SHORT).show();
                disableUI();
            }

        }
        catch (RemoteDeckOfCardsException e){
            Toast.makeText(this, getString(R.string.error_checking_status), Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "ToqApiDemo.refreshUI - error checking if Toq watch is connected or deck of cards is installed", e);
        }

    }

    // Disable all UI components
    private void disableUI(){
        // Disable everything
        setChildrenEnabled(deckOfCardsPanel, false);
        setChildrenEnabled(notificationPanel, false);
    }
    // Set up UI for when deck of cards applet is already installed
    private void updateUIInstalled(){

//        // Panels
//        notificationPanel= (ViewGroup)findViewById(R.id.notification_panel);
//        deckOfCardsPanel= (ViewGroup)findViewById(R.id.doc_panel);

        // Enable everything
        setChildrenEnabled(deckOfCardsPanel, true);
        setChildrenEnabled(notificationPanel, true);

        // Install disabled; update, uninstall enabled
        installDeckOfCardsButton.setEnabled(false);

        uninstallDeckOfCardsButton.setEnabled(true);


    }

    // Set up UI for when deck of cards applet is not installed
    private void updateUINotInstalled(){

        // Disable notification panel
        setChildrenEnabled(notificationPanel, false);

        // Enable deck of cards panel
        setChildrenEnabled(deckOfCardsPanel, true);



        // Install enabled; update, uninstall disabled
        installDeckOfCardsButton.setEnabled(true);

        uninstallDeckOfCardsButton.setEnabled(false);

        // Focus
        installDeckOfCardsButton.requestFocus();
    }





    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    /**
     * Uses SharedPreferences to store the deck of cards
     * This is mainly used to
     */
    private void storeDeckOfCards() throws Exception{
        Log.v("RR", "storeDeckofCards!");
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        //create simpletextcard here
        //listcard must have 6 diff cards (header and image)
        listCard= new ListCard();
        int i=0;


        /*----remove the first two blank cards


        simpleTextCard= new SimpleTextCard("card");
        listCard.add(simpleTextCard);

        simpleTextCard= new SimpleTextCard("card1");
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);*/

        for (Map.Entry<String,String> entry : fsmCardImages.entrySet()) {


            String key = entry.getKey();
            String value = entry.getValue();
            simpleTextCard = new SimpleTextCard(Integer.toString(i));
            try {

                mRemoteResourceStore.addResource(mCardImages[i]);

                simpleTextCard.setCardImage(mRemoteResourceStore,mCardImages[i]);

            }
            catch (Exception e){
                e.printStackTrace();
            }
            simpleTextCard.setHeaderText(key);
            simpleTextCard.setTitleText(value);
            simpleTextCard.setReceivingEvents(true);
            simpleTextCard.setShowDivider(true);

            listCard.add(simpleTextCard);
            Log.v("RR", "card added!");

            i++;
        }


        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards,mRemoteResourceStore);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
        }
        return new RemoteDeckOfCards(this, listCard);

    }

    private void trackLocation(){
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                double startLat = location.getLatitude();
                double startLon = location.getLongitude();
                double endLat = 37.86965;
               // double endLat=37.365755;
                double endLon = -122.25914;
               // double endLon = -122.024196;
                float[] result = new float[1];
                Location.distanceBetween(startLat, startLon, endLat, endLon, result);
               // Toast.makeText(getApplicationContext(), "Distance is"+result[0], Toast.LENGTH_SHORT).show();

               if(result[0]>=50) {
                   //reached Sproul, send notification
                   Toast.makeText(getApplicationContext(), "You have arrived at Sproul Plaza! Check your watch for more instructions.", Toast.LENGTH_SHORT).show();

                   sendNotification();


               }
                else
               {
                   Toast.makeText(getApplicationContext(), "Go to Sproul Plaza!!", Toast.LENGTH_SHORT).show();
               }


            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}

        };

        // Register the listener with the Location Manager to receive location updates
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Get update every 5 seconds
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, locationListener);
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Get update every 5 seconds
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, locationListener);
        }

    }


    // Enable/Disable a view group's children and nested children
    private void setChildrenEnabled(ViewGroup viewGroup, boolean isEnabled){

        for (int i = 0; i < viewGroup.getChildCount();  i++){

            View view= viewGroup.getChildAt(i);

            if (view instanceof ViewGroup){
                setChildrenEnabled((ViewGroup)view, isEnabled);
            }
            else{
                view.setEnabled(isEnabled);
            }

        }

    }
    // Toq app state receiver
    private class ToqAppStateBroadcastReceiver extends BroadcastReceiver {


        /**
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        public void onReceive(Context context, Intent intent){
            Log.v("RR", "in broadcastreceiver!");

            String action= intent.getAction();

            if (action == null){
                Log.w(Constants.TAG, "ToqApiDemo.ToqAppStateBroadcastReceiver.onReceive - action is null, returning");
                return;
            }

            Log.d(Constants.TAG, "ToqApiDemo.ToqAppStateBroadcastReceiver.onReceive - action: " + action);

            // If watch is now connected, refresh UI
            if (action.equals(Constants.TOQ_WATCH_CONNECTED_INTENT)){
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_connected), Toast.LENGTH_SHORT).show();
                refreshUI();
            }
            // Else if watch is now disconnected, disable UI
            else if (action.equals(Constants.TOQ_WATCH_DISCONNECTED_INTENT)){
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_disconnected), Toast.LENGTH_SHORT).show();
                disableUI();
            }

        }

    }
    // Handle service connection lifecycle and installation events
    private class DeckOfCardsManagerListenerImpl implements DeckOfCardsManagerListener{

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onConnected()
         */
        public void onConnected(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_connected));
                    refreshUI();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onDisconnected()
         */
        public void onDisconnected(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_disconnected));
                    disableUI();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationSuccessful()
         */
        public void onInstallationSuccessful(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_installation_successful));
                    updateUIInstalled();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationDenied()
         */
        public void onInstallationDenied(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_installation_denied));
                    updateUINotInstalled();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onUninstalled()
         */
        public void onUninstalled(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_uninstalled));
                    updateUINotInstalled();
                }
            });
        }

    }


    // Handle card events triggered by the user interacting with a card in the installed deck of cards
    private class DeckOfCardsEventListenerImpl implements DeckOfCardsEventListener {



        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardOpen(java.lang.String)
         */
        public void onCardOpen(final String cardId){
            System.out.println("RR in card open event!");
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_card_open) + cardId, Toast.LENGTH_SHORT).show();

                    //Switch to drawing activity --
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                    startActivity(intent);


                }
            });
        }
        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardVisible(java.lang.String)
         */
        public void onCardVisible(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_card_visible) + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardInvisible(java.lang.String)
         */
        public void onCardInvisible(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_card_invisible) + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardClosed(java.lang.String)
         */
        public void onCardClosed(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_card_closed) + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_menu_option_selected) + cardId + " [" + menuOption + "]", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, getString(R.string.event_menu_option_selected) + cardId + " [" + menuOption + ":" + quickReplyOption +
                            "]", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

}


