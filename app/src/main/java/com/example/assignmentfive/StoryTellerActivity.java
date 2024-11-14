package com.example.assignmentfive;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StoryTellerActivity extends AppCompatActivity {
    String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    String API_KEY = "gAAAAABlTUA3tSkK0LBZ9N_7iD0g0_W5-wcVzSl3JZIcMv8MTid_rgvsQ_ZjKYOSPjvKbdgKza1FoI6GssEpDfmHYxe1_6YkRtdJlvdjaCNQs0DAOac0FAEdw8P6oVOb5-7wS-2wCbgB";

    //TextView context, keywords;
    String context, keywords;
    //SQLiteDatabase mydb;
    SQLiteDatabase bothdb;
    TextView story, findtext, tagsyouselected;
    CheckBox includesketches;
    StringBuilder tagsforstory;

    ListView lv;
    ImageListAdapter adapter;
    TextToSpeech tts;
    Button storybutton;
    private ArrayList<ImageItem> imagedata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_teller);

        // Create database for BOTH Photos and Sketches
        bothdb = this.openOrCreateDatabase("images", Context.MODE_PRIVATE, null);

        // Clear Out old images database
        //bothdb.execSQL("DROP TABLE IMAGES");

        bothdb.execSQL("CREATE TABLE IF NOT EXISTS IMAGES (IMAGE BLOB, DATETIME TEXT, TAGS TEXT, TYPE TEXT)");

        imagedata = new ArrayList<>();
        adapter = new ImageListAdapter(this, R.layout.list_item, imagedata);
        lv = findViewById(R.id.imagelist);
        lv.setAdapter(adapter);

        findtext = findViewById(R.id.tagsearch);
        story = findViewById(R.id.story);

        // Checkbox
        includesketches = (CheckBox) findViewById(R.id.checkbox);
        includesketches.setChecked(true);
        includesketches.setOnCheckedChangeListener(((buttonView, isChecked) -> updateImageList()));

        ArrayList<ImageItem> latestimgs = showLatestImages();
        adapter.updateData(latestimgs);

        Button backButton = findViewById(R.id.backbutton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoryTellerActivity.this, MainActivity.class);
            startActivity(intent);
        });

        tagsyouselected = findViewById(R.id.taglist);
        adapter.setItemCheckListener(() -> {
            updateSelectedTagText();
        });

        storybutton = findViewById(R.id.storybtn);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    private void updateImageList() {
        ArrayList<ImageItem> latestimgs = showLatestImages();
        adapter.updateData(latestimgs);
        adapter.notifyDataSetChanged();
    }

    private void updateSelectedTagText() { // “you selected [list of tags]” updates based on list item selection.
        tagsforstory = new StringBuilder();
        for (ImageItem imageItem : adapter.getDataList()) {
            if (imageItem.isChecked()) {
                if (tagsforstory.length() > 0) {
                    tagsforstory.append(", ");
                }
                tagsforstory.append(imageItem.getOnlyTagText());
            }
        }
        tagsyouselected.setText(tagsforstory);
        Log.i("Tags Selected END", tagsyouselected.getText().toString());
    }
    public void makeHttpRequest(String c, String[] k) throws JSONException { // context = c, keywords = k
        JSONObject data = new JSONObject();
        data.put("context", c); //always be story...
        data.put("max_tokens", 280);
        data.put("mode", "twitter");
        data.put("model", "claude-3-haiku");

        String[] keywords = k;
        data.put("keywords", new JSONArray(keywords));

        // JSON object is done now json obj request

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //textView.setText("Response: " + response.toString());
                Log.d("success", response.toString());
                //Log.i("Context and Keywords", context.getText().toString() + " ALSO " + keywords.getText().toString());
                // Try this
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray outputs = data.getJSONArray("outputs");
                    JSONObject resultobj = outputs.getJSONObject(0);
                    //story.setText("Generated Response: " + resultobj.getString("text"));
                    story.setText(resultobj.getString("text"));

                    // to perform the movement action
                    // Moves the cursor or scrolls to the
                    // top or bottom of the document
                    story.setMovementMethod(new ScrollingMovementMethod());

                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1");
                    // Speaking text prompt
                    tts.speak(story.getText().toString(),TextToSpeech.QUEUE_FLUSH,params);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
                Log.e("error", new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };
// Post Request
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    public void find(View view) {
        String searchtext = findtext.getText().toString();
        ArrayList<ImageItem> searchresults = new ArrayList<>();
        boolean withsketch = includesketches.isChecked();  //include sketch checkbox on?
        Cursor c;

        if (searchtext.equals("")) {
            searchresults = showLatestImages();
//            Log.i("SEARCHTEXT PRINTED", searchtext);
        } else {
            String[] findtags = searchtext.split(","); // split by commas
            StringBuilder queryBuilder = new StringBuilder(); // query for each tag
            queryBuilder.append("SELECT * FROM IMAGES WHERE ");

            if (!withsketch) {
                queryBuilder.append("TYPE = 'photo' AND ");
            }

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

            c = null;
            try {
                c = bothdb.rawQuery(query, queryParameters);
                while (c.moveToNext()) { // show/populate searched imgs
                    byte[] ba = c.getBlob(0);
                    String date = c.getString(1);
                    String tagsInDatabase = c.getString(2);
                    searchresults.add(new ImageItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tagsInDatabase + "\n" + date));
                }
            } catch (CursorIndexOutOfBoundsException e) {
                Log.e("Query Error", e.toString());
            } finally {
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            }
        }
        adapter.updateData(searchresults);
    }

    public ArrayList<ImageItem> showLatestImages() {
        boolean withsketches = includesketches.isChecked();
        // CHECKBOX IF STATEMENT for query
        String includequery;

        if (withsketches) {
            includequery = "SELECT * FROM IMAGES";
        } else {
            includequery = "SELECT * FROM IMAGES WHERE TYPE = 'photo'";
        }

        Cursor c = bothdb.rawQuery(includequery, null);
        ArrayList<ImageItem> latestimgs = new ArrayList<>();

        c.moveToLast();
        c.moveToNext();
        while (c.moveToPrevious()) {
            byte[] ba = c.getBlob(0);
            String date = c.getString(1);
            String tags = c.getString(2);
            latestimgs.add(new ImageItem(BitmapFactory.decodeByteArray(ba, 0, ba.length), tags + "\n" + date));
        }
        c.close();
        return latestimgs;
    }

    // Toasts! https://developer.android.com/guide/topics/ui/notifiers/toasts
    public void onSubmitStory(View view) throws JSONException {
        Toast toast = Toast.makeText(this, "Select tags for Story", Toast.LENGTH_SHORT);
        try {
            context = "story"; //it is defaulting to story or whatever we put here for context
            keywords = tagsyouselected.getText().toString(); // selected/checked from list
            if (keywords.isEmpty() || keywords.equals("")) {
                toast.show();
                story.setText("");
                throw new RuntimeException();
            }
            String[] keywordarray = keywords.split(","); // splitting keywords up
            for (int i = 0; i < keywordarray.length; i++) {
                keywordarray[i] = keywordarray[i].trim();
            }

            makeHttpRequest(context, keywordarray); // requesting TextCortex to spit out story

        } catch (Exception e) {
            toast.show(); // if none are selected toast will show up and will not continue
        }
    }
}