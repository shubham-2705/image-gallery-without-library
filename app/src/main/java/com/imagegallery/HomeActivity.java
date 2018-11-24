package com.imagegallery;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.shubhamlamba.imagegallery.R;
import com.google.gson.Gson;
import com.imagegallery.data.ImageItem;
import com.imagegallery.data.ImageListResponse;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private ImageListAdapter mImageAdapter;
    private RecyclerView mRvImageList;
    private int mPageNo;
    private int mListSavedPos;
    private ArrayList<ImageItem> mImageListResponse = new ArrayList<>();
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mRvImageList = findViewById(R.id.rv_image);
        mRvImageList.setLayoutManager(new GridLayoutManager(this,2));
        mImageAdapter = new ImageListAdapter(mImageListResponse, this);
        mRvImageList.setAdapter(mImageAdapter);

        mRvImageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        int totalItems = mImageAdapter.getItemCount();
                        int lastVisibleItemPosition = ((GridLayoutManager) mRvImageList.getLayoutManager()).findLastVisibleItemPosition();
                        if (lastVisibleItemPosition >= totalItems - 2 && mPageNo < 3) {
                            callApi(mPageNo);
                        }
                        break;
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        if (savedInstanceState != null && savedInstanceState.getInt("list_position", 0) > 0) {
            mListSavedPos = savedInstanceState.getInt("list_position");
            getDataFromCache();
        } else {
            if (isNetworkAvailable(this)) {
                callApi(mPageNo);
            } else {
                Toast.makeText(this, " Please connect to Internet", Toast.LENGTH_SHORT).show();
                getDataFromCache();
            }
        }
    }

    private void getDataFromCache() {
        Cache cache = Volley.newRequestQueue(this).getCache();
        Cache.Entry entry = cache.get(getUrl(0));

        if(entry != null){
            try {
                Gson gson = new Gson();
                String data = new String(entry.data ,"UTF-8");
                ImageListResponse imageListResponse = gson.fromJson(data, ImageListResponse.class);
                int size = mImageListResponse.size();
                mImageListResponse.addAll(imageListResponse.getPhotos().getPhoto());
                mImageAdapter.notifyItemRangeInserted(size, imageListResponse.getPhotos().getPhoto().size());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }


    private void callApi(int pageNo) {
        Log.d("Response: " ,""+ getUrl(pageNo));
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, getUrl(pageNo), null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Gson gson = new Gson();
                        ImageListResponse imageListResponse = gson.fromJson(response.toString(), ImageListResponse.class);
                        int size = mImageListResponse.size();
                        mImageListResponse.addAll(imageListResponse.getPhotos().getPhoto());
                        mImageAdapter.notifyItemRangeInserted(size, imageListResponse.getPhotos().getPhoto().size());
                        if (mListSavedPos > 0 && mPageNo == 0) {
                            mRvImageList.scrollToPosition(mListSavedPos);
                        }
                        mPageNo++;
                        Log.d("Response: " ,""+ response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest).setShouldCache(true);
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager ConnectMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (ConnectMgr == null)
            return false;
        NetworkInfo NetInfo = ConnectMgr.getActiveNetworkInfo();
        if (NetInfo == null)
            return false;

        return NetInfo.isConnected();
    }

    private String getUrl(int pageNo) {
        url = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=e3857e3f69de30d8b7fc0c29cf12854e&%20format=json&nojsoncallback=1&safe_search=1&text=android&page="+pageNo;
        return url;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("list_position", ((GridLayoutManager) mRvImageList.getLayoutManager()).findLastVisibleItemPosition());
        super.onSaveInstanceState(outState);
    }
}
