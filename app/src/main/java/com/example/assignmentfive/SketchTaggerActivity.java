package com.example.assignmentfive;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SketchTaggerActivity extends AppCompatActivity {
    private final String API_KEY = "AIzaSyDbD64arfemaYl96DGyYzIaa7GWgAXr0e4";
    private File imageDir;
    private ImageView takenimage, matchimage1, matchimage2, matchimage3;
    TextView textinfo1, textinfo2, textinfo3;
    TextView tagtext, findtext;
    private String[] allFiles;
    SQLiteDatabase mydb, bothdb;

    ListView lv;
    ImageListAdapter adapter;
    private ArrayList<ImageItem> imagedata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch_tagger);

        imagedata = new ArrayList<>();
        adapter = new ImageListAdapter(this, R.layout.list_item, imagedata);

        lv = findViewById(R.id.sketchlist);
        lv.setAdapter(adapter);

        mydb = this.openOrCreateDatabase("sketches", Context.MODE_PRIVATE, null);

        // Clear Out old photos database
        //mydb.execSQL("DROP TABLE SKETCHES");

        mydb.execSQL("CREATE TABLE IF NOT EXISTS SKETCHES (IMAGE BLOB, DATETIME TEXT, TAGS TEXT)");
        // Switching to Separate Tables for Photos and Drawings

        // Create database for BOTH Photos and Sketches
        bothdb = this.openOrCreateDatabase("images", Context.MODE_PRIVATE, null);

        // Clear Out old images database
        //bothdb.execSQL("DROP TABLE IMAGES");

        bothdb.execSQL("CREATE TABLE IF NOT EXISTS IMAGES (IMAGE BLOB, DATETIME TEXT, TAGS TEXT, TYPE TEXT)");

        tagtext = findViewById(R.id.vision_tags);
        findtext = findViewById(R.id.tagsearch);

        ArrayList<ImageItem> latestimgs = showLatestImages();
        adapter.updateData(latestimgs);

        Button backButton = findViewById(R.id.backbutton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
            startActivity(intent);
            //closeKeyboard();
        });
    }

    private void closeKeyboard()
    {
        // this will give us the view
        // which is currently focus
        // in this layout
        View view = this.getCurrentFocus();

        // if nothing is currently
        // focus then this will protect
        // the app from crash
        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            InputMethodManager manager
                = (InputMethodManager)
                    getSystemService(
                        Context.INPUT_METHOD_SERVICE);
            manager
                .hideSoftInputFromWindow(
                    view.getWindowToken(), 0);
        }
    }

    private void myVisionTester(Bitmap img, VisionCallback callback) throws IOException {

        //1. ENCODE image.
        //Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(item.imageResource)).getBitmap(); // food img resource from food class

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport(); // deprecated
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyDbD64arfemaYl96DGyYzIaa7GWgAXr0e4"));
        Vision vision = builder.build();

        //4. CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();
        Log.v("MYTAG", response.toPrettyString());

        String visionname = "";
        if (response != null && response.getResponses() != null && !response.getResponses().isEmpty()) {
            List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();
            if (annotations != null && response.getResponses() != null) {
                StringBuilder namebuilder = new StringBuilder();
                for (EntityAnnotation annotation : annotations) {
                    if (annotation.getScore() != null && annotation.getScore() >= 0.85) {  // matching score
                        namebuilder.append(annotation.getDescription()).append(", ");
                    }
                    if (annotation.getScore() != null && annotation.getScore() <= 0.85) {
                        visionname = annotations.get(0).getDescription();
                    }
                    if (namebuilder.length() > 0) {
                        visionname = namebuilder.substring(0, namebuilder.length() - 2);
                    }
                }
            }
            // callback >> result sent
            callback.onResult(visionname);
        }
    }

    // This table should consist of columns such as image (as a blob), date and time, and a list of tags.
    public void onClick(View view) { // Saving Image into SQL Database
        DrawableView drawableView = findViewById(R.id.drawing);

        Bitmap drawablebitmap = drawableView.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        drawablebitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();

        // We are not creating a database everytime "SAVE" is clicked so make below Global???
        // SQLiteDatabase mydb = this.openOrCreateDatabase("photos", Context.MODE_PRIVATE, null);
        // mydb.execSQL("CREATE TABLE IF NOT EXISTS IMAGES (IMAGE BLOB, DATE DATETIME, TAGS TEXT)");

        String timeStamp = new SimpleDateFormat("MM/dd/yy hh:mm:ssa").format(new Date());
        String tagString = tagtext.getText().toString(); // getting value of tagtext box

        // Insert using Content Values
        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);
        cv.put("DATETIME", timeStamp);
        cv.put("TAGS", tagString);
        mydb.insert("SKETCHES", null, cv);

        // Insert into combined database bothdb
        ContentValues cvboth = new ContentValues();
        cvboth.put("IMAGE", ba);
        cvboth.put("DATETIME", timeStamp);
        cvboth.put("TAGS", tagString);
        cvboth.put("TYPE", "sketch");
        bothdb.insert("IMAGES", null, cvboth);

        adapter.notifyDataSetChanged();
        showLatestImages();
        find(lv); // updates list
        tagtext.setText("");
        closeKeyboard();
    }


    public void find(View view) { // searching with tags
        Cursor c;
        String searchtext = findtext.getText().toString();
        ArrayList<ImageItem> searchresults = new ArrayList<>();

        if (searchtext.equals("")) {
            c = mydb.rawQuery("SELECT * FROM SKETCHES ORDER BY DATETIME DESC", null);
            //showLatestImages(c);
            searchresults = showLatestImages();
            adapter.updateData(searchresults);
            closeKeyboard();
            //Log.i("SEARCHTEXT PRINTED", "THIS IS SEARCHTEXT " + searchtext );
        } else {
            try {
                String[] findtags = searchtext.split(","); // split by commas
                StringBuilder queryBuilder = new StringBuilder(); // query for each tag
                queryBuilder.append("SELECT * FROM SKETCHES WHERE ");

                for (int i = 0; i < findtags.length; i++) {
                    if (i > 0) {
                        queryBuilder.append(" OR ");
                    }
                    queryBuilder.append("TAGS LIKE ?");
                }

                queryBuilder.append(" ORDER BY DATETIME DESC");
                String query = queryBuilder.toString();

                String[] queryParameters = new String[findtags.length]; // array for each param of search tag

                for (int i = 0; i < findtags.length; i++) { // run query for each parameter
                    queryParameters[i] = "%" + findtags[i].trim() + "%";
                }

                c = mydb.rawQuery(query, queryParameters);
                int position = 1;

                while (c.moveToNext() && position <= 3) { // show/populate searched imgs
                    byte[] ba = c.getBlob(0);
                    String date = c.getString(1);
                    String tagsInDatabase = c.getString(2);
                    searchresults.add(new ImageItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tagsInDatabase + "\n" + date));
                    position++;
                }


            } catch (CursorIndexOutOfBoundsException e) {
            }
            adapter.updateData(searchresults);
            closeKeyboard();
            Log.i("SEARCHTEXT PRINTED", "THIS IS THE SEARCH TEXT " + searchtext);
        }
    }

    public ArrayList<ImageItem> showLatestImages() {
        Cursor c = mydb.rawQuery("SELECT * FROM SKETCHES", null);
        ArrayList<ImageItem> latestimgs = new ArrayList<>();

        c.moveToLast();
        c.moveToNext();
        while (c.moveToPrevious()) {
            byte [] ba = c.getBlob(0);
            String date = c.getString(1);
            String tags = c.getString(2);
            latestimgs.add(new ImageItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tags +"\n" + date));
        }
        c.close();
        return latestimgs;
    }

    public void onIdentify(View view) {
        DrawableView drawableView = findViewById(R.id.drawing);
        Bitmap b = drawableView.getBitmap();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myVisionTester(b, new VisionCallback() {
                        @Override
                        public void onResult(String result) {
                            // update with vision generated tags
                            // not needed >> data.get(index).visionname = result;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //adapter.notifyDataSetChanged();
                                    tagtext.setText(result);
                                }
                            });
                        }
                    });
                } catch (IOException e) {
                    Log.e("vision", e.toString());
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void clearCanvas(View view) {
        DrawableView drawableView = findViewById(R.id.drawing);
        tagtext.setText("");
        drawableView.resetPath();
    }

    public interface  VisionCallback {
        void onResult(String result);
    }
}

