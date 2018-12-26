package com.example.user.gmailappclone.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.user.gmailappclone.Adapter.PrimaryRVAdapter;
import com.example.user.gmailappclone.Model.Email;
import com.example.user.gmailappclone.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.android.volley.VolleyLog.TAG;

public class PrimaryFragment extends Fragment implements View.OnClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DividerItemDecoration dividerItemDecoration;
    private FloatingActionButton newEmailFAB, scrollToTopFAB;
    private PrimaryRVAdapter adapter;
    private Handler handler = new Handler();
    private Context context;
    private ArrayList<Email> emailArrayList = new ArrayList<>();
    private static final String databaseUrl = "https://api.jsonbin.io/b/5c2399208c05c52ebaced1b0";
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.primary_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        initializeWidgets(view);

        requestQueue = Volley.newRequestQueue(context);
        parseJsonToAdapter(databaseUrl);

        adapter = new PrimaryRVAdapter(context, emailArrayList);
        recyclerView.setAdapter(adapter);

        addRecyclerViewScrollListener();
        setSwipeToRefresh();

    }

    private void initializeWidgets(View view) {
        recyclerView = view.findViewById(R.id.primary_fragment_recyclerview);
        swipeRefreshLayout = view.findViewById(R.id.primary_fragment_swiprerefreshlayout);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(context, R.color.colorPrimary),
                ContextCompat.getColor(context, R.color.colorTimestamp),
                ContextCompat.getColor(context, R.color.colorIconTintSelected));

        newEmailFAB = view.findViewById(R.id.primary_fragment_newEmail_fab);
        newEmailFAB.setOnClickListener(this);
        scrollToTopFAB = view.findViewById(R.id.primary_fragment_scrolltotop_fab);
        scrollToTopFAB.setOnClickListener(this);

        dividerItemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setNestedScrollingEnabled(false);

    }

    private void addRecyclerViewScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // If cannot scroll up anymore (top of the recyclerview) - FAB hides immediately
                    if (!recyclerView.canScrollVertically(-1) &&
                            scrollToTopFAB.getVisibility() == View.VISIBLE) {
                        scrollToTopFAB.setEnabled(false);
                        scrollToTopFAB.hide();
                    } else if (!recyclerView.canScrollVertically(1)) {
                        // If cannot scroll down anymore (bottom of the recyclerview) - FAB remains shown
                        scrollToTopFAB.setEnabled(true);
                        scrollToTopFAB.show();
                    } else {
                        // If not at bottom or top, FAB will disappear after 500ms
                        if (scrollToTopFAB.getVisibility() == View.VISIBLE)
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    scrollToTopFAB.setEnabled(false);
                                    scrollToTopFAB.hide();
                                }
                            }, 500);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // Detects if the recyclerview is scrolling both up or down
                if (dy > 0 && scrollToTopFAB.getVisibility() == View.GONE || dy < 0 &&
                        scrollToTopFAB.getVisibility() == View.GONE) {
                    scrollToTopFAB.show();
                    scrollToTopFAB.setEnabled(true);
                }
            }
        });
    }


    private void parseJsonToAdapter(String url) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                Log.d(TAG, "onResponse: " + jsonObject);
                                int emailId = jsonObject.getInt("emailId");
                                boolean isImportant = jsonObject.getBoolean("isImportant");
                                boolean isRead = jsonObject.getBoolean("isRead");
                                String imageUrl = jsonObject.getString("imageUrl");
                                String from = jsonObject.getString("from");
                                String subject = jsonObject.getString("subject");
                                String message = jsonObject.getString("message");
                                String timeStamp = jsonObject.getString("timestamp");

                                Email email = new Email(emailId, isRead, isImportant, imageUrl, from, subject,
                                        message, timeStamp);

                                emailArrayList.add(email);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        adapter.notifyDataSetChanged();
                        delaySwipeRefresh();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                delaySwipeRefresh();
            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    /**
     * Add functionality to the swipeRefreshLayout
     */
    private void setSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                emailArrayList.clear();
                adapter.notifyDataSetChanged();
                parseJsonToAdapter(databaseUrl);
            }
        });
    }

    /**
     * Delay the swipe refresh icon from disappearing
     */
    private void delaySwipeRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 500);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.primary_fragment_newEmail_fab:
                Toast.makeText(context, "New email", Toast.LENGTH_SHORT).show();
                break;
            case R.id.primary_fragment_scrolltotop_fab:
                recyclerView.smoothScrollToPosition(0);
                break;
        }
    }
}