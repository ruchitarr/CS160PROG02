package edu.berkeley.cs160.drawing;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;
import com.googlecode.flickrjandroid.FlickrException;
import android.graphics.PorterDuff.Mode;
import java.util.ArrayList;
import java.util.List;
import android.widget.ArrayAdapter;

import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ListView;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.REST;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.SearchParameters;
import com.gmail.yuyang226.flickrj.sample.android.FlickrHelper;
import com.gmail.yuyang226.flickrj.sample.android.FlickrjActivity;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;

import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONException;

import android.widget.TextView;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;




public class MainActivity extends Activity implements OnClickListener {
    private Drawing drawView;
    private ImageView imView;
    private ImageButton currPaint, eraseBtn, brushBtn, newBtn, saveBtn, rectBtn, flickrBtn, speakButton;
    private int eraseBrush;
    public String saveDrawing, saveDrawing2;

    private float smallBrush, mediumBrush, largeBrush;
    private SeekBar seekBar;
    private TextView textView;
    private static final int REQUEST_CODE = 1234;
    private ListView resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        speakButton = (ImageButton) findViewById(R.id.speakButton);
        resultList = (ListView) findViewById(R.id.list);

        //drawView is displayed in the Activity on which we can call the methods in the Drawing class
        drawView = (Drawing) findViewById(R.id.drawing);

        //retrieve the color
        //step 1: first retrieve the linear layout where the color button is present
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors);
        //get the first color button
        currPaint = (ImageButton) paintLayout.getChildAt(0);
        //show currently selected button
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
        eraseBrush = 20;
        //retrieve a reference to the button
        eraseBtn = (ImageButton) findViewById(R.id.erase_btn);
        //set up the class to listen for clicks
        eraseBtn.setOnClickListener(this);

        //retrieve a reference to the button - for brush dialog
        brushBtn = (ImageButton) findViewById(R.id.brush_btn);
        //set up the class to listen for clicks
        brushBtn.setOnClickListener(this);

        //instantiate save button to listen for click events
        saveBtn = (ImageButton) findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        //instantiate save button to listen for click events
        newBtn = (ImageButton) findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        //retrieve a reference to the button
        rectBtn = (ImageButton) findViewById(R.id.circle_btn);
        //set up the class to listen for clicks
        rectBtn.setOnClickListener(this);

        flickrBtn = (ImageButton) findViewById(R.id.flickr_btn);
        //set up the class to listen for clicks
        flickrBtn.setOnClickListener(this);


        resultList = (ListView) findViewById(R.id.list);
        // Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            speakButton.setEnabled(false);
            Toast.makeText(getApplicationContext(), "Recognizer Not Found", Toast.LENGTH_SHORT).show();
        }

        speakButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity();
            }
        });




    }


    //Allow users to toggle between colors
    public void paintClicked(View view) {
        //use chosen color
        //check if the user has selected a color
        if (view != currPaint) {
//update color
            //retrieve tag for each color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            //call setColor method from the Drawing class
            drawView.setColor(color);
            //update the UI to reflect the newly selected color
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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






    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "AndroidBite Voice Recognition...");
        startActivityForResult(intent, REQUEST_CODE);
    }




    private void showImage() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String svr="www.flickr.com";

                    REST rest=new REST();
                    rest.setHost(svr);

                    //initialize Flickr object with key and rest
                    Flickr flickr=new Flickr(FlickrHelper.API_KEY,rest);

                    //initialize SearchParameter object, this object stores the search keyword
                    SearchParameters searchParams=new SearchParameters();
                    searchParams.setSort(SearchParameters.INTERESTINGNESS_DESC);

                    //Create tag keyword array
                    String[] tags=new String[]{"cs160fsm"};
                    searchParams.setTags(tags);

                    //Initialize PhotosInterface object
                    PhotosInterface photosInterface=flickr.getPhotosInterface();
                    //Execute search with entered tags
                    PhotoList photoList=photosInterface.search(searchParams,20,1);

                    //get search result and fetch the photo object and get small square imag's url
                    if(photoList!=null)
                    {
                        //Get search result and check the size of photo result
                        Random random = new Random();
                        int seed = random.nextInt(photoList.size());
                        //get photo object
                        Photo photo=(Photo)photoList.get(seed);

                        //Get small square url photo
                        InputStream is = photo.getMediumAsStream();
                        final Bitmap bm = BitmapFactory.decodeStream(is);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                try{
                                    Bitmap save = Bitmap.createBitmap(250, 288, Bitmap.Config.ARGB_8888);
                                    Paint paint = new Paint();
                                    paint.setColor(Color.WHITE);
                                    Canvas now = new Canvas(save);
                                    now.drawRect(new Rect(0, 0, 250, 288), paint);
                                    now.drawBitmap(bm, new Rect(0, 0, bm.getWidth(), bm.getHeight()), new Rect(0, 0, 250, 288), null);


                                    File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".png");
                                    file.createNewFile();
                                    OutputStream stream = new FileOutputStream(file);

                                    save.compress(Bitmap.CompressFormat.PNG, 100, stream);

                                    //START: Write code for updating cards with a new image
                                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, 250, 288, false);

                                    ListCard listCard = ToqActivity.mRemoteDeckOfCards.getListCard();
                                    int currSize = listCard.size();

                                    // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
                                    SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

                                    simpleTextCard.setHeaderText("Latest Image from Flickr ");
                                    simpleTextCard.setTitleText("#cs160fsm");

                                    simpleTextCard.setShowDivider(true);



                                    CardImage mCardImage= new CardImage("card.image.7", resizedBitmap);
                                    ToqActivity.mRemoteResourceStore.addResource(mCardImage);

                                    simpleTextCard.setCardImage(ToqActivity.mRemoteResourceStore,mCardImage);


                                    listCard.add(simpleTextCard);

                                    try {
                                        ToqActivity.mDeckOfCardsManager.updateDeckOfCards(ToqActivity.mRemoteDeckOfCards,ToqActivity.mRemoteResourceStore);
                                    } catch (RemoteDeckOfCardsException e) {
                                        e.printStackTrace();
                                        Toast.makeText(MainActivity.this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
                                    }

                                }
                                catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (FlickrException e) {
                    e.printStackTrace();
                } catch (IOException e ) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    File fileUri;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102) {

            if (resultCode == Activity.RESULT_OK) {
                Uri tmp_fileUri = data.getData();

                ((ImageView) findViewById(R.id.imview))
                        .setImageURI(tmp_fileUri);

                String selectedImagePath = getPath(tmp_fileUri);
                fileUri = new File(selectedImagePath);
            }

        }

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //resultList.setAdapter(new ArrayAdapter<String>(this,
            //        android.R.layout.simple_list_item_1, matches));
            for (String s : matches) {
                if (s.equals("save")) {
                    //Invoke save button
                    saveBtn.performClick();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }



    public void ShowDialogBrush()
    {
        final Dialog seekDialog = new Dialog(this);
        seekDialog.setTitle("Brush Size:");
        seekDialog.setContentView(R.layout.brush_chooser);


        final TextView seekTxt = (TextView)seekDialog.findViewById(R.id.textView1);
        final SeekBar seekBrushSize = (SeekBar)seekDialog.findViewById(R.id.seekBar1);

        seekBrushSize.setMax(100);

        //show current level
        int currLevel = drawView.getBrushSize();
        seekTxt.setText(currLevel+"%");
        seekBrushSize.setProgress(currLevel);

        //update as user interacts
        seekBrushSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekTxt.setText(Integer.toString(progress + 1)+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drawView.setBrushSize(seekBrushSize.getProgress() + 2);
            }

        });
        //listen for clicks on ok
        Button brushBtn = (Button)seekDialog.findViewById(R.id.brush_ok);
        brushBtn.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                drawView.setBrushSize(drawView.getLastBrushSize());
                seekDialog.dismiss();
            }
        });
        //show dialog
        seekDialog.show();
    }

    //add onClick method for handling brush sizes and other essential functions
    @Override
    public void onClick(View view) {
        //respond to clicks
        if(view.getId()==R.id.brush_btn){
            //draw button clicked
            drawView.setShapeType("Dot");
            ShowDialogBrush();
        }

        if(view.getId()==R.id.new_btn){
            //erase all
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();
        }







        //check for erasing
        if (view.getId() == R.id.erase_btn) {
            drawView.setErase(true);
            drawView.setBrushSize(eraseBrush);
        } else if (view.getId() == R.id.brush_btn) {
            drawView.setErase(false);
            drawView.setBrushSize(eraseBrush);
        } else if (view.getId() == R.id.new_btn) {
            drawView.invalidate();
        } else if (view.getId() == R.id.save_btn)
        {
           //fetch image from flickr
            showImage();

            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to Gallery?");
            //Positive Button Listener
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    drawView.setDrawingCacheEnabled(true);
                    Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString() + ".png", "drawing"));
                    saveDrawing2 = getRealPathFromURI(uri);
                    if (saveDrawing2 != null) {
                        Toast.makeText(getApplicationContext(), "Drawing saved to Gallery!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Image could not be saved.", Toast.LENGTH_SHORT).show();
                    }

                    drawView.destroyDrawingCache();
                }

            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            saveDialog.show();


        }//end save

        if(view.getId()==R.id.flickr_btn){
            //upload to flickr
            Intent intent = new Intent(getApplicationContext(),
                    FlickrjActivity.class);
            intent.putExtra("flickImagePath", saveDrawing2);

            startActivity(intent);

        }

        //circle brush
        else if (view.getId() == R.id.circle_btn)
        {
            drawView.setShapeType("Circle");
        }



    }


}
